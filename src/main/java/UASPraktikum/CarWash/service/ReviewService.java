package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.Review;
import UASPraktikum.CarWash.model.Booking;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.BookingStatus;
import UASPraktikum.CarWash.repository.ReviewRepository;
import UASPraktikum.CarWash.repository.BookingRepository;
import UASPraktikum.CarWash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;

    // Create a new review
    public Review createReview(Long bookingId, Long userId, Integer rating, String komentar) {
        logger.info("Attempting to create review for booking ID: {} by user ID: {}", bookingId, userId);
        
        // Validate booking exists and is completed
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (!bookingOpt.isPresent()) {
            throw new IllegalArgumentException("Booking tidak ditemukan");
        }
        
        Booking booking = bookingOpt.get();
        
        // Check if booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Hanya booking yang sudah selesai yang bisa di-review");
        }
        
        // Validate user exists
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User tidak ditemukan");
        }
        
        User user = userOpt.get();
        
        // Check if user is the one who made the booking
        if (!booking.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Hanya user yang melakukan booking yang bisa memberikan review");
        }
        
        // Check if review already exists for this booking
        if (reviewRepository.existsByBooking(booking)) {
            throw new IllegalArgumentException("Review untuk booking ini sudah ada");
        }
        
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating harus antara 1-5");
        }
        
        // Create and save review
        Review review = new Review(booking, user, rating, komentar);
        Review savedReview = reviewRepository.save(review);
        
        logger.info("Review created successfully with ID: {}", savedReview.getIdReview());
        return savedReview;
    }

    // Get all reviews
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    // Get review by ID
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    // Get reviews by user
    public List<Review> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserUserId(userId);
    }

    // Get review by booking
    public Optional<Review> getReviewByBooking(Long bookingId) {
        return reviewRepository.findByBookingIdBooking(bookingId);
    }

    // Get reviews by rating
    public List<Review> getReviewsByRating(Integer rating) {
        return reviewRepository.findByRating(rating);
    }

    // Get latest reviews
    public List<Review> getLatestReviews() {
        return reviewRepository.findLatestReviews();
    }

    // Get reviews with minimum rating
    public List<Review> getReviewsWithMinRating(Integer minRating) {
        return reviewRepository.findByRatingGreaterThanEqual(minRating);
    }

    // Get average rating
    public Double getAverageRating() {
        Double avg = reviewRepository.getAverageRating();
        return avg != null ? avg : 0.0;
    }

    // Get average rating for a specific service
    public Double getAverageRatingByService(Long serviceId) {
        Double avg = reviewRepository.getAverageRatingByService(serviceId);
        return avg != null ? avg : 0.0;
    }

    // Update review
    public Review updateReview(Long reviewId, Integer rating, String komentar) {
        logger.info("Attempting to update review ID: {}", reviewId);
        
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
        if (!reviewOpt.isPresent()) {
            throw new IllegalArgumentException("Review tidak ditemukan");
        }
        
        Review review = reviewOpt.get();
        
        // Validate rating if provided
        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating harus antara 1-5");
            }
            review.setRating(rating);
        }
        
        if (komentar != null) {
            review.setKomentar(komentar);
        }
        
        Review updatedReview = reviewRepository.save(review);
        logger.info("Review updated successfully");
        return updatedReview;
    }

    // Delete review
    public void deleteReview(Long reviewId) {
        logger.info("Attempting to delete review ID: {}", reviewId);
        
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("Review tidak ditemukan");
        }
        
        reviewRepository.deleteById(reviewId);
        logger.info("Review deleted successfully");
    }

    // Check if booking can be reviewed
    public boolean canReviewBooking(Long bookingId, Long userId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (!bookingOpt.isPresent()) {
            return false;
        }
        
        Booking booking = bookingOpt.get();
        
        // Check if booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            return false;
        }
        
        // Check if user is the one who made the booking
        if (!booking.getUser().getUserId().equals(userId)) {
            return false;
        }
        
        // Check if review already exists
        return !reviewRepository.existsByBooking(booking);
    }

    // Check if review exists for booking
    public boolean hasReview(Long bookingId) {
        return reviewRepository.existsByBookingIdBooking(bookingId);
    }
}
