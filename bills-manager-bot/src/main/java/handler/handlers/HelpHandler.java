package handler.handlers;

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
        btnAdd.setCallbackData("/adicionarDespesa");

        InlineKeyboardButton btnList = new InlineKeyboardButton();
        btnList.setText("📋 Listar Despesas");
        btnList.setCallbackData("/listarDespesas");

        InlineKeyboardButton btnUpdate = new InlineKeyboardButton();
        btnUpdate.setText("✏️ Atualizar Despesa");
        btnUpdate.setCallbackData("/atualizarDespesa");

        InlineKeyboardButton btnAddSpreadsheet = new InlineKeyboardButton();
        btnAddSpreadsheet.setText("📊 Adicionar Planilha");
        btnAddSpreadsheet.setCallbackData("/adicionarPlanilha");

        InlineKeyboardButton btnSummary= new InlineKeyboardButton();
        btnSummary.setText("📈 Resumo");
        btnSummary.setCallbackData("/resumo");

        InlineKeyboardButton btnHelp = new InlineKeyboardButton();
        btnHelp.setText("❓ Ajuda");
        btnHelp.setCallbackData("/ajuda");

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