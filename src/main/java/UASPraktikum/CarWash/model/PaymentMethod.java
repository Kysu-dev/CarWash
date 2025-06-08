package UASPraktikum.CarWash.model;

public enum PaymentMethod {
    CASH("Cash Payment"),
    TRANSFER("Bank Transfer"),
    CARD("Credit/Debit Card"),
    E_WALLET("E-Wallet");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
