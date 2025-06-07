package UASPraktikum.CarWash.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_review")
    private Long idReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_booking", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "komentar", columnDefinition = "TEXT")
    private String komentar;

    @CreationTimestamp
    @Column(name = "tanggal_review", nullable = false, updatable = false)
    private LocalDateTime tanggalReview;

    // Constructors
    public Review() {}

    public Review(Booking booking, User user, Integer rating, String komentar) {
        this.booking = booking;
        this.user = user;
        this.rating = rating;
        this.komentar = komentar;
    }

    // Getters and Setters
    public Long getIdReview() {
        return idReview;
    }

    public void setIdReview(Long idReview) {
        this.idReview = idReview;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        // Validasi rating 1-5
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating harus antara 1-5");
        }
        this.rating = rating;
    }

    public String getKomentar() {
        return komentar;
    }

    public void setKomentar(String komentar) {
        this.komentar = komentar;
    }

    public LocalDateTime getTanggalReview() {
        return tanggalReview;
    }

    public void setTanggalReview(LocalDateTime tanggalReview) {
        this.tanggalReview = tanggalReview;
    }

    @Override
    public String toString() {
        return "Review{" +
                "idReview=" + idReview +
                ", rating=" + rating +
                ", komentar='" + komentar + '\'' +
                ", tanggalReview=" + tanggalReview +
                '}';
    }
}
