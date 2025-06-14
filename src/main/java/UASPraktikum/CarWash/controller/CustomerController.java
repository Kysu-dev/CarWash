package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpSession;
// Replace wildcard import with explicit imports
import UASPraktikum.CarWash.model.Booking;
import UASPraktikum.CarWash.model.BookingMethod;
import UASPraktikum.CarWash.model.BookingStatus;
import UASPraktikum.CarWash.model.Service;
import UASPraktikum.CarWash.model.Transaction;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.model.VehicleType;
import UASPraktikum.CarWash.model.PaymentMethod;
import UASPraktikum.CarWash.service.UserService;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.TransactionService;
import UASPraktikum.CarWash.service.ReviewService;
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
    private ServiceService serviceService;    @Autowired
    private BookingService bookingService;
      @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private ReviewService reviewService;

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
            return "customer/booking/form/index";
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
        
        return "customer/booking/form/new";
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
        }    }    // Create new booking    @PostMapping("/booking/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBooking(@RequestParam String serviceId,
                              @RequestParam String date,
                              @RequestParam String time,
                              @RequestParam(required = false) String notes,
                              @RequestParam(required = false) String vehicleBrand,
                              @RequestParam(required = false) String vehicleModel,
                              @RequestParam(required = false) String licensePlate,
                              @RequestParam(required = false) String vehicleColor,
                              @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate serviceId parameter
        if (serviceId == null || serviceId.trim().isEmpty() || 
            "undefined".equals(serviceId) || "null".equals(serviceId)) {
            logger.warn("Invalid serviceId received: {}", serviceId);
            response.put("success", false);
            response.put("message", "Invalid service ID provided");
            return ResponseEntity.badRequest().body(response);
        }
        
        Long parsedServiceId;
        try {
            parsedServiceId = Long.parseLong(serviceId.trim());
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse serviceId '{}' to Long: {}", serviceId, e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid service ID format");
            return ResponseEntity.badRequest().body(response);
        }
        
        logger.info("Create booking request received - serviceId: {}, date: {}, time: {}", parsedServiceId, date, time);
        logger.info("Vehicle details - brand: {}, model: {}, plate: {}, color: {}", 
                    vehicleBrand, vehicleModel, licensePlate, vehicleColor);
        
        if (!isCustomer(session)) {
            logger.warn("Booking attempt by non-customer");
            response.put("success", false);
            response.put("message", "Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            logger.info("Creating booking for user ID: {}", userId);
            
            User user = userService.findById(userId);
            Service service = serviceService.getServiceById(parsedServiceId).orElse(null);
            
            if (user == null) {
                logger.warn("User not found with ID: {}", userId);
                response.put("success", false);
                response.put("message", "User account not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (service == null) {
                logger.warn("Service not found with ID: {}", parsedServiceId);
                response.put("success", false);
                response.put("message", "Service not found");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate bookingDate;
            LocalTime bookingTime;
            
            try {
                bookingDate = LocalDate.parse(date);
                bookingTime = LocalTime.parse(time);
                logger.info("Parsed date: {}, time: {}", bookingDate, bookingTime);
            } catch (Exception e) {
                logger.error("Error parsing date/time: {} / {}", date, time, e);
                response.put("success", false);
                response.put("message", "Invalid date or time format");
                return ResponseEntity.badRequest().body(response);
            }
              // Create booking with vehicle details
            logger.info("Creating booking with vehicle details...");
            Booking booking = bookingService.createBookingWithVehicle(
                user, service, bookingDate, bookingTime, BookingMethod.BOOKING,
                notes, vehicleBrand, vehicleModel, licensePlate, vehicleColor
            );

            logger.info("Booking created successfully with ID: {}", booking.getIdBooking());
              // Create transaction automatically for the booking based on payment method
            try {
                PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(paymentMethod);
                logger.info("Creating {} payment transaction for booking ID: {}", paymentMethod, booking.getIdBooking());
                
                Transaction transaction = null;
                switch (paymentMethodEnum) {
                    case CASH:
                        transaction = transactionService.createCashPayment(
                            booking, 
                            booking.getService().getPrice(), 
                            user.getFullName()
                        );
                        break;
                    case CARD:
                        transaction = transactionService.createCardPayment(
                            booking,
                            booking.getService().getPrice(),
                            user.getFullName()
                        );
                        break;
                    case TRANSFER:
                    case E_WALLET:
                        // Create transaction with PENDING status - will require proof upload
                        transaction = new Transaction(booking, booking.getService().getPrice());
                        transaction.setPaymentMethod(paymentMethodEnum);
                        transaction = transactionService.saveTransaction(transaction);
                        break;
                }
                
                if (transaction != null) {
                    logger.info("{} transaction created successfully with ID: {}", paymentMethod, transaction.getIdTransaction());
                }
            } catch (Exception e) {
                logger.error("Failed to create transaction: {}", e.getMessage());
                // Continue, as the booking is already created
            }
              response.put("success", true);
            response.put("message", "Booking created successfully!");
            response.put("bookingId", booking.getIdBooking());
            response.put("formattedBookingId", booking.getFormattedBookingId());
            
            // Determine redirect URL based on payment method
            String redirectUrl;
            PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(paymentMethod);
            switch (paymentMethodEnum) {
                case TRANSFER:
                    redirectUrl = "/customer/booking/payment-transfer?bookingId=" + booking.getIdBooking();
                    break;
                case E_WALLET:
                    redirectUrl = "/customer/booking/payment-ewallet?bookingId=" + booking.getIdBooking();
                    break;
                case CARD:
                    redirectUrl = "/customer/booking/payment-card?bookingId=" + booking.getIdBooking();
                    break;
                case CASH:
                default:
                    redirectUrl = "/customer/booking/payment-cash?bookingId=" + booking.getIdBooking();
                    break;
            }
            response.put("redirectUrl", redirectUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating booking: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to create booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }// Show payment instructions for booking
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
            
            return "customer/booking/payment/cash";
            
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
            
            return "customer/booking/payment/cash";
            
        } catch (Exception e) {
            logger.error("Error showing cash payment instructions: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }    @GetMapping("/booking/payment-transfer")
    public String showTransferPaymentForm(@RequestParam Long bookingId, Model model, HttpSession session) {
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
            model.addAttribute("title", "Bank Transfer Payment Instructions");
            model.addAttribute("section", "booking");
            model.addAttribute("booking", booking);
            
            return "customer/booking/payment/transfer";
            
        } catch (Exception e) {
            logger.error("Error showing transfer payment instructions: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    @GetMapping("/booking/payment-card")
    public String showCardPaymentForm(@RequestParam Long bookingId, Model model, HttpSession session) {
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
            model.addAttribute("title", "Card Payment Instructions");
            model.addAttribute("section", "booking");
            model.addAttribute("booking", booking);
            
            return "customer/booking/payment/card";
            
        } catch (Exception e) {
            logger.error("Error showing card payment instructions: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    @GetMapping("/booking/payment-ewallet")
    public String showEWalletPaymentForm(@RequestParam Long bookingId, Model model, HttpSession session) {
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
            model.addAttribute("title", "E-Wallet Payment Instructions");
            model.addAttribute("section", "booking");
            model.addAttribute("booking", booking);
            
            return "customer/booking/payment/ewallet";
            
        } catch (Exception e) {
            logger.error("Error showing e-wallet payment instructions: {}", e.getMessage());
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
            
            // Tambahkan ReviewService untuk memeriksa apakah booking sudah direview
            if (bookings != null && !bookings.isEmpty()) {
                Map<Long, Boolean> reviewedBookings = new HashMap<>();
                
                for (Booking booking : bookings) {
                    // Booking hanya bisa direview jika statusnya COMPLETED
                    if (booking.getStatus() == BookingStatus.COMPLETED) {
                        boolean hasReview = reviewService.hasReview(booking.getIdBooking());
                        reviewedBookings.put(booking.getIdBooking(), hasReview);
                    }
                }
                
                model.addAttribute("reviewedBookings", reviewedBookings);
            }
            
            return "customer/booking/history/list";
            
        } catch (Exception e) {
            logger.error("Error getting bookings: {}", e.getMessage());
            model.addAttribute("error", "Failed to load bookings");
            return "customer/booking/history/list";
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
            
            return "customer/booking/details/index";
            
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
    }      // Get services by vehicle type
    @GetMapping("/api/services-by-vehicle-type")
    public ResponseEntity<?> getServicesByVehicleType(@RequestParam String vehicleType) {
        try {
            VehicleType parsedType = VehicleType.safeFromString(vehicleType);
            if (parsedType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid vehicle type: " + vehicleType));
            }
            List<Service> services = serviceService.getServicesByVehicleType(parsedType);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            logger.error("Error getting services by vehicle type: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get services: " + e.getMessage()));
        }
    }
    
    // Test endpoint to check session
    @GetMapping("/test-session")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testSession(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (session.getAttribute("userId") != null) {
            Long userId = (Long) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            String role = (String) session.getAttribute("role");
            
            response.put("loggedIn", true);
            response.put("userId", userId);
            response.put("email", email);
            response.put("role", role);
            return ResponseEntity.ok(response);
        } else {
            response.put("loggedIn", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
      // Test endpoint for booking creation that accepts form data
    @PostMapping("/booking/test-create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testCreateBooking(
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String vehicleBrand,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String vehicleColor,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
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
            
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Service service = null;
            LocalDate bookingDate = null;
            LocalTime bookingTime = null;
            
            // Use provided values if available, otherwise use defaults
            if (serviceId != null) {
                service = serviceService.getServiceById(serviceId).orElse(null);
            }
            
            if (service == null) {
                // Fallback to first available service
                List<Service> services = serviceService.getAllServices();
                if (services.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "No services available");
                    return ResponseEntity.badRequest().body(response);
                }
                service = services.get(0);
            }
            
            try {
                bookingDate = date != null ? LocalDate.parse(date) : LocalDate.now().plusDays(1);
                bookingTime = time != null ? LocalTime.parse(time) : LocalTime.of(10, 0);
            } catch (Exception e) {
                bookingDate = LocalDate.now().plusDays(1);
                bookingTime = LocalTime.of(10, 0);
            }
            
            // Create a booking with the provided or default values
            Booking booking = bookingService.createBookingWithVehicle(
                user, service, bookingDate, bookingTime, BookingMethod.BOOKING,
                notes != null ? notes : "Booking from form", 
                vehicleBrand != null ? vehicleBrand : "Default Brand", 
                vehicleModel != null ? vehicleModel : "Default Model", 
                licensePlate != null ? licensePlate : "DEFAULT", 
                vehicleColor != null ? vehicleColor : "Default Color"
            );
              // Create transaction based on payment method
            try {
                PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(paymentMethod);
                logger.info("Creating {} payment transaction for booking ID: {}", paymentMethod, booking.getIdBooking());
                
                Transaction transaction = null;
                switch (paymentMethodEnum) {
                    case CASH:
                        transaction = transactionService.createCashPayment(
                            booking, 
                            booking.getService().getPrice(), 
                            user.getFullName()
                        );
                        break;
                    case CARD:
                        transaction = transactionService.createCardPayment(
                            booking,
                            booking.getService().getPrice(),
                            user.getFullName()
                        );
                        break;
                    case TRANSFER:
                    case E_WALLET:
                        // Create transaction with PENDING status - will require proof upload
                        transaction = new Transaction(booking, booking.getService().getPrice());
                        transaction.setPaymentMethod(paymentMethodEnum);
                        transaction = transactionService.saveTransaction(transaction);
                        break;
                }
                
                if (transaction != null) {
                    logger.info("{} transaction created successfully with ID: {}", paymentMethod, transaction.getIdTransaction());
                }
            } catch (Exception e) {
                logger.error("Failed to create transaction: {}", e.getMessage(), e);
                // Continue, as the booking is already created
            }
            
            response.put("success", true);
            response.put("message", "Booking created successfully!");
            response.put("bookingId", booking.getIdBooking());
            response.put("formattedBookingId", booking.getFormattedBookingId());
            
            // Determine redirect URL based on payment method
            String redirectUrl;
            PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(paymentMethod);
            switch (paymentMethodEnum) {
                case TRANSFER:
                    redirectUrl = "/customer/booking/payment-transfer?bookingId=" + booking.getIdBooking();
                    break;
                case E_WALLET:
                    redirectUrl = "/customer/booking/payment-ewallet?bookingId=" + booking.getIdBooking();
                    break;
                case CARD:
                    redirectUrl = "/customer/booking/payment-card?bookingId=" + booking.getIdBooking();
                    break;
                case CASH:
                default:
                    redirectUrl = "/customer/booking/payment-cash?bookingId=" + booking.getIdBooking();
                    break;
            }
            response.put("redirectUrl", redirectUrl);return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating test booking: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to create test booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // GET endpoint for booking creation page (for direct access)
    @GetMapping("/booking/create")
    public String showBookingCreatePage(HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }
        // Redirect to the booking form instead
        return "redirect:/customer/services";
    }      // POST endpoint for booking creation (alternative endpoint)
    @PostMapping("/booking/create-alternative")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBookingEndpoint(
            @RequestParam String serviceId,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String vehicleBrand,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String vehicleColor,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            HttpSession session) {
            
        Map<String, Object> response = new HashMap<>();
        
        // Validate serviceId parameter
        if (serviceId == null || serviceId.trim().isEmpty() || 
            "undefined".equals(serviceId) || "null".equals(serviceId)) {
            logger.warn("Invalid serviceId received: {}", serviceId);
            response.put("success", false);
            response.put("message", "Invalid service ID provided");
            return ResponseEntity.badRequest().body(response);
        }
        
        Long parsedServiceId;
        try {
            parsedServiceId = Long.parseLong(serviceId.trim());
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse serviceId '{}' to Long: {}", serviceId, e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid service ID format");
            return ResponseEntity.badRequest().body(response);
        }
        
        logger.info("Create booking POST request received - serviceId: {}, date: {}, time: {}, payment: {}", 
                    parsedServiceId, date, time, paymentMethod);
        logger.info("Vehicle details - brand: {}, model: {}, plate: {}, color: {}", 
                    vehicleBrand, vehicleModel, licensePlate, vehicleColor);
        
        if (!isCustomer(session)) {
            logger.warn("Booking attempt by non-customer");
            response.put("success", false);
            response.put("message", "Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            logger.info("Creating booking for user ID: {}", userId);
            
            User user = userService.findById(userId);
            Service service = serviceService.getServiceById(parsedServiceId).orElse(null);
            
            if (user == null) {
                logger.warn("User not found with ID: {}", userId);
                response.put("success", false);
                response.put("message", "User account not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (service == null) {
                logger.warn("Service not found with ID: {}", parsedServiceId);
                response.put("success", false);
                response.put("message", "Service not found");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate bookingDate;
            LocalTime bookingTime;
            
            try {
                bookingDate = LocalDate.parse(date);
                bookingTime = LocalTime.parse(time);
                logger.info("Parsed date: {}, time: {}", bookingDate, bookingTime);
            } catch (Exception e) {
                logger.error("Error parsing date/time: {} / {}", date, time, e);
                response.put("success", false);
                response.put("message", "Invalid date or time format");
                return ResponseEntity.badRequest().body(response);
            }
              // Create booking with vehicle details
            logger.info("Creating booking with vehicle details and payment method: {}", paymentMethod);
            Booking booking = bookingService.createBookingWithVehicle(
                user, service, bookingDate, bookingTime, BookingMethod.BOOKING,
                notes, vehicleBrand, vehicleModel, licensePlate, vehicleColor
            );

            logger.info("Booking created successfully with ID: {}", booking.getIdBooking());
              // Create transaction automatically for the booking based on payment method
            try {
                PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(paymentMethod);
                logger.info("Creating {} payment transaction for booking ID: {}", paymentMethod, booking.getIdBooking());
                
                Transaction transaction = null;
                switch (paymentMethodEnum) {
                    case CASH:
                        transaction = transactionService.createCashPayment(
                            booking, 
                            booking.getService().getPrice(), 
                            user.getFullName()
                        );
                        break;
                    case CARD:
                        transaction = transactionService.createCardPayment(
                            booking,
                            booking.getService().getPrice(),
                            user.getFullName()
                        );
                        break;
                    case TRANSFER:
                    case E_WALLET:
                        // Create transaction with PENDING status - will require proof upload
                        transaction = new Transaction(booking, booking.getService().getPrice());
                        transaction.setPaymentMethod(paymentMethodEnum);
                        transaction = transactionService.saveTransaction(transaction);
                        break;
                }
                
                if (transaction != null) {
                    logger.info("{} transaction created successfully with ID: {}", paymentMethod, transaction.getIdTransaction());
                }
            } catch (Exception e) {
                logger.error("Failed to create transaction: {}", e.getMessage(), e);
                // Continue, as the booking is already created
            }
              response.put("success", true);
            response.put("message", "Booking created successfully!");
            response.put("bookingId", booking.getIdBooking());
            response.put("formattedBookingId", booking.getFormattedBookingId());
            
            // Determine redirect URL based on payment method
            String redirectUrl;
            PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(paymentMethod);
            switch (paymentMethodEnum) {
                case TRANSFER:
                    redirectUrl = "/customer/booking/payment-transfer?bookingId=" + booking.getIdBooking();
                    break;
                case E_WALLET:
                    redirectUrl = "/customer/booking/payment-ewallet?bookingId=" + booking.getIdBooking();
                    break;
                case CARD:
                    redirectUrl = "/customer/booking/payment-card?bookingId=" + booking.getIdBooking();
                    break;
                case CASH:
                default:
                    redirectUrl = "/customer/booking/payment-cash?bookingId=" + booking.getIdBooking();
                    break;
            }
            response.put("redirectUrl", redirectUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating booking: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to create booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // ====== PROFILE ENDPOINTS ======
    
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }
        
        try {
            Long userId = (Long) session.getAttribute("userId");
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            User user = userService.findById(userId);
            
            if (user == null) {
                logger.error("User not found for userId: {}", userId);
                return "redirect:/login";
            }
            
            model.addAttribute("user", user);
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "My Profile");
            model.addAttribute("section", "profile");
            
            return "customer/profile/edit";
        } catch (Exception e) {
            logger.error("Error loading profile page: {}", e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }
    
    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String fullName, 
                              @RequestParam(required = false) String phoneNumber,
                              @RequestParam(required = false) String address,
                              HttpSession session, 
                              RedirectAttributes redirectAttributes) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }
        
        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.findById(userId);
            
            if (user == null) {
                logger.error("User not found for userId: {}", userId);
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/profile";
            }
            
            // Update user details if provided
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName);
                session.setAttribute("fullName", fullName);
            }
            
            if (phoneNumber != null) {
                user.setPhoneNumber(phoneNumber);
            }
            
            if (address != null) {
                user.setAddress(address);
            }
            
            userService.updateUser(user);
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/customer/profile";
            
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/customer/profile";
        }
    }
}
