package handler.enums;

public enum ConversationState {
    NONE,
    AWAITING_AMOUNT,
    AWAITING_DESCRIPTION,
    AWAITING_PAYMENT_METHOD,
    AWAITING_CATEGORY,
    AWAITING_EXPENSE_ID_UPDATE_EXPENSE,
    AWAITING_AMOUNT_UPDATE_EXPENSE,
    AWAITING_DESCRIPTION_UPDATE_EXPENSE,
    AWAITING_PAYMENT_METHOD_UPDATE_EXPENSE,
    AWAITING_CATEGORY_UPDATE_EXPENSE,
    AWAITING_EXPENSE_ID_DELETE_EXPENSE,
    AWAITING_SPREADSHEET_URL;

    public static ConversationState fromString(String state) {
        for (ConversationState cs : ConversationState.values()) {
            if (cs.name().equalsIgnoreCase(state)) {
                return cs;
            }
        }
        return NONE;
    }
}
