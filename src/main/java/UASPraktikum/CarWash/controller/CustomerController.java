package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.service.UserService;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ServiceService serviceService;
      @Autowired
    private BookingService bookingService;
    
    @Autowired
    private TransactionService transactionService;

    private boolean isCustomer(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.CUSTOMER;
    }    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.CUSTOMER) {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            Long userId = (Long) session.getAttribute("userId");
            
            // Add null checks for session attributes
            if (userId == null || fullName == null) {
                logger.warn("Missing session data - userId: {}, fullName: {}", userId, fullName);
                return "redirect:/login";
            }
            
            try {
                User user = userService.findById(userId);
                if (user == null) {
                    logger.warn("User not found for userId: {}", userId);
                    return "redirect:/login";
                }
                  // Get booking statistics
                List<Booking> userBookings = bookingService.getBookingsByUser(user);
                if (userBookings == null) {
                    userBookings = List.of(); // Empty list if null
                }
                
                // Today's bookings
                LocalDate today = LocalDate.now();
                long todayBookings = userBookings.stream()
                    .filter(booking -> booking.getTanggal() != null && booking.getTanggal().equals(today))
                    .count();
                  // Pending bookings
                long pendingBookings = userBookings.stream()
                    .filter(booking -> booking.getStatus() != null && 
                                     (booking.getStatus() == BookingStatus.PENDING || 
                                      booking.getStatus() == BookingStatus.CONFIRMED))
                    .count();
                  // This month's completed bookings
                long thisMonthCompleted = userBookings.stream()
                    .filter(booking -> booking.getTanggal() != null && 
                                     booking.getTanggal().getMonthValue() == today.getMonthValue() &&
                                     booking.getTanggal().getYear() == today.getYear() &&
                                     booking.getStatus() == BookingStatus.COMPLETED)
                    .count();                  // Recent bookings (latest 3)
                List<Booking> recentBookings = userBookings.stream()
                    .filter(booking -> booking.getTanggal() != null)
                    .sorted((b1, b2) -> b2.getTanggal().compareTo(b1.getTanggal()))
                    .limit(3)
                    .toList();
                  // Next upcoming booking
                Booking nextBooking = userBookings.stream()
                    .filter(booking -> booking.getTanggal() != null && 
                                     booking.getTanggal().isAfter(today.minusDays(1)) &&
                                     booking.getStatus() != null &&
                                     (booking.getStatus() == BookingStatus.CONFIRMED || 
                                      booking.getStatus() == BookingStatus.PENDING))
                    .sorted((b1, b2) -> b1.getTanggal().compareTo(b2.getTanggal()))
                    .findFirst()
                    .orElse(null);
                
                model.addAttribute("todayBookings", todayBookings);
                model.addAttribute("pendingBookings", pendingBookings);
                model.addAttribute("thisMonthCompleted", thisMonthCompleted);
                model.addAttribute("recentBookings", recentBookings);
                model.addAttribute("nextBooking", nextBooking);
                
            } catch (Exception e) {
                logger.error("Error loading dashboard data: {}", e.getMessage());
                // Set default values if error occurs
                model.addAttribute("todayBookings", 0);
                model.addAttribute("pendingBookings", 0);
                model.addAttribute("thisMonthCompleted", 0);
                model.addAttribute("recentBookings", List.of());
                model.addAttribute("nextBooking", null);
            }
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Dashboard");
            model.addAttribute("section", "dashboard");
            return "customer/index";
        }
        return "redirect:/login";
    }
        @GetMapping("/booking")
    public String booking(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.CUSTOMER) {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            // Get available services
            List<Service> services = serviceService.getAllServices();
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Book Service");
            model.addAttribute("section", "booking");
            model.addAttribute("services", services);
            return "customer/booking/form";
        }
        return "redirect:/login";
    }
    
    // ====== BOOKING ENDPOINTS ======
    
    // Show new booking form
    @GetMapping("/booking/new")
    public String newBookingForm(Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");
        String fullName = (String) session.getAttribute("fullName");
        
        // Get available services
        List<Service> services = serviceService.getAllServices();
        
        model.addAttribute("email", email);
        model.addAttribute("fullName", fullName);
        model.addAttribute("title", "Book New Service");
        model.addAttribute("section", "booking");
        model.addAttribute("services", services);
        
        return "customer/booking/new-form";
    }

    // API endpoint to get available time slots for a date
    @GetMapping("/api/available-slots")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableSlots(@RequestParam String date) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            List<LocalTime> availableSlots = bookingService.getAvailableTimeSlots(bookingDate);
            
            // Convert to string format for frontend
            List<String> slotStrings = availableSlots.stream()
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm")))
                .toList();
                
            return ResponseEntity.ok(slotStrings);
        } catch (Exception e) {
            logger.error("Error getting available slots: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    // Create new booking
    @PostMapping("/booking/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBooking(@RequestParam Long serviceId,
                              @RequestParam String date,
                              @RequestParam String time,
                              @RequestParam(required = false) String notes,
                              @RequestParam(required = false) String vehicleType,
                              @RequestParam(required = false) String vehicleBrand,
                              @RequestParam(required = false) String vehicleModel,
                              @RequestParam(required = false) String licensePlate,
                              @RequestParam(required = false) String vehicleColor,
                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isCustomer(session)) {
            response.put("success", false);
            response.put("message", "Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.findById(userId);
            Service service = serviceService.getServiceById(serviceId).orElse(null);
            
            if (user == null || service == null) {
                response.put("success", false);
                response.put("message", "Invalid user or service");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate bookingDate = LocalDate.parse(date);
            LocalTime bookingTime = LocalTime.parse(time);
              // Create booking with vehicle details
            Booking booking = bookingService.createBookingWithVehicle(
                user, service, bookingDate, bookingTime, BookingMethod.BOOKING,
                notes, vehicleBrand, vehicleModel, licensePlate, vehicleColor
            );

            response.put("success", true);
            response.put("message", "Booking created successfully!");
            response.put("bookingId", booking.getFormattedBookingId());
            response.put("redirectUrl", "/customer");
              return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating booking: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to create booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }    // Show payment instructions for booking
    @GetMapping("/booking/payment/{bookingId}")
    public String showPaymentForm(@PathVariable Long bookingId, Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            if (booking == null || !booking.getUser().getUserId().equals(userId)) {
                return "redirect:/customer/bookings";
            }

            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Payment Instructions");
            model.addAttribute("section", "booking");
            model.addAttribute("booking", booking);
            
            return "customer/booking/payment-cash";
            
        } catch (Exception e) {
            logger.error("Error showing payment instructions: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }    // Alternative endpoint for cash payment instructions (matches JavaScript redirect)
    @GetMapping("/booking/payment-cash")
    public String showCashPaymentForm(@RequestParam Long bookingId, Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            if (booking == null || !booking.getUser().getUserId().equals(userId)) {
                return "redirect:/customer/bookings";
            }

            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Cash Payment Instructions");
            model.addAttribute("section", "booking");
            model.addAttribute("booking", booking);
            
            return "customer/booking/payment-cash";
            
        } catch (Exception e) {
            logger.error("Error showing cash payment instructions: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }    @GetMapping("/bookings")
    public String bookings(Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.findById(userId);
            List<Booking> bookings = bookingService.getBookingsByUser(user);
            
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "My Bookings");
            model.addAttribute("section", "bookings");
            model.addAttribute("bookings", bookings);
            
            return "customer/booking/list";
            
        } catch (Exception e) {
            logger.error("Error getting bookings: {}", e.getMessage());
            model.addAttribute("error", "Failed to load bookings");
            return "customer/booking/list";
        }
    }

    // View booking details
    @GetMapping("/booking/details/{bookingId}")
    public String bookingDetails(@PathVariable Long bookingId, Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            if (booking == null || !booking.getUser().getUserId().equals(userId)) {
                return "redirect:/customer/bookings";
            }            // Get transaction details if exists
            Transaction transaction = transactionService.getTransactionByBooking(booking).orElse(null);

            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Booking Details");
            model.addAttribute("section", "bookings");
            model.addAttribute("booking", booking);
            model.addAttribute("transaction", transaction);
            
            return "customer/booking/details";
            
        } catch (Exception e) {
            logger.error("Error getting booking details: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }    // Cancel booking endpoint (simple form POST)
    @PostMapping("/booking/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        logger.info("Cancel booking form POST for booking ID: {}", bookingId);
        
        if (!isCustomer(session)) {
            logger.warn("Unauthorized access attempt to cancel booking");
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            logger.info("User ID from session: {}", userId);
            
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            // Verify booking exists and belongs to user
            if (booking == null) {
                logger.warn("Booking not found: {}", bookingId);
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/customer/bookings";
            }
            
            if (!booking.getUser().getUserId().equals(userId)) {
                logger.warn("Access denied - booking belongs to different user");
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/bookings";
            }

            // Check if booking can be cancelled
            if (!bookingService.canBeCancelled(booking)) {
                logger.warn("Booking cannot be cancelled - status: {}", booking.getStatus());
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot cancel this booking. Only pending or confirmed bookings can be cancelled.");
                return "redirect:/customer/bookings";
            }

            // Cancel the booking
            logger.info("Cancelling booking with reason: {}", reason);
            bookingService.cancelBooking(bookingId, reason);
            
            redirectAttributes.addFlashAttribute("success", 
                "Booking has been cancelled successfully.");
            
        } catch (Exception e) {
            logger.error("Error cancelling booking: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to cancel booking: " + e.getMessage());
        }
        
        return "redirect:/customer/bookings";
    }
    
    // Alternative cancel booking endpoint that accepts POST parameter
    @PostMapping("/booking/cancel")
    public String cancelBookingAlt(@RequestParam Long bookingId,
                                  @RequestParam(value = "reason", required = false) String reason,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        logger.info("Cancel booking alternative POST for booking ID: {}", bookingId);
        
        if (!isCustomer(session)) {
            logger.warn("Unauthorized access attempt to cancel booking");
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            logger.info("User ID from session: {}", userId);
            
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            // Verify booking exists and belongs to user
            if (booking == null) {
                logger.warn("Booking not found: {}", bookingId);
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/customer/bookings";
            }
            
            if (!booking.getUser().getUserId().equals(userId)) {
                logger.warn("Access denied - booking belongs to different user");
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/bookings";
            }

            // Check if booking can be cancelled
            if (!bookingService.canBeCancelled(booking)) {
                logger.warn("Booking cannot be cancelled - status: {}", booking.getStatus());
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot cancel this booking. Only pending or confirmed bookings can be cancelled.");
                return "redirect:/customer/bookings";
            }

            // Cancel the booking
            logger.info("Cancelling booking with reason: {}", reason);
            bookingService.cancelBooking(bookingId, reason);
            
            redirectAttributes.addFlashAttribute("success", 
                "Booking has been cancelled successfully.");
            
        } catch (Exception e) {
            logger.error("Error cancelling booking: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to cancel booking: " + e.getMessage());
        }
        
        return "redirect:/customer/bookings";
    }// API endpoint for cancelling booking (for AJAX calls)
    @PostMapping("/api/booking/cancel/{bookingId}")
    @ResponseBody
    public ResponseEntity<?> cancelBookingApi(@PathVariable Long bookingId,
                                             @RequestParam(value = "reason", required = false) String reason,
                                             HttpSession session) {
        logger.info("Cancel booking API called for booking ID: {}", bookingId);
        
        if (!isCustomer(session)) {
            logger.warn("Unauthorized access attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            logger.info("User ID from session: {}", userId);
            
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            logger.info("Booking found: {}", booking != null);
            
            // Verify booking exists and belongs to user
            if (booking == null) {
                logger.warn("Booking not found: {}", bookingId);
                return ResponseEntity.status(404).body(Map.of("error", "Booking not found"));
            }
            
            if (!booking.getUser().getUserId().equals(userId)) {
                logger.warn("Access denied - booking belongs to different user");
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            logger.info("Booking status: {}", booking.getStatus());
            
            // Check if booking can be cancelled
            if (!bookingService.canBeCancelled(booking)) {
                logger.warn("Booking cannot be cancelled - status: {}", booking.getStatus());
                return ResponseEntity.status(400).body(Map.of("error", 
                    "Cannot cancel this booking. Only pending or confirmed bookings can be cancelled."));
            }

            // Cancel the booking
            logger.info("Attempting to cancel booking with reason: {}", reason);
            Booking cancelledBooking = bookingService.cancelBooking(bookingId, reason);
            logger.info("Booking cancelled successfully. New status: {}", cancelledBooking.getStatus());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Booking cancelled successfully",
                "status", cancelledBooking.getStatus().toString()
            ));
              } catch (Exception e) {
            logger.error("Error cancelling booking via API: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to cancel booking: " + e.getMessage()));
        }
    }
}
