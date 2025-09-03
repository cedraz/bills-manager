package handler.expense;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import handler.utils.LocalDateAdapter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

public class SpreadsheetAPI {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void request(Expense expense, String spreadsheetId, Context context) {
        String url = System.getenv("SPREADSHEET_API_URL");

        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("SPREADSHEET_API_URL is not set in environment variables.");
        }
        String finalUrl = url.endsWith("/") ? url + "add" : url + "/add";

        var logger = context.getLogger();

        SpreadsheetPayload payload = new SpreadsheetPayload(expense, spreadsheetId);
        String jsonPayload = gson.toJson(payload);

        try {
            logger.log("Fazendo requisição para API de planilhas com payload: " + jsonPayload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.log("Status da resposta: " + response.statusCode());
            logger.log("Corpo da resposta: " + response.body());
        } catch (IOException | InterruptedException e) {
            logger.log("ERRO ao fazer a requisição: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static class SpreadsheetPayload {
        @SerializedName("spreadsheet_id")
        private final String spreadsheetId;

        @SerializedName("id")
        private final int id;

        @SerializedName("amount")
        private final int amount;

        @SerializedName("description")
        private final String description;

        @SerializedName("date")
        private final LocalDate date;

        @SerializedName("category")
        private final String category;

        @SerializedName("payment_method")
        private final String paymentMethod;

        public SpreadsheetPayload(Expense expense, String spreadsheetId) {
            this.spreadsheetId = spreadsheetId;
            this.id = expense.getId();
            this.amount = expense.getAmount();
            this.description = expense.getDescription();
            this.date = expense.getDate();
            this.category = expense.getCategory();
            this.paymentMethod = expense.getPaymentMethod();
        }
    }
}
