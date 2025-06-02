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
import UASPraktikum.CarWash.service.ServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ServiceService serviceService;

    private boolean isAdmin(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.ADMIN;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }        try {
            String email = (String) session.getAttribute("email");
            String fullName = (String) session.getAttribute("fullName");
            int totalUsers = userService.getAllUsers().size();
            int totalServices = serviceService.getAllServices().size();
            // TODO: Implement these features later
            int todayBookings = 0;
            String revenue = "Rp 0";

            model.addAttribute("email", email);
            model.addAttribute("fullName", fullName);
            model.addAttribute("pageTitle", "Dashboard");
            model.addAttribute("section", "dashboard");
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalServices", totalServices);
            model.addAttribute("todayBookings", todayBookings);
            model.addAttribute("revenue", revenue);
            
            return "admin/index";
        } catch (Exception e) {
            logger.error("Error loading dashboard: {}", e.getMessage());
            return "redirect:/login?error=Failed to load dashboard";
        }
    }    @GetMapping("/users")
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
        }
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
