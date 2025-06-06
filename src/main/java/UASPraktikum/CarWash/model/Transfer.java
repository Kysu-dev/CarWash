package UASPraktikum.CarWash.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer")
public class Transfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaksi")
    private Long idTransaksi;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_booking", nullable = false)
    private Booking booking;

    @Column(name = "tanggal_transaksi", nullable = false)
    private LocalDateTime tanggalTransaksi;

    @Column(name = "jumlah_transfer", nullable = false, precision = 10, scale = 2)
    private BigDecimal jumlahTransfer;

    @Column(name = "bukti_transfer")
    private String buktiTransfer; // Path to the uploaded file

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pembayaran", nullable = false)
    private PaymentStatus statusPembayaran = PaymentStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Default constructor
    public Transfer() {}

    // Constructor
    public Transfer(Booking booking, BigDecimal jumlahTransfer, String buktiTransfer) {
        this.booking = booking;
        this.jumlahTransfer = jumlahTransfer;
        this.buktiTransfer = buktiTransfer;
        this.tanggalTransaksi = LocalDateTime.now();
        this.statusPembayaran = PaymentStatus.PENDING;
    }

    // Getters and Setters
    public Long getIdTransaksi() {
        return idTransaksi;
    }

    public void setIdTransaksi(Long idTransaksi) {
        this.idTransaksi = idTransaksi;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDateTime getTanggalTransaksi() {
        return tanggalTransaksi;
    }

    public void setTanggalTransaksi(LocalDateTime tanggalTransaksi) {
        this.tanggalTransaksi = tanggalTransaksi;
    }

    public BigDecimal getJumlahTransfer() {
        return jumlahTransfer;
    }

    public void setJumlahTransfer(BigDecimal jumlahTransfer) {
        this.jumlahTransfer = jumlahTransfer;
    }

    public String getBuktiTransfer() {
        return buktiTransfer;
    }

    public void setBuktiTransfer(String buktiTransfer) {
        this.buktiTransfer = buktiTransfer;
    }

    public PaymentStatus getStatusPembayaran() {
        return statusPembayaran;
    }

    public void setStatusPembayaran(PaymentStatus statusPembayaran) {
        this.statusPembayaran = statusPembayaran;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper method to get formatted transaction ID
    public String getFormattedTransactionId() {
        return String.format("TRX-%04d-%05d", 
            createdAt != null ? createdAt.getYear() : LocalDateTime.now().getYear(), 
            idTransaksi != null ? idTransaksi : 0);
    }

    // Helper method to verify payment
    public void verify(String verifiedBy, String notes) {
        this.statusPembayaran = PaymentStatus.VALID;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedBy;
        this.notes = notes;
    }

    // Helper method to reject payment
    public void reject(String verifiedBy, String notes) {
        this.statusPembayaran = PaymentStatus.INVALID;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedBy;
        this.notes = notes;
    }
}
