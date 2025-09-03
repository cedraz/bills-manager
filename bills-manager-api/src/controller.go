package src

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/cedraz/bills-manager-api/src/dtos"
	"github.com/streadway/amqp"
)

func AddExpenseController(w http.ResponseWriter, r *http.Request, ch *amqp.Channel) {
	var dto dtos.AddExpenseDto
	err := json.NewDecoder(r.Body).Decode(&dto)
	if err != nil {
		http.Error(w, "Invalid request payload", http.StatusBadRequest)
		return
	}

	fmt.Printf("Received DTO: %+v\n", dto)

	err = AddToQueue(dto, ch)
	if err != nil {
		http.Error(w, "Failed to enqueue message", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusAccepted)
	w.Write([]byte("Expense added to the queue"))
}
