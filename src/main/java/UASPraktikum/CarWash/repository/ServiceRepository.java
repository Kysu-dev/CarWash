package UASPraktikum.CarWash.repository;

import UASPraktikum.CarWash.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByIsActiveTrue();
    List<Service> findByServiceNameContainingIgnoreCase(String serviceName);
    List<Service> findByVehicleType(UASPraktikum.CarWash.model.VehicleType vehicleType);
    List<Service> findByVehicleTypeAndIsActiveTrue(UASPraktikum.CarWash.model.VehicleType vehicleType);
}
