package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;      public String encodePassword(String password) {
        // Simple Base64 encoding - Note: This is not secure for production use
        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        String encodedInput = encodePassword(rawPassword);
        return encodedInput.equals(encodedPassword);
    }
    
    public User registerNewUser(String username, String email, String phoneNumber, String fullName, String password) {
        // Create new user with default CUSTOMER role
        User user = new User(
            username,
            email,
            phoneNumber,
            fullName,
            encodePassword(password)
        );
        
        // The constructor already sets role to CUSTOMER, but we can verify it here
        if (user.getRole() == null) {
            user.setRole(UserRole.CUSTOMER);
        }
        
        return userRepository.save(user);
    }
    
    public User createAdminUser(String username, String email, String phoneNumber, String fullName, String password) {
        User user = new User(
            username,
            email,
            phoneNumber,
            fullName,
            encodePassword(password)
        );
        user.setRole(UserRole.ADMIN);
        
        return userRepository.save(user);
    }
    
    // Create walk-in customer
    public User createWalkInCustomer(String fullName, String phoneNumber, String email) {
        // Check if customer already exists by phone
        User existingUser = findByPhone(phoneNumber);
        if (existingUser != null) {
            return existingUser;
        }
        
        // Create username from phone number
        String username = "walkin_" + phoneNumber.replaceAll("[^0-9]", "");
        
        // Use phone as temporary password
        String tempPassword = phoneNumber;
        
        User user = new User(
            username,
            email != null ? email : phoneNumber + "@walkin.temp",
            phoneNumber,
            fullName,
            encodePassword(tempPassword)
        );
        user.setRole(UserRole.CUSTOMER);
        
        return userRepository.save(user);
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }
      // Find user by phone number
    public User findByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }
      // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // Get all customers
    public List<User> getAllCustomers() {
        return userRepository.findByRole(UserRole.CUSTOMER);
    }
    
    // Find user by ID
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    // Save user
    public User save(User user) {
        return userRepository.save(user);
    }
    
    // Save user (alias for save method)
    public User saveUser(User user) {
        return save(user);
    }
    
    // Delete user
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    // Count all users
    public long countAllUsers() {
        return userRepository.count();
    }
    
    // Get current user from session
    public User getCurrentUser(jakarta.servlet.http.HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            return findById(userId);
        }
        return null;
    }
}