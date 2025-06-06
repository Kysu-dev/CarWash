package UASPraktikum.CarWash.repository;

import UASPraktikum.CarWash.model.Transfer;
import UASPraktikum.CarWash.model.PaymentStatus;
import UASPraktikum.CarWash.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    
    // Find transfer by booking
    Optional<Transfer> findByBooking(Booking booking);
    
    // Find transfers by status
    List<Transfer> findByStatusPembayaranOrderByCreatedAtDesc(PaymentStatus status);
    
    // Find transfers by status and date range
    @Query("SELECT t FROM Transfer t WHERE t.statusPembayaran = :status AND t.tanggalTransaksi BETWEEN :startDate AND :endDate ORDER BY t.tanggalTransaksi DESC")
    List<Transfer> findByStatusAndDateRange(@Param("status") PaymentStatus status, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    // Find pending transfers (for admin review)
    List<Transfer> findByStatusPembayaranOrderByTanggalTransaksiAsc(PaymentStatus status);
    
    // Count transfers by status
    long countByStatusPembayaran(PaymentStatus status);
    
    // Find transfers that need verification (older than X hours)
    @Query("SELECT t FROM Transfer t WHERE t.statusPembayaran = 'PENDING' AND t.tanggalTransaksi < :cutoffTime ORDER BY t.tanggalTransaksi ASC")
    List<Transfer> findPendingTransfersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find all transfers with their bookings
    @Query("SELECT t FROM Transfer t JOIN FETCH t.booking b JOIN FETCH b.user ORDER BY t.tanggalTransaksi DESC")
    List<Transfer> findAllWithBookingDetails();
    
    // Find recent transfers (for dashboard)
    @Query("SELECT t FROM Transfer t WHERE t.tanggalTransaksi >= :fromDate ORDER BY t.tanggalTransaksi DESC")
    List<Transfer> findRecentTransfers(@Param("fromDate") LocalDateTime fromDate);
}
