package UASPraktikum.CarWash.controller;

import UASPraktikum.CarWash.model.Review;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/review-management")
public class AdminReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminReviewController.class);
    
    @Autowired
    private ReviewService reviewService;
    
    /**
     * Check if current user is admin
     */
    private boolean isAdmin(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.ADMIN;
    }
    
    /**
     * List all reviews
     */
    @GetMapping({"", "/"})
    public String listReviews(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            List<Review> reviews = reviewService.getAllReviews();
            
            // Calculate average rating
            double averageRating = 0;
            if (!reviews.isEmpty()) {
                averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0);
            }
            
            // Get ratings distribution
            int[] ratingCounts = new int[5]; // For ratings 1-5
            for (Review review : reviews) {
                int rating = review.getRating();
                if (rating >= 1 && rating <= 5) {
                    ratingCounts[rating - 1]++;
                }
            }
            
            model.addAttribute("reviews", reviews);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("ratingCounts", ratingCounts);
            model.addAttribute("totalReviews", reviews.size());
            model.addAttribute("section", "reviews");
            model.addAttribute("pageTitle", "Review Management");
            
            return "admin/review/list";
        } catch (Exception e) {
            logger.error("Error listing reviews", e);
            model.addAttribute("error", "Failed to load reviews: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * View review details
     */
    @GetMapping("/details/{id}")
    public String reviewDetails(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            Optional<Review> reviewOpt = reviewService.getReviewById(id);
            if (reviewOpt.isEmpty()) {
                return "redirect:/admin/review-management";
            }
            
            Review review = reviewOpt.get();
            model.addAttribute("review", review);
            model.addAttribute("section", "reviews");
            model.addAttribute("pageTitle", "Review Details");
            
            return "admin/review/details";
        } catch (Exception e) {
            logger.error("Error viewing review details", e);
            model.addAttribute("error", "Failed to load review details: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Filter reviews by rating
     */
    @GetMapping("/filter/{rating}")
    public String filterReviews(@PathVariable("rating") Integer rating, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            List<Review> allReviews = reviewService.getAllReviews();
            List<Review> filteredReviews;
            
            if (rating > 0 && rating <= 5) {
                filteredReviews = allReviews.stream()
                    .filter(r -> r.getRating() == rating)
                    .collect(Collectors.toList());
            } else {
                filteredReviews = allReviews;
            }
            
            // Calculate average rating of filtered reviews
            double averageRating = 0;
            if (!filteredReviews.isEmpty()) {
                averageRating = filteredReviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0);
            }
            
            // Get ratings distribution from all reviews
            int[] ratingCounts = new int[5]; // For ratings 1-5
            for (Review review : allReviews) {
                int r = review.getRating();
                if (r >= 1 && r <= 5) {
                    ratingCounts[r - 1]++;
                }
            }
            
            model.addAttribute("reviews", filteredReviews);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("ratingCounts", ratingCounts);
            model.addAttribute("totalReviews", allReviews.size());
            model.addAttribute("filteredCount", filteredReviews.size());
            model.addAttribute("currentRatingFilter", rating);
            model.addAttribute("section", "reviews");
            model.addAttribute("pageTitle", "Reviews - Rating " + rating);
            
            return "admin/review/list";
        } catch (Exception e) {
            logger.error("Error filtering reviews", e);
            model.addAttribute("error", "Failed to filter reviews: " + e.getMessage());
            return "error";
        }
    }
}
