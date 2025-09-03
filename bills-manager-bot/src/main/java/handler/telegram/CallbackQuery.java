package handler.telegram;

public class CallbackQuery {
    public String id;
    public From from;
    public Message message;
    public String chat_instance;
    public String data;

    public String getId() { return id; }
    public From getFrom() { return from; }
    public Message getMessage() { return message; }
    public String getChatInstance() { return chat_instance; }
    public String getData() { return data; }
}