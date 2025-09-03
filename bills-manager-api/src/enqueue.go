package src

import (
	"encoding/json"
	"log"

	"github.com/cedraz/bills-manager-api/src/dtos"
	"github.com/streadway/amqp"
)

func AddToQueue(body dtos.AddExpenseDto, ch *amqp.Channel) error {
	jsonBody, err := json.Marshal(body)
	if err != nil {
		return err
	}

	err = ch.Publish(
		"",
		"jobs",
		false,
		false,
		amqp.Publishing{
			ContentType: "application/json",
			Body:        jsonBody,
		},
	)
	if err != nil {
		log.Fatalf("failed to publish a message: %v", err)
		return err
	}

	log.Printf("message sent: %v", body)
	return nil
}
