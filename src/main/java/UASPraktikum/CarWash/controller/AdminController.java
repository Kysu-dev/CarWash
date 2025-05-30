package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    private boolean isAdmin(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.ADMIN;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");
        model.addAttribute("email", email);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("section", "dashboard");
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        
        // Add placeholder values for now
        model.addAttribute("totalServices", 4);  // Placeholder
        model.addAttribute("todayBookings", 0);  // Placeholder
        model.addAttribute("revenue", "$0.00");  // Placeholder
        
        return "admin/index";
    }

    @GetMapping("/users")
    public String users(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("section", "users");
        model.addAttribute("users", userService.getAllUsers());
        
        return "admin/user/list";
    }

    @GetMapping("/users/add")
    public String addUserForm(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "Add New User");
        model.addAttribute("section", "users");
        model.addAttribute("user", new User());
        
        return "admin/user/form";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }

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
        }
    }
}
