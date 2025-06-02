package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    
    @Autowired
    private UserService userService;
    
    private boolean isEmployee(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.EMPLOYEE;
    }@GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == UserRole.EMPLOYEE) {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("section", "dashboard");
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
        }

        return "redirect:/employee/profile";
    }
}
