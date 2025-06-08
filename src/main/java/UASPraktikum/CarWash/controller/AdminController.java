package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.service.UserService;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);    @Autowired
    private UserService userService;

    @Autowired
    private ServiceService serviceService;
      @Autowired
    private BookingService bookingService;
    
    @Autowired
    private TransactionService transactionService;

    private boolean isAdmin(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.ADMIN;
    }    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }        
        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            int totalUsers = userService.getAllUsers().size();
            int totalServices = serviceService.getAllServices().size();
            
            // Get booking statistics
            long totalBookings = bookingService.getTotalBookings();
            long pendingBookings = bookingService.getBookingsByStatus(BookingStatus.PENDING).size();
            long todayBookings = bookingService.getBookingsByDate(LocalDate.now()).size();
            long pendingPayments = transactionService.getTransactionsByStatus(PaymentStatus.PENDING).size();

            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("section", "dashboard");
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalServices", totalServices);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("todayBookings", todayBookings);
            model.addAttribute("pendingPayments", pendingPayments);
            
            // Get recent bookings for dashboard
            List<Booking> recentBookings = bookingService.getRecentBookings(5);
            model.addAttribute("recentBookings", recentBookings);
            
            return "admin/index";
        } catch (Exception e) {
            logger.error("Error loading dashboard: {}", e.getMessage());
            return "redirect:/login?error=Failed to load dashboard";
        }
    }@GetMapping("/users")
    public String users(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String fullName = (String) session.getAttribute("fullName");
        model.addAttribute("fullName", fullName);
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("section", "users");
        model.addAttribute("users", userService.getAllUsers());
        
        return "admin/user/list";
    }    @GetMapping("/users/add")
    public String addUserForm(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String fullName = (String) session.getAttribute("fullName");
        model.addAttribute("fullName", fullName);
        model.addAttribute("pageTitle", "Add New User");
        model.addAttribute("section", "users");
        model.addAttribute("user", new User());
        
        return "admin/user/form";
    }    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }

        String fullName = (String) session.getAttribute("fullName");
        model.addAttribute("fullName", fullName);
        model.addAttribute("pageTitle", "Edit User");
        model.addAttribute("section", "users");
        model.addAttribute("user", user);
        
        return "admin/user/form";
    }

    @PostMapping("/users")
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) String password, @RequestParam(required = true) String role, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            // Set the role from the form input
            try {
                user.setRole(UserRole.valueOf(role));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid role value: {}", role);
                return "redirect:/admin/users?error=Invalid role selected";
            }

            if (user.getUserId() == null) {
                // Creating new user
                if (password == null || password.trim().isEmpty()) {
                    logger.error("Password is required for new users");
                    return "redirect:/admin/users/add?error=Password is required";
                }

                // Check if username or email already exists
                if (userService.findByUsername(user.getUsername()) != null) {
                    logger.error("Username already exists: {}", user.getUsername());
                    return "redirect:/admin/users/add?error=Username already exists";
                }
                if (userService.findByEmail(user.getEmail()) != null) {
                    logger.error("Email already exists: {}", user.getEmail());
                    return "redirect:/admin/users/add?error=Email already exists";
                }

                userService.registerNewUser(
                    user.getUsername(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getFullName(),
                    password
                );
                
                // Update the role after registration
                User createdUser = userService.findByEmail(user.getEmail());
                if (createdUser != null) {
                    createdUser.setRole(UserRole.valueOf(role));
                    userService.save(createdUser);
                }
            } else {
                // Updating existing user
                User existingUser = userService.findById(user.getUserId());
                if (existingUser != null) {
                    existingUser.setUsername(user.getUsername());
                    existingUser.setEmail(user.getEmail());
                    existingUser.setPhoneNumber(user.getPhoneNumber());
                    existingUser.setFullName(user.getFullName());
                    existingUser.setRole(UserRole.valueOf(role));
                    
                    if (password != null && !password.trim().isEmpty()) {
                        existingUser.setPasswordHash(userService.encodePassword(password));
                    }
                    
                    userService.save(existingUser);
                } else {
                    logger.error("User not found with ID: {}", user.getUserId());
                    return "redirect:/admin/users?error=User not found";
                }
            }
            
            return "redirect:/admin/users?success=User saved successfully";
        } catch (Exception e) {
            logger.error("Error saving user: {}", e.getMessage());
            return "redirect:/admin/users?error=" + e.getMessage();
        }
    }

    @DeleteMapping("/users/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }    }

    // Booking Management
    @GetMapping("/bookings")
    public String viewBookings(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String date,
                              Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Booking Management");
            model.addAttribute("section", "bookings");
            
            List<Booking> bookings;
            
            if (status != null && !status.isEmpty()) {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingService.getBookingsByStatus(bookingStatus);
            } else if (date != null && !date.isEmpty()) {
                LocalDate bookingDate = LocalDate.parse(date);
                bookings = bookingService.getBookingsByDate(bookingDate);
            } else {
                bookings = bookingService.getAllBookings();
            }
            
            model.addAttribute("bookings", bookings);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedDate", date);
            model.addAttribute("statuses", BookingStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading bookings", e);
            model.addAttribute("error", "Error loading bookings");
            model.addAttribute("bookings", List.of());
        }
        
        return "admin/booking/list";
    }
    
    @GetMapping("/booking/{id}")
    public String viewBookingDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Booking Detail");
            model.addAttribute("section", "bookings");
              Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                return "redirect:/admin/bookings?error=Booking not found";
            }
              // Get transaction information if exists
            Transaction transaction = transactionService.getTransactionByBookingId(id);
            
            model.addAttribute("booking", booking);
            model.addAttribute("transaction", transaction);
            model.addAttribute("statuses", BookingStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading booking detail", e);
            return "redirect:/admin/bookings?error=Error loading booking";
        }
        
        return "admin/booking/detail";
    }
    
    @PostMapping("/booking/{id}/status")
    public String updateBookingStatus(@PathVariable Long id,
                                     @RequestParam BookingStatus status,
                                     @RequestParam(required = false) String notes,
                                     RedirectAttributes redirectAttributes,
                                     HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            bookingService.updateBookingStatus(id, status, notes);
            redirectAttributes.addFlashAttribute("success", "Booking status updated successfully");
        } catch (Exception e) {
            logger.error("Error updating booking status", e);
            redirectAttributes.addFlashAttribute("error", "Error updating booking status");
        }
        
        return "redirect:/admin/booking/" + id;
    }
    
    // Payment Verification
    @GetMapping("/payments")
    public String viewPayments(@RequestParam(required = false) String status,
                              Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Payment Verification");
            model.addAttribute("section", "payments");
              List<Transaction> transactions;
            
            if (status != null && !status.isEmpty()) {
                PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
                transactions = transactionService.getTransactionsByStatus(paymentStatus);
            } else {
                transactions = transactionService.getAllTransactions();
            }
            
            model.addAttribute("transactions", transactions);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("paymentStatuses", PaymentStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading payments", e);
            model.addAttribute("error", "Error loading payments");
            model.addAttribute("transactions", List.of());
        }
        
        return "admin/payment/list";
    }
    
    @GetMapping("/payment/{id}")
    public String viewPaymentDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Payment Detail");
            model.addAttribute("section", "payments");              Transaction transaction = transactionService.getTransactionById(id).orElse(null);
            if (transaction == null) {
                return "redirect:/admin/payments?error=Payment not found";
            }
            
            model.addAttribute("transaction", transaction);
            model.addAttribute("paymentStatuses", PaymentStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading payment detail", e);
            return "redirect:/admin/payments?error=Error loading payment";
        }
        
        return "admin/payment/detail";
    }
    
    @PostMapping("/payment/{id}/verify")
    public String verifyPayment(@PathVariable Long id,
                               @RequestParam PaymentStatus status,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
          try {
            transactionService.verifyPayment(id, status, notes);
            
            if (status == PaymentStatus.VALID) {
                redirectAttributes.addFlashAttribute("success", "Payment verified and booking confirmed");
            } else {
                redirectAttributes.addFlashAttribute("success", "Payment status updated");
            }
            
        } catch (Exception e) {
            logger.error("Error verifying payment", e);
            redirectAttributes.addFlashAttribute("error", "Error verifying payment");
        }
        
        return "redirect:/admin/payment/" + id;
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Long userId = (Long) session.getAttribute("userId");
        User user = userService.findById(userId);
        
        if (user == null) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");
        String fullName = (String) session.getAttribute("fullName");
        model.addAttribute("email", email);
        model.addAttribute("fullName", fullName);
        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("section", "profile");
        model.addAttribute("user", user);
        
        return "admin/profile/edit";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User userForm, 
                              @RequestParam(required = false) String currentPassword,
                              @RequestParam(required = false) String newPassword,
                              @RequestParam(required = false) String confirmPassword,
                              HttpSession session, 
                              RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User existingUser = userService.findById(userId);
            
            if (existingUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/admin/profile";
            }

            // Check if username or email already exists (but not for current user)
            User userWithSameUsername = userService.findByUsername(userForm.getUsername());
            if (userWithSameUsername != null && !userWithSameUsername.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/profile";
            }

            User userWithSameEmail = userService.findByEmail(userForm.getEmail());
            if (userWithSameEmail != null && !userWithSameEmail.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/profile";
            }

            // Update basic info
            existingUser.setUsername(userForm.getUsername());
            existingUser.setEmail(userForm.getEmail());
            existingUser.setPhoneNumber(userForm.getPhoneNumber());
            existingUser.setFullName(userForm.getFullName());
            existingUser.setAddress(userForm.getAddress());

            // Handle password change
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Current password is required to change password!");
                    return "redirect:/admin/profile";
                }

                if (!userService.verifyPassword(currentPassword, existingUser.getPasswordHash())) {
                    redirectAttributes.addFlashAttribute("error", "Current password is incorrect!");
                    return "redirect:/admin/profile";
                }

                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("error", "New passwords do not match!");
                    return "redirect:/admin/profile";
                }

                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long!");
                    return "redirect:/admin/profile";
                }

                existingUser.setPasswordHash(userService.encodePassword(newPassword));
            }

            // Save updated user
            userService.save(existingUser);

            // Update session attributes
            session.setAttribute("email", existingUser.getEmail());
            session.setAttribute("fullName", existingUser.getFullName());

            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            logger.info("Profile updated for admin user: {}", existingUser.getEmail());

        } catch (Exception e) {
            logger.error("Error updating admin profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/admin/profile";
    }
}
