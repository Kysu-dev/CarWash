package UASPraktikum.CarWash.repository;

import UASPraktikum.CarWash.model.Transaction;
import UASPraktikum.CarWash.model.PaymentStatus;
import UASPraktikum.CarWash.model.PaymentMethod;
import UASPraktikum.CarWash.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transaction by booking
    Optional<Transaction> findByBooking(Booking booking);
    
    // Find transactions by payment status
    List<Transaction> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    // Find transactions by payment method
    List<Transaction> findByPaymentMethodOrderByCreatedAtDesc(PaymentMethod method);
    
    // Find transactions by status and date range
    @Query("SELECT t FROM Transaction t WHERE t.paymentStatus = :status AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByStatusAndDateRange(@Param("status") PaymentStatus status, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    // Find pending transactions (for admin review)
    List<Transaction> findByPaymentStatusOrderByTransactionDateAsc(PaymentStatus status);
    
    // Count transactions by status
    long countByPaymentStatus(PaymentStatus status);
    
    // Count transactions by payment method
    long countByPaymentMethod(PaymentMethod method);
    
    // Find transactions that need verification (older than X hours)
    @Query("SELECT t FROM Transaction t WHERE t.paymentStatus = 'PENDING' AND t.transactionDate < :cutoffTime ORDER BY t.transactionDate ASC")
    List<Transaction> findPendingTransactionsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Find all transactions with their bookings
    @Query("SELECT t FROM Transaction t JOIN FETCH t.booking b JOIN FETCH b.user ORDER BY t.transactionDate DESC")
    List<Transaction> findAllWithBookingDetails();
    
    // Find recent transactions (for dashboard)
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate >= :fromDate ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactions(@Param("fromDate") LocalDateTime fromDate);
    
    // Find cash transactions (for employee dashboard)
    @Query("SELECT t FROM Transaction t WHERE t.paymentMethod = 'CASH' AND t.transactionDate >= :fromDate ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentCashTransactions(@Param("fromDate") LocalDateTime fromDate);
    
    // Find transactions by cashier
    List<Transaction> findByCashierNameOrderByCreatedAtDesc(String cashierName);
    
    // Daily revenue by payment method
    @Query("SELECT t.paymentMethod, SUM(t.amount) FROM Transaction t WHERE DATE(t.transactionDate) = DATE(:date) AND t.paymentStatus = 'VALID' GROUP BY t.paymentMethod")
    List<Object[]> findDailyRevenueByPaymentMethod(@Param("date") LocalDateTime date);
    
    // Monthly revenue
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month AND t.paymentStatus = 'VALID'")
    Double findMonthlyRevenue(@Param("year") int year, @Param("month") int month);
}
