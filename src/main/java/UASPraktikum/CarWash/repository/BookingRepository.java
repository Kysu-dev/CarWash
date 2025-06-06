package UASPraktikum.CarWash.repository;

import UASPraktikum.CarWash.model.Booking;
import UASPraktikum.CarWash.model.BookingStatus;
import UASPraktikum.CarWash.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Find bookings by user
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
    
    // Find bookings by user and status
    List<Booking> findByUserAndStatusOrderByCreatedAtDesc(User user, BookingStatus status);
    
    // Find bookings by status
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
    
    // Find bookings by date
    List<Booking> findByTanggalOrderByJamAsc(LocalDate tanggal);
    
    // Find bookings by date and status
    List<Booking> findByTanggalAndStatusOrderByJamAsc(LocalDate tanggal, BookingStatus status);
    
    // Check if time slot is available
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tanggal = :tanggal AND b.jam = :jam AND b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')")
    long countBookingsAtDateTime(@Param("tanggal") LocalDate tanggal, @Param("jam") LocalTime jam);
    
    // Get all booked time slots for a specific date
    @Query("SELECT b.jam FROM Booking b WHERE b.tanggal = :tanggal AND b.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')")
    List<LocalTime> findBookedTimeSlotsForDate(@Param("tanggal") LocalDate tanggal);
    
    // Find upcoming bookings for a user
    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.tanggal >= :currentDate AND b.status IN ('PENDING', 'CONFIRMED') ORDER BY b.tanggal ASC, b.jam ASC")
    List<Booking> findUpcomingBookingsByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);
    
    // Find bookings for today
    @Query("SELECT b FROM Booking b WHERE b.tanggal = :today ORDER BY b.jam ASC")
    List<Booking> findTodaysBookings(@Param("today") LocalDate today);
    
    // Count bookings by status
    long countByStatus(BookingStatus status);
    
    // Find bookings within date range
    @Query("SELECT b FROM Booking b WHERE b.tanggal BETWEEN :startDate AND :endDate ORDER BY b.tanggal DESC, b.jam DESC")
    List<Booking> findBookingsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
      // Find recent bookings (last N days)
    @Query("SELECT b FROM Booking b WHERE b.tanggal >= :fromDate ORDER BY b.createdAt DESC")
    List<Booking> findRecentBookings(@Param("fromDate") LocalDate fromDate);
    
    // Find bookings by date range
    List<Booking> findByTanggalBetween(LocalDate startDate, LocalDate endDate);
}
