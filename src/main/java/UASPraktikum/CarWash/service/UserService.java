package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.model.UserRole;
import UASPraktikum.CarWash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
      private String encodePassword(String password) {
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
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}