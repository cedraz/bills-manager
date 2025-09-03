package src

import (
	"context"
	"fmt"
	"log"
	"os"

	"google.golang.org/api/option"
	"google.golang.org/api/sheets/v4"
)

func NewSheetsService(spreadsheetId string) (*sheets.Service, error) {
	ctx := context.Background()

	b, err := os.ReadFile("./credentials.json")
	if err != nil {
		log.Fatalf("Não foi possível ler o arquivo de credenciais: %v", err)
		return nil, err
	}

	srv, err := sheets.NewService(ctx, option.WithCredentialsJSON(b), option.WithScopes(sheets.SpreadsheetsScope))
	if err != nil {
		log.Fatalf("Não foi possível criar o cliente do Sheets: %v", err)
		return nil, err
	}

	CheckHeaders(srv, spreadsheetId, []interface{}{
		"ID",
		"Descrição",
		"Valor",
		"Categoria",
		"Método de Pagamento",
		"Data",
	})

	FormatColumns(srv, spreadsheetId, "Página1")

	return srv, nil
}

func AppendRow(srv *sheets.Service, spreadsheetId string, rowData []interface{}) error {
	rangeData := "Página1"

	var vr sheets.ValueRange
	vr.Values = append(vr.Values, rowData)

	_, err := srv.Spreadsheets.Values.Append(spreadsheetId, rangeData, &vr).ValueInputOption("USER_ENTERED").Do()
	if err != nil {
		log.Printf("Não foi possível adicionar a linha na planilha: %v", err)
		return err
	}

	log.Printf("Linha adicionada com sucesso na planilha %s! 🚀", spreadsheetId)
	return nil
}

func CheckHeaders(srv *sheets.Service, spreadsheetId string, headers []interface{}) error {
	log.Printf("Verificando cabeçalhos na planilha %s...", spreadsheetId)

	var rr sheets.ValueRange
	resp, err := srv.Spreadsheets.Values.Get(spreadsheetId, "Página1!A1:F1").Do()
	if err != nil {
		log.Fatalf("Não foi possível ler os dados da planilha: %v", err)
	}

	if len(resp.Values) == 0 {
		rr.Values = append(rr.Values, headers)
		_, err = srv.Spreadsheets.Values.Append(spreadsheetId, "Página1!A1", &rr).ValueInputOption("RAW").Do()
		if err != nil {
			log.Fatalf("Não foi possível adicionar os cabeçalhos na planilha: %v", err)
			return err
		}
		log.Printf("Cabeçalhos adicionados com sucesso na planilha %s! 🚀", spreadsheetId)
	} else {
		log.Printf("A planilha %s já contém dados. Nenhuma ação necessária.", spreadsheetId)
	}

	return nil
}

func FormatColumns(srv *sheets.Service, spreadsheetId string, sheetName string) error {
	spreadsheet, err := srv.Spreadsheets.Get(spreadsheetId).Do()
	if err != nil {
		return fmt.Errorf("unable to retrieve spreadsheet data: %v", err)
	}

	var sheetId int64
	var found bool
	for _, sheet := range spreadsheet.Sheets {
		if sheet.Properties.Title == sheetName {
			sheetId = sheet.Properties.SheetId
			found = true
			break
		}
	}

	if !found {
		return fmt.Errorf("sheet '%s' not found", sheetName)
	}

	batchUpdateRequest := &sheets.BatchUpdateSpreadsheetRequest{
		Requests: []*sheets.Request{
			{
				RepeatCell: &sheets.RepeatCellRequest{
					Range: &sheets.GridRange{
						SheetId:          sheetId,
						StartRowIndex:    1,
						StartColumnIndex: 2,
						EndColumnIndex:   3,
					},
					Cell: &sheets.CellData{
						UserEnteredFormat: &sheets.CellFormat{
							NumberFormat: &sheets.NumberFormat{
								Type:    "CURRENCY",
								Pattern: "R$ #,##0.00",
							},
						},
					},
					Fields: "userEnteredFormat.numberFormat",
				},
			},
			{
				RepeatCell: &sheets.RepeatCellRequest{
					Range: &sheets.GridRange{
						SheetId:          sheetId,
						StartRowIndex:    1,
						StartColumnIndex: 5,
						EndColumnIndex:   6,
					},
					Cell: &sheets.CellData{
						UserEnteredFormat: &sheets.CellFormat{
							NumberFormat: &sheets.NumberFormat{
								Type:    "DATE",
								Pattern: "dd/MM/yyyy",
							},
						},
					},
					Fields: "userEnteredFormat.numberFormat",
				},
			},
		},
	}

	_, err = srv.Spreadsheets.BatchUpdate(spreadsheetId, batchUpdateRequest).Do()
	if err != nil {

		return fmt.Errorf("unable to format column: %v", err)
	}

	log.Printf("Coluna %d na aba '%s' formatada como moeda.", 3, sheetName)
	return nil
}
