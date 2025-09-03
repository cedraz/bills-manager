package handler.expense;

import handler.enums.PaymentMethod;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Expense {
    public int id;
    public int amount;
    public String description;
    public LocalDate date;
    public String category;
    public PaymentMethod paymentMethod;

    public Expense(int amount, String description, LocalDate date) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.category = "Outros";
        this.paymentMethod = PaymentMethod.OTHER;
    }

    public Expense(int amount, String description, LocalDate date, String category) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.category = category;
        this.paymentMethod = PaymentMethod.OTHER;
    }

    public Expense(int amount, String description, LocalDate date, String category, PaymentMethod paymentMethod) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
    }

    public static Expense fromAttributeMap(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }

        Expense expense = new Expense(
                Integer.parseInt(item.get("amount").n()),
                item.get("description").s(),
                LocalDate.parse(item.get("date").s()),
                item.get("category").s()
        );

        if (item.containsKey("paymentMethod")) {
            expense.setPaymentMethod(PaymentMethod.valueOf(item.get("paymentMethod").s()));
        }
        if (item.containsKey("id")) {
            expense.setId(Integer.parseInt(item.get("id").n()));
        }

        return expense;
    }

    public Map<String, AttributeValue> toAttributeValueMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().n(String.valueOf(this.id)).build());
        item.put("amount", AttributeValue.builder().n(String.valueOf(this.amount)).build());
        item.put("description", AttributeValue.builder().s(this.description).build());
        item.put("date", AttributeValue.builder().s(this.date.toString()).build());
        item.put("category", AttributeValue.builder().s(this.category).build());

        if (this.paymentMethod != null) {
            item.put("paymentMethod", AttributeValue.builder().s(this.paymentMethod.name()).build());
        }
        return item;
    }

    public Map<String, AttributeValue> getUpdatableAttributes() {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("amount", AttributeValue.builder().n(String.valueOf(this.amount)).build());
        item.put("description", AttributeValue.builder().s(this.description).build());
        item.put("date", AttributeValue.builder().s(this.date.toString()).build());
        item.put("category", AttributeValue.builder().s(this.category).build());

        if (this.paymentMethod != null) {
            item.put("paymentMethod", AttributeValue.builder().s(this.paymentMethod.name()).build());
        }
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public String getAmountAsString() {
        return String.format("%,.2f", amount / 100.0).replace(".", ",");
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() { return date; }

    public String getCategory() {
        return category;
    }

    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod.getDescription(): "N/A";
    }

    public String getPaymentMethodDescription() {
        return paymentMethod != null ? paymentMethod.getDescription() : "OTHER";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    @Override
    public String toString() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return String.format(
                " <b>ID:</b> %d\n" +
                "🗓️ <b>Data:</b> %s\n" +
                "📝 <b>Descrição:</b> %s\n" +
                "📦 <b>Categoria:</b> %s\n" +
                "💰 <b>Valor:</b> R$ %s\n" +
                "💳 <b>Método de Pagamento:</b> %s",
                this.id,
                this.date.format(dateFormatter),
                this.description,
                this.category,
                this.getAmountAsString(),
                this.getPaymentMethodDescription()
        );
    }
}
