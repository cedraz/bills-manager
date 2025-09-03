package handler.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.telegram.Update;
import handler.telegram.TelegramBot;

public abstract class BaseHandler {
    protected final TelegramBot telegramBot;

    public BaseHandler(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public abstract void handle(Update update, Context context);
}
