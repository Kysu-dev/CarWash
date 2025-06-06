package UASPraktikum.CarWash.service;

import UASPraktikum.CarWash.model.BookingSlot;
import UASPraktikum.CarWash.repository.BookingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingSlotService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingSlotService.class);
    
    @Autowired
    private BookingSlotRepository bookingSlotRepository;
    
    // Available time slots (can be configured)
    private static final LocalTime[] AVAILABLE_TIMES = {
        LocalTime.of(8, 0),   // 08:00
        LocalTime.of(9, 0),   // 09:00
        LocalTime.of(10, 0),  // 10:00
        LocalTime.of(11, 0),  // 11:00
        LocalTime.of(13, 0),  // 13:00
        LocalTime.of(14, 0),  // 14:00
        LocalTime.of(15, 0),  // 15:00
        LocalTime.of(16, 0),  // 16:00
        LocalTime.of(17, 0),  // 17:00
        LocalTime.of(18, 0)   // 18:00
    };
    
    // Initialize slots for a specific date
    public void initializeSlotsForDate(LocalDate date) {
        logger.info("Initializing slots for date: {}", date);
        
        for (LocalTime time : AVAILABLE_TIMES) {
            if (!bookingSlotRepository.existsByTanggalAndJam(date, time)) {
                BookingSlot slot = new BookingSlot(date, time);
                bookingSlotRepository.save(slot);
                logger.debug("Created slot: {} at {}", date, time);
            }
        }
    }
    
    // Initialize slots for a date range
    public void initializeSlotsForDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            initializeSlotsForDate(currentDate);
            currentDate = currentDate.plusDays(1);
        }
    }
    
    // Get all available time slots for a specific date
    public List<LocalTime> getAvailableTimeSlots(LocalDate date) {
        // Initialize slots if they don't exist
        initializeSlotsForDate(date);
        
        // Get available slots
        List<BookingSlot> availableSlots = bookingSlotRepository.findAvailableSlotsByDate(date, BookingSlot.MAX_CAPACITY);
        
        return availableSlots.stream()
                .map(BookingSlot::getJam)
                .collect(Collectors.toList());
    }
    
    // Get available time slots as formatted strings
    public List<String> getAvailableTimeSlotsAsStrings(LocalDate date) {
        return getAvailableTimeSlots(date).stream()
                .map(time -> time.toString())
                .collect(Collectors.toList());
    }
    
    // Book a time slot
    public boolean bookSlot(LocalDate date, LocalTime time) {
        try {
            Optional<BookingSlot> slotOpt = bookingSlotRepository.findByTanggalAndJam(date, time);
            
            BookingSlot slot;
            if (slotOpt.isPresent()) {
                slot = slotOpt.get();
            } else {
                // Create new slot if it doesn't exist
                slot = new BookingSlot(date, time);
                slot = bookingSlotRepository.save(slot);
            }
            
            if (slot.canBook()) {
                slot.incrementBooking();
                bookingSlotRepository.save(slot);
                logger.info("Booked slot: {} at {} (occupied: {}/{})", date, time, slot.getJumlahTerisi(), BookingSlot.MAX_CAPACITY);
                return true;
            } else {
                logger.warn("Cannot book slot: {} at {} - slot is full", date, time);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error booking slot: {} at {}", date, time, e);
            return false;
        }
    }
    
    // Release a time slot (when booking is cancelled)
    public boolean releaseSlot(LocalDate date, LocalTime time) {
        try {
            Optional<BookingSlot> slotOpt = bookingSlotRepository.findByTanggalAndJam(date, time);
            
            if (slotOpt.isPresent()) {
                BookingSlot slot = slotOpt.get();
                slot.decrementBooking();
                bookingSlotRepository.save(slot);
                logger.info("Released slot: {} at {} (occupied: {}/{})", date, time, slot.getJumlahTerisi(), BookingSlot.MAX_CAPACITY);
                return true;
            } else {
                logger.warn("Cannot release slot: {} at {} - slot not found", date, time);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error releasing slot: {} at {}", date, time, e);
            return false;
        }
    }
    
    // Check if a specific time slot is available
    public boolean isSlotAvailable(LocalDate date, LocalTime time) {
        Optional<BookingSlot> slotOpt = bookingSlotRepository.findByTanggalAndJam(date, time);
        
        if (slotOpt.isPresent()) {
            return slotOpt.get().isAvailable();
        } else {
            // If slot doesn't exist, it means it's available (will be created when booked)
            return true;
        }
    }
    
    // Get slot details
    public Optional<BookingSlot> getSlot(LocalDate date, LocalTime time) {
        return bookingSlotRepository.findByTanggalAndJam(date, time);
    }
    
    // Get all slots for a date
    public List<BookingSlot> getSlotsForDate(LocalDate date) {
        // Initialize slots if they don't exist
        initializeSlotsForDate(date);
        return bookingSlotRepository.findByTanggalOrderByJam(date);
    }
    
    // Get slots with occupancy info for admin/employee view
    public List<BookingSlot> getSlotsWithOccupancy(LocalDate date) {
        initializeSlotsForDate(date);
        return bookingSlotRepository.findByTanggalOrderByJam(date);
    }
    
    // Clean up old slots (can be scheduled)
    public void cleanupOldSlots(LocalDate beforeDate) {
        logger.info("Cleaning up slots before: {}", beforeDate);
        bookingSlotRepository.deleteByTanggalBefore(beforeDate);
    }
    
    // Get booking statistics
    public long getTotalAvailableSlots(LocalDate date) {
        return bookingSlotRepository.countAvailableSlotsByDate(date, BookingSlot.MAX_CAPACITY);
    }
    
    // Initialize slots for the next N days
    public void initializeNextDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        initializeSlotsForDateRange(today, endDate);
        logger.info("Initialized slots for next {} days", days);
    }
}
