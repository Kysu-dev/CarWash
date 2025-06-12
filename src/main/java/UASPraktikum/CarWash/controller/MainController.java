package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.service.ReviewService;
import UASPraktikum.CarWash.model.Service;
import UASPraktikum.CarWash.model.Review;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private ServiceService serviceService;
    
    @Autowired
    private ReviewService reviewService;

    @GetMapping("/")
    public String index(Model model) {
        try {
            // Get all active services from the database
            List<Service> services = serviceService.getAllServices();
            
            // Filter only active services for the main page
            List<Service> activeServices = services.stream()
                .filter(Service::isActive)
                .toList();
            
            logger.info("Loaded {} active services for main page", activeServices.size());
            model.addAttribute("services", activeServices);
            
            // Get reviews for testimonials section
            // Get only reviews with a minimum rating of 4 (good reviews) and limit to 10
            List<Review> testimonials = reviewService.getReviewsWithMinRating(4);
            if (testimonials.size() > 10) {
                testimonials = testimonials.subList(0, 10);
            }
            
            logger.info("Loaded {} testimonials for main page", testimonials.size());
            model.addAttribute("testimonials", testimonials);
            
            // Calculate average rating for display
            Double averageRating = reviewService.getAverageRating();
            model.addAttribute("averageRating", averageRating);
            
            return "index";
        } catch (Exception e) {
            logger.error("Error loading data for main page: {}", e.getMessage());
            // If there's an error, still return the page but with empty lists
            model.addAttribute("services", List.of());
            model.addAttribute("testimonials", List.of());
            model.addAttribute("averageRating", 0.0);
            return "index";
        }
    }
}
