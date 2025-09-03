package handler.telegram;

public class From {
    public long id;
    public boolean is_bot;
    public String first_name;
    public String username;
    public String language_code;

    public long getId() { return id; }
    public boolean getIsBot() { return is_bot; }
    public String getFirstName() { return first_name; }
    public String getUsername() { return username; }
    public String getLanguageCode() { return language_code; }
}