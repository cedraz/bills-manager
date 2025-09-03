package handler.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.enums.ConversationState;
import handler.enums.ParseMode;
import handler.telegram.TelegramBot;
import handler.telegram.Update;
import handler.user.User;
import handler.user.UserRepository;

public class SpreadsheetHandler extends BaseHandler {
    private final UserRepository userRepository;
    private User currentUser;

    public SpreadsheetHandler(TelegramBot telegramBot, UserRepository userRepository) {
        super(telegramBot);
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update, Context context) {
        var logger = context.getLogger();

        long chatId = update.message.chat.id;
        User user = this.userRepository.findByChatId(chatId);

        if (user == null) {
            logger.log("User not found for chatId: " + chatId);
            String response = "Usuário não encontrado. Por favor, inicie o bot com /start.";
            this.telegramBot.sendMessage(chatId, response, context);
            return;
        }

        this.currentUser = user;
        this.currentUser.setConversationState(ConversationState.AWAITING_SPREADSHEET_URL);
        try {
            this.userRepository.updateUser(this.currentUser);
        } catch (Exception e) {
            logger.log("ERROR updating user state: " + e.getMessage());
            String response = "Erro ao processar sua solicitação. Tente novamente mais tarde.";
            this.telegramBot.sendMessage(chatId, response, context);
            return;
        }

        String message = "Ao vincular a planilha, será necessário ter uma página com o seguinte nome: 'Página1'\n\n" +
                "É importante que você coloque exatamente esse nome, com acento e maiúscula.\n" +
                "Também será necessário adicionar um email como editor da sua planilha. " +
                "Feito isso, todas as despesas cadastradas serão automaticamente adicionadas na planilha.\n\n" +
                "Por favor, envie a <b>URL</b> completa da planilha do Google Sheets que você deseja vincular.";
        this.telegramBot.sendMessage(chatId, message, ParseMode.HTML, context);
    }

    public void saveSpreadsheetId(Update update, Context context) {
        var logger = context.getLogger();
        long chatId = update.message.chat.id;
        String spreadsheetURL = update.message.text;
        String spreadsheetId = getSpreadsheetId(spreadsheetURL);

        if (spreadsheetId == null) {
            String response = "URL inválida. Por favor, envie uma URL válida do Google Sheets.";
            this.telegramBot.sendMessage(chatId, response, context);
            return;
        }

        if (this.currentUser == null) {
            User user = this.userRepository.findByChatId(chatId);
            if (user == null) {
                String response = "Usuário não encontrado. Por favor, inicie o bot com /start.";
                this.telegramBot.sendMessage(chatId, response, context);
                return;
            }
            this.currentUser = user;
        }

        if (this.currentUser.getConversationState() != ConversationState.AWAITING_SPREADSHEET_URL) {
            String response = "Comando inesperado. Por favor, inicie o processo de vinculação da planilha novamente com /adicionarPlanilha.";
            this.telegramBot.sendMessage(chatId, response, context);
            return;
        }

        this.currentUser.setSpreadsheetId(spreadsheetId);
        this.currentUser.setConversationState(ConversationState.NONE);

        try {
            this.userRepository.updateUser(this.currentUser);

            String response = "Planilha vinculada com sucesso! Não se esqueca de adicionar o email na aba compartilhar da planilha, com opção de editor: ";

            this.telegramBot.sendMessage(chatId, response, context);

            this.telegramBot.sendMessage(chatId, "bills-manager-bot@micro-saas-434304.iam.gserviceaccount.com", context);
        } catch (Exception e) {
            logger.log("ERROR handling spreadsheet command: " + e.getMessage());
            String response = "Erro ao processar sua solicitação. Tente novamente mais tarde.";
            this.telegramBot.sendMessage(chatId, response, context);
        }
    }

    private String getSpreadsheetId(String text) {
        String[] parts = text.split("/d/");
        if (parts.length < 2) {
            return null;
        }

        String[] idParts = parts[1].split("/");
        return idParts[0];
    }
}
