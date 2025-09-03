package handler.enums;

public enum Command {
    START("/start"),
    HELP("/ajuda"),

    ADD_EXPENSE("/adicionar_despesa"),
    CANCEL_EXPENSE_PROCESS("/cancelExpenseProcess"),
    SKIP_PAYMENT_METHOD("/skipPaymentMethod"),
    SKIP_CATEGORY("/skipCategory"),

    VIEW_EXPENSES("/listar_despesas"),

    UPDATE_EXPENSE("/atualizar_despesa"),
    CANCEL_EXPENSE_UPDATE("/cancelExpenseUpdate"),
    SKIP_AMOUNT_UPDATE("/skipAmountUpdate"),
    SKIP_DESCRIPTION_UPDATE("/skipDescriptionUpdate"),
    SKIP_PAYMENT_METHOD_UPDATE("/skipPaymentMethodUpdate"),
    SKIP_CATEGORY_UPDATE("/skipCategoryUpdate"),

    DELETE_EXPENSE("/deletar_despesa"),
    CANCEL_EXPENSE_DELETION("/cancelExpenseDeletion"),

    ADD_SPREADSHEET("/adicionar_planilha"),

    SUMMARY("/resumo"),

    UNKNOWN("/unknown");

    private final String commandText;

    Command(String commandText) {
        this.commandText = commandText;
    }

    public String getCommandText() {
        return commandText;
    }

    public static Command fromString(String text) {
        if (text == null) {
            return UNKNOWN;
        }

        for (Command command : Command.values()) {
            if (command.commandText.equalsIgnoreCase(text)) {
                return command;
            }
        }
        return UNKNOWN;
    }
}
