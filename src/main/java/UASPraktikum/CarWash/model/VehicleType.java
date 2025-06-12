package UASPraktikum.CarWash.model;

/**
 * Enum untuk menentukan jenis kendaraan yang dilayani
 */
public enum VehicleType {
    MOBIL("Mobil"),
    MOTOR("Motor");
    
    private final String displayName;
    
    VehicleType(String displayName) {
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
