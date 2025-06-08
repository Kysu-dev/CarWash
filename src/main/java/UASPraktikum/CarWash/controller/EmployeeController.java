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
import UASPraktikum.CarWash.service.TransactionService;
import UASPraktikum.CarWash.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private boolean isEmployee(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.EMPLOYEE;
    }

    // Dashboard - menampilkan booking yang perlu dikonfirmasi pembayarannya
    @GetMapping({"", "/", "/dashboard"})
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
            
            // Get bookings yang memerlukan konfirmasi pembayaran
            List<Booking> pendingPaymentBookings = bookingService.getBookingsByStatus(BookingStatus.PENDING);
            List<Booking> confirmedBookings = bookingService.getBookingsByStatus(BookingStatus.CONFIRMED);
            List<Booking> inProgressBookings = bookingService.getBookingsByStatus(BookingStatus.IN_PROGRESS);
            
            model.addAttribute("pendingPaymentBookings", pendingPaymentBookings);
            model.addAttribute("confirmedBookings", confirmedBookings);
            model.addAttribute("inProgressBookings", inProgressBookings);
            
            // Statistics
            model.addAttribute("pendingCount", pendingPaymentBookings.size());
            model.addAttribute("confirmedCount", confirmedBookings.size());
            model.addAttribute("inProgressCount", inProgressBookings.size());
            
        } catch (Exception e) {
            logger.error("Error loading employee dashboard", e);
            model.addAttribute("error", "Error loading dashboard data");
        }
        
        return "employee/index";
    }    // Halaman untuk melihat dan konfirmasi pembayaran
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
            model.addAttribute("pageTitle", "Payment Confirmations");
            model.addAttribute("section", "payments");
            
            // Get bookings dengan transaction yang perlu dikonfirmasi
            List<Booking> pendingPaymentBookings = bookingService.getBookingsByStatus(BookingStatus.PENDING);
            
            model.addAttribute("pendingPaymentBookings", pendingPaymentBookings);
            
        } catch (Exception e) {
            logger.error("Error loading payments page", e);
            model.addAttribute("error", "Error loading payment data");
        }
        
        return "employee/payments";
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
            model.addAttribute("pageTitle", "Booking Detail");
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
        
        return "employee/booking-detail";
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

    // Tolak pembayaran
    @PostMapping("/booking/{id}/reject-payment")
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
            }              // Update transaction status jika ada
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            if (transaction != null) {
                transactionService.rejectPayment(transaction.getIdTransaction(), "Employee", "Payment rejected by employee");
            }
            
            // Update booking status
            String rejectNotes = notes != null ? notes : "Payment rejected by employee";
            bookingService.updateBookingStatus(id, BookingStatus.CANCELLED, rejectNotes);
            
            redirectAttributes.addFlashAttribute("success", "Payment rejected");
            logger.info("Payment rejected for booking ID: {} by employee", id);
            
        } catch (Exception e) {
            logger.error("Error rejecting payment", e);
            redirectAttributes.addFlashAttribute("error", "Error rejecting payment");
        }
        
        return "redirect:/employee/booking/" + id;
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
}
