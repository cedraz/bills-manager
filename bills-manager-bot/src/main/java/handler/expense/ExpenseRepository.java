package handler.expense;

import handler.dynamo.DynamoDB;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpenseRepository {
    private final DynamoDB dynamoDB;

    public ExpenseRepository(DynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    public void create(long userId, Expense expense) {
        Map<String, AttributeValue> item = expense.toAttributeValueMap();

        item.put("user_id", AttributeValue.builder().n(String.valueOf(userId)).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(this.dynamoDB.getExpensesTableName())
                .item(item)
                .build();

        dynamoDB.getDynamoDbClient().putItem(request);
    }

    public List<Expense> getExpenses(long userId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(dynamoDB.getExpensesTableName())
                .keyConditionExpression("user_id = :userIdVal")
                .expressionAttributeValues(Map.of(
                        ":userIdVal", AttributeValue.builder().n(String.valueOf(userId)).build()
                ))
                .build();

        List<Map<String, AttributeValue>> items = dynamoDB.getDynamoDbClient().query(request).items();

        return items.stream()
                .map(Expense::fromAttributeMap)
                .collect(Collectors.toList());
    }

    public Expense findById(long userId, int expenseId) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(this.dynamoDB.getExpensesTableName())
                .key(Map.of(
                        "user_id", AttributeValue.builder().n(String.valueOf(userId)).build(),
                        "id", AttributeValue.builder().n(String.valueOf(expenseId)).build()
                ))
                .build();

        Map<String, AttributeValue> item = dynamoDB.getDynamoDbClient().getItem(request).item();
        if (item == null || item.isEmpty()) {
            return null;
        }
        return Expense.fromAttributeMap(item);
    }

    public void update(long userId, int expenseId, Map<String, AttributeValue> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        Map<String, String> expressionAttributeNames = new HashMap<>();

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        StringBuilder updateExpression = new StringBuilder("SET ");

        int i = 0;
        for (Map.Entry<String, AttributeValue> entry : updates.entrySet()) {
            String attributeName = entry.getKey();
            String namePlaceholder = "#attr" + i;
            String valuePlaceholder = ":val" + i;

            expressionAttributeNames.put(namePlaceholder, attributeName);
            expressionAttributeValues.put(valuePlaceholder, entry.getValue());

            updateExpression.append(namePlaceholder).append(" = ").append(valuePlaceholder).append(", ");
            i++;
        }

        updateExpression.setLength(updateExpression.length() - 2);

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(this.dynamoDB.getExpensesTableName())
                .key(Map.of(
                        "user_id", AttributeValue.builder().n(String.valueOf(userId)).build(),
                        "id", AttributeValue.builder().n(String.valueOf(expenseId)).build()
                ))
                .updateExpression(updateExpression.toString())
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        dynamoDB.getDynamoDbClient().updateItem(request);
    }

    public void delete(long userId, int expenseId) {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(this.dynamoDB.getExpensesTableName())
                .key(Map.of(
                        "user_id", AttributeValue.builder().n(String.valueOf(userId)).build(),
                        "id", AttributeValue.builder().n(String.valueOf(expenseId)).build()
                ))
                .build();

        dynamoDB.getDynamoDbClient().deleteItem(request);
    }
}
