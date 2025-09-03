package handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import handler.dynamo.DynamoDB;
import handler.enums.Command;
import handler.enums.ConversationState;
import handler.expense.ExpenseRepository;
import handler.handlers.HelpHandler;
import handler.handlers.SpreadsheetHandler;
import handler.handlers.SummaryHandler;
import handler.handlers.expense_handlers.AddExpenseHandler;
import handler.handlers.StartHandler;
import handler.handlers.expense_handlers.GetExpensesHandler;
import handler.handlers.expense_handlers.UpdateExpenseHandler;
import handler.telegram.Chat;
import handler.telegram.Message;
import handler.telegram.TelegramBot;
import handler.telegram.Update;
import handler.user.User;
import handler.user.UserRepository;

import java.net.http.HttpClient;
import java.util.Map;

public class AwsLambdaFunctions implements RequestHandler<Map<String, Object>, String> {
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final TelegramBot telegramBot = new TelegramBot(httpClient);
    private static final DynamoDB dynamoDB = new DynamoDB();
    private static final UserRepository userRepository = new UserRepository(dynamoDB);

    private static final ExpenseRepository expenseRepository = new ExpenseRepository(dynamoDB);

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        var logger = context.getLogger();
        logger.log("Evento completo do API Gateway recebido.");

        String requestBody = (String) input.get("body");
        Update update = gson.fromJson(requestBody, Update.class);
        logger.log("Update recebido: " + gson.toJson(update));

        try {
            if (update.callback_query != null) {
                handleCallbackQuery(update, context);
            } else if (update.message != null && update.message.text != null) {

                long chatId = update.message.chat.id;
                String text = update.message.text;
                processTextMessage(chatId, text, update, context);
            } else {
                logger.log("Update ignorado (não é mensagem de texto ou callback).");
            }
        } catch (Exception e) {
            logger.log("ERRO CRÍTICO DURANTE A EXECUÇÃO DO HANDLER: " + e.getClass().getName() + " - " + e.getMessage());
            long chatId = (update.message != null) ? update.message.chat.id : (update.callback_query != null ? update.callback_query.message.chat.id : 0);
            if (chatId != 0) {
                telegramBot.sendMessage(chatId, "Desculpe, ocorreu um erro interno. A equipe de suporte já foi notificada.", context);
            }
        }

        return "Processado.";
    }

    private void processTextMessage(long chatId, String text, Update update, Context context) {
        var logger = context.getLogger();
        boolean isCommand = handleCommand(chatId, text, update, context);
        if (isCommand) {
            return;
        }

        User user = userRepository.findByChatId(chatId);

        if (user == null) {
            telegramBot.sendMessage(chatId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
            return;
        }

        if (user.getConversationState() == null || user.getConversationState() == ConversationState.NONE) {
            logger.log("Usuário " + chatId + " não está em um estado de conversa. Enviando mensagem de ajuda." + "ConversationState: " + user.getConversationState());
            new HelpHandler(telegramBot).handle(update, context);
            return;
        }

        context.getLogger().log("Usuário " + chatId + " em estado de conversa: " + user.getConversationState());
        AddExpenseHandler addExpenseHandler = new AddExpenseHandler(telegramBot, expenseRepository, userRepository, user);
        UpdateExpenseHandler updateExpenseHandler = new UpdateExpenseHandler(telegramBot, expenseRepository, userRepository, user);

        switch (user.getConversationState()) {
            case AWAITING_AMOUNT:
                addExpenseHandler.addAmount(update, context);
                break;
            case AWAITING_DESCRIPTION:
                addExpenseHandler.addDescription(update, context);
                break;
            case AWAITING_PAYMENT_METHOD:
                addExpenseHandler.addPaymentMethod(update, context);
                break;
            case AWAITING_CATEGORY:
                addExpenseHandler.addCategory(update, context);
                break;
            case AWAITING_EXPENSE_ID_UPDATE_EXPENSE:
                updateExpenseHandler.handleFindExpense(update, context);
                break;
            case AWAITING_AMOUNT_UPDATE_EXPENSE:
                updateExpenseHandler.handleUpdateExpenseAmount(update, context);
                break;
            case AWAITING_DESCRIPTION_UPDATE_EXPENSE:
                updateExpenseHandler.handleUpdateExpenseDescription(update, context);
                break;
            case AWAITING_PAYMENT_METHOD_UPDATE_EXPENSE:
                updateExpenseHandler.handleUpdateExpensePaymentMethod(update, context);
                break;
            case AWAITING_CATEGORY_UPDATE_EXPENSE:
                updateExpenseHandler.handleUpdateExpenseCategory(update, context);
                break;
            case AWAITING_SPREADSHEET_URL:
                SpreadsheetHandler spreadsheetHandler = new SpreadsheetHandler(telegramBot, userRepository);
                spreadsheetHandler.saveSpreadsheetId(update, context);
                break;
            default:
                telegramBot.sendMessage(chatId, "Estado de conversa desconhecido. Por favor, use /ajuda.", context);
                user.setConversationState(ConversationState.NONE);
                userRepository.updateUser(user);
                break;
        }
    }

    private void handleCallbackQuery(Update update, Context context) {
        String callbackQueryId = update.callback_query.id;
        String callbackData = update.callback_query.data;
        long chatId = update.callback_query.message.chat.id;

        telegramBot.answerCallbackQuery(callbackQueryId, context);

        update.message = new Message();
        update.message.chat = new Chat();
        update.message.chat.id = chatId;
        update.message.text = callbackData;

        processTextMessage(chatId, callbackData, update, context);
    }

    private boolean handleCommand(long chatId, String messageText, Update update, Context context) {
        Command command = Command.fromString(messageText);
        if (command != Command.UNKNOWN) {
            context.getLogger().log("Comando reconhecido: " + command);
        } else {
            context.getLogger().log("Comando desconhecido: " + messageText);
            return false;
        }

        switch (command) {
            case START:
                new StartHandler(telegramBot, userRepository).handle(update, context);
                return true;
            case ADD_EXPENSE:
                new AddExpenseHandler(telegramBot, expenseRepository, userRepository).handle(update, context);
                return true;
            case HELP:
                new HelpHandler(telegramBot).handle(update, context);
                return true;
            case VIEW_EXPENSES:
                new GetExpensesHandler(telegramBot, expenseRepository).handle(update, context);
                return true;
            case CANCEL_EXPENSE_PROCESS:
                new AddExpenseHandler(telegramBot, expenseRepository, userRepository).cancelExpenseProcess(update, context);
                return true;
            case SKIP_CATEGORY:
                return false;
            case SKIP_PAYMENT_METHOD:
                return false;
            case UPDATE_EXPENSE:
                new UpdateExpenseHandler(telegramBot, expenseRepository, userRepository).handle(update, context);
                return true;
            case CANCEL_EXPENSE_UPDATE:
                new UpdateExpenseHandler(telegramBot, expenseRepository, userRepository).handleCancel(update, context);
                return true;
            case SKIP_AMOUNT_UPDATE:
                return false;
            case SKIP_DESCRIPTION_UPDATE:
                return false;
            case SKIP_PAYMENT_METHOD_UPDATE:
                return false;
            case SKIP_CATEGORY_UPDATE:
                return false;
            case DELETE_EXPENSE:
                return false;
            case ADD_SPREADSHEET:
                SpreadsheetHandler spreadsheetHandler = new SpreadsheetHandler(telegramBot, userRepository);
                spreadsheetHandler.handle(update, context);
                return true;
            case SUMMARY:
                new SummaryHandler(telegramBot, userRepository, expenseRepository).handle(update, context);
                return true;
            default:
                context.getLogger().log("Texto '" + messageText + "' não é um comando reconhecido.");
                return false;
        }
    }
}