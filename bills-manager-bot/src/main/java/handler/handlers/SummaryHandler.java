package handler.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.enums.ParseMode;
import handler.expense.Expense;
import handler.expense.ExpenseRepository;
import handler.telegram.TelegramBot;
import handler.telegram.Update;
import handler.user.User;
import handler.user.UserRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryHandler extends BaseHandler {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public SummaryHandler(TelegramBot telegramBot, UserRepository userRepository, ExpenseRepository expenseRepository) {
        super(telegramBot);
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void handle(Update update, Context context) {
        long chatId = update.message.chat.id;

        User user = this.userRepository.findByChatId(chatId);

        if (user == null) {
            String response = "Usuário não encontrado. Por favor, inicie o bot com /start.";
            this.telegramBot.sendMessage(chatId, response, context);
            return;
        }

        List<Expense> expenses = this.expenseRepository.getExpenses(chatId);

        int totalAmount = 0;
        String mostUsedPaymentMethod = null;
        String mostUsedCategory = null;
        Map<String, Integer> paymentMethodCount = new HashMap<>();
        Map<String, Integer> categoryCount = new HashMap<>();
        LocalDate minDate = null;
        LocalDate maxDate = null;

        for (Expense expense : expenses) {
            totalAmount += expense.getAmount();
            String paymentMethod = expense.getPaymentMethodDescription();
            String category = expense.getCategory();

            paymentMethodCount.put(paymentMethod, paymentMethodCount.getOrDefault(paymentMethod, 0) + 1);
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);

            LocalDate expenseDate = expense.getDate();
            if (minDate == null || expenseDate.isBefore(minDate)) {
                minDate = expenseDate;
            }
            if (maxDate == null || expenseDate.isAfter(maxDate)) {
                maxDate = expenseDate;
            }
        }

        int paymentMethodMaxCount = 0;
        for (Map.Entry<String, Integer> entry : paymentMethodCount.entrySet()) {
            if (entry.getValue() > paymentMethodMaxCount) {
                paymentMethodMaxCount = entry.getValue();
                mostUsedPaymentMethod = entry.getKey();
            }
        }

        int categoryMaxCount = 0;
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() > categoryMaxCount) {
                categoryMaxCount = entry.getValue();
                mostUsedCategory = entry.getKey();
            }
        }

        String summary = String.format(
                "Resumo de suas despesas: \n\n" +
                "💰 <b>Total gasto:</b> R$ %s \n" +
                "💳 <b>Método de pagamento mais usado:</b> %s \n" +
                "🗓️ <b>Período:</b> %s a %s \n" +
                "📦 <b>Categoria mais comprada:</b> %s",
                String.format("%,.2f", totalAmount / 100.0).replace(".", ","),
                mostUsedPaymentMethod != null ? mostUsedPaymentMethod : "N/A",
                minDate != null ? minDate.toString() : "N/A",
                maxDate != null ? maxDate.toString() : "N/A",
                mostUsedCategory != null ? mostUsedCategory : "N/A"
        );

        this.telegramBot.sendMessage(chatId, summary, ParseMode.HTML, context);
    }
}
