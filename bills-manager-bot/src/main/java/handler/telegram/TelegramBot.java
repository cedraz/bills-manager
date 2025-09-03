package handler.telegram;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.Gson;
import handler.enums.ParseMode;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class TelegramBot {
    private final HttpClient httpClient;
    private static final Gson gson = new Gson();
    private final String botToken;

    public TelegramBot(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.botToken = System.getenv("TELEGRAM_BOT_TOKEN");
    }

    public void sendMessage(long chatId, String text, Context context) {
        sendMessage(chatId, text, (Object) null, null, context);
    }

    public void sendMessage(long chatId, String text, ParseMode parseMode, Context context) {
        sendMessage(chatId, text, (Object) null, parseMode, context);
    }

    public void sendMessage(long chatId, String text, ReplyKeyboardMarkup keyboard, Context context) {
        sendMessage(chatId, text, (Object) keyboard, null, context);
    }

    public void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard, Context context) {
        sendMessage(chatId, text, (Object) keyboard, null, context);
    }

    public void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard, ParseMode parseMode, Context context) {
        sendMessage(chatId, text, (Object) keyboard, parseMode, context);
    }

    private void sendMessage(long chatId, String text, Object keyboard, ParseMode parseMode, Context context) {
        var logger = context.getLogger();
        logger.log("Enviando mensagem para chatId " + chatId + ": " + text);

        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        if (parseMode != null) {
            payload.put("parse_mode", parseMode.name());
        }

        if (keyboard != null) {
            if (keyboard instanceof ReplyKeyboardMarkup) {
                Map<String, Object> replyMarkup = new HashMap<>();
                List<List<Map<String, String>>> keyboardRows = new ArrayList<>();

                for (KeyboardRow row : ((ReplyKeyboardMarkup) keyboard).getKeyboard()) {
                    List<Map<String, String>> buttonRow = new ArrayList<>();
                    for (KeyboardButton button : row) {
                        Map<String, String> buttonMap = new HashMap<>();
                        buttonMap.put("text", button.getText());
                        buttonRow.add(buttonMap);
                    }
                    keyboardRows.add(buttonRow);
                }

                replyMarkup.put("keyboard", keyboardRows);
                replyMarkup.put("resize_keyboard", true);
                payload.put("reply_markup", replyMarkup);
            } else if (keyboard instanceof InlineKeyboardMarkup) {
                Map<String, Object> replyMarkup = new HashMap<>();
                List<List<Map<String, String>>> inlineKeyboard = new ArrayList<>();

                for (List<InlineKeyboardButton> row : ((InlineKeyboardMarkup) keyboard).getKeyboard()) {
                    List<Map<String, String>> inlineRow = new ArrayList<>();
                    for (InlineKeyboardButton button : row) {
                        Map<String, String> buttonMap = new HashMap<>();
                        buttonMap.put("text", button.getText());
                        buttonMap.put("callback_data", button.getCallbackData());
                        inlineRow.add(buttonMap);
                    }
                    inlineKeyboard.add(inlineRow);
                }

                replyMarkup.put("inline_keyboard", inlineKeyboard);
                payload.put("reply_markup", replyMarkup);
            }
        }

        logger.log("Payload JSON: " + gson.toJson(payload));

        String jsonPayload = gson.toJson(payload);
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.log("Resposta da API do Telegram: Status " + response.statusCode() + ", Body: " + response.body());
        } catch (Exception e) {
            logger.log("ERRO ao enviar mensagem para o Telegram: " + e.getMessage());
        }
    }

    public void answerCallbackQuery(String callbackQueryId, Context context) {
        var logger = context.getLogger();
        logger.log("Respondendo ao callback query: " + callbackQueryId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("callback_query_id", callbackQueryId);

        String jsonPayload = gson.toJson(payload);
        String url = "https://api.telegram.org/bot" + botToken + "/answerCallbackQuery";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.log("Resposta da API do Telegram (answerCallbackQuery): Status " + response.statusCode() + ", Body: " + response.body());
        } catch (Exception e) {
            logger.log("ERRO ao responder callback query: " + e.getMessage());
        }
    }

    public ReplyKeyboardMarkup createReplyKeyboard(List<List<String>> options, boolean resizeKeyboard) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(resizeKeyboard);
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (List<String> rowOptions : options) {
            KeyboardRow row = new KeyboardRow();
            for (String option : rowOptions) {
                KeyboardButton button = new KeyboardButton(option);
                row.add(button);
            }
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup createSingleRowKeyboard(List<String> options, boolean resizeKeyboard) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(resizeKeyboard);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (String option : options) {
            row.add(option);
        }
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createInlineKeyboard(List<List<InlineKeyboardButton>> buttons) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttons);
        return keyboardMarkup;
    }
}