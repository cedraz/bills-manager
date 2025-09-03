package handler.handlers.expense_handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.enums.Command;
import handler.enums.ConversationState;
import handler.enums.ParseMode;
import handler.enums.PaymentMethod;
import handler.expense.Expense;
import handler.expense.ExpenseRepository;
import handler.handlers.BaseHandler;
import handler.telegram.TelegramBot;
import handler.telegram.Update;
import handler.user.User;
import handler.user.UserRepository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UpdateExpenseHandler extends BaseHandler {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private User currentUser;

    public UpdateExpenseHandler(TelegramBot telegramBot, ExpenseRepository expenseRepository, UserRepository userRepository) {
        super(telegramBot);
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public UpdateExpenseHandler(TelegramBot telegramBot, ExpenseRepository expenseRepository, UserRepository userRepository, User user) {
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

        this.currentUser.setConversationState(ConversationState.AWAITING_EXPENSE_ID_UPDATE_EXPENSE);
        this.userRepository.updateUser(this.currentUser);

        String message = "Por favor digite o número do identificador da despesa que deseja atualizar (ex: 3): \n";

        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData("/cancelExpenseUpdate");
        List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel) );
        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

        this.telegramBot.sendMessage(chatId, message, inlineKeyboard, context);
    }

    public void handleFindExpense(Update update, Context context) {
        long userId = update.message.chat.id;
        String text = update.message.text;

        if (text.startsWith(Command.CANCEL_EXPENSE_UPDATE.getCommandText())) {
            handleCancel(update, context);
            return;
        }

        if (this.currentUser == null) {
            this.telegramBot.sendMessage(userId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
            return;
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_EXPENSE_ID_UPDATE_EXPENSE) {
            this.telegramBot.sendMessage(userId, "Você não está em um processo de atualização de despesa. Use /ajuda para ver os comandos disponíveis.", context);
            return;
        }

        int expenseId;
        try {
            expenseId = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            InlineKeyboardButton btnCancel = new InlineKeyboardButton();
            btnCancel.setText("Cancelar");
            btnCancel.setCallbackData("/cancelExpenseUpdate");
            List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel) );
            InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);
            String message = "Por favor, insira um número de identificador válido ou clique em 'Cancelar'.";
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }

        Expense expense = this.expenseRepository.findById(userId, expenseId);

        if (expense == null) {
            this.telegramBot.sendMessage(userId, "Despesa com o ID " + expenseId + " não encontrada. Por favor, tente novamente.", context);
            return;
        }

        this.currentUser.setInProgressExpense(expense);
        this.currentUser.setConversationState(ConversationState.AWAITING_AMOUNT_UPDATE_EXPENSE);
        this.userRepository.updateUser(this.currentUser);

        InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_AMOUNT_UPDATE);

        String message = "Despesa selecionada: \n" +
                expense.toString() + "\n" +
                "Por favor, insira o novo valor da despesa:\n";
        this.telegramBot.sendMessage(userId, message, inlineKeyboard, ParseMode.HTML, context);
    }

    public void handleUpdateExpenseAmount(Update update, Context context) {
        long userId = update.message.chat.id;
        String text = update.message.text;

        if (this.currentUser == null) {
            this.telegramBot.sendMessage(userId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
            return;
        }

        if (this.currentUser.getInProgressExpense() == null) {
            this.telegramBot.sendMessage(update.message.chat.id, "Nenhuma despesa selecionada para atualização. Por favor, inicie o processo novamente.", context);
            this.currentUser.setConversationState(ConversationState.NONE);
            this.userRepository.updateUser(this.currentUser);
            return;
        }

        if (Objects.equals(text, Command.SKIP_AMOUNT_UPDATE.getCommandText())) {
            this.currentUser.setConversationState(ConversationState.AWAITING_DESCRIPTION_UPDATE_EXPENSE);
            this.userRepository.updateUser(this.currentUser);

            String message = "Valor mantido. Por favor, insira a nova descrição ou clique em 'Pular'.";
            InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_DESCRIPTION_UPDATE);
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }

        if (text.startsWith(Command.CANCEL_EXPENSE_UPDATE.getCommandText())) {
            handleCancel(update, context);
            return;
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_AMOUNT_UPDATE_EXPENSE) {
            this.telegramBot.sendMessage(userId, "Você não está em um processo de atualização de despesa. Use /ajuda para ver os comandos disponíveis.", context);
            return;
        }

        int expenseId = this.currentUser.getInProgressExpense().getId();


        double newAmount;
        try {
            newAmount = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_AMOUNT_UPDATE);
            String message = "Por favor, insira um valor numérico válido para a despesa ou clique em 'Pular'.";
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }
        int amountInCents = (int) Math.round(newAmount * 100);
        this.currentUser.getInProgressExpense().setAmount(amountInCents);

        this.expenseRepository.update(userId, expenseId, this.currentUser.getInProgressExpense().getUpdatableAttributes());
        this.currentUser.setConversationState(ConversationState.AWAITING_DESCRIPTION_UPDATE_EXPENSE);
        this.userRepository.updateUser(this.currentUser);

        InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_DESCRIPTION_UPDATE);

        String message = "Despesa atualizada com sucesso! Novo valor: " + newAmount +
                "\nPor favor, insira a nova descrição da despesa ou clique em 'Pular'.";
        this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
    }

    public void handleUpdateExpenseDescription(Update update, Context context) {
        long userId = update.message.chat.id;
        String text = update.message.text;

        if (this.currentUser == null) {
            this.telegramBot.sendMessage(userId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
            return;
        }

        if (this.currentUser.getInProgressExpense() == null) {
            this.telegramBot.sendMessage(update.message.chat.id, "Nenhuma despesa selecionada para atualização. Por favor, inicie o processo novamente.", context);
            this.currentUser.setConversationState(ConversationState.NONE);
            this.userRepository.updateUser(this.currentUser);
            return;
        }

        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData("/cancelExpenseUpdate");

        InlineKeyboardButton btnSkip = new InlineKeyboardButton();
        btnSkip.setText("Pular");
        btnSkip.setCallbackData(Command.SKIP_PAYMENT_METHOD_UPDATE.getCommandText());

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(PaymentMethod.getButtonLists());

        keyboard.add(Arrays.asList(btnCancel));
        keyboard.add(Arrays.asList(btnSkip));

        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);

        if (Objects.equals(text, Command.SKIP_DESCRIPTION_UPDATE.getCommandText())) {
            this.currentUser.setConversationState(ConversationState.AWAITING_PAYMENT_METHOD_UPDATE_EXPENSE);
            this.userRepository.updateUser(this.currentUser);

            String message = "Descrição mantida. Por favor, escolha o novo método de pagamento ou clique em 'Pular'.";
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }

        if (text.startsWith(Command.CANCEL_EXPENSE_UPDATE.getCommandText())) {
            handleCancel(update, context);
            return;
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_DESCRIPTION_UPDATE_EXPENSE) {
            this.telegramBot.sendMessage(userId, "Você não está em um processo de atualização de despesa. Use /ajuda para ver os comandos disponíveis.", context);
            return;
        }

        int expenseId = this.currentUser.getInProgressExpense().getId();

        String newDescription = text.trim();
        if (newDescription.isEmpty()) {
            String message = "Por favor, insira uma descrição válida para a despesa ou clique em 'Pular'.";
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }

        this.currentUser.getInProgressExpense().setDescription(newDescription);
        this.expenseRepository.update(userId, expenseId, this.currentUser.getInProgressExpense().getUpdatableAttributes());
        this.currentUser.setConversationState(ConversationState.AWAITING_PAYMENT_METHOD_UPDATE_EXPENSE);
        this.userRepository.updateUser(this.currentUser);

        String message = "Despesa atualizada com sucesso! Nova descrição: " + newDescription +
                "\nPor favor, insira o novo método de pagamento da despesa ou clique em 'Pular'.";
        this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
    }

    public void handleUpdateExpensePaymentMethod(Update update, Context context) {
        long userId = update.message.chat.id;
        String text = update.message.text;

        if (this.currentUser == null) {
            this.telegramBot.sendMessage(userId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
            return;
        }

        if (this.currentUser.getInProgressExpense() == null) {
            this.telegramBot.sendMessage(update.message.chat.id, "Nenhuma despesa selecionada para atualização. Por favor, inicie o processo novamente.", context);
            this.currentUser.setConversationState(ConversationState.AWAITING_AMOUNT_UPDATE_EXPENSE);
            this.userRepository.updateUser(this.currentUser);
            return;
        }

        if (Objects.equals(text, Command.SKIP_PAYMENT_METHOD_UPDATE.getCommandText())) {
            this.currentUser.setConversationState(ConversationState.AWAITING_CATEGORY_UPDATE_EXPENSE);
            this.userRepository.updateUser(this.currentUser);

            String message = "Valor mantido. Por favor, insira a nova categoria ou clique em 'Pular'.";
            InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_CATEGORY_UPDATE);
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }

        if (text.startsWith(Command.CANCEL_EXPENSE_UPDATE.getCommandText())) {
            handleCancel(update, context);
            return;
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_PAYMENT_METHOD_UPDATE_EXPENSE) {
            this.telegramBot.sendMessage(userId, "Você não está em um processo de atualização de despesa. Use /ajuda para ver os comandos disponíveis.", context);
            return;
        }

        int expenseId = this.currentUser.getInProgressExpense().getId();

        String newPaymentMethodText = text.trim();
        PaymentMethod paymentMethod = PaymentMethod.fromString(newPaymentMethodText);
        if (newPaymentMethodText.isEmpty() || paymentMethod == null) {
            InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_PAYMENT_METHOD_UPDATE);
            String message = "Por favor, insira uma descrição válida para a despesa ou clique em 'Pular'.";
            this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
            return;
        }
        this.currentUser.getInProgressExpense().setPaymentMethod(paymentMethod);

        this.expenseRepository.update(userId, expenseId, this.currentUser.getInProgressExpense().getUpdatableAttributes());
        this.currentUser.setConversationState(ConversationState.AWAITING_CATEGORY_UPDATE_EXPENSE);
        this.userRepository.updateUser(this.currentUser);

        InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_CATEGORY_UPDATE);

        String message = "Despesa atualizada com sucesso! Novo método de pagamento: " + paymentMethod.getDescription() +
                "\nPor favor, insira a nova categoria da despesa ou clique em 'Pular'.";
        this.telegramBot.sendMessage(userId, message, inlineKeyboard, context);
    }

    public void handleUpdateExpenseCategory(Update update, Context context) {
        long userId = update.message.chat.id;
        String text = update.message.text;

        if (this.currentUser == null) {
            this.telegramBot.sendMessage(userId, "Por favor, inicie uma conversa comigo usando o comando /start.", context);
            return;
        }

        if (this.currentUser.getInProgressExpense() == null) {
            this.telegramBot.sendMessage(update.message.chat.id, "Nenhuma despesa selecionada para atualização. Por favor, inicie o processo novamente.", context);
            this.currentUser.setConversationState(ConversationState.AWAITING_AMOUNT_UPDATE_EXPENSE);
            this.userRepository.updateUser(this.currentUser);
            return;
        }

        int expenseId = this.currentUser.getInProgressExpense().getId();
        String message = "Despesa atualizada com sucesso! \n" +
                this.currentUser.getInProgressExpense().toString() +
                "\nProcesso de atualização de despesa concluído. Você pode usar /ajuda para ver os comandos disponíveis.";

        if (Objects.equals(text, Command.SKIP_CATEGORY_UPDATE.getCommandText())) {
            Expense finalUpdatedExpense = this.currentUser.getInProgressExpense();

            this.expenseRepository.update(userId, expenseId, finalUpdatedExpense.getUpdatableAttributes());

            this.currentUser.setInProgressExpense(null);
            this.currentUser.setConversationState(ConversationState.NONE);
            this.userRepository.updateUser(this.currentUser);

            this.telegramBot.sendMessage(userId, message, ParseMode.HTML, context);
            return;
        }

        if (text.startsWith(Command.CANCEL_EXPENSE_UPDATE.getCommandText())) {
            handleCancel(update, context);
            return;
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_CATEGORY_UPDATE_EXPENSE) {
            this.telegramBot.sendMessage(userId, "Você não está em um processo de atualização de despesa. Use /ajuda para ver os comandos disponíveis.", context);
            return;
        }

        String newCategory = text.trim();
        if (newCategory.isEmpty()) {
            InlineKeyboardMarkup inlineKeyboard = this.getInlineKeyboard(Command.SKIP_CATEGORY_UPDATE);
            String response = "Por favor, insira uma categoria válida para a despesa ou clique em 'Pular'.";
            this.telegramBot.sendMessage(userId, response, inlineKeyboard, context);
            return;
        }
        this.currentUser.getInProgressExpense().setCategory(newCategory);

        this.expenseRepository.update(userId, expenseId, this.currentUser.getInProgressExpense().getUpdatableAttributes());
        this.currentUser.setConversationState(ConversationState.NONE);
        this.userRepository.updateUser(this.currentUser);

        this.telegramBot.sendMessage(userId, message, ParseMode.HTML, context);
    }

    public void handleCancel(Update update, Context context) {
        long chatId = update.callback_query.message.chat.id;

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

        String message = "Processo de atualização de despesa cancelado. Você pode usar /ajuda para ver os comandos disponíveis.";
        this.telegramBot.sendMessage(chatId, message, context);
    }

    private InlineKeyboardMarkup getInlineKeyboard(Command skipCommand) {
        InlineKeyboardButton btnCancel = new InlineKeyboardButton();
        btnCancel.setText("Cancelar");
        btnCancel.setCallbackData("/cancelExpenseUpdate");

        InlineKeyboardButton btnSkip = new InlineKeyboardButton();
        btnSkip.setText("Pular");
        btnSkip.setCallbackData(skipCommand.getCommandText());

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList( Arrays.asList(btnCancel), Arrays.asList(btnSkip) );
        return telegramBot.createInlineKeyboard(keyboard);
    }
}
