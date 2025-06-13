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
      /**
     * Safely converts a string to VehicleType enum
     * Handles special cases like "suv" by mapping it to MOBIL karena SUV termasuk kategori mobil
     */
    public static VehicleType safeFromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String upperValue = value.toUpperCase();
        try {
            return VehicleType.valueOf(upperValue);
        } catch (IllegalArgumentException e) {
            // Map alternative values to existing enum values
            if ("SUV".equals(upperValue)) {
                return MOBIL; // SUV dianggap sebagai jenis MOBIL
            }
            // Untuk semua kendaraan roda empat atau lebih dianggap MOBIL
            if (upperValue.contains("CAR") || 
                upperValue.contains("TRUCK") || 
                upperValue.contains("PICKUP") || 
                upperValue.contains("JEEP") ||
                upperValue.contains("SEDAN")) {
                return MOBIL;
            }
            // Untuk kendaraan roda dua dianggap MOTOR
            if (upperValue.contains("BIKE") || 
                upperValue.contains("MOTORCYCLE") || 
                upperValue.contains("SCOOTER")) {
                return MOTOR;
            }
            return null; // Return null for other unknown values
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
