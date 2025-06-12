package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BookingService bookingService;
    
    // Upload directory for payment proofs (only for non-cash payments)
    private static final String UPLOAD_DIR = "uploads/payment-proofs/";
    
    // Create cash payment transaction (for in-person payments)
    public Transaction createCashPayment(Booking booking, BigDecimal amount, String cashierName) {
        Transaction transaction = new Transaction(booking, amount, cashierName);
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Update booking status to confirmed when cash payment is received
        bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
        
        return savedTransaction;
    }
    
    // Create transfer payment transaction (for online payments)
    public Transaction createTransferPayment(Booking booking, BigDecimal amount, MultipartFile proofFile) throws IOException {
        String proofPath = null;
        if (proofFile != null && !proofFile.isEmpty()) {
            proofPath = saveProofFile(proofFile);
        }
        
        Transaction transaction = new Transaction(booking, amount, proofPath, PaymentMethod.TRANSFER);
        return transactionRepository.save(transaction);
    }
    
    // Create card payment transaction
    public Transaction createCardPayment(Booking booking, BigDecimal amount, String cashierName) {
        Transaction transaction = new Transaction(booking, amount, cashierName);
        transaction.setPaymentMethod(PaymentMethod.CARD);
        transaction.setPaymentStatus(PaymentStatus.VALID); // Card payments are immediately valid
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Update booking status to confirmed
        bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
        
        return savedTransaction;
    }
    
    // Save proof file to upload directory
    private String saveProofFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
            : "";
        String filename = UUID.randomUUID().toString() + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }
    
    // Get transaction by booking
    public Optional<Transaction> getTransactionByBooking(Booking booking) {
        return transactionRepository.findByBooking(booking);
    }
    
    // Get all transactions
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    /**
     * Calculate monthly revenue for the current year
     * @return Array of monthly revenues (index 0 = January, 11 = December)
     */
    public BigDecimal[] getMonthlyRevenueForCurrentYear() {
        int currentYear = LocalDateTime.now().getYear();
        return getMonthlyRevenue(currentYear);
    }
    
    /**
     * Calculate monthly revenue for a specific year
     * @param year The year to calculate revenue for
     * @return Array of monthly revenues (index 0 = January, 11 = December)
     */
    public BigDecimal[] getMonthlyRevenue(int year) {
        List<Transaction> allTransactions = getAllTransactions();
        BigDecimal[] monthlyRevenue = new BigDecimal[12];
        
        // Initialize all months with zero
        for (int i = 0; i < 12; i++) {
            monthlyRevenue[i] = BigDecimal.ZERO;
        }
        
        // Calculate revenue for each month
        for (Transaction transaction : allTransactions) {
            // Only count valid payments
            if (transaction.getPaymentStatus() == PaymentStatus.VALID) {
                LocalDateTime date = transaction.getTransactionDate();
                if (date.getYear() == year) {
                    int month = date.getMonthValue() - 1; // 0-based index
                    monthlyRevenue[month] = monthlyRevenue[month].add(transaction.getAmount());
                }
            }
        }
        
        return monthlyRevenue;
    }
    
    /**
     * Get total revenue from all valid transactions
     */
    public BigDecimal getTotalRevenue() {
        List<Transaction> validTransactions = getTransactionsByStatus(PaymentStatus.VALID);
        return validTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Get total revenue for current month
     */
    public BigDecimal getCurrentMonthRevenue() {
        int currentYear = LocalDateTime.now().getYear();
        int currentMonth = LocalDateTime.now().getMonthValue() - 1; // 0-based
        return getMonthlyRevenue(currentYear)[currentMonth];
    }
    
    // Get transaction by ID
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }
    
    // Get transactions by status
    public List<Transaction> getTransactionsByStatus(PaymentStatus status) {
        return transactionRepository.findByPaymentStatusOrderByCreatedAtDesc(status);
    }
    
    // Get transactions by payment method
    public List<Transaction> getTransactionsByPaymentMethod(PaymentMethod method) {
        return transactionRepository.findByPaymentMethodOrderByCreatedAtDesc(method);
    }
    
    // Get pending transactions (for admin review)
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findByPaymentStatusOrderByTransactionDateAsc(PaymentStatus.PENDING);
    }
    
    // Get transaction by booking ID
    public Transaction getTransactionByBookingId(Long bookingId) {
        Optional<Booking> booking = bookingService.getBookingById(bookingId);
        if (booking.isPresent()) {
            Optional<Transaction> transaction = transactionRepository.findByBooking(booking.get());
            return transaction.orElse(null);
        }
        return null;
    }

    // Verify payment (admin function)
    public Transaction verifyPayment(Long transactionId, String verifiedBy, String notes) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            transaction.verify(verifiedBy, notes);
            
            // Update booking status to confirmed when payment is verified
            Booking booking = transaction.getBooking();
            bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
            
            return transactionRepository.save(transaction);
        }
        throw new RuntimeException("Transaction not found");
    }
    
    // Reject payment (admin function)
    public Transaction rejectPayment(Long transactionId, String verifiedBy, String notes) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            transaction.reject(verifiedBy, notes);
            return transactionRepository.save(transaction);
        }
        throw new RuntimeException("Transaction not found");
    }
    
    // Update payment proof (customer can re-upload if rejected)
    public Transaction updatePaymentProof(Long transactionId, MultipartFile newProofFile, User user) throws IOException {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            
            // Check if user owns this transaction
            if (!transaction.getBooking().getUser().getUserId().equals(user.getUserId())) {
                throw new RuntimeException("You can only update your own transactions");
            }
            
            // Check if transaction can be updated (only if invalid or pending)
            if (transaction.getPaymentStatus() == PaymentStatus.VALID) {
                throw new RuntimeException("Cannot update verified payment");
            }
            
            // Delete old proof file if exists
            if (transaction.getPaymentProof() != null) {
                try {
                    Files.deleteIfExists(Paths.get(transaction.getPaymentProof()));
                } catch (IOException e) {
                    // Log error but continue
                    System.err.println("Error deleting old proof file: " + e.getMessage());
                }
            }
            
            // Save new proof file
            String newProofPath = saveProofFile(newProofFile);
            transaction.setPaymentProof(newProofPath);
            transaction.setPaymentStatus(PaymentStatus.PENDING);
            transaction.setTransactionDate(LocalDateTime.now());
            
            return transactionRepository.save(transaction);
        }
        throw new RuntimeException("Transaction not found");
    }
      
    // Verify payment with status and notes
    public Transaction verifyPayment(Long transactionId, PaymentStatus status, String notes) {
        Optional<Transaction> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            
            if (status == PaymentStatus.VALID) {
                transaction.verify("System", notes);
                // Update booking status when payment is verified
                Booking booking = transaction.getBooking();
                bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
            } else {
                transaction.reject("System", notes);
            }
            
            return transactionRepository.save(transaction);
        }
        throw new RuntimeException("Transaction not found");
    }
    
    // Get transaction statistics
    public TransactionStats getTransactionStats() {
        long total = transactionRepository.count();
        long pending = transactionRepository.countByPaymentStatus(PaymentStatus.PENDING);
        long valid = transactionRepository.countByPaymentStatus(PaymentStatus.VALID);
        long invalid = transactionRepository.countByPaymentStatus(PaymentStatus.INVALID);
        long cash = transactionRepository.countByPaymentMethod(PaymentMethod.CASH);
        long transfer = transactionRepository.countByPaymentMethod(PaymentMethod.TRANSFER);
        
        return new TransactionStats(total, pending, valid, invalid, cash, transfer);
    }
    
    // Get old pending transactions (for alerts)
    public List<Transaction> getOldPendingTransactions(int hoursOld) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursOld);
        return transactionRepository.findPendingTransactionsOlderThan(cutoffTime);
    }
    
    // Get recent cash transactions (for employee dashboard)
    public List<Transaction> getRecentCashTransactions(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return transactionRepository.findRecentCashTransactions(fromDate);
    }
    
    // Get transactions by cashier
    public List<Transaction> getTransactionsByCashier(String cashierName) {
        return transactionRepository.findByCashierNameOrderByCreatedAtDesc(cashierName);
    }
    
    // Update payment status
    public Transaction updatePaymentStatus(Long transactionId, PaymentStatus status) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setPaymentStatus(status);
            
            // If marking as valid, update booking status to confirmed
            if (status == PaymentStatus.VALID && transaction.getBooking().getStatus() == BookingStatus.PENDING) {
                Booking booking = transaction.getBooking();
                bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
            }
            
            return transactionRepository.save(transaction);
        } else {
            throw new RuntimeException("Transaction not found");
        }
    }
    
    // Verify transaction
    public Transaction verifyTransaction(Long transactionId, String verifiedBy) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setPaymentStatus(PaymentStatus.VALID);
            transaction.setVerifiedAt(LocalDateTime.now());
            transaction.setVerifiedBy(verifiedBy);
            
            // Update booking status to confirmed
            if (transaction.getBooking().getStatus() == BookingStatus.PENDING) {
                Booking booking = transaction.getBooking();
                bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
            }
            
            return transactionRepository.save(transaction);
        } else {
            throw new RuntimeException("Transaction not found");
        }
    }
    
    // Update transaction notes
    public Transaction updateNotes(Long transactionId, String notes) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setNotes(notes);
            return transactionRepository.save(transaction);
        } else {
            throw new RuntimeException("Transaction not found");
        }
    }
    
    // Save transaction (generic)
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
    
    // Inner class for transaction statistics
    public static class TransactionStats {
        private final long total;
        private final long pending;
        private final long valid;
        private final long invalid;
        private final long cash;
        private final long transfer;
        
        public TransactionStats(long total, long pending, long valid, long invalid, long cash, long transfer) {
            this.total = total;
            this.pending = pending;
            this.valid = valid;
            this.invalid = invalid;
            this.cash = cash;
            this.transfer = transfer;
        }
        
        // Getters
        public long getTotal() { return total; }
        public long getPending() { return pending; }
        public long getValid() { return valid; }
        public long getInvalid() { return invalid; }
        public long getCash() { return cash; }
        public long getTransfer() { return transfer; }
        
        public double getPendingPercentage() {
            return total > 0 ? (double) pending / total * 100 : 0;
        }
        
        public double getValidPercentage() {
            return total > 0 ? (double) valid / total * 100 : 0;
        }
        
        public double getCashPercentage() {
            return total > 0 ? (double) cash / total * 100 : 0;
        }
    }
}
