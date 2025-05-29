package UASPraktikum.CarWash.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.UserRole;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.ADMIN) {
            String email = (String) session.getAttribute("email");
            model.addAttribute("email", email);
            return "dashboard/admin";
        }
        return "redirect:/login";
    }
}
