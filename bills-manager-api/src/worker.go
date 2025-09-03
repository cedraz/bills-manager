package src

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/cedraz/bills-manager-api/src/dtos"
	"github.com/streadway/amqp"
)

func Worker() {
	rabbitMQURL := os.Getenv("RABBITMQ_URL")

	fmt.Printf("RABBITMQ_URL: %s", rabbitMQURL)

	conn, err := amqp.Dial(rabbitMQURL)
	if err != nil {
		log.Fatalf("failed to connect to RabbitMQ: %v", err)
	}
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		log.Fatalf("failed to open a channel: %v", err)
	}
	defer ch.Close()

	q, err := ch.QueueDeclare(
		"jobs",
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("failed to declare a queue: %v", err)
	}

	msgs, err := ch.Consume(
		q.Name,
		"",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("failed to register a consumer: %v", err)
	}

	sigchan := make(chan os.Signal, 1)
	signal.Notify(sigchan, syscall.SIGINT, syscall.SIGTERM)

	forever := make(chan bool)

	if err != nil {
		log.Fatalf("Não foi possível criar o cliente do Sheets: %v", err)
	}

	go func() {
		for d := range msgs {
			var dto dtos.AddExpenseDto
			err := json.Unmarshal(d.Body, &dto)
			if err != nil {
				log.Printf("Erro ao decodificar JSON: %s", err)
				continue
			}

			sheetsService, err := NewSheetsService(dto.SpreadsheetID)
			if err != nil {
				log.Fatalf("Falha ao inicializar o serviço do Google Sheets: %v", err)
			}

			log.Printf("Processando despesa: ID=%d, Descrição=%s, Valor=%.2f", dto.ID, dto.Description, float64(dto.Amount)/100)

			rowData := []interface{}{
				dto.ID,
				dto.Description,
				float64(dto.Amount) / 100,
				dto.Category,
				dto.PaymentMethod,
				dto.Date,
			}

			err = AppendRow(sheetsService, dto.SpreadsheetID, rowData)
			if err != nil {
				log.Printf("ERRO: Falha ao processar a despesa com ID %d. A linha não foi adicionada.", dto.ID)
			}
		}
	}()

	log.Printf(" [*] Waiting for messages. To exit press CTRL+C")
	<-sigchan

	log.Printf("interrupted, shutting down")
	forever <- true
}
