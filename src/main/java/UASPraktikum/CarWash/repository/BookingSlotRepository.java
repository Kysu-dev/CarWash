package UASPraktikum.CarWash.repository;

import UASPraktikum.CarWash.model.BookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingSlotRepository extends JpaRepository<BookingSlot, Long> {
    
    // Find slot by date and time
    Optional<BookingSlot> findByTanggalAndJam(LocalDate tanggal, LocalTime jam);
    
    // Find all slots for a specific date
    List<BookingSlot> findByTanggalOrderByJam(LocalDate tanggal);
    
    // Find available slots for a specific date
    @Query("SELECT bs FROM BookingSlot bs WHERE bs.tanggal = :tanggal AND bs.jumlahTerisi < :maxCapacity ORDER BY bs.jam")
    List<BookingSlot> findAvailableSlotsByDate(@Param("tanggal") LocalDate tanggal, @Param("maxCapacity") int maxCapacity);
    
    // Find slots with available capacity
    @Query("SELECT bs FROM BookingSlot bs WHERE bs.jumlahTerisi < :maxCapacity ORDER BY bs.tanggal, bs.jam")
    List<BookingSlot> findAllAvailableSlots(@Param("maxCapacity") int maxCapacity);
    
    // Find slots for date range
    List<BookingSlot> findByTanggalBetweenOrderByTanggalAscJamAsc(LocalDate startDate, LocalDate endDate);
    
    // Find fully booked slots for a date
    @Query("SELECT bs FROM BookingSlot bs WHERE bs.tanggal = :tanggal AND bs.jumlahTerisi >= :maxCapacity ORDER BY bs.jam")
    List<BookingSlot> findFullyBookedSlotsByDate(@Param("tanggal") LocalDate tanggal, @Param("maxCapacity") int maxCapacity);
    
    // Count available slots for a date
    @Query("SELECT COUNT(bs) FROM BookingSlot bs WHERE bs.tanggal = :tanggal AND bs.jumlahTerisi < :maxCapacity")
    long countAvailableSlotsByDate(@Param("tanggal") LocalDate tanggal, @Param("maxCapacity") int maxCapacity);
    
    // Find slots by time range for a specific date
    @Query("SELECT bs FROM BookingSlot bs WHERE bs.tanggal = :tanggal AND bs.jam BETWEEN :startTime AND :endTime ORDER BY bs.jam")
    List<BookingSlot> findByDateAndTimeRange(@Param("tanggal") LocalDate tanggal, 
                                           @Param("startTime") LocalTime startTime, 
                                           @Param("endTime") LocalTime endTime);
    
    // Delete old slots (cleanup)
    void deleteByTanggalBefore(LocalDate date);
    
    // Check if slot exists
    boolean existsByTanggalAndJam(LocalDate tanggal, LocalTime jam);
}
