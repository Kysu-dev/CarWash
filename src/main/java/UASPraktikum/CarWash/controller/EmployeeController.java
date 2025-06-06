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
import UASPraktikum.CarWash.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
      @Autowired
    private UserService userService;
    
    @Autowired
    private ServiceService serviceService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private TransferService transferService;
    
    private boolean isEmployee(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.EMPLOYEE;
    }@GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.EMPLOYEE) {
            try {
                String email = (String) session.getAttribute("email");
                String fullName = (String) session.getAttribute("fullName");
                
                // Get today's statistics
                LocalDate today = LocalDate.now();
                List<Booking> todayBookings = bookingService.getBookingsByDate(today);
                long confirmedToday = todayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .count();
                long inProgressToday = todayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.IN_PROGRESS)
                    .count();
                long completedToday = todayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                    .count();
                
                model.addAttribute("email", email);
                model.addAttribute("fullName", fullName);
                model.addAttribute("pageTitle", "Dashboard");
                model.addAttribute("section", "dashboard");
                model.addAttribute("todayBookings", todayBookings.size());
                model.addAttribute("confirmedToday", confirmedToday);
                model.addAttribute("inProgressToday", inProgressToday);
                model.addAttribute("completedToday", completedToday);
                
                // Get upcoming bookings for today
                List<Booking> upcomingBookings = todayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.IN_PROGRESS)
                    .sorted((b1, b2) -> b1.getJam().compareTo(b2.getJam()))
                    .limit(10)
                    .toList();
                
                model.addAttribute("upcomingBookings", upcomingBookings);
                
            } catch (Exception e) {
                logger.error("Error loading employee dashboard", e);
                model.addAttribute("error", "Error loading dashboard data");
            }
            
            return "employee/index";
        }
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        if (!isEmployee(session)) {
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
        
        return "employee/profile/edit";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User userForm, 
                              @RequestParam(required = false) String currentPassword,
                              @RequestParam(required = false) String newPassword,
                              @RequestParam(required = false) String confirmPassword,
                              HttpSession session, 
                              RedirectAttributes redirectAttributes) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        try {
            Long userId = (Long) session.getAttribute("userId");
            User existingUser = userService.findById(userId);
            
            if (existingUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found!");
                return "redirect:/employee/profile";
            }

            // Check if username or email already exists (but not for current user)
            User userWithSameUsername = userService.findByUsername(userForm.getUsername());
            if (userWithSameUsername != null && !userWithSameUsername.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/employee/profile";
            }

            User userWithSameEmail = userService.findByEmail(userForm.getEmail());
            if (userWithSameEmail != null && !userWithSameEmail.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/employee/profile";
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
                    return "redirect:/employee/profile";
                }

                if (!userService.verifyPassword(currentPassword, existingUser.getPasswordHash())) {
                    redirectAttributes.addFlashAttribute("error", "Current password is incorrect!");
                    return "redirect:/employee/profile";
                }

                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("error", "New passwords do not match!");
                    return "redirect:/employee/profile";
                }

                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long!");
                    return "redirect:/employee/profile";
                }

                existingUser.setPasswordHash(userService.encodePassword(newPassword));
            }

            // Save updated user
            userService.save(existingUser);

            // Update session attributes
            session.setAttribute("email", existingUser.getEmail());
            session.setAttribute("fullName", existingUser.getFullName());

            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            logger.info("Profile updated for employee user: {}", existingUser.getEmail());

        } catch (Exception e) {
            logger.error("Error updating employee profile: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }        return "redirect:/employee/profile";
    }    @GetMapping("/bookings/today")
    public String todaysBookings(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Today's Bookings");
            model.addAttribute("section", "bookings");
            
            LocalDate today = LocalDate.now();
            List<Booking> todayBookings = bookingService.getBookingsByDate(today);
            
            model.addAttribute("bookings", todayBookings);
            model.addAttribute("date", today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            model.addAttribute("statuses", BookingStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading today's bookings", e);
            model.addAttribute("error", "Error loading bookings");
            model.addAttribute("bookings", List.of());
        }
        
        return "employee/bookings/today";
    }    @GetMapping("/bookings/history")
    public String bookingHistory(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String date,
                                Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Booking History");
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
            logger.error("Error loading booking history", e);
            model.addAttribute("error", "Error loading bookings");
            model.addAttribute("bookings", List.of());
        }
        
        return "employee/bookings/history";
    }
    
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
            model.addAttribute("pageTitle", "Booking Detail");            model.addAttribute("section", "bookings");
            
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                return "redirect:/employee/bookings/history?error=Booking not found";
            }
            
            // Get transfer information if exists
            Transfer transfer = transferService.getTransferByBookingId(id);
            
            model.addAttribute("booking", booking);
            model.addAttribute("transfer", transfer);
            model.addAttribute("statuses", BookingStatus.values());
            
        } catch (Exception e) {
            logger.error("Error loading booking detail", e);
            return "redirect:/employee/bookings/history?error=Error loading booking";
        }
        
        return "employee/booking-detail";
    }
    
    @PostMapping("/booking/{id}/start")
    public String startBooking(@PathVariable Long id,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
          try {
            Booking booking = bookingService.getBookingById(id).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/employee/bookings/history";
            }
            
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                redirectAttributes.addFlashAttribute("error", "Only confirmed bookings can be started");
                return "redirect:/employee/booking/" + id;
            }
            
            bookingService.updateBookingStatus(id, BookingStatus.IN_PROGRESS, "Service started by employee");
            redirectAttributes.addFlashAttribute("success", "Booking started successfully");
            
        } catch (Exception e) {
            logger.error("Error starting booking", e);
            redirectAttributes.addFlashAttribute("error", "Error starting booking");
        }
        
        return "redirect:/employee/booking/" + id;
    }
    
    @PostMapping("/booking/{id}/complete")
    public String completeBooking(@PathVariable Long id,
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
                return "redirect:/employee/bookings/history";
            }
            
            if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
                redirectAttributes.addFlashAttribute("error", "Only in-progress bookings can be completed");
                return "redirect:/employee/booking/" + id;
            }
            
            String completeNotes = notes != null ? notes : "Service completed by employee";
            bookingService.updateBookingStatus(id, BookingStatus.COMPLETED, completeNotes);
            redirectAttributes.addFlashAttribute("success", "Booking completed successfully");
            
        } catch (Exception e) {
            logger.error("Error completing booking", e);
            redirectAttributes.addFlashAttribute("error", "Error completing booking");
        }
        
        return "redirect:/employee/booking/" + id;
    }
    
    // Walk-in Customer Registration
    @GetMapping("/walkin")
    public String walkinForm(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Walk-in Customer");
            model.addAttribute("section", "operations");
            
            List<Service> services = serviceService.getAllServices();
            model.addAttribute("services", services);
            model.addAttribute("booking", new Booking());
              // Get available time slots for today
            LocalDate today = LocalDate.now();
            List<String> availableSlots = bookingService.getAvailableTimeSlotsAsStrings(today);
            model.addAttribute("availableSlots", availableSlots);
            model.addAttribute("selectedDate", today.toString());
            
        } catch (Exception e) {
            logger.error("Error loading walk-in form", e);
            model.addAttribute("error", "Error loading form");
        }
        
        return "employee/walkin-form";
    }
    
    @PostMapping("/walkin")
    public String processWalkin(@RequestParam Long serviceId,
                               @RequestParam String customerName,
                               @RequestParam String customerPhone,
                               @RequestParam String customerEmail,
                               @RequestParam String date,
                               @RequestParam String time,
                               @RequestParam String vehicleType,
                               @RequestParam String vehicleBrand,
                               @RequestParam String vehicleModel,
                               @RequestParam String vehiclePlate,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }
        
        try {
            // Check if customer exists, if not create a new one
            User customer = userService.findByEmail(customerEmail);
            if (customer == null) {
                // Create temporary customer account
                String tempPassword = "temp123"; // You might want to generate a random password
                customer = userService.registerNewUser(
                    customerEmail, // username same as email
                    customerEmail,
                    customerPhone,
                    customerName,
                    tempPassword
                );
                customer.setRole(UserRole.CUSTOMER);
                userService.save(customer);
            }
              Service service = serviceService.getServiceById(serviceId).orElse(null);
            if (service == null) {
                redirectAttributes.addFlashAttribute("error", "Service not found");
                return "redirect:/employee/walkin";
            }
            
            LocalDate bookingDate = LocalDate.parse(date);
            LocalTime bookingTime = LocalTime.parse(time);
            
            // Check availability
            if (!bookingService.isTimeSlotAvailable(bookingDate, bookingTime)) {
                redirectAttributes.addFlashAttribute("error", "Selected time slot is not available");
                return "redirect:/employee/walkin";
            }
            
            // Create booking
            Booking booking = new Booking();
            booking.setUser(customer);
            booking.setService(service);
            booking.setTanggal(bookingDate);
            booking.setJam(bookingTime);
            booking.setStatus(BookingStatus.CONFIRMED); // Walk-in bookings are immediately confirmed
            booking.setMetode(BookingMethod.WALKIN);
            booking.setCatatan(notes);
            booking.setVehicleType(vehicleType);
            booking.setVehicleBrand(vehicleBrand);
            booking.setVehicleModel(vehicleModel);
            booking.setLicensePlate(vehiclePlate);
            
            Booking savedBooking = bookingService.createBooking(booking);
            
            redirectAttributes.addFlashAttribute("success", 
                "Walk-in booking created successfully! Booking ID: " + savedBooking.getIdBooking());
            
            return "redirect:/employee/booking/" + savedBooking.getIdBooking();
            
        } catch (Exception e) {
            logger.error("Error processing walk-in booking", e);
            redirectAttributes.addFlashAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/employee/walkin";
        }
    }    @GetMapping("/slots")
    public String manageSlots(@RequestParam(required = false) String date,
                             Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Manage Slots");
            model.addAttribute("section", "operations");
              LocalDate viewDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            List<Booking> dayBookings = bookingService.getBookingsByDate(viewDate);
            List<String> availableSlots = bookingService.getAvailableTimeSlotsAsStrings(viewDate);
            
            model.addAttribute("bookings", dayBookings);
            model.addAttribute("availableSlots", availableSlots);
            model.addAttribute("viewDate", viewDate);
            model.addAttribute("formattedDate", viewDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            
            // Group bookings by time slots
            Map<String, List<Booking>> scheduleMap = new HashMap<>();
            for (Booking booking : dayBookings) {
                String timeSlot = booking.getJam().toString();
                scheduleMap.computeIfAbsent(timeSlot, k -> new java.util.ArrayList<>()).add(booking);
            }
            model.addAttribute("scheduleMap", scheduleMap);
            
        } catch (Exception e) {
            logger.error("Error loading slots", e);
            model.addAttribute("error", "Error loading slots");
        }
        
        return "employee/slots";
    }
    
    // API Endpoints for AJAX calls
    @GetMapping("/api/available-slots")
    @ResponseBody    public ResponseEntity<List<String>> getAvailableSlots(@RequestParam String date) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            List<String> availableSlots = bookingService.getAvailableTimeSlotsAsStrings(bookingDate);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            logger.error("Error fetching available slots", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/api/booking/{id}/update-status")
    @ResponseBody
    public ResponseEntity<String> updateBookingStatusApi(@PathVariable Long id,
                                                        @RequestParam BookingStatus status,
                                                        @RequestParam(required = false) String notes) {
        try {
            bookingService.updateBookingStatus(id, status, notes);
            return ResponseEntity.ok("Status updated successfully");
        } catch (Exception e) {
            logger.error("Error updating booking status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating status");
        }
    }

    @GetMapping("/qr-scanner")
    public String qrScanner(Model model, HttpSession session) {
        if (!isEmployee(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");
        String fullName = (String) session.getAttribute("fullName");
        model.addAttribute("email", email);
        model.addAttribute("fullName", fullName);
        model.addAttribute("pageTitle", "QR Scanner");
        model.addAttribute("section", "operations");
        
        return "employee/qr-scanner";
    }
}
