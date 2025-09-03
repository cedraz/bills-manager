package handler.handlers.expense_handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.enums.Command;
import handler.enums.ConversationState;
import handler.enums.ParseMode;
import handler.expense.Expense;
import handler.expense.ExpenseRepository;
import handler.handlers.BaseHandler;
import handler.telegram.TelegramBot;
import handler.telegram.Update;
import handler.user.User;
import handler.user.UserRepository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

public class DeleteExpenseHandler extends BaseHandler {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private User currentUser;

    public DeleteExpenseHandler(TelegramBot telegramBot, ExpenseRepository expenseRepository, UserRepository userRepository) {
        super(telegramBot);
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public DeleteExpenseHandler(TelegramBot telegramBot, ExpenseRepository expenseRepository, UserRepository userRepository, User user) {
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

        this.currentUser.setConversationState(ConversationState.AWAITING_EXPENSE_ID_DELETE_EXPENSE);
        this.userRepository.updateUser(this.currentUser);

        String message = "Por favor digite o número do identificador da despesa que deseja deletar (ex: 3): \n";

        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData(Command.CANCEL_EXPENSE_DELETION.getCommandText());

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel) );
        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

        this.telegramBot.sendMessage(chatId, message, inlineKeyboard, context);
    }

    public void deleteExpense(Update update, Context context) {
        long chatId = update.message.chat.id;
        String text = update.message.text;

        if (this.currentUser == null) {
            this.currentUser = this.userRepository.findByChatId(chatId);
            if (this.currentUser == null) {
                this.telegramBot.sendMessage(chatId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
                return;
            }
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_EXPENSE_ID_DELETE_EXPENSE) {
            this.telegramBot.sendMessage(chatId, "Por favor, use o comando /deletar_despesa para iniciar o processo de deleção de despesa.", context);
            return;
        }

        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData(Command.CANCEL_EXPENSE_DELETION.getCommandText());

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel) );
        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

        int expenseId;
        try {
            expenseId = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            String message = "ID inválido. Por favor, digite um número válido para o ID da despesa ou clique em 'Cancelar'";
            this.telegramBot.sendMessage(chatId, message, inlineKeyboard, context);
            return;
        }

        Expense expense = this.expenseRepository.findById(chatId, expenseId);
        if (expense == null) {
            String message = "Despesa com ID " + expenseId + " não encontrada. Por favor, verifique o ID e tente novamente ou clique em 'Cancelar'.";
            this.telegramBot.sendMessage(chatId, message, inlineKeyboard, context);
            return;
        }

        this.expenseRepository.delete(chatId, expenseId);

        String confirmationMessage = String.format("Despesa deletada com sucesso:\n\n" +
                "<b>ID:</b> %d\n" +
                "<b>Valor:</b> R$ %s\n" +
                "<b>Descrição:</b> %s\n" +
                "<b>Método de Pagamento:</b> %s\n" +
                "<b>Categoria:</b> %s",
                expense.getId(),
                expense.getAmountAsString(),
                expense.getDescription(),
                expense.getPaymentMethodDescription(),
                expense.getCategory()
        );

        this.telegramBot.sendMessage(chatId, confirmationMessage, ParseMode.HTML, context);

        this.currentUser.setConversationState(ConversationState.NONE);
        this.userRepository.updateUser(this.currentUser);
    }

    public void cancelExpenseDeletion(Update update, Context context) {
        long chatId = update.message.chat.id;

        if (this.currentUser == null) {
            this.currentUser = this.userRepository.findByChatId(chatId);
            if (this.currentUser == null) {
                this.telegramBot.sendMessage(chatId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
                return;
            }
        }

        this.currentUser.setConversationState(ConversationState.NONE);
        this.userRepository.updateUser(this.currentUser);

        String message = "Deleção de despesa cancelada. Você pode usar outros comandos para gerenciar suas despesas.";
        this.telegramBot.sendMessage(chatId, message, context);
    }
}
