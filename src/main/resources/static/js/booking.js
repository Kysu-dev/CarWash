// Booking System JavaScript

class BookingSystem {
    constructor() {
        this.currentStep = 1;
        this.maxStep = 5;        this.bookingData = {
            service: null,
            date: null,
            time: null,
            vehicle: {},
            specialNotes: '',
            payment: 'cash',
            total: 0
        };
        
        this.init();
    }
      init() {
        this.setupEventListeners();
        this.generateCalendar();
        this.showStep(1); // Show the first step initially
        this.updateStepIndicators();
        this.updateNavigation();
    }
    
    setupEventListeners() {        // Service selection
        document.querySelectorAll('.service-card').forEach(card => {
            card.addEventListener('click', () => this.selectService(card));
        });
        
        // Navigation buttons
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        
        if (nextBtn) nextBtn.addEventListener('click', () => this.nextStep());
        if (prevBtn) prevBtn.addEventListener('click', () => this.prevStep());
        if (confirmBtn) confirmBtn.addEventListener('click', () => this.confirmBooking());
        
        // Payment method selection
        document.querySelectorAll('input[name="payment"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.bookingData.payment = e.target.value;
            });
        });
        
        // Vehicle form
        this.setupVehicleForm();
    }
      selectService(card) {
        // Remove selection from other cards
        document.querySelectorAll('.service-card').forEach(c => {
            c.classList.remove('selected', 'border-blue-500', 'bg-blue-50');
            c.classList.add('border-gray-200');
        });
        
        // Select current card
        card.classList.add('selected', 'border-blue-500', 'bg-blue-50');
        card.classList.remove('border-gray-200');
        
        // Store service data - use serviceId from data attribute or fallback to service name
        this.bookingData.service = {
            id: card.dataset.serviceId || card.dataset.service,
            name: card.querySelector('h3').textContent,
            price: parseInt(card.dataset.price),
            duration: parseInt(card.dataset.duration)
        };
        
        this.bookingData.total = this.bookingData.service.price;
        
        // Enable next button
        this.updateNavigation();
    }
    
    generateCalendar() {
        const calendarContainer = document.getElementById('custom-calendar');
        if (!calendarContainer) return;
        
        const now = new Date();
        const currentMonth = now.getMonth();
        const currentYear = now.getFullYear();
        
        const calendar = this.createCalendarHTML(currentYear, currentMonth);
        calendarContainer.innerHTML = calendar;
        
        // Add click listeners to calendar days
        this.setupCalendarListeners();
    }
    
    createCalendarHTML(year, month) {
        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];
        
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const firstDay = new Date(year, month, 1).getDay();
        const today = new Date();
        
        let html = `
            <div class="calendar-header flex justify-between items-center mb-4">
                <button type="button" class="calendar-nav-btn" onclick="bookingSystem.navigateCalendar(-1)">
                    <i class="fas fa-chevron-left"></i>
                </button>
                <h4 class="font-semibold text-lg">${monthNames[month]} ${year}</h4>
                <button type="button" class="calendar-nav-btn" onclick="bookingSystem.navigateCalendar(1)">
                    <i class="fas fa-chevron-right"></i>
                </button>
            </div>
            <div class="calendar-grid grid grid-cols-7 gap-1 text-center text-sm">
                <div class="calendar-day-header font-medium text-gray-600 p-2">Sun</div>
                <div class="calendar-day-header font-medium text-gray-600 p-2">Mon</div>
                <div class="calendar-day-header font-medium text-gray-600 p-2">Tue</div>
                <div class="calendar-day-header font-medium text-gray-600 p-2">Wed</div>
                <div class="calendar-day-header font-medium text-gray-600 p-2">Thu</div>
                <div class="calendar-day-header font-medium text-gray-600 p-2">Fri</div>
                <div class="calendar-day-header font-medium text-gray-600 p-2">Sat</div>
        `;
        
        // Empty cells for days before the first day of the month
        for (let i = 0; i < firstDay; i++) {
            html += '<div class="calendar-day-empty"></div>';
        }
        
        // Days of the month
        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(year, month, day);
            const isPast = date < today.setHours(0, 0, 0, 0);
            const isToday = date.toDateString() === new Date().toDateString();
            
            let classes = 'calendar-day cursor-pointer p-2 rounded hover:bg-blue-100 transition-colors';
            
            if (isPast) {
                classes += ' disabled text-gray-300 cursor-not-allowed hover:bg-transparent';
            } else if (isToday) {
                classes += ' border-2 border-blue-500 font-bold';
            }
            
            html += `<div class="${classes}" data-date="${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}">${day}</div>`;
        }
        
        html += '</div>';
        return html;
    }
    
    setupCalendarListeners() {
        document.querySelectorAll('.calendar-day:not(.disabled)').forEach(day => {
            day.addEventListener('click', () => this.selectDate(day));
        });
    }
    
    selectDate(dayElement) {
        // Remove selection from other days
        document.querySelectorAll('.calendar-day').forEach(d => {
            d.classList.remove('selected', 'bg-blue-600', 'text-white');
        });
        
        // Select current day
        dayElement.classList.add('selected', 'bg-blue-600', 'text-white');
        
        this.bookingData.date = dayElement.dataset.date;
        this.generateTimeSlots();
    }
      async generateTimeSlots() {
        const timeSlotsContainer = document.getElementById('time-slots');
        const noSlotsMessage = document.getElementById('no-slots');
        
        if (!timeSlotsContainer || !this.bookingData.date) return;
        
        try {
            // Show loading state
            timeSlotsContainer.innerHTML = '<div class="text-center py-4"><i class="fas fa-spinner fa-spin"></i> Loading time slots...</div>';
            
            // Fetch available time slots from backend
            const response = await fetch(`/customer/api/available-slots?date=${this.bookingData.date}`);
            
            if (!response.ok) {
                throw new Error('Failed to fetch time slots');
            }
            
            const timeSlots = await response.json();
            
            if (timeSlots.length === 0) {
                timeSlotsContainer.classList.add('hidden');
                noSlotsMessage.classList.remove('hidden');
                return;
            }
            
            timeSlotsContainer.classList.remove('hidden');
            noSlotsMessage.classList.add('hidden');
            
            let slotsHTML = '';
            timeSlots.forEach(time => {
                const period = this.getTimePeriod(time);
                slotsHTML += `
                    <button type="button" class="time-slot bg-white border-2 border-gray-200 rounded-lg p-3 text-center hover:border-blue-300 hover:bg-blue-50 transition-all" 
                            data-time="${time}">
                        <div class="font-semibold">${time}</div>
                        <div class="text-xs text-gray-500 capitalize">${period}</div>
                    </button>
                `;
            });
            
            timeSlotsContainer.innerHTML = slotsHTML;
            
            // Add click listeners to time slots
            document.querySelectorAll('.time-slot').forEach(slot => {
                slot.addEventListener('click', () => this.selectTimeSlot(slot));
            });
            
        } catch (error) {
            console.error('Error loading time slots:', error);
            timeSlotsContainer.innerHTML = '<div class="text-center py-4 text-red-600">Failed to load time slots. Please try again.</div>';
        }
    }
    
    getTimePeriod(time) {
        const hour = parseInt(time.split(':')[0]);
        if (hour < 12) return 'morning';
        if (hour < 17) return 'afternoon';
        return 'evening';
    }
    
    selectTimeSlot(slotElement) {
        // Remove selection from other slots
        document.querySelectorAll('.time-slot').forEach(s => {
            s.classList.remove('selected', 'border-blue-500', 'bg-blue-100');
            s.classList.add('border-gray-200');
        });
        
        // Select current slot
        slotElement.classList.add('selected', 'border-blue-500', 'bg-blue-100');
        slotElement.classList.remove('border-gray-200');
        
        this.bookingData.time = slotElement.dataset.time;
        this.updateNavigation();
    }
    
    setupVehicleForm() {
        const vehicleForm = document.getElementById('vehicle-form');
        if (!vehicleForm) return;
        
        const inputs = vehicleForm.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('change', () => {
                this.updateVehicleData();
                this.updateNavigation();
            });
        });
        
        // Special notes
        const specialNotes = document.getElementById('special-notes');
        if (specialNotes) {
            specialNotes.addEventListener('input', () => {
                this.bookingData.specialNotes = specialNotes.value;
            });
        }
    }
    
    updateVehicleData() {
        this.bookingData.vehicle = {
            type: document.getElementById('vehicle-type')?.value || '',
            brand: document.getElementById('vehicle-brand')?.value || '',
            model: document.getElementById('vehicle-model')?.value || '',
            licensePlate: document.getElementById('license-plate')?.value || '',
            color: document.getElementById('vehicle-color')?.value || ''
        };
    }
    
    validateStep(step) {
        switch (step) {
            case 1:
                return this.bookingData.service !== null;
            case 2:
                return this.bookingData.date && this.bookingData.time;
            case 3:
                const vehicle = this.bookingData.vehicle;
                return vehicle.type && vehicle.brand && vehicle.model && 
                       vehicle.licensePlate && vehicle.color;
            case 4:
                return this.bookingData.payment;
            default:
                return true;
        }
    }
    
    nextStep() {
        if (!this.validateStep(this.currentStep)) {
            this.showError('Please complete all required fields before proceeding.');
            return;
        }
        
        if (this.currentStep < this.maxStep) {
            this.showStep(this.currentStep + 1);
        }
    }
    
    prevStep() {
        if (this.currentStep > 1) {
            this.showStep(this.currentStep - 1);
        }
    }
    
    showStep(step) {
        // Hide all steps
        document.querySelectorAll('.step-content').forEach(content => {
            content.classList.add('hidden');
        });
        
        // Show current step
        const currentStepContent = document.getElementById(`step-${step}-content`);
        if (currentStepContent) {
            currentStepContent.classList.remove('hidden');
        }
        
        this.currentStep = step;
        this.updateStepIndicators();
        this.updateNavigation();
        
        // Update summary on step 4
        if (step === 4) {
            this.updateSummary();
        }
        
        // Generate booking ID on step 5
        if (step === 5) {
            this.generateBookingId();
            this.updateFinalDetails();
        }
    }    updateStepIndicators() {
        for (let i = 1; i <= this.maxStep; i++) {
            const stepItem = document.getElementById(`step-${i}`);
            if (!stepItem) continue;
            
            // Remove all state classes
            stepItem.classList.remove('active', 'completed', 'pending');
            
            if (i < this.currentStep) {
                // Completed step
                stepItem.classList.add('completed');
            } else if (i === this.currentStep) {
                // Active step
                stepItem.classList.add('active');
            }
            // Pending steps don't need a specific class as they use default styling
        }
        
        // Update current step counter and title
        const stepNumberElement = document.getElementById('current-step-number');
        const stepTitleElement = document.getElementById('current-step-title');
        
        if (stepNumberElement) {
            stepNumberElement.textContent = this.currentStep;
        }
        
        if (stepTitleElement) {
            const stepTitles = [
                'Choose Your Service Package',
                'Select Date & Time',
                'Enter Vehicle Details', 
                'Review & Payment',
                'Booking Confirmation'
            ];
            stepTitleElement.textContent = stepTitles[this.currentStep - 1] || 'Step';
        }
    }    
    updateNavigation() {
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        
        // Show/hide previous button
        if (prevBtn) {
            if (this.currentStep > 1) {
                prevBtn.classList.remove('hidden');
            } else {
                prevBtn.classList.add('hidden');
            }
        }
        
        // Show/hide next and confirm buttons
        if (this.currentStep === this.maxStep) {
            if (nextBtn) nextBtn.classList.add('hidden');
            if (confirmBtn) confirmBtn.classList.remove('hidden');
        } else {
            if (nextBtn) nextBtn.classList.remove('hidden');
            if (confirmBtn) confirmBtn.classList.add('hidden');
        }
        
        // Enable/disable next button based on validation
        if (nextBtn) {
            const isValid = this.validateStep(this.currentStep);
            nextBtn.disabled = !isValid;
            if (isValid) {
                nextBtn.classList.remove('opacity-50', 'cursor-not-allowed');
            } else {
                nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
            }
        }
    }
    
    updateSummary() {
        // Update service
        const summaryService = document.getElementById('summary-service');
        if (summaryService && this.bookingData.service) {
            summaryService.textContent = this.bookingData.service.name;
        }
        
        // Update date
        const summaryDate = document.getElementById('summary-date');
        if (summaryDate && this.bookingData.date) {
            const date = new Date(this.bookingData.date);
            summaryDate.textContent = date.toLocaleDateString('en-US', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
        }
        
        // Update time
        const summaryTime = document.getElementById('summary-time');
        if (summaryTime && this.bookingData.time) {
            summaryTime.textContent = this.bookingData.time;
        }
        
        // Update duration
        const summaryDuration = document.getElementById('summary-duration');
        if (summaryDuration && this.bookingData.service) {
            summaryDuration.textContent = `${this.bookingData.service.duration} minutes`;
        }
        
        // Update vehicle
        const summaryVehicle = document.getElementById('summary-vehicle');
        if (summaryVehicle && this.bookingData.vehicle) {
            const vehicle = this.bookingData.vehicle;
            summaryVehicle.textContent = `${vehicle.brand} ${vehicle.model} (${vehicle.licensePlate})`;
        }
        
        // Update total
        const summaryTotal = document.getElementById('summary-total');
        if (summaryTotal) {
            summaryTotal.textContent = `Rp ${this.bookingData.total.toLocaleString('id-ID')}`;
        }
    }
    
    generateBookingId() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
        
        const bookingId = `CW-${year}${month}${day}-${random}`;
        this.bookingData.bookingId = bookingId;
        
        const bookingIdElement = document.getElementById('booking-id');
        if (bookingIdElement) {
            bookingIdElement.textContent = bookingId;
        }
    }
    
    updateFinalDetails() {
        // Update final service
        const finalService = document.getElementById('final-service');
        if (finalService && this.bookingData.service) {
            finalService.textContent = this.bookingData.service.name;
        }
        
        // Update final datetime
        const finalDatetime = document.getElementById('final-datetime');
        if (finalDatetime && this.bookingData.date && this.bookingData.time) {
            const date = new Date(this.bookingData.date);
            const formattedDate = date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
            finalDatetime.textContent = `${formattedDate} at ${this.bookingData.time}`;
        }
        
        // Update final amount
        const finalAmount = document.getElementById('final-amount');
        if (finalAmount) {
            finalAmount.textContent = `Rp ${this.bookingData.total.toLocaleString('id-ID')}`;
        }
    }    async confirmBooking() {
        // Update vehicle data one more time
        this.updateVehicleData();
        
        // Validate required fields
        if (!this.bookingData.service || !this.bookingData.date || !this.bookingData.time) {
            this.showError('Please complete all required fields');
            return;
        }
        
        // Show loading state
        const confirmBtn = document.getElementById('confirm-btn');
        const originalText = confirmBtn.innerHTML;
        confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Processing...';
        confirmBtn.disabled = true;
        
        try {
            // Create form data for submission
            const formData = new FormData();
            formData.append('serviceId', this.bookingData.service.id);
            formData.append('date', this.bookingData.date);
            formData.append('time', this.bookingData.time);
            formData.append('notes', this.bookingData.specialNotes || '');
            formData.append('vehicleType', this.bookingData.vehicle.type || '');
            formData.append('vehicleBrand', this.bookingData.vehicle.brand || '');
            formData.append('vehicleModel', this.bookingData.vehicle.model || '');
            formData.append('licensePlate', this.bookingData.vehicle.licensePlate || '');
            formData.append('vehicleColor', this.bookingData.vehicle.color || '');            // Submit booking to backend
            const response = await fetch('/customer/booking/create', {
                method: 'POST',
                body: formData
            });
            
            // Check if response is ok
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            // Parse JSON response
            const result = await response.json();
            
            if (result.success) {
                // Show success popup and redirect to dashboard
                this.showSuccessPopup(result.message, result.bookingId);
                
                // Redirect to dashboard after 2 seconds
                setTimeout(() => {
                    window.location.href = '/customer';
                }, 2000);
            } else {
                throw new Error(result.message || 'Booking submission failed');
            }
            
        } catch (error) {
            console.error('Error creating booking:', error);
            this.showError('Failed to create booking. Please try again.');
            
            // Reset button
            confirmBtn.innerHTML = originalText;
            confirmBtn.disabled = false;
        }
    }
      navigateCalendar(direction) {
        // This would update the calendar month
        console.log('Navigate calendar:', direction);
        // Implementation would regenerate calendar for previous/next month
    }
    
    showError(message) {
        // Simple error display - you can enhance this with better UI
        alert(message);
    }
    
    showSuccessPopup(message, bookingId) {
        // Create popup overlay
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 1000;
        `;
        
        // Create popup content
        const popup = document.createElement('div');
        popup.style.cssText = `
            background: white;
            padding: 2rem;
            border-radius: 1rem;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
            max-width: 400px;
            text-align: center;
            animation: popup-appear 0.3s ease-out;
        `;
        
        popup.innerHTML = `
            <div style="color: #10B981; font-size: 3rem; margin-bottom: 1rem;">
                <i class="fas fa-check-circle"></i>
            </div>
            <h2 style="color: #1F2937; margin-bottom: 0.5rem; font-size: 1.5rem;">Booking Berhasil!</h2>
            <p style="color: #6B7280; margin-bottom: 1rem;">${message}</p>
            <p style="color: #3B82F6; font-weight: bold; margin-bottom: 1.5rem;">
                Booking ID: ${bookingId}
            </p>
            <div style="color: #6B7280; font-size: 0.875rem;">
                Mengarahkan ke dashboard...
            </div>
        `;
        
        // Add CSS animation
        if (!document.getElementById('popup-animation-style')) {
            const style = document.createElement('style');
            style.id = 'popup-animation-style';
            style.textContent = `
                @keyframes popup-appear {
                    from {
                        opacity: 0;
                        transform: scale(0.8) translateY(-20px);
                    }
                    to {
                        opacity: 1;
                        transform: scale(1) translateY(0);
                    }
                }
            `;
            document.head.appendChild(style);
        }
        
        overlay.appendChild(popup);
        document.body.appendChild(overlay);
        
        // Auto-remove popup after 2 seconds
        setTimeout(() => {
            overlay.remove();
        }, 2000);
    }
}

// Initialize booking system when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.bookingSystem = new BookingSystem();
});
