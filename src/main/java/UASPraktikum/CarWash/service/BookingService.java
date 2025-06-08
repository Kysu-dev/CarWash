package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.*;
import UASPraktikum.CarWash.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
      @Autowired
    private BookingRepository bookingRepository;
      @Autowired
    private BookingSlotService bookingSlotService;
    
    @Autowired
    private ServiceService serviceService;
    
    // Business hours configuration
    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0); // 08:00
    private static final LocalTime CLOSING_TIME = LocalTime.of(17, 0); // 17:00
    
    // Create a new booking
    public Booking createBooking(User user, UASPraktikum.CarWash.model.Service service, 
                               LocalDate tanggal, LocalTime jam, BookingMethod metode, String catatan) {
        // Check if time slot is available using BookingSlotService
        if (!bookingSlotService.isSlotAvailable(tanggal, jam)) {
            throw new RuntimeException("Time slot is not available");
        }
        
        // Check if date is valid (not in the past)
        if (tanggal.isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot book for past dates");
        }
        
        // Book the slot first
        if (!bookingSlotService.bookSlot(tanggal, jam)) {
            throw new RuntimeException("Failed to book time slot");
        }
        
        try {
            Booking booking = new Booking(user, service, tanggal, jam, metode, catatan);
            return bookingRepository.save(booking);
        } catch (Exception e) {
            // If booking creation fails, release the slot
            bookingSlotService.releaseSlot(tanggal, jam);
            throw e;
        }
    }
    
    // Create booking with vehicle details
    public Booking createBookingWithVehicle(User user, UASPraktikum.CarWash.model.Service service, 
                                          LocalDate tanggal, LocalTime jam, BookingMethod metode, 
                                          String catatan, String vehicleType, String vehicleBrand, 
                                          String vehicleModel, String licensePlate, String vehicleColor) {
        Booking booking = createBooking(user, service, tanggal, jam, metode, catatan);
        
        // Set vehicle details
        booking.setVehicleType(vehicleType);
        booking.setVehicleBrand(vehicleBrand);
        booking.setVehicleModel(vehicleModel);
        booking.setLicensePlate(licensePlate);
        booking.setVehicleColor(vehicleColor);
        
        return bookingRepository.save(booking);
    }
      // Create booking from Booking object
    public Booking createBooking(Booking booking) {
        // Check if time slot is available using BookingSlotService
        if (!bookingSlotService.isSlotAvailable(booking.getTanggal(), booking.getJam())) {
            throw new RuntimeException("Time slot is not available");
        }
        
        // Check if date is valid (not in the past)
        if (booking.getTanggal().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot book for past dates");
        }
        
        // Book the slot first
        if (!bookingSlotService.bookSlot(booking.getTanggal(), booking.getJam())) {
            throw new RuntimeException("Failed to book time slot");
        }
        
        try {
            return bookingRepository.save(booking);
        } catch (Exception e) {
            // If booking creation fails, release the slot
            bookingSlotService.releaseSlot(booking.getTanggal(), booking.getJam());
            throw e;
        }
    }

    // Get booking by ID
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
    
    // Get all bookings for a user
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    // Get upcoming bookings for a user
    public List<Booking> getUpcomingBookingsByUser(User user) {
        return bookingRepository.findUpcomingBookingsByUser(user, LocalDate.now());
    }
    
    // Get bookings by status
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    // Get all bookings
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    // Get today's bookings
    public List<Booking> getTodaysBookings() {
        return bookingRepository.findTodaysBookings(LocalDate.now());
    }
    
    // Get bookings for a specific date
    public List<Booking> getBookingsForDate(LocalDate date) {
        return bookingRepository.findByTanggalOrderByJamAsc(date);
    }
    
    // Get bookings by date
    public List<Booking> getBookingsByDate(LocalDate date) {
        return bookingRepository.findByTanggalOrderByJamAsc(date);
    }    // Check if time slot is available
    public boolean isTimeSlotAvailable(LocalDate tanggal, LocalTime jam) {
        // Check if date is valid
        if (tanggal.isBefore(LocalDate.now())) {
            return false;
        }
        
        // Check if time is within business hours
        if (jam.isBefore(OPENING_TIME) || jam.isAfter(CLOSING_TIME)) {
            return false;
        }
        
        // Use BookingSlotService to check availability
        return bookingSlotService.isSlotAvailable(tanggal, jam);
    }      // Get available time slots for a date
    public List<LocalTime> getAvailableTimeSlots(LocalDate date) {
        // Use BookingSlotService to get available slots
        return bookingSlotService.getAvailableTimeSlots(date);
    }
    
    // Update booking status
    public Booking updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        Optional<Booking> optionalBooking = bookingRepository.findById(bookingId);
        if (optionalBooking.isPresent()) {
            Booking booking = optionalBooking.get();
            booking.setStatus(newStatus);
            return bookingRepository.save(booking);
        }
        throw new RuntimeException("Booking not found");
    }

    // Update booking status with notes
    public Booking updateBookingStatus(Long bookingId, BookingStatus newStatus, String notes) {
        Optional<Booking> optionalBooking = bookingRepository.findById(bookingId);
        if (optionalBooking.isPresent()) {
            Booking booking = optionalBooking.get();
            booking.setStatus(newStatus);
            if (notes != null && !notes.trim().isEmpty()) {
                String currentNotes = booking.getCatatan();
                String updatedNotes = currentNotes != null ? currentNotes + "\n" + notes : notes;
                booking.setCatatan(updatedNotes);
            }
            return bookingRepository.save(booking);
        }
        throw new RuntimeException("Booking not found");
    }
    
    // Cancel booking
    public boolean cancelBooking(Long bookingId, User user) {
        Optional<Booking> optionalBooking = bookingRepository.findById(bookingId);
        if (optionalBooking.isPresent()) {
            Booking booking = optionalBooking.get();
            
            // Check if user owns this booking
            if (!booking.getUser().getUserId().equals(user.getUserId())) {
                throw new RuntimeException("You can only cancel your own bookings");
            }
              // Check if booking can be cancelled
            if (!booking.canBeCancelled()) {
                throw new RuntimeException("This booking cannot be cancelled");
            }
            
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            // Release the booking slot
            bookingSlotService.releaseSlot(booking.getTanggal(), booking.getJam());
            
            return true;
        }
        return false;
    }
    
    // Confirm booking (admin/employee function)
    public Booking confirmBooking(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
    }
    
    // Start service (employee function)
    public Booking startService(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.IN_PROGRESS);
    }
    
    // Complete service (employee function)
    public Booking completeService(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.COMPLETED);
    }
      // Cancel booking (customer function)
    public Booking cancelBooking(Long bookingId, String reason) {
        logger.info("Attempting to cancel booking ID: {} with reason: {}", bookingId, reason);
        
        Optional<Booking> bookingOpt = getBookingById(bookingId);
        if (bookingOpt.isEmpty()) {
            logger.error("Booking not found with ID: {}", bookingId);
            throw new RuntimeException("Booking not found");
        }
        
        Booking booking = bookingOpt.get();
        logger.info("Found booking with status: {}", booking.getStatus());
        
        // Only allow cancellation for PENDING or CONFIRMED bookings
        if (booking.getStatus() != BookingStatus.PENDING && 
            booking.getStatus() != BookingStatus.CONFIRMED) {
            logger.error("Cannot cancel booking with status: {}", booking.getStatus());
            throw new RuntimeException("Cannot cancel booking with status: " + booking.getStatus());
        }
        
        // Release the booking slot
        logger.info("Releasing booking slot for date: {} time: {}", booking.getTanggal(), booking.getJam());
        boolean slotReleased = bookingSlotService.releaseSlot(booking.getTanggal(), booking.getJam());
        logger.info("Slot release result: {}", slotReleased);
        
        // Update booking status to CANCELLED
        String cancellationNotes = "Cancelled by customer";
        if (reason != null && !reason.trim().isEmpty()) {
            cancellationNotes += ": " + reason;
        }
        
        logger.info("Updating booking status to CANCELLED with notes: {}", cancellationNotes);
        Booking cancelledBooking = updateBookingStatus(bookingId, BookingStatus.CANCELLED, cancellationNotes);
        logger.info("Booking cancelled successfully. New status: {}", cancelledBooking.getStatus());
        
        return cancelledBooking;
    }
      // Check if booking can be cancelled
    public boolean canBeCancelled(Booking booking) {
        if (booking == null) {
            logger.warn("Cannot cancel null booking");
            return false;
        }
        
        logger.info("Checking if booking can be cancelled. Status: {}, Date: {}", 
                   booking.getStatus(), booking.getTanggal());
        
        // Can only cancel PENDING or CONFIRMED bookings
        if (booking.getStatus() != BookingStatus.PENDING && 
            booking.getStatus() != BookingStatus.CONFIRMED) {
            logger.warn("Cannot cancel booking with status: {}", booking.getStatus());
            return false;
        }
        
        // Can only cancel future bookings (allow same day cancellation before the time)
        LocalDate today = LocalDate.now();
        if (booking.getTanggal().isBefore(today)) {
            logger.warn("Cannot cancel past booking. Booking date: {}, Today: {}", 
                       booking.getTanggal(), today);
            return false;
        }
        
        logger.info("Booking can be cancelled");
        return true;
    }
    
    // Get total bookings count
    public long getTotalBookings() {
        return bookingRepository.count();
    }

    // Get recent bookings
    public List<Booking> getRecentBookings(int limit) {
        return bookingRepository.findAll().stream()
            .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
            .limit(limit)
            .toList();
    }
    
    // Get available time slots as strings
    public List<String> getAvailableTimeSlotsAsStrings(LocalDate date) {
        List<LocalTime> timeSlots = getAvailableTimeSlots(date);
        return timeSlots.stream()
            .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm")))
            .toList();
    }

    // Get booking statistics for date range
    public java.util.Map<String, Object> getBookingStatistics(LocalDate startDate, LocalDate endDate) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        List<Booking> bookings = bookingRepository.findByTanggalBetween(startDate, endDate);
        
        stats.put("totalBookings", bookings.size());
        stats.put("confirmedBookings", bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count());
        stats.put("completedBookings", bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count());
        stats.put("cancelledBookings", bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count());
        
        return stats;
    }
    
    // Create walk-in booking for employees
    public Booking createWalkInBooking(User customer, Long serviceId, String date, String time,
                                       String vehicleType, String vehicleBrand, String vehicleModel,
                                       String licensePlate, String vehicleColor, String notes,
                                       String employeeName) {
        try {
            // Parse date and time
            LocalDate bookingDate = LocalDate.parse(date);
            LocalTime bookingTime = LocalTime.parse(time);
            
            // Check if time slot is available
            if (!bookingSlotService.isSlotAvailable(bookingDate, bookingTime)) {
                throw new RuntimeException("Time slot is not available");
            }
            
            // Check if date is valid (not in the past)
            if (bookingDate.isBefore(LocalDate.now())) {
                throw new RuntimeException("Cannot book for past dates");
            }
              // Get service (assuming ServiceService is available via autowiring)
            // We'll need to add this service
            
            UASPraktikum.CarWash.model.Service service = serviceService.getServiceById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceId));
            
            // For now, create a simple booking record
            Booking booking = new Booking();
            booking.setUser(customer);
            booking.setService(service);
            booking.setTanggal(bookingDate);
            booking.setJam(bookingTime);
            booking.setMetode(BookingMethod.WALKIN);
            booking.setCatatan(notes != null ? notes + " (Created by: " + employeeName + ")" : "Created by: " + employeeName);
            booking.setStatus(BookingStatus.CONFIRMED); // Walk-in bookings are confirmed immediately
            
            // Set vehicle details
            booking.setVehicleType(vehicleType);
            booking.setVehicleBrand(vehicleBrand);
            booking.setVehicleModel(vehicleModel);
            booking.setLicensePlate(licensePlate);
            booking.setVehicleColor(vehicleColor);
            
            // Book the slot
            if (!bookingSlotService.bookSlot(bookingDate, bookingTime)) {
                throw new RuntimeException("Failed to book time slot");
            }
            
            try {
                return bookingRepository.save(booking);
            } catch (Exception e) {
                // If booking creation fails, release the slot
                bookingSlotService.releaseSlot(bookingDate, bookingTime);
                throw e;
            }
            
        } catch (Exception e) {
            logger.error("Error creating walk-in booking", e);
            throw new RuntimeException("Failed to create walk-in booking: " + e.getMessage());
        }
    }

    // Get booking statistics
    public BookingStats getBookingStats() {
        BookingStats stats = new BookingStats();
        stats.setPendingCount(bookingRepository.countByStatus(BookingStatus.PENDING));
        stats.setConfirmedCount(bookingRepository.countByStatus(BookingStatus.CONFIRMED));
        stats.setInProgressCount(bookingRepository.countByStatus(BookingStatus.IN_PROGRESS));
        stats.setCompletedCount(bookingRepository.countByStatus(BookingStatus.COMPLETED));
        stats.setCancelledCount(bookingRepository.countByStatus(BookingStatus.CANCELLED));
        return stats;    }
  
    
    // Update booking notes
    public void updateBookingNotes(Long bookingId, String notes) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setCatatan(notes);
        bookingRepository.save(booking);
    }

    // Inner class for booking statistics
    public static class BookingStats {
        private long pendingCount;
        private long confirmedCount;
        private long inProgressCount;
        private long completedCount;
        private long cancelledCount;
        
        // Getters and setters
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        
        public long getConfirmedCount() { return confirmedCount; }
        public void setConfirmedCount(long confirmedCount) { this.confirmedCount = confirmedCount; }
        
        public long getInProgressCount() { return inProgressCount; }
        public void setInProgressCount(long inProgressCount) { this.inProgressCount = inProgressCount; }
        
        public long getCompletedCount() { return completedCount; }
        public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
        
        public long getCancelledCount() { return cancelledCount; }
        public void setCancelledCount(long cancelledCount) { this.cancelledCount = cancelledCount; }
        
        public long getTotalCount() { 
            return pendingCount + confirmedCount + inProgressCount + completedCount + cancelledCount; 
        }
    }
}
