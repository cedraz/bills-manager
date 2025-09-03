package handler.user;

import handler.enums.ConversationState;
import handler.expense.Expense;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

public class User {
    public long chat_id;
    public String first_name;
    public String username;
    public ArrayList<Expense> expenses;
    public ConversationState conversationState;
    public int expenseCounter;
    public String spreadsheetId;
    public Expense inProgressExpense;

    public User(long chat_id, String first_name, String username) {
        this.chat_id = chat_id;
        this.first_name = first_name;
        this.username = username;
        this.conversationState = ConversationState.NONE;
        this.expenses = new ArrayList<>();
    }

    public User(long chat_id, String first_name, String username, String spreadsheetId) {
        this.chat_id = chat_id;
        this.first_name = first_name;
        this.username = username;
        this.conversationState = ConversationState.NONE;
        this.spreadsheetId = spreadsheetId;
        this.expenses = new ArrayList<>();
    }

    public ConversationState getConversationState() {
        return conversationState;
    }

    public void setConversationState(ConversationState conversationState) {
        this.conversationState = conversationState;
    }

    public Expense getInProgressExpense() {
        return inProgressExpense;
    }

    public void setInProgressExpense(Expense inProgressExpense) {
        this.inProgressExpense = inProgressExpense;
    }

    public long getChat_id() {
        return chat_id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getUsername() {
        return username;
    }

    public int getExpenseCounter() {
        return expenseCounter;
    }

    public void setExpenseCounter(int expenseCounter) {
        this.expenseCounter = expenseCounter;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }
}
