package handler.handlers;

import handler.enums.Command;
import handler.telegram.TelegramBot;
import handler.telegram.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

public class HelpHandler extends BaseHandler {
    public HelpHandler(TelegramBot telegramBot) {
        super(telegramBot);
    }

    @Override
    public void handle(Update update, com.amazonaws.services.lambda.runtime.Context context) {
        long chatId = update.message.chat.id;
        String helpMessage = "Comandos disponíveis:\n\n" +
                "Selecione uma opção:";

        InlineKeyboardButton btnAdd = new InlineKeyboardButton();
        btnAdd.setText("➕ Adicionar Despesa");
        btnAdd.setCallbackData(Command.ADD_EXPENSE.getCommandText());

        InlineKeyboardButton btnList = new InlineKeyboardButton();
        btnList.setText("📋 Listar Despesas");
        btnList.setCallbackData(Command.VIEW_EXPENSES.getCommandText());

        InlineKeyboardButton btnUpdate = new InlineKeyboardButton();
        btnUpdate.setText("✏️ Atualizar Despesa");
        btnUpdate.setCallbackData(Command.UPDATE_EXPENSE.getCommandText());

        InlineKeyboardButton btnAddSpreadsheet = new InlineKeyboardButton();
        btnAddSpreadsheet.setText("📊 Adicionar Planilha");
        btnAddSpreadsheet.setCallbackData(Command.ADD_SPREADSHEET.getCommandText());

        InlineKeyboardButton btnSummary= new InlineKeyboardButton();
        btnSummary.setText("📈 Resumo");
        btnSummary.setCallbackData(Command.SUMMARY.getCommandText());

        InlineKeyboardButton btnHelp = new InlineKeyboardButton();
        btnHelp.setText("❓ Ajuda");
        btnHelp.setCallbackData(Command.HELP.getCommandText());

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
                Arrays.asList(btnAdd),
                Arrays.asList(btnList),
                Arrays.asList(btnUpdate),
                Arrays.asList(btnAddSpreadsheet),
                Arrays.asList(btnSummary),
                Arrays.asList(btnHelp)
        );

        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);
        this.telegramBot.sendMessage(chatId, helpMessage, inlineKeyboard, context);
    }
}