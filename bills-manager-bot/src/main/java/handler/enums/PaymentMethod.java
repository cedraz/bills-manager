package handler.enums;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public enum PaymentMethod {
    PIX("PIX"),
    CREDIT_CARD("Cartão de Crédito"),
    DEBIT_CARD("Cartão de Débito"),
    CASH("Dinheiro"),
    OTHER("Outros");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentMethod fromString(String text) {
        if (text == null) {
            return OTHER;
        }

        if (text.startsWith("/skipPaymentMethod") || text.equalsIgnoreCase("Pular")) {
            return OTHER;
        }

        for (PaymentMethod pm : PaymentMethod.values()) {
            if (pm.name().equalsIgnoreCase(text) || pm.getDescription().equalsIgnoreCase(text)) {
                return pm;
            }
        }

        return null;
    }
    public static List<List<InlineKeyboardButton>> getButtonLists() {
        InlineKeyboardButton btnCash = new InlineKeyboardButton();
        btnCash.setText(PaymentMethod.CASH.getDescription());
        btnCash.setCallbackData(PaymentMethod.CASH.getDescription());

        InlineKeyboardButton btnCreditCard = new InlineKeyboardButton();
        btnCreditCard.setText(PaymentMethod.CREDIT_CARD.getDescription());
        btnCreditCard.setCallbackData(PaymentMethod.CREDIT_CARD.getDescription());

        InlineKeyboardButton btnDebitCard = new InlineKeyboardButton();
        btnDebitCard.setText(PaymentMethod.DEBIT_CARD.getDescription());
        btnDebitCard.setCallbackData(PaymentMethod.DEBIT_CARD.getDescription());

        InlineKeyboardButton btnPix = new InlineKeyboardButton();
        btnPix.setText(PaymentMethod.PIX.getDescription());
        btnPix.setCallbackData(PaymentMethod.PIX.getDescription());

        InlineKeyboardButton btnOther = new InlineKeyboardButton();
        btnOther.setText(PaymentMethod.OTHER.getDescription());
        btnOther.setCallbackData(PaymentMethod.OTHER.getDescription());

        return List.of(
                List.of(btnCash),
                List.of(btnCreditCard),
                List.of(btnDebitCard),
                List.of(btnPix),
                List.of(btnOther)
        );
    }
}
