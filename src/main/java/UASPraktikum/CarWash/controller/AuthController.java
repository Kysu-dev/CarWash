package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.service.UserService;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpSession session) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        logger.info("Login attempt for email: {}", email);

        // Validate input
        if (email == null || password == null) {
            logger.warn("Login failed: Email or password is null");
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        // Find user by email
        User user = userService.findByEmail(email);
        if (user == null) {
            logger.warn("Login failed: User not found for email: {}", email);
            return ResponseEntity.badRequest().body("Invalid email or password");
        }

        // Verify password
        if (!userService.verifyPassword(password, user.getPasswordHash())) {
            logger.warn("Login failed: Invalid password for email: {}", email);
            return ResponseEntity.badRequest().body("Invalid email or password");
        }        // Set user info in session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("email", user.getEmail());

        logger.info("Login successful for user: {} with role: {}", email, user.getRole());

        // Create response
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("role", user.getRole().toString());
        response.put("email", user.getEmail());

        // Set redirect URL
        String redirectUrl = switch (user.getRole()) {
            case ADMIN -> "/admin";  // Changed from /admin/dashboard to /admin
            case EMPLOYEE -> "/employee/dashboard";
            default -> "/customer/dashboard";
        };
        response.put("redirect", redirectUrl);

        logger.info("Redirecting to: {}", redirectUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute("userRole");
        if (userRole == null) {
            return "redirect:/login";
        }
        
        // Redirect to appropriate dashboard based on role
        return switch (userRole) {
            case ADMIN -> "redirect:/admin";  // Changed to match new admin route
            case EMPLOYEE -> "redirect:/employee/dashboard";
            case CUSTOMER -> "redirect:/customer/dashboard";
        };
    }

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String phoneNumber = request.get("phone");
        String fullName = request.get("fullName");
        String password = request.get("password");

        // Validate required fields
        if (username == null || email == null || password == null || fullName == null) {
            return ResponseEntity.badRequest().body("All fields are required");
        }

        // Check if username or email already exists
        if (userService.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userService.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }        try {
            User newUser = userService.registerNewUser(username, email, phoneNumber, fullName, password);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Registration successful");
            response.put("role", newUser.getRole().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Clear the session
        return "redirect:/";
    }
}
