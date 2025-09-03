package handler.handlers.expense_handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.enums.ConversationState;
import handler.enums.ParseMode;
import handler.enums.PaymentMethod;
import handler.expense.SpreadsheetAPI;
import handler.handlers.BaseHandler;
import handler.telegram.Update;
import handler.expense.Expense;
import handler.expense.ExpenseRepository;
import handler.telegram.TelegramBot;
import handler.user.User;
import handler.user.UserRepository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddExpenseHandler extends BaseHandler {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private User currentUser;

    public AddExpenseHandler(TelegramBot telegramBot, ExpenseRepository expenseRepository, UserRepository userRepository) {
        super(telegramBot);
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public AddExpenseHandler(TelegramBot telegramBot, ExpenseRepository expenseRepository, UserRepository userRepository, User user) {
        super(telegramBot);
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.currentUser = user;
    }

    @Override
    public void handle(Update update, Context context) {
        long chatId = update.message.chat.id;

        if (this.currentUser == null) {
            this.currentUser = this.userRepository.findByChatId(chatId);
            if (this.currentUser == null) {
                this.telegramBot.sendMessage(chatId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
                return;
            }
        }

        this.currentUser.setConversationState(ConversationState.AWAITING_AMOUNT);
        this.userRepository.updateUser(this.currentUser);

        String message = "Iniciando o processo de adição de despesa. \n\n Envie o <b>valor</b> da despesa (ex: 12,34 ou 12.34).";

        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData("/cancelExpenseProcess");
        List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel) );
        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

        this.telegramBot.sendMessage(chatId, message, inlineKeyboard, context);
    }

    public void addAmount(Update update, Context context) {
        long chatId = update.message.chat.id;
        try {
            String message = update.message.text;
            double amountValue = Double.parseDouble(message.trim().replace(",", "."));
            int amountInCents = (int) Math.round(amountValue * 100);

            this.currentUser.setInProgressExpense(new Expense(amountInCents, "", LocalDate.now()));
            this.currentUser.setConversationState(ConversationState.AWAITING_DESCRIPTION);
            this.userRepository.updateUser(this.currentUser);

            String response = "Valor definido como R$ " + String.format("%.2f", amountValue) + ". \n\n Agora, envie a <b>descrição</b> da despesa.";

            InlineKeyboardButton btnCancel = new InlineKeyboardButton();
            btnCancel.setText("Cancelar");
            btnCancel.setCallbackData("/cancelExpenseProcess");
            List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel) );
            InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

            this.telegramBot.sendMessage(chatId, response, inlineKeyboard, ParseMode.HTML, context);
        } catch (NumberFormatException e) {
            this.telegramBot.sendMessage(chatId, "Valor inválido. Por favor, envie apenas o número (ex: 35.50).", context);
        }
    }

    public void addDescription(Update update, Context context) {
        long chatId = update.message.chat.id;
        String description = update.message.text.trim();

        if (description.isEmpty()) {
            this.telegramBot.sendMessage(chatId, "<b>A descrição não pode estar vazia</b>. Por favor, tente novamente.", ParseMode.HTML, context);
            return;
        }

        Expense inProgressExpense = this.currentUser.getInProgressExpense();

        try {
            if (inProgressExpense == null) {
                this.telegramBot.sendMessage(chatId, "Ocorreu um erro ao buscar a despesa em andamento. Por favor, inicie novamente com /adicionarDespesa.", context);
                this.currentUser.setConversationState(ConversationState.NONE);
                this.userRepository.updateUser(this.currentUser);
                return;
            }

            inProgressExpense.setDescription(description);
            this.currentUser.setConversationState(ConversationState.AWAITING_PAYMENT_METHOD);
            this.userRepository.updateUser(this.currentUser);

            String response = "✅ Descrição adicionada. \n\n Agora, escolha o <b>método de pagamento</b> ou clique em 'Pular' para deixar como 'OUTROS'.";

            InlineKeyboardButton btnSkip = new InlineKeyboardButton();
            btnSkip.setText("Pular");
            btnSkip.setCallbackData("/skipPaymentMethod");

            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(PaymentMethod.getButtonLists());

            keyboard.add(Arrays.asList(btnSkip));

            InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

            this.telegramBot.sendMessage(chatId, response, inlineKeyboard, ParseMode.HTML, context);
        } catch (Exception e) {
            this.telegramBot.sendMessage(chatId, "Ocorreu um erro interno. Por favor, tente novamente mais tarde.", context);
            throw new RuntimeException(e);
        }
    }

    public void addPaymentMethod(Update update, Context context) {
        long chatId = update.message.chat.id;
        String message = update.message.text.trim();

        PaymentMethod paymentMethod = PaymentMethod.fromString(message);

        if (paymentMethod == null) {
            String response = "Descrição adicionada. Agora, escolha a forma de pagamento ou clique em 'Pular' para deixar como 'OUTROS'.";

            InlineKeyboardButton btnSkip = new InlineKeyboardButton();
            btnSkip.setText("Pular");
            btnSkip.setCallbackData("/skipPaymentMethod");

            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(PaymentMethod.getButtonLists());

            keyboard.add(Arrays.asList(btnSkip));

            InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

            this.telegramBot.sendMessage(chatId, response, inlineKeyboard, context);
            return;
        }

        this.currentUser.getInProgressExpense().setPaymentMethod(paymentMethod);
        this.currentUser.setConversationState(ConversationState.AWAITING_CATEGORY);
        this.userRepository.updateUser(this.currentUser);

        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData("/cancelExpenseProcess");

        InlineKeyboardButton btnSkip = new InlineKeyboardButton();
        btnSkip.setText("Pular");
        btnSkip.setCallbackData("/skipCategory");

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
                Arrays.asList(btnCancel),
                Arrays.asList(btnSkip)
        );
        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

        String response = "✅ Método de pagamento '" + paymentMethod.getDescription() + "' adicionado à despesa. \n\n" +
                "Por fim, envie a <b>categoria</b> da despesa ou clique em 'Pular' para categorizar como 'Outros'.";
        this.telegramBot.sendMessage(chatId, response, inlineKeyboard, ParseMode.HTML, context);
    }

    public void addCategory(Update update, Context context) {
        var logger = context.getLogger();
        long chatId = update.message.chat.id;
        String message = update.message.text.trim();

        String category = message;

        if (message.startsWith("/skipCategory")) {
            category = "Outros";
            logger.log("Usuário optou por pular a categoria. Definindo como 'Outros'.");
        }

        try {
            Expense finalExpense = this.currentUser.getInProgressExpense();
            finalExpense.setCategory(category);

            int newExpenseId = this.userRepository.getNextExpenseId(this.currentUser);
            finalExpense.setId(newExpenseId);

            this.expenseRepository.create(chatId, finalExpense);

            this.currentUser.setInProgressExpense(null);
            this.currentUser.setConversationState(ConversationState.NONE);
            this.userRepository.updateUser(this.currentUser);

            String response = "✅ Despesa adicionada com sucesso na categoria: " + category + "." + "\n\n" + finalExpense.toString();
            this.telegramBot.sendMessage(chatId, response, ParseMode.HTML, context);

            logger.log("User spreadsheetId: " + this.currentUser.getSpreadsheetId());

            if (this.currentUser.getSpreadsheetId() != null && !this.currentUser.getSpreadsheetId().isEmpty()) {
                logger.log("Enviando despesa para a planilha do usuário.");
                SpreadsheetAPI spreadsheetAPI = new SpreadsheetAPI();
                spreadsheetAPI.request(finalExpense, this.currentUser.getSpreadsheetId(), context);
                logger.log("Despesa enviada para a planilha com sucesso.");
                this.telegramBot.sendMessage(chatId, "Despesa registrada na sua planilha do Google Sheets.", context);
            }
        } catch (Exception e) {
            logger.log(e.getMessage());
            this.telegramBot.sendMessage(chatId, "Ocorreu um erro interno. Por favor, tente novamente mais tarde.", context);
            throw new RuntimeException(e);
        }
    }

    public void cancelExpenseProcess(Update update, Context context) {
        long chatId = update.message.chat.id;

        if (this.currentUser == null) {
            this.currentUser = this.userRepository.findByChatId(chatId);
            if (this.currentUser == null) {
                this.telegramBot.sendMessage(chatId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
                return;
            }
        }

        this.currentUser.setInProgressExpense(null);
        this.currentUser.setConversationState(ConversationState.NONE);
        this.userRepository.updateUser(this.currentUser);

        String response = "Processo de adição de despesa cancelado.";
        this.telegramBot.sendMessage(chatId, response, context);
    }
}
