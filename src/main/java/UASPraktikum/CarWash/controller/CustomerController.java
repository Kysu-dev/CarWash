package UASPraktikum.CarWash.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.UserRole;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.CUSTOMER) {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
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
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "Book Service");
            model.addAttribute("section", "booking");
            return "customer/booking/form";
        }
        return "redirect:/login";
    }
    
    @GetMapping("/bookings")
    public String bookings(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.CUSTOMER) {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("title", "My Bookings");
            model.addAttribute("section", "bookings");
            return "customer/booking/list";
        }
        return "redirect:/login";
    }
}
