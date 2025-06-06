package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.service.UserService;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.counting;

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
    private TransferService transferService;

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
                notes, vehicleType, vehicleBrand, vehicleModel, licensePlate, vehicleColor
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
    }

    // Show payment form for booking
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
            model.addAttribute("title", "Payment");
            model.addAttribute("section", "booking");
            model.addAttribute("booking", booking);
            
            return "customer/booking/payment";
            
        } catch (Exception e) {
            logger.error("Error showing payment form: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    // Process payment upload
    @PostMapping("/booking/payment/{bookingId}")
    public String processPayment(@PathVariable Long bookingId,
                               @RequestParam BigDecimal amount,
                               @RequestParam("proofFile") MultipartFile proofFile,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            if (booking == null || !booking.getUser().getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking");
                return "redirect:/customer/bookings";
            }

            // Validate amount
            if (amount.compareTo(booking.getService().getPrice()) != 0) {
                redirectAttributes.addFlashAttribute("error", "Payment amount does not match service price");
                return "redirect:/customer/booking/payment/" + bookingId;
            }

            // Create transfer record
            transferService.createTransfer(booking, amount, proofFile);
            
            redirectAttributes.addFlashAttribute("success", 
                "Payment proof uploaded successfully! Your booking is pending verification.");
            
            return "redirect:/customer/bookings";
            
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to process payment: " + e.getMessage());
            return "redirect:/customer/booking/payment/" + bookingId;
        }
    }

    @GetMapping("/bookings")
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

    // Cancel booking
    @PostMapping("/booking/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId, 
                              HttpSession session, 
                              RedirectAttributes redirectAttributes) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.findById(userId);
            
            boolean cancelled = bookingService.cancelBooking(bookingId, user);
            
            if (cancelled) {
                redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to cancel booking");
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to cancel booking: " + e.getMessage());
        }

        return "redirect:/customer/bookings";
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
            }

            // Get transfer details if exists
            Transfer transfer = transferService.getTransferByBooking(booking).orElse(null);

            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Booking Details");
            model.addAttribute("section", "bookings");
            model.addAttribute("booking", booking);
            model.addAttribute("transfer", transfer);
            
            return "customer/booking/details";
            
        } catch (Exception e) {
            logger.error("Error getting booking details: {}", e.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    // Update payment proof (if rejected)
    @PostMapping("/booking/update-payment/{bookingId}")
    public String updatePaymentProof(@PathVariable Long bookingId,
                                   @RequestParam("newProofFile") MultipartFile newProofFile,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.findById(userId);
            Booking booking = bookingService.getBookingById(bookingId).orElse(null);
            
            if (booking == null || !booking.getUser().getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking");
                return "redirect:/customer/bookings";
            }

            Transfer transfer = transferService.getTransferByBooking(booking).orElse(null);
            
            if (transfer == null) {
                redirectAttributes.addFlashAttribute("error", "No payment record found");
                return "redirect:/customer/bookings";
            }

            transferService.updateTransferProof(transfer.getIdTransaksi(), newProofFile, user);
            
            redirectAttributes.addFlashAttribute("success", 
                "Payment proof updated successfully! Your payment is pending verification.");
            
        } catch (Exception e) {
            logger.error("Error updating payment proof: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update payment proof: " + e.getMessage());
        }

        return "redirect:/customer/booking/details/" + bookingId;
    }

    // History and Services endpoints
    @GetMapping("/history")
    public String showHistory(Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.findById(userId);
            
            if (user == null) {
                return "redirect:/login";
            }

            // Get user's booking history
            List<Booking> allBookings = bookingService.getBookingsByUser(user);
            
            // Separate bookings by status for filtering
            List<Booking> completedBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .toList();
                
            List<Booking> cancelledBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .toList();
                  List<Booking> upcomingBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || 
                            b.getStatus() == BookingStatus.CONFIRMED ||
                            b.getStatus() == BookingStatus.IN_PROGRESS)
                .toList();            // Calculate statistics
            double totalSpent = completedBookings.stream()
                .mapToDouble(b -> b.getService().getPrice().doubleValue())
                .sum();
                
            long totalServices = completedBookings.size();
            
            // Calculate this month's services
            LocalDate now = LocalDate.now();
            long thisMonthServices = completedBookings.stream()
                .filter(b -> b.getTanggal().getMonth() == now.getMonth() && 
                           b.getTanggal().getYear() == now.getYear())
                .count();
                
            // Find favorite service (most booked completed service)
            String favoriteService = completedBookings.stream()
                .collect(groupingBy(b -> b.getService().getServiceName(), counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

            model.addAttribute("email", session.getAttribute("email"));
            model.addAttribute("fullName", session.getAttribute("fullName"));
            model.addAttribute("title", "Service History");
            model.addAttribute("section", "history");
            model.addAttribute("allBookings", allBookings);
            model.addAttribute("completedBookings", completedBookings);
            model.addAttribute("cancelledBookings", cancelledBookings);
            model.addAttribute("upcomingBookings", upcomingBookings);
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("totalServices", totalServices);
            model.addAttribute("thisMonthServices", thisMonthServices);
            model.addAttribute("favoriteService", favoriteService);
            
            return "customer/history";
            
        } catch (Exception e) {
            logger.error("Error loading history: {}", e.getMessage());
            model.addAttribute("error", "Failed to load service history");
            return "customer/history";
        }
    }

    @GetMapping("/services")
    public String showServices(Model model, HttpSession session) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            // Get available services
            List<Service> services = serviceService.getAllServices();
            
            model.addAttribute("email", session.getAttribute("email"));
            model.addAttribute("fullName", session.getAttribute("fullName"));
            model.addAttribute("title", "Our Services");
            model.addAttribute("section", "services");
            model.addAttribute("services", services);
            
            return "customer/services";
            
        } catch (Exception e) {
            logger.error("Error loading services: {}", e.getMessage());
            model.addAttribute("error", "Failed to load services");
            return "customer/services";
        }
    }

}
