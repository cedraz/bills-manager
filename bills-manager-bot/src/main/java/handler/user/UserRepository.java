package handler.user;

import handler.dynamo.DynamoDB;
import handler.enums.ConversationState;
import handler.enums.PaymentMethod;
import handler.expense.Expense;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final DynamoDB dynamoDB;

    public UserRepository(DynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    public User findByChatId(long chat_id) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(dynamoDB.getTableName())
                .key(Map.of("chat_id", AttributeValue.builder().n(String.valueOf(chat_id)).build()))
                .build();

        Map<String, AttributeValue> item = dynamoDB.getDynamoDbClient().getItem(request).item();

        if (item == null || item.isEmpty()) return null;

        User user = new User(
                chat_id,
                item.get("first_name").s(),
                item.containsKey("username") ? item.get("username").s() : null
        );

        if (item.containsKey("expenseCounter")) {
            user.setExpenseCounter(Integer.parseInt(item.get("expenseCounter").n()));
        }

        if (item.containsKey("conversationState")) {
            user.setConversationState(ConversationState.valueOf(item.get("conversationState").s()));
        }

        if (item.containsKey("inProgressExpense") && item.get("inProgressExpense").hasM()) {
            Map<String, AttributeValue> expenseMap = item.get("inProgressExpense").m();
            user.setInProgressExpense(Expense.fromAttributeMap(expenseMap));
        }

        if (item.containsKey("spreadsheetId")) {
            user.setSpreadsheetId(item.get("spreadsheetId").s());
        }

        return user;
    }

    public String saveUser(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("chat_id", AttributeValue.builder().n(String.valueOf(user.getChat_id())).build());
        item.put("first_name", AttributeValue.builder().s(user.getFirst_name()).build());
        if (user.getUsername() != null) {
            item.put("username", AttributeValue.builder().s(user.getUsername()).build());
        }

        PutItemRequest request = PutItemRequest.builder().tableName(this.dynamoDB.getTableName()).item(item).build();

        try {
            dynamoDB.getDynamoDbClient().putItem(request);
            return "User saved successfully";
        } catch (DynamoDbException e) {
            System.out.println("ERROR saving user to DynamoDB: " + e.getMessage());
            throw new RuntimeException("Failed to add user", e);
        }
    }

    public void updateUser(User user) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("chat_id", AttributeValue.builder().n(String.valueOf(user.getChat_id())).build());
        item.put("first_name", AttributeValue.builder().s(user.getFirst_name()).build());

        if (user.getUsername() != null) {
            item.put("username", AttributeValue.builder().s(user.getUsername()).build());
        }
        if (user.getConversationState() != null) {
            item.put("conversationState", AttributeValue.builder().s(user.getConversationState().name()).build());
        }
        if (user.getExpenseCounter() > 0) {
            item.put("expenseCounter", AttributeValue.builder().n(String.valueOf(user.getExpenseCounter())).build());
        }

        if (user.getSpreadsheetId() != null && !user.getSpreadsheetId().isEmpty()) {
            item.put("spreadsheetId", AttributeValue.builder().s(user.getSpreadsheetId()).build());
        }

        if (user.getInProgressExpense() != null) {
            item.put("inProgressExpense", AttributeValue.builder().m(user.getInProgressExpense().toAttributeValueMap()).build());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(dynamoDB.getTableName())
                .item(item)
                .build();

        try {
            dynamoDB.getDynamoDbClient().putItem(request);
        } catch (DynamoDbException e) {
            System.err.println("ERROR updating user with PutItem: " + e.getMessage());
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public int getNextExpenseId(User user) {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(this.dynamoDB.getTableName())
                .key(Map.of("chat_id", AttributeValue.builder().n(String.valueOf(user.getChat_id())).build()))
                .updateExpression("SET expenseCounter = if_not_exists(expenseCounter, :start) + :inc")
                .expressionAttributeValues(Map.of(
                        ":inc", AttributeValue.builder().n("1").build(),
                        ":start", AttributeValue.builder().n("0").build()
                ))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build();

        try {
            UpdateItemResponse response = dynamoDB.getDynamoDbClient().updateItem(request);
            String newIdString = response.attributes().get("expenseCounter").n();
            int newId = new BigDecimal(newIdString).intValue();

            user.setExpenseCounter(newId);

            return newId;
        } catch (DynamoDbException e) {
            System.err.println("ERROR updating expense counter: " + e.getMessage());
            throw new RuntimeException("Could not generate new expense ID for user " + user.getChat_id(), e);
        }
    }
}
