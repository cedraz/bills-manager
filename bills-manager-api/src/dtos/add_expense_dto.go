package dtos

type AddExpenseDto struct {
	SpreadsheetID string `json:"spreadsheet_id" binding:"required"`
	ID            int    `json:"id" binding:"required"`
	Amount        int    `json:"amount" binding:"required"`
	Description   string `json:"description" binding:"required"`
	Date          string `json:"date" binding:"required"`
	Category      string `json:"category" binding:"required"`
	PaymentMethod string `json:"payment_method" binding:"required"`
}
