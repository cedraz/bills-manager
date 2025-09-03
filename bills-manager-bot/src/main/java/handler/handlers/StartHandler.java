package handler.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import handler.enums.ParseMode;
import handler.telegram.Update;
import handler.telegram.TelegramBot;
import handler.user.User;
import handler.user.UserRepository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

public class StartHandler extends BaseHandler{
    private final UserRepository userRepository;

    public StartHandler(TelegramBot telegramBot,  UserRepository userRepository) {
        super(telegramBot);
        this.userRepository = userRepository;
    }

    @Override
    public void handle(Update update, Context context) {
        long chatId = update.message.chat.id;

        User user = new User(update.message.chat.id, update.message.chat.first_name, update.message.chat.username);

        String welcomeText = "Olá, <b>" + user.getFirst_name() + "</b>! Seja bem-vindo(a) ao Bills Manager Bot.\n\n" +
                "Use os botões abaixo para navegar pelas opções disponíveis.\n\n" +
                "É possível vincular uma planilha do Google Sheets para gerenciar suas despesas.\n" +
                "Para isso, clique em 'Adicionar Planilha' e siga as instruções.";

        InlineKeyboardButton btnAdd = new InlineKeyboardButton();
        btnAdd.setText("➕ Adicionar Despesa");
        btnAdd.setCallbackData("/adicionarDespesa");

        InlineKeyboardButton btnAddSpreadsheet = new InlineKeyboardButton();
        btnAddSpreadsheet.setText("📊 Adicionar Planilha");
        btnAddSpreadsheet.setCallbackData("/adicionarPlanilha");

        InlineKeyboardButton btnHelp = new InlineKeyboardButton();
        btnHelp.setText("❓ Ajuda");
        btnHelp.setCallbackData("/ajuda");

        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
                Arrays.asList(btnAddSpreadsheet),
                Arrays.asList(btnAdd),
                Arrays.asList(btnHelp)
        );

        InlineKeyboardMarkup inlineKeyboard = telegramBot.createInlineKeyboard(keyboard);
        this.telegramBot.sendMessage(chatId, welcomeText, inlineKeyboard, ParseMode.HTML, context);

        this.userRepository.saveUser(user);
    }
}
