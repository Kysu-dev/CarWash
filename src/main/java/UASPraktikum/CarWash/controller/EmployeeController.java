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
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.service.TransactionService;
import UASPraktikum.CarWash.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);    
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ServiceService serviceService;
    
    private boolean isEmployee(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.EMPLOYEE;
    }    // Dashboard - menampilkan booking yang perlu dikonfirmasi pembayarannya
    @GetMapping({"", "/", "/dashboard", "/index"})
    public String dashboard(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Employee Dashboard");
            model.addAttribute("section", "dashboard");
            
            // Today's date
            LocalDate today = LocalDate.now();
            
            // Get bookings by status
            List<Booking> pendingPaymentBookings = bookingService.getBookingsByStatus(BookingStatus.PENDING);
            List<Booking> confirmedBookings = bookingService.getBookingsByStatus(BookingStatus.CONFIRMED);
            List<Booking> inProgressBookings = bookingService.getBookingsByStatus(BookingStatus.IN_PROGRESS);
            List<Booking> completedBookings = bookingService.getBookingsByStatus(BookingStatus.COMPLETED);
            List<Booking> paidBookings = bookingService.getBookingsByStatusAndDate(BookingStatus.PAID, today);
            
            // Get today's bookings for quick overview
            List<Booking> todayBookings = bookingService.getBookingsByDate(today);
            
            model.addAttribute("pendingPaymentBookings", pendingPaymentBookings);
            model.addAttribute("confirmedBookings", confirmedBookings);
            model.addAttribute("inProgressBookings", inProgressBookings);
            model.addAttribute("completedBookings", completedBookings);
            model.addAttribute("todayBookings", todayBookings);
            
            // Statistics
            model.addAttribute("pendingCount", pendingPaymentBookings.size());
            model.addAttribute("confirmedCount", confirmedBookings.size());
            model.addAttribute("inProgressCount", inProgressBookings.size());
            model.addAttribute("completedCount", completedBookings.size());
            model.addAttribute("paidTodayCount", paidBookings.size());
            model.addAttribute("todayCount", todayBookings.size());
            
        } catch (Exception e) {
            logger.error("Error loading employee dashboard", e);
            model.addAttribute("error", "Error loading dashboard data");
        }
          return "employee/index";
    }
    
    // Halaman untuk melihat dan konfirmasi pembayaran
    @GetMapping("/payments")
    public String paymentsPage(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Konfirmasi Pembayaran");
            model.addAttribute("section", "payments");
            
            // Get bookings dengan transaction yang perlu dikonfirmasi
            List<Booking> pendingPaymentBookings = bookingService.getBookingsByStatus(BookingStatus.PENDING);
            
            model.addAttribute("pendingPaymentBookings", pendingPaymentBookings);
            
        } catch (Exception e) {
            logger.error("Error loading payments page", e);
            model.addAttribute("error", "Error loading payment data");
        }
          return "employee/payments/list";
    }
    
    // Detail booking untuk konfirmasi pembayaran
    @GetMapping("/booking/{id}")
    public String viewBookingDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Detail Booking");
            model.addAttribute("section", "bookings");
            
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                return "redirect:/employee/payments?error=Booking not found";
            }
              // Get transaction information
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            
            model.addAttribute("booking", booking);
            model.addAttribute("transaction", transaction);
            model.addAttribute("statuses", BookingStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading booking detail", e);
            return "redirect:/employee/payments?error=Error loading booking";
        }
        
        return "employee/bookings/details";
    }

    // Konfirmasi pembayaran dan ubah status menjadi CONFIRMED
    @PostMapping("/booking/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes,
                                HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/payments";
            }
            
            if (booking.getStatus() != BookingStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "Only pending bookings can be confirmed");
                return "redirect:/employee/booking/" + id;
            }
                String employeeName = (String) session.getAttribute("fullName");
            
            // Check if transaction already exists (for online bookings)
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            if (transaction != null) {
                // For online bookings that already have transaction record (but pending)
                transactionService.verifyPayment(transaction.getIdTransaction(), employeeName, "Payment confirmed by cashier");
            } else {
                // For walk-in customers or online bookings without transaction record
                transactionService.createCashPayment(booking, booking.getService().getPrice(), employeeName);
            }
            
            // Update booking status
            String confirmNotes = notes != null ? notes : "Payment confirmed by cashier";
            bookingService.updateBookingStatus(id, BookingStatus.CONFIRMED, confirmNotes);
            
            redirectAttributes.addFlashAttribute("success", "Payment confirmed successfully");
            logger.info("Payment confirmed for booking ID: {} by employee: {}", id, employeeName);
            
        } catch (Exception e) {
            logger.error("Error confirming payment", e);
            redirectAttributes.addFlashAttribute("error", "Error confirming payment: " + e.getMessage());
        }
        
        return "redirect:/employee/booking/" + id;
    }

    // Tolak pembayaran    @PostMapping("/booking/{id}/reject-payment")
    public String rejectPayment(@PathVariable Long id,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/payments";
            }
            
            // Update transaction status jika ada
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            if (transaction != null) {
                transactionService.rejectPayment(transaction.getIdTransaction(), "Employee", "Payment rejected by employee");
            }
            
            // Update booking status
            String rejectNotes = notes != null ? notes : "Payment rejected by employee";
            bookingService.updateBookingStatus(id, BookingStatus.CANCELLED, rejectNotes);
            
            redirectAttributes.addFlashAttribute("success", "Pembayaran ditolak dan booking dibatalkan");
            logger.info("Payment rejected for booking ID: {} by employee", id);
            
        } catch (Exception e) {
            logger.error("Error rejecting payment", e);
            redirectAttributes.addFlashAttribute("error", "Error rejecting payment");
        }
        
        return "redirect:/employee/payments";
    }    // Mulai layanan (ubah status menjadi IN_PROGRESS)
    @PostMapping("/booking/{id}/start")
    public String startBooking(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            String employeeEmail = (String) session.getAttribute("email");
            User employee = userService.findByEmail(employeeEmail);
            
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/dashboard";
            }
            
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                redirectAttributes.addFlashAttribute("error", "Booking must be confirmed before starting");
                return "redirect:/employee/dashboard";
            }
            
            // Update status to IN_PROGRESS
            bookingService.updateBookingStatus(id, BookingStatus.IN_PROGRESS);
            
            // Add employee note
            String currentNotes = booking.getCatatan() != null ? booking.getCatatan() : "";
            String newNote = "\n[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "] " +
                           "Started by employee: " + employee.getFullName();
            bookingService.updateBookingNotes(id, currentNotes + newNote);
            
            redirectAttributes.addFlashAttribute("success", "Booking started successfully");
            
        } catch (Exception e) {
            logger.error("Error starting booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to start booking: " + e.getMessage());
        }
        
        return "redirect:/employee/dashboard";
    }
    
    // Update booking status - from IN_PROGRESS to COMPLETED
    @PostMapping("/booking/{id}/complete")
    public String completeBooking(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            String employeeEmail = (String) session.getAttribute("email");
            User employee = userService.findByEmail(employeeEmail);
            
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/dashboard";
            }
            
            if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
                redirectAttributes.addFlashAttribute("error", "Booking must be in progress before completing");
                return "redirect:/employee/dashboard";
            }
            
            // Update status to COMPLETED
            bookingService.updateBookingStatus(id, BookingStatus.COMPLETED);
            
            // Add employee note
            String currentNotes = booking.getCatatan() != null ? booking.getCatatan() : "";
            String newNote = "\n[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "] " +
                           "Completed by employee: " + employee.getFullName();
            bookingService.updateBookingNotes(id, currentNotes + newNote);
            
            redirectAttributes.addFlashAttribute("success", "Booking completed successfully. Customer can now pay.");
            
        } catch (Exception e) {
            logger.error("Error completing booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to complete booking: " + e.getMessage());
        }
        
        return "redirect:/employee/dashboard";
    }
    
    // Process cash payment - from COMPLETED to PAID (Kasir function)
    @PostMapping("/booking/{id}/payment")
    public String processPayment(@PathVariable Long id, 
                               @RequestParam("paymentMethod") String paymentMethod,
                               HttpSession session, 
                               RedirectAttributes redirectAttributes) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            String employeeEmail = (String) session.getAttribute("email");
            User employee = userService.findByEmail(employeeEmail);
            
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/dashboard";
            }
            
            if (booking.getStatus() != BookingStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("error", "Service must be completed before payment");
                return "redirect:/employee/dashboard";
            }
            
            // Create cash payment transaction
            java.math.BigDecimal amount = booking.getService().getPrice();
            
            if ("CASH".equals(paymentMethod)) {
                transactionService.createCashPayment(booking, amount, employee.getFullName());
            } else if ("CARD".equals(paymentMethod)) {
                transactionService.createCardPayment(booking, amount, employee.getFullName());
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid payment method");
                return "redirect:/employee/dashboard";
            }
            
            // Update status to PAID
            bookingService.updateBookingStatus(id, BookingStatus.PAID);
            
            // Add payment note
            String currentNotes = booking.getCatatan() != null ? booking.getCatatan() : "";
            String newNote = "\n[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "] " +
                           "Payment received (" + paymentMethod + ") by cashier: " + employee.getFullName();
            bookingService.updateBookingNotes(id, currentNotes + newNote);
            
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to process payment: " + e.getMessage());
        }
        
        return "redirect:/employee/dashboard";
    }
    
    // Get today's work queue (bookings for today ordered by time)
    @GetMapping("/work-queue")
    public String workQueue(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
          try {
            // Get today's bookings
            java.time.LocalDate today = java.time.LocalDate.now();
            List<Booking> todayBookings = bookingService.getBookingsByDate(today);
            
            // Add section for navigation
            model.addAttribute("section", "work-queue");
            model.addAttribute("pageTitle", "Work Queue");
            
            // Group by status for better organization
            Map<BookingStatus, List<Booking>> bookingsByStatus = new HashMap<>();
            for (BookingStatus status : BookingStatus.values()) {
                bookingsByStatus.put(status, new ArrayList<>());
            }
              for (Booking booking : todayBookings) {
                bookingsByStatus.get(booking.getStatus()).add(booking);
            }
            
            // Add individual status lists to model for easy access in template
            model.addAttribute("bookedBookings", bookingsByStatus.get(BookingStatus.CONFIRMED));
            model.addAttribute("inProgressBookings", bookingsByStatus.get(BookingStatus.IN_PROGRESS));
            model.addAttribute("completedBookings", bookingsByStatus.get(BookingStatus.COMPLETED));
            model.addAttribute("paidBookings", bookingsByStatus.get(BookingStatus.PAID));
            
            // Add counts
            model.addAttribute("bookedCount", bookingsByStatus.get(BookingStatus.CONFIRMED).size());
            model.addAttribute("inProgressCount", bookingsByStatus.get(BookingStatus.IN_PROGRESS).size());
            model.addAttribute("completedCount", bookingsByStatus.get(BookingStatus.COMPLETED).size());
            model.addAttribute("paidCount", bookingsByStatus.get(BookingStatus.PAID).size());
            model.addAttribute("totalBookings", todayBookings.size());
            
            model.addAttribute("bookingsByStatus", bookingsByStatus);
            model.addAttribute("todayDate", today);
            
        } catch (Exception e) {
            logger.error("Error loading work queue: {}", e.getMessage());
            model.addAttribute("error", "Failed to load work queue");
        }
        
        return "employee/work-queue";
    }
    
    // Get booking details (AJAX endpoint)
    @GetMapping("/booking/{id}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBookingDetails(@PathVariable Long id, HttpSession session) {
        if (!isEmployee(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", booking.getIdBooking());
            response.put("customerName", booking.getUser().getFullName());
            response.put("customerPhone", booking.getUser().getPhoneNumber());
            response.put("serviceName", booking.getService().getServiceName());
            response.put("price", booking.getService().getPrice());
            response.put("date", booking.getTanggal().toString());
            response.put("time", booking.getJam().toString());
            response.put("status", booking.getStatus().toString());
            response.put("vehicleInfo", booking.getVehicleDisplayName());
            response.put("licensePlate", booking.getLicensePlate());
            response.put("notes", booking.getCatatan());
            
            return ResponseEntity.ok(response);
              } catch (Exception e) {
            logger.error("Error getting booking details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Walk-in booking form
    @GetMapping("/create-booking")
    public String createBookingForm(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            model.addAttribute("pageTitle", "Create Walk-in Booking");
            model.addAttribute("section", "create-booking");
              // Get list of all services for the form
            List<UASPraktikum.CarWash.model.Service> services = serviceService.getAllServices();
            model.addAttribute("services", services);
            
            // Today's date for the form
            model.addAttribute("today", LocalDate.now().toString());
            
        } catch (Exception e) {
            logger.error("Error loading create booking form: {}", e.getMessage());
            model.addAttribute("error", "Failed to load booking form: " + e.getMessage());
        }
        
        return "employee/create-booking";
    }

    // Process walk-in booking
    @PostMapping("/create-booking")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createWalkInBooking(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerPhone") String customerPhone,
            @RequestParam("customerEmail") String customerEmail,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam("bookingDate") String bookingDateStr,
            @RequestParam("bookingTime") String bookingTimeStr,
            @RequestParam("vehicleBrand") String vehicleBrand,
            @RequestParam("vehicleModel") String vehicleModel,
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam("vehicleColor") String vehicleColor,
            @RequestParam(value = "notes", required = false) String notes,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!isEmployee(session)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            // Parse date and time
            LocalDate bookingDate = LocalDate.parse(bookingDateStr);
            LocalTime bookingTime = LocalTime.parse(bookingTimeStr);
            
            // Get service
            UASPraktikum.CarWash.model.Service service = serviceService.getServiceById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
            
            // Check if time slot is available
            if (!bookingService.isSlotAvailable(bookingDate, bookingTime)) {
                response.put("success", false);
                response.put("message", "Time slot is not available");
                return ResponseEntity.ok(response);
            }
            
            // Create or get user for walk-in customer
            User user = userService.findByEmail(customerEmail);
            if (user == null) {
                // Create new walk-in user with minimum details
                user = new User();
                user.setEmail(customerEmail);
                user.setFullName(customerName);
                user.setPhoneNumber(customerPhone);
                user.setPasswordHash("walkIn" + System.currentTimeMillis()); // Generate random password
                user.setRole(UserRole.CUSTOMER);
                user.setIsActive(true);
                
                user = userService.saveUser(user);
                logger.info("Created new walk-in user: {}", user.getUserId());
            }
            
            // Create booking for walk-in customer
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setService(service);
            booking.setTanggal(bookingDate);
            booking.setJam(bookingTime);
            booking.setMetode(BookingMethod.WALKIN);
            booking.setStatus(BookingStatus.CONFIRMED); // Walk-in bookings are automatically confirmed
            booking.setCatatan("Walk-in booking created by employee: " + session.getAttribute("fullName"));
            booking.setVehicleBrand(vehicleBrand);
            booking.setVehicleModel(vehicleModel);
            booking.setLicensePlate(licensePlate);
            booking.setVehicleColor(vehicleColor);
            
            if (notes != null && !notes.trim().isEmpty()) {
                booking.setCatatan(booking.getCatatan() + "\n\nNotes: " + notes);
            }
            
            Booking savedBooking = bookingService.saveBooking(booking);
            
            response.put("success", true);
            response.put("message", "Walk-in booking created successfully");
            response.put("bookingId", savedBooking.getIdBooking());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating walk-in booking: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to create booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // AJAX endpoint to confirm payment
    @PostMapping("/booking/{id}/ajax-confirm-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ajaxConfirmPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!isEmployee(session)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (booking.getStatus() != BookingStatus.PENDING) {
                response.put("success", false);
                response.put("message", "Only pending bookings can be confirmed");
                return ResponseEntity.ok(response);
            }
            
            String employeeName = (String) session.getAttribute("fullName");
            
            // Check if transaction exists
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            if (transaction != null) {
                // For online bookings with existing transaction
                transactionService.verifyPayment(transaction.getIdTransaction(), employeeName, 
                                               "Payment confirmed by employee: " + employeeName);
            } else {
                // For walk-in customers or online bookings without transaction record
                transactionService.createCashPayment(booking, booking.getService().getPrice(), employeeName);
            }
            
            // Update booking status
            String confirmNotes = notes != null && !notes.trim().isEmpty() ? 
                                 notes : "Payment confirmed by employee: " + employeeName;
            bookingService.updateBookingStatus(id, BookingStatus.CONFIRMED, confirmNotes);
            
            response.put("success", true);
            response.put("message", "Payment confirmed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error confirming payment: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Error confirming payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // AJAX endpoint untuk konfirmasi pembayaran
    @PostMapping("/api/confirm-payment/{id}")
    @ResponseBody
    public Map<String, Object> confirmPaymentAPI(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (!isEmployee(session)) {
            response.put("success", false);
            response.put("message", "Unauthorized access");
            return response;
        }
        
        try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                response.put("success", false);
                response.put("message", "Booking not found");
                return response;
            }
            
            if (booking.getStatus() != BookingStatus.PENDING) {
                response.put("success", false);
                response.put("message", "Only pending bookings can be confirmed");
                return response;
            }
            
            String employeeName = (String) session.getAttribute("fullName");
            String notes = payload.getOrDefault("notes", "Payment confirmed by cashier");
            
            // Check if transaction already exists (for online bookings)
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            if (transaction != null) {
                // For online bookings that already have transaction record (but pending)
                transactionService.verifyPayment(transaction.getIdTransaction(), employeeName, notes);
            } else {
                // For walk-in customers or online bookings tanpa transaction record
                transactionService.createCashPayment(booking, booking.getService().getPrice(), employeeName);
            }
            
            // Update booking status
            bookingService.updateBookingStatus(id, BookingStatus.CONFIRMED, notes);
            
            logger.info("Payment confirmed for booking ID: {} by employee: {}", id, employeeName);
            
            response.put("success", true);
            response.put("message", "Pembayaran berhasil dikonfirmasi");
            
        } catch (Exception e) {
            logger.error("Error confirming payment via API", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return response;
    }
      // AJAX endpoint to update booking status
    @PostMapping("/api/booking/{id}/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateBookingStatusAPI(
            @PathVariable Long id,
            @RequestParam("status") String statusStr,
            @RequestParam(value = "notes", required = false) String notes,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (!isEmployee(session)) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            // Parse the status
            BookingStatus newStatus;
            try {
                newStatus = BookingStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("message", "Invalid status: " + statusStr);
                return ResponseEntity.ok(response);
            }
            
            // Validate status transition
            if (!isValidStatusTransition(booking.getStatus(), newStatus)) {
                response.put("success", false);
                response.put("message", "Invalid status transition from " + booking.getStatus() + " to " + newStatus);
                return ResponseEntity.ok(response);
            }
            
            String employeeName = (String) session.getAttribute("fullName");
            String statusNotes = notes != null && !notes.trim().isEmpty() ? 
                               notes : "Status updated to " + newStatus + " by employee: " + employeeName;
            
            // Update booking status
            bookingService.updateBookingStatus(id, newStatus, statusNotes);
            
            // Handle additional actions based on new status
            if (newStatus == BookingStatus.COMPLETED) {
                // For completed bookings, ensure there's a transaction
                if (transactionService.getTransactionByBookingId(id) == null) {
                    // Create a transaction for cash payment
                    transactionService.createCashPayment(booking, booking.getService().getPrice(), employeeName);
                }
            }
            
            response.put("success", true);
            response.put("message", "Booking status updated successfully to " + newStatus);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating booking status: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Error updating status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Payment details page for viewing and confirming payment
    @GetMapping("/payments/details/{id}")
    public String paymentDetailsPage(@PathVariable Long id, Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Detail Pembayaran");
            model.addAttribute("section", "payments");
            
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                return "redirect:/employee/payments?error=Booking not found";
            }
            
            // Get transaction information
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            
            model.addAttribute("booking", booking);
            model.addAttribute("transaction", transaction);
            
        } catch (Exception e) {
            logger.error("Error loading payment details", e);
            return "redirect:/employee/payments?error=Error loading payment details";
        }
        
        return "employee/payments/details";
    }    // Non-AJAX endpoint to update booking status from the form
    @PostMapping("/booking/{id}/update-status")
    public String updateBookingStatusForm(@PathVariable Long id,
                                    @RequestParam String status,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/work-queue";
            }
            
            // Validate status transition
            BookingStatus newStatus = BookingStatus.valueOf(status);
            BookingStatus currentStatus = booking.getStatus();
            
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                redirectAttributes.addFlashAttribute("error", "Invalid status transition from " + currentStatus + " to " + newStatus);
                return "redirect:/employee/booking/" + id;
            }
            
            // Update booking status
            String employeeName = (String) session.getAttribute("fullName");
            String statusNotes = notes != null ? notes : "Status updated to " + newStatus + " by " + employeeName;
            bookingService.updateBookingStatus(id, newStatus, statusNotes);
            
            redirectAttributes.addFlashAttribute("success", "Status booking berhasil diubah menjadi " + newStatus);
            logger.info("Status updated for booking ID: {} to {}", id, newStatus);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value", e);
            redirectAttributes.addFlashAttribute("error", "Invalid status value");
        } catch (Exception e) {
            logger.error("Error updating booking status", e);
            redirectAttributes.addFlashAttribute("error", "Error updating booking status: " + e.getMessage());
        }
        
        return "redirect:/employee/booking/" + id;
    }
    
    // Reject payment endpoint
    @PostMapping("/payments/reject/{id}")
    public String rejectPaymentFromList(@PathVariable Long id,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/payments";
            }
            
            // Update transaction status if it exists
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            if (transaction != null) {
                String employeeName = (String) session.getAttribute("fullName");
                transactionService.rejectPayment(transaction.getIdTransaction(), employeeName, notes);
            }
            
            // Update booking status
            String rejectNotes = notes != null ? notes : "Payment rejected by employee";
            bookingService.updateBookingStatus(id, BookingStatus.CANCELLED, rejectNotes);
            
            redirectAttributes.addFlashAttribute("success", "Pembayaran ditolak dan booking dibatalkan");
            logger.info("Payment rejected for booking ID: {} by employee", id);
            
        } catch (Exception e) {
            logger.error("Error rejecting payment", e);
            redirectAttributes.addFlashAttribute("error", "Error rejecting payment: " + e.getMessage());
        }
        
        return "redirect:/employee/payments";
    }
    
    // Helper method to validate status transitions
    private boolean isValidStatusTransition(BookingStatus currentStatus, BookingStatus newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == BookingStatus.CONFIRMED;
            case CONFIRMED:
                return newStatus == BookingStatus.IN_PROGRESS;
            case IN_PROGRESS:
                return newStatus == BookingStatus.COMPLETED;
            default:
                return false;
        }
    }
}
