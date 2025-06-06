package UASPraktikum.CarWash.config;

import UASPraktikum.CarWash.model.BookingSlot;
import UASPraktikum.CarWash.repository.BookingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BookingSlotDataLoader implements CommandLineRunner {

    @Autowired
    private BookingSlotRepository bookingSlotRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if no slots exist
        if (bookingSlotRepository.count() == 0) {
            initializeBookingSlots();
        }
    }

    private void initializeBookingSlots() {
        System.out.println("Initializing booking slots...");
        
        List<BookingSlot> slots = new ArrayList<>();
        LocalDate startDate = LocalDate.now();
        
        // Create slots for the next 30 days
        for (int day = 0; day < 30; day++) {
            LocalDate currentDate = startDate.plusDays(day);
            
            // Create time slots from 8:00 AM to 6:00 PM (every hour)
            for (int hour = 8; hour <= 18; hour++) {
                LocalTime slotTime = LocalTime.of(hour, 0);
                
                BookingSlot slot = new BookingSlot(currentDate, slotTime);
                slots.add(slot);
            }
        }
        
        // Save all slots to database
        bookingSlotRepository.saveAll(slots);
        
        System.out.println("Initialized " + slots.size() + " booking slots successfully!");
    }
}
