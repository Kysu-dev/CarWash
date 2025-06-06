package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.repository.TransferRepository;
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
public class TransferService {
    
    @Autowired
    private TransferRepository transferRepository;
    
    @Autowired
    private BookingService bookingService;
    
    // Upload directory for payment proofs
    private static final String UPLOAD_DIR = "uploads/payment-proofs/";
    
    // Create transfer payment
    public Transfer createTransfer(Booking booking, BigDecimal amount, MultipartFile proofFile) throws IOException {
        // Check if transfer already exists for this booking
        Optional<Transfer> existingTransfer = transferRepository.findByBooking(booking);
        if (existingTransfer.isPresent()) {
            throw new RuntimeException("Payment already exists for this booking");
        }
        
        // Save proof file if provided
        String proofFilePath = null;
        if (proofFile != null && !proofFile.isEmpty()) {
            proofFilePath = saveProofFile(proofFile);
        }
        
        Transfer transfer = new Transfer(booking, amount, proofFilePath);
        return transferRepository.save(transfer);
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
        String extension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return UPLOAD_DIR + filename;
    }
    
    // Get transfer by booking
    public Optional<Transfer> getTransferByBooking(Booking booking) {
        return transferRepository.findByBooking(booking);
    }
    
    // Get transfer by ID
    public Optional<Transfer> getTransferById(Long id) {
        return transferRepository.findById(id);
    }
    
    // Get all transfers
    public List<Transfer> getAllTransfers() {
        return transferRepository.findAllWithBookingDetails();
    }
    
    // Get transfers by status
    public List<Transfer> getTransfersByStatus(PaymentStatus status) {
        return transferRepository.findByStatusPembayaranOrderByCreatedAtDesc(status);
    }
    
    // Get pending transfers (for admin review)
    public List<Transfer> getPendingTransfers() {
        return transferRepository.findByStatusPembayaranOrderByTanggalTransaksiAsc(PaymentStatus.PENDING);
    }
    
    // Get transfer by booking ID
    public Transfer getTransferByBookingId(Long bookingId) {
        Optional<Booking> booking = bookingService.getBookingById(bookingId);
        if (booking.isPresent()) {
            Optional<Transfer> transfer = transferRepository.findByBooking(booking.get());
            return transfer.orElse(null);
        }
        return null;
    }

    // Verify payment (admin function)
    public Transfer verifyPayment(Long transferId, String verifiedBy, String notes) {
        Optional<Transfer> optionalTransfer = transferRepository.findById(transferId);
        if (optionalTransfer.isPresent()) {
            Transfer transfer = optionalTransfer.get();
            transfer.verify(verifiedBy, notes);
            
            // Update booking status to confirmed when payment is verified
            Booking booking = transfer.getBooking();
            bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
            
            return transferRepository.save(transfer);
        }
        throw new RuntimeException("Transfer not found");
    }
    
    // Reject payment (admin function)
    public Transfer rejectPayment(Long transferId, String verifiedBy, String notes) {
        Optional<Transfer> optionalTransfer = transferRepository.findById(transferId);
        if (optionalTransfer.isPresent()) {
            Transfer transfer = optionalTransfer.get();
            transfer.reject(verifiedBy, notes);
            
            // Keep booking status as pending when payment is rejected
            // Customer can upload new proof
            
            return transferRepository.save(transfer);
        }
        throw new RuntimeException("Transfer not found");
    }
    
    // Update transfer proof (customer can re-upload if rejected)
    public Transfer updateTransferProof(Long transferId, MultipartFile newProofFile, User user) throws IOException {
        Optional<Transfer> optionalTransfer = transferRepository.findById(transferId);
        if (optionalTransfer.isPresent()) {
            Transfer transfer = optionalTransfer.get();
            
            // Check if user owns this transfer
            if (!transfer.getBooking().getUser().getUserId().equals(user.getUserId())) {
                throw new RuntimeException("You can only update your own transfers");
            }
            
            // Check if transfer can be updated (only if invalid or pending)
            if (transfer.getStatusPembayaran() == PaymentStatus.VALID) {
                throw new RuntimeException("Cannot update verified payment");
            }
            
            // Delete old proof file if exists
            if (transfer.getBuktiTransfer() != null) {
                try {
                    Files.deleteIfExists(Paths.get(transfer.getBuktiTransfer()));
                } catch (IOException e) {
                    // Log error but continue
                    System.err.println("Error deleting old proof file: " + e.getMessage());
                }
            }
            
            // Save new proof file
            String newProofPath = saveProofFile(newProofFile);
            transfer.setBuktiTransfer(newProofPath);
            transfer.setStatusPembayaran(PaymentStatus.PENDING);
            transfer.setTanggalTransaksi(LocalDateTime.now());
            
            return transferRepository.save(transfer);
        }
        throw new RuntimeException("Transfer not found");
    }
      // Verify payment with status and notes
    public Transfer verifyPayment(Long transferId, PaymentStatus status, String notes) {
        Optional<Transfer> optionalTransfer = transferRepository.findById(transferId);
        if (optionalTransfer.isPresent()) {
            Transfer transfer = optionalTransfer.get();
            transfer.setStatusPembayaran(status);
            
            if (notes != null && !notes.trim().isEmpty()) {
                transfer.setNotes(notes);
            }
            
            if (status == PaymentStatus.VALID) {
                // Update booking status to confirmed when payment is verified
                Booking booking = transfer.getBooking();
                bookingService.updateBookingStatus(booking.getIdBooking(), BookingStatus.CONFIRMED);
            }
            
            return transferRepository.save(transfer);
        }
        throw new RuntimeException("Transfer not found");
    }
    
    // Get transfer statistics
    public TransferStats getTransferStats() {
        TransferStats stats = new TransferStats();
        stats.setPendingCount(transferRepository.countByStatusPembayaran(PaymentStatus.PENDING));
        stats.setValidCount(transferRepository.countByStatusPembayaran(PaymentStatus.VALID));
        stats.setInvalidCount(transferRepository.countByStatusPembayaran(PaymentStatus.INVALID));
        return stats;
    }
    
    // Get old pending transfers (for alerts)
    public List<Transfer> getOldPendingTransfers(int hoursOld) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursOld);
        return transferRepository.findPendingTransfersOlderThan(cutoffTime);
    }
    
    // Inner class for transfer statistics
    public static class TransferStats {
        private long pendingCount;
        private long validCount;
        private long invalidCount;
        
        // Getters and setters
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        
        public long getValidCount() { return validCount; }
        public void setValidCount(long validCount) { this.validCount = validCount; }
        
        public long getInvalidCount() { return invalidCount; }
        public void setInvalidCount(long invalidCount) { this.invalidCount = invalidCount; }        public long getTotalCount() { return pendingCount + validCount + invalidCount; }
    }
}
