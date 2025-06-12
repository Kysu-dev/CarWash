package UASPraktikum.CarWash.controller;

import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.UserService;
import UASPraktikum.CarWash.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/booking-management")
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private ServiceService serviceService;    @GetMapping("")
    public String listBookings(Model model) {
        List<Booking> bookings = bookingService.getAllBookingsWithUserAndService();
        model.addAttribute("bookings", bookings);
        model.addAttribute("section", "bookings");
        return "admin/booking/list";
    }    @GetMapping("/form")
    public String showCreateForm(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("services", serviceService.getAllServices());
        model.addAttribute("bookingMethods", BookingMethod.values());
        return "admin/booking/form";
    }@PostMapping("/create")
    public String createBooking(@RequestParam("userId") Long userId,
                              @RequestParam("serviceId") Long serviceId,
                              @RequestParam("tanggal") String tanggal,
                              @RequestParam("jam") String jam,
                              @RequestParam("metode") BookingMethod metode,
                              @RequestParam(value = "catatan", required = false) String catatan,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            UASPraktikum.CarWash.model.Service service = serviceService.getServiceById(serviceId).orElseThrow(() -> new RuntimeException("Service not found"));
              LocalDate bookingDate = LocalDate.parse(tanggal);
            LocalTime bookingTime = LocalTime.parse(jam);
            
            bookingService.createBooking(user, service, bookingDate, bookingTime, metode, catatan);            redirectAttributes.addFlashAttribute("success", "Booking created successfully");
            return "redirect:/admin/booking-management";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/booking-management/form";
        }
    }    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Booking> bookingOpt = bookingService.getBookingById(id);
        if (bookingOpt.isEmpty()) {
            return "redirect:/admin/booking-management";
        }
        
        Booking booking = bookingOpt.get();
        model.addAttribute("booking", booking);
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("services", serviceService.getAllServices());
        model.addAttribute("bookingMethods", BookingMethod.values());
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/booking/form";
    }@PostMapping("/edit/{id}")
    public String updateBooking(@PathVariable("id") Long id,
                              @RequestParam("status") BookingStatus status,
                              RedirectAttributes redirectAttributes) {
        try {
            bookingService.updateBookingStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Booking updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/booking-management";
    }    @PostMapping("/delete/{id}")
    public String deleteBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Since there's no direct deleteBooking method, we'll use cancelBooking instead
            bookingService.cancelBooking(id, "Deleted by admin");
            redirectAttributes.addFlashAttribute("success", "Booking deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/booking-management";
    }
}
