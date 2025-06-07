// booking.js - Versi Final Menggunakan Library Flatpickr (Lebih Stabil)

class BookingSystem {
    constructor() {
        this.currentStep = 1;
        this.maxStep = 5;
        this.flatpickrInstance = null; // Untuk menyimpan instance flatpickr

        this.bookingData = {
            service: null,
            date: null,
            time: null,
            vehicle: {},
            specialNotes: '',
            payment: 'cash',
            total: 0
        };

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }

    init() {
        this.setupEventListeners();
        this.initializeCalendar();
        this.showStep(1);
    }

    // Fungsi untuk inisialisasi Flatpickr
    initializeCalendar() {
        const calendarContainer = document.getElementById('custom-calendar');
        if (!calendarContainer) return;

        this.flatpickrInstance = flatpickr(calendarContainer, {
            inline: true, // Membuat kalender selalu terlihat
            minDate: "today", // Tidak bisa memilih tanggal lampau
            dateFormat: "Y-m-d", // Format tanggal yang dikirim
            onChange: (selectedDates, dateStr) => {
                // Fungsi ini akan berjalan setiap kali tanggal dipilih
                this.selectDate(dateStr);
            },
        });
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
    }

    setupEventListeners() {
        document.querySelectorAll('.service-card').forEach(card => card.addEventListener('click', () => this.selectService(card)));
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        if (nextBtn) nextBtn.addEventListener('click', () => this.nextStep());
        if (prevBtn) prevBtn.addEventListener('click', () => this.prevStep());
        if (confirmBtn) confirmBtn.addEventListener('click', () => this.confirmBooking());
        document.querySelectorAll('input[name="payment"]').forEach(radio => radio.addEventListener('change', e => this.bookingData.payment = e.target.value));
        this.setupVehicleForm();
    }

    selectService(card) {
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
    }

    selectTimeSlot(slotElement) {
        document.querySelectorAll('.time-slot').forEach(s => {
            s.classList.remove('selected', 'border-blue-500', 'bg-blue-100');
        });
        slotElement.classList.add('selected', 'border-blue-500', 'bg-blue-100');
        this.bookingData.time = slotElement.dataset.time;
        console.log('Time selected:', this.bookingData.time);
        console.log('Booking data after time selection:', this.bookingData);
        console.log('Step 2 validation result:', this.validateStep(2));
        this.updateNavigation();
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
    }

    validateStep(step) {
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
                return !!this.bookingData.payment;
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
        const titles = ['Choose Service', 'Select Schedule', 'Vehicle Details', 'Review & Payment', 'Booking Confirmed'];
        document.getElementById('current-step-title').textContent = titles[this.currentStep - 1];
    }

    updateNavigation() {
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        const isValid = this.validateStep(this.currentStep);
        
        console.log(`Navigation update - Step: ${this.currentStep}, Valid: ${isValid}`);
        
        // Show/hide buttons based on current step
        prevBtn.classList.toggle('hidden', this.currentStep <= 1 || this.currentStep === 5);
        nextBtn.classList.toggle('hidden', this.currentStep >= 4);
        confirmBtn.classList.toggle('hidden', this.currentStep !== 4);
        
        // Enable/disable next button based on validation
        if (nextBtn) {
            nextBtn.disabled = !isValid;
            nextBtn.classList.toggle('opacity-50', !isValid);
            nextBtn.classList.toggle('cursor-not-allowed', !isValid);
        }
    }

    updateSummary() {
        document.getElementById('summary-service').textContent = this.bookingData.service?.name || '-';
        document.getElementById('summary-duration').textContent = `~${this.bookingData.service?.duration || 0} min`;
        document.getElementById('summary-total').textContent = `Rp ${this.bookingData.total?.toLocaleString('id-ID') || 0}`;
        
        const v = this.bookingData.vehicle;
        document.getElementById('summary-vehicle').textContent = (v.brand && v.model) ? `${v.brand} ${v.model} (${v.licensePlate})` : '-';
        
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
    }

    async confirmBooking() {
        const confirmBtn = document.getElementById('confirm-btn');
        confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Processing...';
        confirmBtn.disabled = true;
        
        const dataToSend = {
            serviceId: this.bookingData.service.id,
            bookingDateTime: `${this.bookingData.date}T${this.bookingData.time}:00`,
            notes: this.bookingData.specialNotes,
            vehicleType: this.bookingData.vehicle.type,
            vehicleBrand: this.bookingData.vehicle.brand,
            vehicleModel: this.bookingData.vehicle.model,
            vehicleLicensePlate: this.bookingData.vehicle.licensePlate,
            vehicleColor: this.bookingData.vehicle.color
        };
        
        try {
            const response = await fetch('/customer/api/create-booking', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(dataToSend)
            });
            
            if (!response.ok) throw new Error('Booking failed');
            
            const result = await response.json();
            this.bookingData.bookingId = result.bookingId || 'BOOK-' + Date.now();
            this.showStep(5);
            this.updateFinalSummary();
        } catch (error) {
            console.error('Booking error:', error);
            this.showError('Failed to create booking. Please try again.');
            confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
            confirmBtn.disabled = false;
        }
    }

    updateFinalSummary() {
        document.getElementById('booking-id').textContent = this.bookingData.bookingId || 'N/A';
        document.getElementById('final-service').textContent = this.bookingData.service?.name || '-';
        document.getElementById('final-amount').textContent = `Rp ${this.bookingData.total?.toLocaleString('id-ID') || 0}`;
        
        if (this.bookingData.date && this.bookingData.time) {
            const dateObj = new Date(`${this.bookingData.date}T${this.bookingData.time}`);
            document.getElementById('final-datetime').textContent = dateObj.toLocaleDateString('en-GB', { 
                day: 'numeric', 
                month: 'long', 
                year: 'numeric' 
            }) + ` at ${this.bookingData.time}`;
        }
    }

    showError(message) {
        alert(message);
    }
}

// Initialize booking system
const bookingSystem = new BookingSystem();