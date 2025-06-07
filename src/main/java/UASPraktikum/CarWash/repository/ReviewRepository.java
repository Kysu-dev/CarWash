package UASPraktikum.CarWash.repository;

import UASPraktikum.CarWash.model.Review;
import UASPraktikum.CarWash.model.Booking;
import UASPraktikum.CarWash.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Mencari review berdasarkan booking
    Optional<Review> findByBooking(Booking booking);
    
    // Mencari review berdasarkan user
    List<Review> findByUser(User user);
    
    // Mencari review berdasarkan rating
    List<Review> findByRating(Integer rating);
    
    // Mencari review berdasarkan booking ID
    Optional<Review> findByBookingIdBooking(Long idBooking);
    
    // Mencari review berdasarkan user ID
    List<Review> findByUserUserId(Long userId);
    
    // Query untuk mendapatkan rata-rata rating
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getAverageRating();
    
    // Query untuk mendapatkan rata-rata rating berdasarkan service
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.booking.service.idService = :serviceId")
    Double getAverageRatingByService(@Param("serviceId") Long serviceId);
    
    // Query untuk mendapatkan review terbaru
    @Query("SELECT r FROM Review r ORDER BY r.tanggalReview DESC")
    List<Review> findLatestReviews();
    
    // Query untuk mendapatkan review dengan rating tertentu atau lebih tinggi
    @Query("SELECT r FROM Review r WHERE r.rating >= :minRating ORDER BY r.tanggalReview DESC")
    List<Review> findByRatingGreaterThanEqual(@Param("minRating") Integer minRating);
    
    // Mengecek apakah booking sudah memiliki review
    boolean existsByBooking(Booking booking);
    
    // Mengecek apakah booking sudah memiliki review berdasarkan ID
    boolean existsByBookingIdBooking(Long idBooking);
}
