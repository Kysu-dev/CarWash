package UASPraktikum.CarWash.model;

public enum BookingMethod {
    BOOKING("Booking"),
    WALKIN("Walk-in");

    private final String displayName;

    BookingMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
