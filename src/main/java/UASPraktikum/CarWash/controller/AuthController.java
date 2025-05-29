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

@Controller
public class AuthController {    
    @Autowired
    private UserService userService;    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpSession session) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        // Validate input
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        // Find user by email
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid email or password");
        }

        // Verify password (using Base64 encoding as implemented in UserService)
        if (!userService.verifyPassword(password, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Invalid email or password");
        }

        // Set user info in session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("email", user.getEmail());

        // Create response with user role and redirect URL
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("role", user.getRole().toString());
        
        // Set the redirect URL based on user role
        String redirectUrl = switch (user.getRole()) {
            case ADMIN -> "/admin/dashboard";
            case EMPLOYEE -> "/employee/dashboard";
            default -> "/customer/dashboard";
        };
        response.put("redirect", redirectUrl);

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
            case ADMIN -> "redirect:/admin/dashboard";
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
        return "redirect:/login";
    }
}
