package UASPraktikum.CarWash.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "booking_slot")
public class BookingSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_slot")
    private Long idSlot;
    
    @Column(name = "tanggal", nullable = false)
    private LocalDate tanggal;
    
    @Column(name = "jam", nullable = false)
    private LocalTime jam;
    
    @Column(name = "jumlah_terisi", nullable = false)
    private Integer jumlahTerisi = 0;
    
    // Maximum capacity per slot
    public static final int MAX_CAPACITY = 10;
    
    // Constructors
    public BookingSlot() {}
    
    public BookingSlot(LocalDate tanggal, LocalTime jam) {
        this.tanggal = tanggal;
        this.jam = jam;
        this.jumlahTerisi = 0;
    }
    
    public BookingSlot(LocalDate tanggal, LocalTime jam, Integer jumlahTerisi) {
        this.tanggal = tanggal;
        this.jam = jam;
        this.jumlahTerisi = jumlahTerisi;
    }
    
    // Getters and Setters
    public Long getIdSlot() {
        return idSlot;
    }
    
    public void setIdSlot(Long idSlot) {
        this.idSlot = idSlot;
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
    
    public Integer getJumlahTerisi() {
        return jumlahTerisi;
    }
    
    public void setJumlahTerisi(Integer jumlahTerisi) {
        this.jumlahTerisi = jumlahTerisi;
    }
    
    // Business logic methods
    public boolean isAvailable() {
        return jumlahTerisi < MAX_CAPACITY;
    }
    
    public boolean canBook() {
        return isAvailable();
    }
    
    public void incrementBooking() {
        if (canBook()) {
            jumlahTerisi++;
        } else {
            throw new IllegalStateException("Slot is already full");
        }
    }
    
    public void decrementBooking() {
        if (jumlahTerisi > 0) {
            jumlahTerisi--;
        }
    }
    
    public int getAvailableSpots() {
        return MAX_CAPACITY - jumlahTerisi;
    }
    
    public double getOccupancyRate() {
        return (double) jumlahTerisi / MAX_CAPACITY * 100;
    }
    
    @Override
    public String toString() {
        return String.format("BookingSlot{idSlot=%d, tanggal=%s, jam=%s, jumlahTerisi=%d/%d}", 
                           idSlot, tanggal, jam, jumlahTerisi, MAX_CAPACITY);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BookingSlot that = (BookingSlot) obj;
        return tanggal.equals(that.tanggal) && jam.equals(that.jam);
    }
    
    @Override
    public int hashCode() {
        return tanggal.hashCode() + jam.hashCode();
    }
}
