package handler.dynamo;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDB {
    public static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    private static final String TABLE_NAME = "BillsBotUsers";
    private static final String EXPENSES_TABLE_NAME = "BillsBotExpenses";

    public String getTableName() {
        return TABLE_NAME;
    }

    public String getExpensesTableName() {
        return EXPENSES_TABLE_NAME;
    }

    public DynamoDbClient getDynamoDbClient() {
        return dynamoDbClient;
    }
}
