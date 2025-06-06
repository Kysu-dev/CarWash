package UASPraktikum.CarWash.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_booking")
    private Long idBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;

    @Column(name = "jam", nullable = false)
    private LocalTime jam;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "metode", nullable = false)
    private BookingMethod metode = BookingMethod.BOOKING;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Vehicle information
    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "vehicle_brand")
    private String vehicleBrand;

    @Column(name = "vehicle_model")
    private String vehicleModel;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "vehicle_color")
    private String vehicleColor;

    // Default constructor
    public Booking() {}

    // Constructor
    public Booking(User user, Service service, LocalDate tanggal, LocalTime jam, 
                   BookingMethod metode, String catatan) {
        this.user = user;
        this.service = service;
        this.tanggal = tanggal;
        this.jam = jam;
        this.metode = metode;
        this.catatan = catatan;
        this.status = BookingStatus.PENDING;
    }

    // Getters and Setters
    public Long getIdBooking() {
        return idBooking;
    }

    public void setIdBooking(Long idBooking) {
        this.idBooking = idBooking;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
    }

    public LocalTime getJam() {
        return jam;
    }

    public void setJam(LocalTime jam) {
        this.jam = jam;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BookingMethod getMetode() {
        return metode;
    }

    public void setMetode(BookingMethod metode) {
        this.metode = metode;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVehicleColor() {
        return vehicleColor;
    }

    public void setVehicleColor(String vehicleColor) {
        this.vehicleColor = vehicleColor;
    }

    // Helper method to get formatted booking ID
    public String getFormattedBookingId() {
        return String.format("CW-%04d-%05d", 
            createdAt != null ? createdAt.getYear() : LocalDateTime.now().getYear(), 
            idBooking != null ? idBooking : 0);
    }

    // Helper method to check if booking can be cancelled
    public boolean canBeCancelled() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    // Helper method to get vehicle display name
    public String getVehicleDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (vehicleBrand != null && !vehicleBrand.isEmpty()) {
            sb.append(vehicleBrand);
        }
        if (vehicleModel != null && !vehicleModel.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(vehicleModel);
        }
        return sb.toString();
    }
}
