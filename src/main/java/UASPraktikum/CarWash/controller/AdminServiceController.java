package UASPraktikum.CarWash.controller;

import UASPraktikum.CarWash.model.Service;
import UASPraktikum.CarWash.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import UASPraktikum.CarWash.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin/services")
public class AdminServiceController {
    private static final Logger logger = LoggerFactory.getLogger(AdminServiceController.class);

    @Autowired
    private ServiceService serviceService;

    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            logger.warn("Session is null");
            return false;
        }
        
        UserRole role = (UserRole) session.getAttribute("userRole");
        if (role == null) {
            logger.warn("UserRole is null in session");
            return false;
        }
        
        logger.info("Checking admin access for role: {}", role);
        return role == UserRole.ADMIN;
    }

    // List all services
    @GetMapping("")
    public String listServices(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "Services");
        model.addAttribute("section", "services");
        model.addAttribute("services", serviceService.getAllServices());
        return "admin/services/list";
    }

    // Show service creation form
    @GetMapping("/create")
    public String showCreateForm(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");
        model.addAttribute("email", email);
        model.addAttribute("pageTitle", "Create New Service");
        model.addAttribute("section", "services");
        model.addAttribute("service", new Service());
        return "admin/services/form";
    }

    // Handle service creation
    @PostMapping("/create")
    public String createService(@ModelAttribute Service service, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            serviceService.createService(service);
            redirectAttributes.addFlashAttribute("success", "Service created successfully!");
            return "redirect:/admin/services";
        } catch (Exception e) {
            logger.error("Error creating service: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to create service: " + e.getMessage());
            return "redirect:/admin/services/create";
        }
    }

    // Show service edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("email");
        model.addAttribute("email", email);
        model.addAttribute("pageTitle", "Edit Service");
        model.addAttribute("section", "services");
        
        serviceService.getServiceById(id).ifPresent(service -> {
            model.addAttribute("service", service);
        });
        
        return "admin/services/form";
    }

    // Handle service update
    @PostMapping("/edit/{id}")
    public String updateService(@PathVariable Long id, @ModelAttribute Service service, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            service.setServiceId(id);
            serviceService.updateService(service);
            redirectAttributes.addFlashAttribute("success", "Service updated successfully!");
            return "redirect:/admin/services";
        } catch (Exception e) {
            logger.error("Error updating service: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update service: " + e.getMessage());
            return "redirect:/admin/services/edit/" + id;
        }
    }

    // Handle service deletion (soft delete)
    @PostMapping("/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            if (serviceService.deleteService(id)) {
                redirectAttributes.addFlashAttribute("success", "Service deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Service not found!");
            }
        } catch (Exception e) {
            logger.error("Error deleting service: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to delete service: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    // Toggle service status (active/inactive)
    @PostMapping("/toggle/{id}")
    public String toggleServiceStatus(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            serviceService.getServiceById(id).ifPresent(service -> {
                service.setActive(!service.isActive());
                serviceService.updateService(service);
                redirectAttributes.addFlashAttribute("success", 
                    "Service status updated to " + (service.isActive() ? "active" : "inactive") + "!");
            });
        } catch (Exception e) {
            logger.error("Error toggling service status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to update service status: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }
}
