package UASPraktikum.CarWash.model;

public enum PaymentStatus {
    PENDING("Pending"),
    VALID("Valid"),
    INVALID("Invalid");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
