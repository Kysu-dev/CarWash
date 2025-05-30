package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.Service;
import UASPraktikum.CarWash.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceService {
    
    @Autowired
    private ServiceRepository serviceRepository;

    // Create
    public Service createService(Service service) {
        return serviceRepository.save(service);
    }

    // Read
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Optional<Service> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    // Update
    public Service updateService(Service service) {
        return serviceRepository.save(service);
    }

    // Delete
    public boolean deleteService(Long id) {
        try {
            if (serviceRepository.existsById(id)) {
                serviceRepository.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
