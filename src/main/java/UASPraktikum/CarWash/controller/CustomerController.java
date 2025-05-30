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
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("section", "dashboard");
            return "customer/index";
        }
        return "redirect:/login";
    }
}
