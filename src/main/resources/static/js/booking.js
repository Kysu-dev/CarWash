// booking.js - Versi Final Menggunakan Library Flatpickr (Lebih Stabil)

class BookingSystem {
    constructor() {
        this.currentStep = 1;
        this.maxStep = 4; // Updated from 5 to 4 steps (removed payment method selection)
        this.flatpickrInstance = null; // Untuk menyimpan instance flatpickr

        this.bookingData = {
            service: null,
            date: null,
            time: null,
            vehicle: {},
            specialNotes: '',
            payment: 'cash', // Always cash payment
            total: 0
        };

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }    init() {
        this.setupEventListeners();
        this.initializeCalendar();
        this.showStep(1);
        
        // Add debugging helper (remove in production)
        this.addDebugHelpers();
    }    // Debug helpers - remove in production
    addDebugHelpers() {
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            // Add debugging buttons
            const debugDiv = document.createElement('div');
            debugDiv.style.position = 'fixed';
            debugDiv.style.top = '10px';
            debugDiv.style.right = '10px';
            debugDiv.style.zIndex = '9999';
            debugDiv.style.background = 'rgba(0,0,0,0.8)';
            debugDiv.style.color = 'white';
            debugDiv.style.padding = '10px';
            debugDiv.style.borderRadius = '5px';
            debugDiv.style.fontSize = '12px';
            
            debugDiv.innerHTML = `
                <div>Debug Panel</div>
                <button onclick="window.bookingSystem.debugForceNextStep()" style="background: #007bff; color: white; border: none; padding: 5px; margin: 2px; border-radius: 3px;">Force Next</button>
                <button onclick="window.bookingSystem.debugLogState()" style="background: #28a745; color: white; border: none; padding: 5px; margin: 2px; border-radius: 3px;">Log State</button>
                <button onclick="window.bookingSystem.debugSetTestData()" style="background: #ffc107; color: black; border: none; padding: 5px; margin: 2px; border-radius: 3px;">Set Test Data</button>
            `;
            
            document.body.appendChild(debugDiv);
            
            // Make instance globally accessible for debugging
            window.bookingSystem = this;
        }
    }

    // Debug methods - remove in production
    debugForceNextStep() {
        console.log('Debug: Forcing next step');
        this.showStep(this.currentStep + 1);
    }

    debugLogState() {
        console.log('Current state:', {
            step: this.currentStep,
            bookingData: this.bookingData,
            validation: this.validateStep(this.currentStep)
        });
    }

    debugSetTestData() {
        console.log('Debug: Setting test data for step 2');
        this.bookingData.date = '2025-06-07';
        this.bookingData.time = '10:00';
        console.log('Test data set:', this.bookingData);
        this.updateNavigation();
    }// Fungsi untuk inisialisasi Flatpickr
    initializeCalendar() {
        const calendarContainer = document.getElementById('custom-calendar');
        if (!calendarContainer) {
            console.warn('Calendar container not found');
            return;
        }

        // Check if flatpickr is available
        if (typeof flatpickr === 'undefined') {
            console.warn('Flatpickr library not loaded, using fallback');
            this.createFallbackCalendar();
            return;
        }

        this.flatpickrInstance = flatpickr(calendarContainer, {
            inline: true, // Membuat kalender selalu terlihat
            minDate: "today", // Tidak bisa memilih tanggal lampau
            dateFormat: "Y-m-d", // Format tanggal yang dikirim
            onChange: (selectedDates, dateStr) => {
                // Fungsi ini akan berjalan setiap kali tanggal dipilih
                console.log('Flatpickr onChange triggered:', dateStr);
                this.selectDate(dateStr);
            },
        });
    }

    // Fallback calendar jika flatpickr tidak tersedia
    createFallbackCalendar() {
        const calendarContainer = document.getElementById('custom-calendar');
        const today = new Date();
        const currentDate = today.toISOString().split('T')[0];
        
        calendarContainer.innerHTML = `
            <div class="fallback-calendar">
                <label for="date-input" class="block text-sm font-medium text-gray-700 mb-2">Select Date:</label>
                <input type="date" id="date-input" class="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500" min="${currentDate}" />
            </div>
        `;
        
        const dateInput = document.getElementById('date-input');
        if (dateInput) {
            dateInput.addEventListener('change', (e) => {
                console.log('Fallback date input changed:', e.target.value);
                this.selectDate(e.target.value);
            });
        }
    }

    showStep(step) {
        document.querySelectorAll('.step-content').forEach(content => content.classList.add('hidden'));
        const currentStepContent = document.getElementById(`step-${step}-content`);
        if (currentStepContent) {
            currentStepContent.classList.remove('hidden');
        }
        this.currentStep = step;
        this.updateStepIndicators();
        this.updateNavigation();
        if (step === 4) {
            this.updateSummary();
        }
    }

    selectDate(dateString) {
        if (!dateString) return;
        console.log('Date selected:', dateString);
        this.bookingData.date = dateString;
        this.bookingData.time = null; // Reset waktu saat tanggal berubah
        console.log('Booking data after date selection:', this.bookingData);
        this.generateTimeSlots(); // Langsung generate time slot
        this.updateNavigation(); // Update tombol navigasi
    }    setupEventListeners() {
        document.querySelectorAll('.service-card').forEach(card => card.addEventListener('click', () => this.selectService(card)));
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        if (nextBtn) nextBtn.addEventListener('click', () => this.nextStep());
        if (prevBtn) prevBtn.addEventListener('click', () => this.prevStep());
        if (confirmBtn) confirmBtn.addEventListener('click', () => this.confirmBooking());
        
        this.setupVehicleForm();
    }    selectService(card) {
        document.querySelectorAll('.service-card').forEach(c => c.classList.remove('selected', 'border-blue-500', 'bg-blue-50'));
        card.classList.add('selected', 'border-blue-500', 'bg-blue-50');
        this.bookingData.service = {
            id: card.dataset.serviceId,
            name: card.dataset.service,
            price: parseInt(card.dataset.price),
            duration: card.dataset.duration
        };
        this.bookingData.total = this.bookingData.service.price;
        this.updateNavigation();
    }

    async generateTimeSlots() {
        const timeSlotsContainer = document.getElementById('time-slots');
        const noSlotsMessage = document.getElementById('no-slots');
        if (!timeSlotsContainer || !this.bookingData.date) return;
        
        try {
            timeSlotsContainer.innerHTML = '<div class="text-center py-4 col-span-2"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
            noSlotsMessage.classList.add('hidden');
            timeSlotsContainer.classList.remove('hidden');
            
            const response = await fetch(`/customer/api/available-slots?date=${this.bookingData.date}`);
            if (!response.ok) throw new Error('Failed to fetch time slots');
            
            const timeSlots = await response.json();
            
            if (timeSlots.length === 0) {
                timeSlotsContainer.innerHTML = '';
                timeSlotsContainer.classList.add('hidden');
                noSlotsMessage.classList.remove('hidden');
                return;
            }
            
            let slotsHTML = '';
            timeSlots.forEach(time => {
                slotsHTML += `<button type="button" class="time-slot border-2 rounded-lg p-3 text-center transition-all bg-white border-gray-200 hover:border-blue-300 hover:bg-blue-50" data-time="${time}">${time}</button>`;
            });
            
            timeSlotsContainer.innerHTML = slotsHTML;
            document.querySelectorAll('.time-slot').forEach(slot => {
                slot.addEventListener('click', () => this.selectTimeSlot(slot));
            });
        } catch (error) {
            console.error('Error loading time slots:', error);
            timeSlotsContainer.innerHTML = '<div class="text-center py-4 text-red-600 col-span-2">Failed to load time slots.</div>';
        }
    }    selectTimeSlot(slotElement) {
        // Remove selection from all slots
        document.querySelectorAll('.time-slot').forEach(s => {
            s.classList.remove('selected', 'border-blue-500', 'bg-blue-100');
        });
        
        // Add selection to clicked slot
        slotElement.classList.add('selected', 'border-blue-500', 'bg-blue-100');
        
        // Store the selected time
        this.bookingData.time = slotElement.dataset.time;
        
        console.log('Time selected:', this.bookingData.time);
        console.log('Booking data after time selection:', this.bookingData);
        console.log('Step 2 validation result:', this.validateStep(2));
        
        // Force update navigation
        setTimeout(() => {
            this.updateNavigation();
        }, 100);
    }

    setupVehicleForm() {
        const vehicleForm = document.getElementById('vehicle-form');
        if (!vehicleForm) return;
        
        const inputs = vehicleForm.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('input', () => {
                this.updateVehicleData();
                this.updateNavigation();
            });
        });
    }

    updateVehicleData() {
        this.bookingData.vehicle = {
            type: document.getElementById('vehicle-type')?.value || '',
            brand: document.getElementById('vehicle-brand')?.value || '',
            model: document.getElementById('vehicle-model')?.value || '',
            licensePlate: document.getElementById('license-plate')?.value || '',
            color: document.getElementById('vehicle-color')?.value || ''
        };
        this.bookingData.specialNotes = document.getElementById('special-notes')?.value || '';
    }    validateStep(step) {
        switch (step) {
            case 1: 
                return !!this.bookingData.service;
            case 2: 
                const dateValid = !!this.bookingData.date;
                const timeValid = !!this.bookingData.time;
                console.log('Date validation:', dateValid, this.bookingData.date);
                console.log('Time validation:', timeValid, this.bookingData.time);
                return dateValid && timeValid;
            case 3: 
                const v = this.bookingData.vehicle; 
                return !!v.type && !!v.brand && !!v.model && !!v.licensePlate && !!v.color;
            case 4: 
                // Review step - all data should be valid and payment is automatically cash
                return !!this.bookingData.service && !!this.bookingData.date && !!this.bookingData.time && this.bookingData.payment === 'cash';
            default: 
                return true;
        }
    }

    nextStep() {
        const isValid = this.validateStep(this.currentStep);
        console.log(`Step ${this.currentStep} validation:`, isValid);
        
        if (!isValid) {
            this.showError('Please complete all required fields.');
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

    updateStepIndicators() {
        document.querySelectorAll('.step-circle').forEach((circle, index) => {
            circle.classList.remove('active', 'completed');
            if (index + 1 < this.currentStep) {
                circle.classList.add('completed');
            } else if (index + 1 === this.currentStep) {
                circle.classList.add('active');
            }
        });
          document.getElementById('current-step-number').textContent = this.currentStep;
        const titles = ['Choose Service', 'Select Schedule', 'Vehicle Details', 'Review & Confirmation'];
        document.getElementById('current-step-title').textContent = titles[this.currentStep - 1];
    }    updateNavigation() {
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        const isValid = this.validateStep(this.currentStep);
        
        console.log(`Navigation update - Step: ${this.currentStep}, Valid: ${isValid}`);
        console.log('Current booking data:', this.bookingData);
        
        // Show/hide buttons based on current step (updated for 4 steps)
        prevBtn.classList.toggle('hidden', this.currentStep <= 1 || this.currentStep === 4);
        nextBtn.classList.toggle('hidden', this.currentStep >= 3);
        confirmBtn.classList.toggle('hidden', this.currentStep !== 4);
        
        // Enable/disable next button based on validation
        if (nextBtn) {
            nextBtn.disabled = !isValid;
            nextBtn.classList.toggle('opacity-50', !isValid);
            nextBtn.classList.toggle('cursor-not-allowed', !isValid);
            
            // Additional debugging for step 2
            if (this.currentStep === 2) {
                console.log('Step 2 debugging:');
                console.log('Date:', this.bookingData.date);
                console.log('Time:', this.bookingData.time);
                console.log('Date valid:', !!this.bookingData.date);
                console.log('Time valid:', !!this.bookingData.time);
                console.log('Overall valid:', !!this.bookingData.date && !!this.bookingData.time);
            }
        }
    }    updateSummary() {
        document.getElementById('summary-service').textContent = this.bookingData.service?.name || '-';
        document.getElementById('summary-duration').textContent = `~${this.bookingData.service?.duration || 0} min`;
        document.getElementById('summary-total').textContent = `Rp ${this.bookingData.total?.toLocaleString('id-ID') || 0}`;
        
        const v = this.bookingData.vehicle;
        document.getElementById('summary-vehicle').textContent = (v.brand && v.model) ? `${v.brand} ${v.model} (${v.licensePlate})` : '-';
        
        // Payment is always cash - no need to display payment method selection
        const summaryPaymentElement = document.getElementById('summary-payment');
        if (summaryPaymentElement) {
            summaryPaymentElement.textContent = 'Cash Payment at Location';
        }
        
        if (this.bookingData.date) {
            const dateObj = new Date(this.bookingData.date + 'T00:00:00');
            document.getElementById('summary-date').textContent = dateObj.toLocaleDateString('en-GB', { 
                weekday: 'long', 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
            });
        }
        
        document.getElementById('summary-time').textContent = this.bookingData.time || '-';
    }    async confirmBooking() {
        const confirmBtn = document.getElementById('confirm-btn');
        confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Processing...';
        confirmBtn.disabled = true;
        
        // Ensure payment is always set to cash
        this.bookingData.payment = 'cash';
          
        // Prepare form data to match the controller's @RequestParam expectations
        const formData = new FormData();
        formData.append('serviceId', this.bookingData.service.id);
        formData.append('date', this.bookingData.date);
        formData.append('time', this.bookingData.time);
        formData.append('notes', this.bookingData.specialNotes || '');
        formData.append('vehicleType', this.bookingData.vehicle.type || '');
        formData.append('vehicleBrand', this.bookingData.vehicle.brand || '');
        formData.append('vehicleModel', this.bookingData.vehicle.model || '');
        formData.append('licensePlate', this.bookingData.vehicle.licensePlate || '');
        formData.append('vehicleColor', this.bookingData.vehicle.color || '');
        
        try {
            const response = await fetch('/customer/booking/create', {
                method: 'POST',
                body: formData
            });            
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Booking failed: ${errorText}`);
            }
            
            const result = await response.json();
            console.log('Booking response:', result);
            
            if (result.success) {
                this.bookingData.bookingId = result.bookingId || 'BOOK-' + Date.now();
                
                // Redirect to cash payment instructions page
                window.location.href = '/customer/booking/payment-cash?bookingId=' + this.bookingData.bookingId;
            } else {
                throw new Error(result.message || 'Booking failed');
            }
        } catch (error) {
            console.error('Booking error:', error);
            this.showError('Failed to create booking. Please try again.');
            confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
            confirmBtn.disabled = false;
        }
    }    showError(message) {
        alert(message);
    }
}

// Initialize booking system
const bookingSystem = new BookingSystem();