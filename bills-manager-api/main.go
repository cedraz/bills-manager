package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/cedraz/bills-manager-api/src"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/joho/godotenv"
	"github.com/streadway/amqp"
)

func main() {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using environment variables from system")
	}

	r := chi.NewRouter()
	r.Use(middleware.Logger)

	conn, ch, err := rabbitMQConn()

	if err != nil {
		log.Fatalf("Error connecting to RabbitMQ: %s", err)
	}
	defer conn.Close()
	defer ch.Close()

	r.Post("/add", func(w http.ResponseWriter, r *http.Request) {
		src.AddExpenseController(w, r, ch)
	})

	port := os.Getenv("PORT")

	if port == "" {
		port = "3005"
	}

	go src.Worker()

	fmt.Printf("\n API running on port: %s\n", port)
	http.ListenAndServe(fmt.Sprintf(":%s", port), r)
}

func rabbitMQConn() (*amqp.Connection, *amqp.Channel, error) {
	rabbitMQURL := os.Getenv("RABBITMQ_URL")

	fmt.Printf("RABBITMQ_URL: %s %s", rabbitMQURL, "\n")

	conn, err := amqp.Dial(rabbitMQURL)
	if err != nil {
		fmt.Println("\n Failed to connect to RabbitMQ: ", err)
		return nil, nil, err
	}

	fmt.Println("Connected to RabbitMQ: " + os.Getenv("RABBITMQ_URL"))

	ch, err := conn.Channel()
	if err != nil {
		fmt.Println("Failed to open a channel: ", err)
		conn.Close()
		return nil, nil, err
	}

	_, err = ch.QueueDeclare(
		"jobs",
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		fmt.Println("Failed to declare a queue: ", err)
		ch.Close()
		conn.Close()
		return nil, nil, err
	}

	log.Printf("Queue 'jobs' declared")
	return conn, ch, nil
}
