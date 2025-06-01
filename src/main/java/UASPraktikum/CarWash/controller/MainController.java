package UASPraktikum.CarWash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import UASPraktikum.CarWash.service.ServiceService;
import UASPraktikum.CarWash.model.Service;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private ServiceService serviceService;

    @GetMapping("/")
    public String index(Model model) {
        try {
            // Get all active services from the database
            List<Service> services = serviceService.getAllServices();
            
            // Filter only active services for the main page
            List<Service> activeServices = services.stream()
                .filter(Service::isActive)
                .toList();
            
            logger.info("Loaded {} active services for main page", activeServices.size());
            model.addAttribute("services", activeServices);
            
            return "index";
        } catch (Exception e) {
            logger.error("Error loading services for main page: {}", e.getMessage());
            // If there's an error, still return the page but with empty services list
            model.addAttribute("services", List.of());
            return "index";
        }
    }
}
