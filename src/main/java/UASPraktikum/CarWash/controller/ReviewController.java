package UASPraktikum.CarWash.controller;

import UASPraktikum.CarWash.model.Review;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/review")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    @Autowired
    private ReviewService reviewService;

    // API endpoint to create review
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestParam Long bookingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String komentar,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user from session
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User tidak terautentikasi");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            Review review = reviewService.createReview(bookingId, currentUser.getUserId(), rating, komentar);
            
            response.put("success", true);
            response.put("message", "Review berhasil dibuat");
            response.put("reviewId", review.getIdReview());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error creating review: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error creating review", e);
            response.put("success", false);
            response.put("message", "Terjadi kesalahan sistem");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API endpoint to update review
    @PostMapping("/update/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable Long reviewId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String komentar,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user from session
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User tidak terautentikasi");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Check if review belongs to current user
            Optional<Review> reviewOpt = reviewService.getReviewById(reviewId);
            if (!reviewOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Review tidak ditemukan");
                return ResponseEntity.badRequest().body(response);
            }
            
            Review existingReview = reviewOpt.get();
            if (!existingReview.getUser().getUserId().equals(currentUser.getUserId())) {
                response.put("success", false);
                response.put("message", "Anda tidak memiliki akses untuk mengubah review ini");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Review updatedReview = reviewService.updateReview(reviewId, rating, komentar);
            
            response.put("success", true);
            response.put("message", "Review berhasil diperbarui");
            response.put("reviewId", updatedReview.getIdReview());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error updating review: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error updating review", e);
            response.put("success", false);
            response.put("message", "Terjadi kesalahan sistem");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API endpoint to delete review
    @PostMapping("/delete/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReview(
            @PathVariable Long reviewId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user from session
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User tidak terautentikasi");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Check if review belongs to current user (unless admin)
            Optional<Review> reviewOpt = reviewService.getReviewById(reviewId);
            if (!reviewOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Review tidak ditemukan");
                return ResponseEntity.badRequest().body(response);
            }
            
            Review existingReview = reviewOpt.get();
            if (!existingReview.getUser().getUserId().equals(currentUser.getUserId()) && 
                currentUser.getRole() != UserRole.ADMIN) {
                response.put("success", false);
                response.put("message", "Anda tidak memiliki akses untuk menghapus review ini");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            reviewService.deleteReview(reviewId);
            
            response.put("success", true);
            response.put("message", "Review berhasil dihapus");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting review: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Unexpected error deleting review", e);
            response.put("success", false);
            response.put("message", "Terjadi kesalahan sistem");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API endpoint to get reviews by user
    @GetMapping("/user/{userId}")
    @ResponseBody
    public ResponseEntity<List<Review>> getReviewsByUser(@PathVariable Long userId) {
        try {
            List<Review> reviews = reviewService.getReviewsByUser(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            logger.error("Error getting reviews by user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get review by booking
    @GetMapping("/booking/{bookingId}")
    @ResponseBody
    public ResponseEntity<Review> getReviewByBooking(@PathVariable Long bookingId) {
        try {
            Optional<Review> review = reviewService.getReviewByBooking(bookingId);
            if (review.isPresent()) {
                return ResponseEntity.ok(review.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting review by booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get all reviews
    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<List<Review>> getAllReviews() {
        try {
            List<Review> reviews = reviewService.getAllReviews();
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            logger.error("Error getting all reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get latest reviews
    @GetMapping("/latest")
    @ResponseBody
    public ResponseEntity<List<Review>> getLatestReviews() {
        try {
            List<Review> reviews = reviewService.getLatestReviews();
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            logger.error("Error getting latest reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get average rating
    @GetMapping("/average")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAverageRating() {
        try {
            Double averageRating = reviewService.getAverageRating();
            Map<String, Object> response = new HashMap<>();
            response.put("averageRating", averageRating);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting average rating", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to get average rating by service
    @GetMapping("/average/service/{serviceId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAverageRatingByService(@PathVariable Long serviceId) {
        try {
            Double averageRating = reviewService.getAverageRatingByService(serviceId);
            Map<String, Object> response = new HashMap<>();
            response.put("averageRating", averageRating);
            response.put("serviceId", serviceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting average rating by service", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API endpoint to check if booking can be reviewed
    @GetMapping("/can-review/{bookingId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> canReviewBooking(
            @PathVariable Long bookingId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                response.put("canReview", false);
                response.put("message", "User tidak terautentikasi");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            boolean canReview = reviewService.canReviewBooking(bookingId, currentUser.getUserId());
            response.put("canReview", canReview);
            
            if (!canReview) {
                response.put("message", "Booking tidak dapat di-review");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking if booking can be reviewed", e);
            response.put("canReview", false);
            response.put("message", "Terjadi kesalahan sistem");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
