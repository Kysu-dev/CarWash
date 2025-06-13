// booking.js - Clean Implementation with 5-step Booking Process

class BookingSystem {    constructor() {
        this.currentStep = 1;
        this.maxStep = 6; // 6 steps with payment selection
        this.flatpickrInstance = null; // For storing flatpickr instance
        this.bookingData = {
            vehicleType: null, // Add vehicle type selection
            service: null,
            date: null,
            time: null,
            vehicle: {
                type: null,
                brand: '',
                model: '',
                licensePlate: '',
                color: ''
            },
            specialNotes: '',
            payment: null, // Payment method will be selected by user
            total: 0
        };

        // Initialize when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.init());
        } else {
            this.init();
        }
    }
      init() {
        console.log('Booking system initializing...');
        this.setupEventListeners();
        this.initializeCalendar();
        this.showStep(1);
        
        // Initialize payment method selection
        this.initializePaymentMethods();
        
        // Add debugging helper (remove in production)
        this.addDebugHelpers();
    }
      // Initialize payment method selection
    initializePaymentMethods() {
        // Add event listeners to payment method cards
        const paymentCards = document.querySelectorAll('.payment-method-card');
        console.log(`Found ${paymentCards.length} payment method cards`);
        
        if (paymentCards.length === 0) {
            console.warn('No payment method cards found in the DOM yet.');
            
            // Set a default payment method for safety
            if (!this.bookingData.payment) {
                this.bookingData.payment = 'CASH';
                console.log('Set default payment method to CASH');
            }
            
            return;
        }
        
        paymentCards.forEach(card => {
            console.log('Setting up payment method card:', card.dataset.method);
            card.addEventListener('click', () => {
                console.log('Payment method clicked:', card.dataset.method);
                this.selectPaymentMethod(card);
            });
            
            // Pre-select the first card (CASH) for better UX
            if (card.dataset.method === 'CASH' && !this.bookingData.payment) {
                this.selectPaymentMethod(card);
            }
            
            // Restore previous selection if any
            if (this.bookingData.payment && card.dataset.method === this.bookingData.payment) {
                this.selectPaymentMethod(card);
            }
        });
        
        // Add explanatory text about payment confirmation
        const paymentInfoDiv = document.querySelector('#payment-info');
        if (paymentInfoDiv) {
            paymentInfoDiv.innerHTML = `
                <div class="bg-blue-50 text-blue-800 p-4 rounded-lg mb-4">
                    <h4 class="font-semibold mb-2"><i class="fas fa-info-circle mr-2"></i>Payment Information</h4>
                    <p>Please select your preferred payment method. Your booking will be confirmed after payment is verified by our staff.</p>
                    <ul class="list-disc ml-5 mt-2">
                        <li>For <strong>Cash</strong> payments, please pay at our location before your service.</li>
                        <li>For <strong>Transfer</strong> payments, you can upload proof of payment after booking.</li>
                        <li>For <strong>Card</strong> or <strong>E-Wallet</strong> payments, you can pay at our location.</li>
                    </ul>
                </div>
            `;
        }
    }
    
    // Debug helpers - remove in production
    addDebugHelpers() {
        // Create debug panel with more useful tools
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
            <div style="font-weight:bold;padding-bottom:5px;border-bottom:1px solid #555;margin-bottom:5px;">Booking Debug Panel</div>
            <div style="margin-bottom:5px">Step: <span id="debug-step">1</span></div>
            <div class="debug-buttons" style="display:flex;flex-wrap:wrap">
                <button id="debug-force-next" style="background: #007bff; color: white; border: none; padding: 5px; margin: 2px; border-radius: 3px; cursor:pointer">Force Next</button>
                <button id="debug-log-state" style="background: #28a745; color: white; border: none; padding: 5px; margin: 2px; border-radius: 3px; cursor:pointer">Log State</button>
                <button id="debug-force-mobil" style="background: #f8f9fa; color: black; border: none; padding: 5px; margin: 2px; border-radius: 3px; cursor:pointer">Select MOBIL</button>
                <button id="debug-force-motor" style="background: #f8f9fa; color: black; border: none; padding: 5px; margin: 2px; border-radius: 3px; cursor:pointer">Select MOTOR</button>
                <button id="debug-fix-step" style="background: #dc3545; color: white; border: none; padding: 5px; margin: 2px; border-radius: 3px; cursor:pointer">Fix Step</button>
            </div>
        `;
        
        document.body.appendChild(debugDiv);
        
        // Update step info in debug panel
        const updateDebugStep = () => {
            const debugStepEl = document.getElementById('debug-step');
            if (debugStepEl) {
                debugStepEl.textContent = this.currentStep;
            }
        };
        
        // Setup event listeners for debug buttons
        document.getElementById('debug-force-next').addEventListener('click', () => {
            console.log('Debug: Forcing next step');
            this.showStep(this.currentStep + 1);
            updateDebugStep();
        });
        
        document.getElementById('debug-log-state').addEventListener('click', () => {
            console.log('Current state:', {
                step: this.currentStep,
                bookingData: this.bookingData,
                validation: this.validateStep(this.currentStep)
            });
            
            // Check if vehicle type cards have correct listeners
            console.log('Vehicle type cards:', document.querySelectorAll('.vehicle-type-card').length);
            document.querySelectorAll('.vehicle-type-card').forEach((card, i) => {
                console.log(`Card ${i}:`, card.dataset.vehicleType, 'Selected:', card.classList.contains('selected'));
            });
            
            console.log('HTML structure of step 1:', document.getElementById('step-1-content')?.innerHTML);
            console.log('Hidden status of step content:');
            document.querySelectorAll('.step-content').forEach((content, i) => {
                console.log(`Step ${i+1} content:`, content.id, content.classList.contains('hidden') ? 'hidden' : 'visible');
            });
        });
        
        document.getElementById('debug-force-mobil').addEventListener('click', () => {
            console.log('Debug: Setting MOBIL as vehicle type');
            const mobilCard = document.querySelector('.vehicle-type-card[data-vehicle-type="MOBIL"]');
            if (mobilCard) {
                this.selectVehicleType(mobilCard);
            } else {
                console.error('MOBIL card not found');
            }
            updateDebugStep();
        });
        
        document.getElementById('debug-force-motor').addEventListener('click', () => {
            console.log('Debug: Setting MOTOR as vehicle type');
            const motorCard = document.querySelector('.vehicle-type-card[data-vehicle-type="MOTOR"]');
            if (motorCard) {
                this.selectVehicleType(motorCard);
            } else {
                console.error('MOTOR card not found');
            }
            updateDebugStep();
        });
        
        document.getElementById('debug-fix-step').addEventListener('click', () => {
            // Fix common issues with steps
            console.log('Debug: Fixing step display');
            
            // Ensure correct step content is shown
            document.querySelectorAll('.step-content').forEach((content, i) => {
                if (i+1 === this.currentStep) {
                    content.classList.remove('hidden');
                } else {
                    content.classList.add('hidden');
                }
            });
            
            // If on step 2, ensure services are loaded
            if (this.currentStep === 2 && this.bookingData.vehicleType) {
                this.loadServicesByVehicleType();
            }
            
            this.updateStepIndicators();
            this.updateNavigation();
            updateDebugStep();
        });
        
        // Make instance globally accessible for debugging
        window.bookingSystem = this;
        
        // Initial update
        updateDebugStep();
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
        
        // Check vehicle type selection
        if (this.currentStep === 1) {
            console.log('Vehicle type cards:', document.querySelectorAll('.vehicle-type-card').length);
            console.log('Selected cards:', document.querySelectorAll('.vehicle-type-card.selected').length);
            document.querySelectorAll('.vehicle-type-card').forEach((card, index) => {
                console.log(`Card ${index}:`, {
                    classList: Array.from(card.classList),
                    dataVehicleType: card.dataset.vehicleType,
                    isSelected: card.classList.contains('selected')
                });
            });
        }
    }

    debugSetTestData() {
        console.log('Debug: Setting test data for current step', this.currentStep);
        
        switch (this.currentStep) {
            case 1:
                this.bookingData.vehicleType = 'MOBIL';
                document.getElementById('vehicle-type').value = 'MOBIL';
                document.querySelectorAll('.vehicle-type-card')[0].classList.add('selected', 'border-blue-500', 'bg-blue-50');
                break;
            case 2:
                this.bookingData.service = {
                    id: 1,
                    name: 'Regular Wash',
                    price: 50000,
                    duration: 30
                };
                break;
            case 3:
                this.bookingData.date = '2025-06-15';
                this.bookingData.time = '10:00';
                break;
            case 4:
                this.bookingData.vehicle = {
                    type: this.bookingData.vehicleType || 'MOBIL',
                    brand: 'Toyota',
                    model: 'Avanza',
                    licensePlate: 'B 1234 CD',
                    color: 'Silver'
                };
                break;
        }
        console.log('Test data set:', this.bookingData);
        this.updateNavigation();
    }    // Set up event listeners for all interactive elements
    setupEventListeners() {
        console.log('Setting up event listeners');
        
        // Vehicle type selection
        document.querySelectorAll('.vehicle-type-card').forEach(card => {
            console.log('Adding event listener to vehicle type card:', card.dataset.vehicleType);
            card.addEventListener('click', () => {
                console.log('Vehicle type card clicked:', card.dataset.vehicleType);
                this.selectVehicleType(card);
            });
        });
        
        // Navigation buttons
        document.getElementById('next-btn')?.addEventListener('click', () => this.nextStep());
        document.getElementById('prev-btn')?.addEventListener('click', () => this.prevStep());
        document.getElementById('confirm-btn')?.addEventListener('click', () => this.confirmBooking());
        
        // Set up vehicle form input listeners
        this.setupVehicleForm();
        
        console.log('Event listeners setup complete');
    }
    
    // Select vehicle type
    selectVehicleType(card) {
        console.log('Selecting vehicle type:', card.dataset.vehicleType);
        
        // Remove selected class from all cards
        document.querySelectorAll('.vehicle-type-card').forEach(c => c.classList.remove('selected', 'border-blue-500', 'bg-blue-50'));
        
        // Add selected class to clicked card
        card.classList.add('selected', 'border-blue-500', 'bg-blue-50');
        
        // Update booking data
        this.bookingData.vehicleType = card.dataset.vehicleType;
        this.bookingData.vehicle.type = card.dataset.vehicleType;
        
        // Update hidden field if exists
        const vehicleTypeInput = document.getElementById('vehicle-type');
        if (vehicleTypeInput) {
            vehicleTypeInput.value = card.dataset.vehicleType;
        }
        
        console.log('Vehicle type selected:', this.bookingData.vehicleType);
        this.updateNavigation();
    }
    
    // Select payment method
    selectPaymentMethod(card) {
        console.log('Selecting payment method:', card.dataset.method);
        
        // Remove selected class from all payment cards
        document.querySelectorAll('.payment-method-card').forEach(c => c.classList.remove('selected', 'border-blue-500', 'bg-blue-50'));
        
        // Add selected class to clicked card
        card.classList.add('selected', 'border-blue-500', 'bg-blue-50');
        
        // Update booking data
        this.bookingData.payment = card.dataset.method;
        
        // Update payment info text based on selected method
        const paymentInfoDetails = document.querySelector('#payment-method-details');
        if (paymentInfoDetails) {
            let detailsHtml = '';
            switch (card.dataset.method) {
                case 'CASH':
                    detailsHtml = `
                        <div class="mt-3 p-3 bg-gray-50 rounded-lg">
                            <p><i class="fas fa-money-bill-wave text-green-500 mr-2"></i>Pay in cash at our location before your service begins.</p>
                            <p class="text-gray-600 mt-2 text-sm">Note: Payment will be confirmed by our staff.</p>
                        </div>
                    `;
                    break;
                case 'TRANSFER':
                    detailsHtml = `
                        <div class="mt-3 p-3 bg-gray-50 rounded-lg">
                            <p><i class="fas fa-university text-blue-500 mr-2"></i>Please transfer to our bank account:</p>
                            <p class="font-semibold mt-1">Bank Name: BCA</p>
                            <p class="font-semibold">Account: 1234567890</p>
                            <p class="font-semibold">Name: CarWash</p>
                            <p class="text-gray-600 mt-2 text-sm">You will be able to upload the payment receipt after booking.</p>
                        </div>
                    `;
                    break;
                case 'CARD':
                    detailsHtml = `
                        <div class="mt-3 p-3 bg-gray-50 rounded-lg">
                            <p><i class="fas fa-credit-card text-indigo-500 mr-2"></i>Pay with credit or debit card at our location.</p>
                            <p class="text-gray-600 mt-2 text-sm">We accept Visa, Mastercard, and American Express.</p>
                        </div>
                    `;
                    break;
                case 'E_WALLET':
                    detailsHtml = `
                        <div class="mt-3 p-3 bg-gray-50 rounded-lg">
                            <p><i class="fas fa-wallet text-purple-500 mr-2"></i>Pay with your e-wallet at our location.</p>
                            <p class="text-gray-600 mt-2 text-sm">We accept GoPay, OVO, DANA, and LinkAja.</p>
                        </div>
                    `;
                    break;
            }
            paymentInfoDetails.innerHTML = detailsHtml;
        }
        
        console.log('Payment method selected:', this.bookingData.payment);
        this.updateNavigation();
    }
    
    // Fungsi untuk inisialisasi Flatpickr
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
        console.log(`Showing step ${step}`);
        
        if (step < 1 || step > this.maxStep) {
            console.error('Invalid step:', step);
            return;
        }
        
        // Hide all step contents
        document.querySelectorAll('.step-content').forEach(content => {
            content.classList.add('hidden');
        });
        
        // Show current step content
        const currentStepContent = document.getElementById(`step-${step}-content`);
        if (currentStepContent) {
            currentStepContent.classList.remove('hidden');
            console.log(`Step ${step} content shown`);
        } else {
            console.error(`Step ${step} content element not found: step-${step}-content`);
        }
        
        // Update current step
        this.currentStep = step;
        
        // If moving to step 2 (service selection), load services 
        if (step === 2 && this.bookingData.vehicleType) {
            console.log('Loading services for vehicle type:', this.bookingData.vehicleType);
            this.loadServicesByVehicleType();
        }
        
        // If moving to step 3, initialize calendar if not done yet
        if (step === 3) {
            // Make sure calendar is initialized
            if (!this.flatpickrInstance) {
                this.initializeCalendar();
            }
        }
          // If moving to step 4, setup vehicle form
        if (step === 4) {
            this.setupVehicleForm();
        }
        
        // If moving to step 5 (payment method selection), setup payment methods
        if (step === 5) {
            console.log('Initializing payment methods for step 5');
            setTimeout(() => this.initializePaymentMethods(), 100); // Small delay to ensure DOM is ready
            
            // Make sure we have a default selection
            if (!this.bookingData.payment) {
                this.bookingData.payment = 'CASH';
                console.log('Default payment method set to CASH');
                
                // Pre-select the CASH card visually
                setTimeout(() => {
                    const cashCard = document.querySelector('.payment-method-card[data-method="CASH"]');
                    if (cashCard) {
                        this.selectPaymentMethod(cashCard);
                    }
                }, 200);
            }
        }
        
        // If moving to step 6 (confirmation), populate the summary
        if (step === 6) {
            this.updateSummary();
        }
        
        this.updateStepIndicators();
        this.updateNavigation();
        
        console.log(`Step changed to ${step}, booking data:`, this.bookingData);
    }
    
    selectDate(dateStr) {
        console.log('Date selected:', dateStr);
        this.bookingData.date = dateStr;
        
        // Generate time slots for the selected date
        this.generateTimeSlots();
        
        // Update navigation (might be disabled until time slot is selected)
        this.updateNavigation();
        
        // Store the date in a hidden field if it exists
        const hiddenDateField = document.getElementById('booking-date');
        if (hiddenDateField) {
            hiddenDateField.value = dateStr;
            console.log('Stored date in hidden field:', dateStr);
        }
    }
    
    // Load services based on vehicle type
    async loadServicesByVehicleType() {
        if (!this.bookingData.vehicleType) {
            console.error('Vehicle type not selected');
            return;
        }
        
        const serviceContainer = document.getElementById('service-container');
        if (!serviceContainer) {
            console.error('Service container not found');
            return;
        }
        
        console.log('Loading services for vehicle type:', this.bookingData.vehicleType);
        
        // Show loading state
        serviceContainer.innerHTML = `
            <div class="col-span-3 text-center p-8">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
                <p class="text-gray-600">Loading services for ${this.bookingData.vehicleType}...</p>
            </div>
        `;
        
        try {
            // Fetch services
            try {
                const response = await fetch(`/customer/api/services-by-vehicle-type?vehicleType=${this.bookingData.vehicleType}`);
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                const services = await response.json();
                console.log('Services fetched:', services);
                
                if (services.length === 0) {
                    serviceContainer.innerHTML = `
                        <div class="col-span-3 text-center p-8">
                            <div class="w-16 h-16 bg-gray-200 rounded-full flex items-center justify-center mx-auto mb-4">
                                <i class="fas fa-exclamation-triangle text-gray-500 text-2xl"></i>
                            </div>
                            <p class="text-gray-600">No services available for ${this.bookingData.vehicleType}</p>
                        </div>
                    `;
                    return;
                }
                
                // Clear the container
                serviceContainer.innerHTML = '';
                
                // Create service cards using the template
                services.forEach(service => {
                    // Get the template
                    const template = document.getElementById('service-card-template');
                    if (!template) {
                        console.error('Service card template not found');
                        return;
                    }
                    
                    // Clone the template
                    const card = template.content.cloneNode(true).querySelector('.service-card');
                      // Set data attributes for service selection
                    // Ensure service.id is valid and not undefined
                    const serviceId = service.id || service.serviceId;
                    if (!serviceId || serviceId === 'undefined' || serviceId === 'null') {
                        console.warn('Invalid service ID for service:', service.serviceName);
                        return; // Skip this service
                    }
                    
                    card.dataset.serviceId = serviceId;
                    card.dataset.service = service.serviceName;
                    card.dataset.price = service.price;
                    card.dataset.duration = service.estimatedDurationMinutes;
                    
                    // Set service name
                    const nameElement = card.querySelector('h3');
                    if (nameElement) nameElement.textContent = service.serviceName;
                    
                    // Set service icon
                    const iconElement = card.querySelector('.fa-2x');
                    if (iconElement) {
                        iconElement.classList.add('fa-car-wash', 'text-blue-500');
                    }
                    
                    // Set service features if available
                    if (service.description) {
                        const features = service.description.split('-').filter(f => f.trim().length > 0);
                        const featuresList = card.querySelector('ul');
                        if (featuresList && features.length > 0) {
                            features.forEach(feature => {
                                const li = document.createElement('li');
                                li.className = 'flex items-center text-sm';
                                li.innerHTML = `<i class="fas fa-check text-green-500 mr-2"></i><span>${feature.trim()}</span>`;
                                featuresList.appendChild(li);
                            });
                        }
                    }
                    
                    // Set service price and duration
                    const priceElement = card.querySelector('.text-2xl');
                    if (priceElement) {
                        priceElement.textContent = `Rp ${service.price.toLocaleString('id-ID')}`;
                        const premiumClass = service.serviceName.toLowerCase().includes('premium') 
                            ? 'text-purple-600' 
                            : (service.serviceName.toLowerCase().includes('deluxe') ? 'text-yellow-600' : 'text-blue-600');
                        priceElement.classList.add(premiumClass);
                    }
                    
                    const durationElement = card.querySelector('.text-gray-500');
                    if (durationElement) {
                        durationElement.textContent = `~${service.estimatedDurationMinutes} minutes`;
                    }
                    
                    // Add click event listener
                    card.addEventListener('click', () => this.selectService(card));
                    
                    // Add the card to the container
                    serviceContainer.appendChild(card);
                });
            } catch (error) {
                console.error('Error loading services:', error);
                serviceContainer.innerHTML = `
                    <div class="col-span-3 text-center p-8 text-red-600">
                        <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <i class="fas fa-exclamation-circle text-red-500 text-2xl"></i>
                        </div>
                        <p>Failed to load services. Please try again later.</p>
                    </div>
                `;
            }
        } catch (error) {
            console.error('Error in loadServicesByVehicleType:', error);
            serviceContainer.innerHTML = `
                <div class="col-span-3 text-center p-8 text-red-600">
                    <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <i class="fas fa-exclamation-circle text-red-500 text-2xl"></i>
                    </div>
                    <p>Failed to load services. Please try again later.</p>
                </div>
            `;
        }
    }
      selectService(card) {
        document.querySelectorAll('.service-card').forEach(c => c.classList.remove('selected', 'border-blue-500', 'bg-blue-50'));
        card.classList.add('selected', 'border-blue-500', 'bg-blue-50');
        
        const serviceId = card.dataset.serviceId;
        const serviceName = card.dataset.service;
        const servicePrice = parseInt(card.dataset.price);
        const serviceDuration = card.dataset.duration;
        
        // Validate service ID
        if (!serviceId || serviceId === 'undefined' || serviceId === 'null') {
            console.error('Invalid service ID:', serviceId);
            this.showError('Invalid service selected. Please try again.');
            return;
        }
        
        console.log('Service selected:', {
            id: serviceId,
            name: serviceName,
            price: servicePrice,
            duration: serviceDuration
        });
        
        this.bookingData.service = {
            id: serviceId,
            name: serviceName,
            price: servicePrice,
            duration: serviceDuration
        };
        
        this.bookingData.total = this.bookingData.service.price;
        this.updateNavigation();
        
        // Store the ID in a hidden field if it exists
        const hiddenServiceIdField = document.getElementById('service-id');
        if (hiddenServiceIdField) {
            hiddenServiceIdField.value = serviceId;
            console.log('Stored service ID in hidden field:', serviceId);
        }
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
        console.log('Step 2 validation result:', this.validateStep(3)); // Updated step index
        
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
            type: this.bookingData.vehicleType, // Use the already selected vehicle type
            brand: document.getElementById('vehicle-brand')?.value || '',
            model: document.getElementById('vehicle-model')?.value || '',
            licensePlate: document.getElementById('license-plate')?.value || '',
            color: document.getElementById('vehicle-color')?.value || ''
        };
        this.bookingData.specialNotes = document.getElementById('special-notes')?.value || '';
    }
      validateStep(step) {
        switch (step) {
            case 1: // Vehicle type selection
                const vehicleTypeValid = !!this.bookingData.vehicleType && ['MOBIL', 'MOTOR'].includes(this.bookingData.vehicleType);
                console.log('Vehicle type validation:', vehicleTypeValid, this.bookingData.vehicleType);
                
                // Double-check the UI selection state
                const selectedCard = document.querySelector('.vehicle-type-card.selected');
                console.log('Selected card found:', selectedCard ? 'Yes' : 'No');
                
                if (selectedCard) {
                    // Always take the selected card's value
                    const cardVehicleType = selectedCard.dataset.vehicleType;
                    console.log('Selected card type:', cardVehicleType);
                    this.bookingData.vehicleType = cardVehicleType;
                    this.bookingData.vehicle.type = cardVehicleType;
                    return true;
                }
                
                return vehicleTypeValid;
            case 2: // Service selection 
                const serviceValid = !!this.bookingData.service;
                console.log('Service validation:', serviceValid, this.bookingData.service);
                return serviceValid;
            case 3: // Schedule selection
                const dateValid = !!this.bookingData.date;
                const timeValid = !!this.bookingData.time;
                console.log('Date validation:', dateValid, this.bookingData.date);
                console.log('Time validation:', timeValid, this.bookingData.time);
                return dateValid && timeValid;
            case 4: // Vehicle details 
                const v = this.bookingData.vehicle;
                return !!v.type && !!v.brand && !!v.model && !!v.licensePlate && !!v.color;
            case 5: // Payment method selection
                // Check for selected payment method in the UI
                const selectedPaymentCard = document.querySelector('.payment-method-card.selected');
                
                // If there's a visually selected card, use its value
                if (selectedPaymentCard) {
                    this.bookingData.payment = selectedPaymentCard.dataset.method;
                    console.log('Payment validation: true (from selected card)', this.bookingData.payment);
                    return true;
                }
                
                // If there's a value in the bookingData, validate it
                const validPaymentMethods = ['CASH', 'TRANSFER', 'CARD', 'E_WALLET'];
                const paymentValid = !!this.bookingData.payment && validPaymentMethods.includes(this.bookingData.payment);
                
                console.log('Payment validation:', paymentValid, this.bookingData.payment);
                
                // If no payment is selected, default to cash for better UX
                if (!paymentValid) {
                    console.log('No valid payment method, defaulting to CASH');
                    this.bookingData.payment = 'CASH';
                    
                    // Try to select the CASH card visually
                    const cashCard = document.querySelector('.payment-method-card[data-method="CASH"]');
                    if (cashCard) {
                        setTimeout(() => this.selectPaymentMethod(cashCard), 100);
                    }
                    
                    return true; // Return true so the user can proceed
                }
                
                return paymentValid;
            case 6: // Review step - all data should be valid
                console.log("Validating step 6:", {
                    vehicleType: !!this.bookingData.vehicleType,
                    service: !!this.bookingData.service,
                    serviceId: this.bookingData.service?.id,
                    date: !!this.bookingData.date,
                    time: !!this.bookingData.time,
                    vehicleBrand: !!this.bookingData.vehicle?.brand,
                    vehicleModel: !!this.bookingData.vehicle?.model,
                    licensePlate: !!this.bookingData.vehicle?.licensePlate,
                    payment: this.bookingData.payment
                });
                
                // One final check for payment method
                if (!this.bookingData.payment) {
                    console.log('No payment method in final step, defaulting to CASH');
                    this.bookingData.payment = 'CASH';
                    // Update summary
                    this.updateSummary();
                }
                
                return !!this.bookingData.vehicleType && 
                       !!this.bookingData.service && 
                       !!this.bookingData.service.id &&
                       !!this.bookingData.date && 
                       !!this.bookingData.time && 
                       !!this.bookingData.vehicle?.brand && 
                       !!this.bookingData.vehicle?.model && 
                       !!this.bookingData.vehicle?.licensePlate &&
                       !!this.bookingData.payment;
            default: 
                return true;
        }
    }
    
    nextStep() {
        // Special handling for step 1 - Vehicle Type
        if (this.currentStep === 1) {
            console.log('Step 1: Checking vehicle type selection');
            const selectedCard = document.querySelector('.vehicle-type-card.selected');
            if (selectedCard) {
                console.log('Step 1: Found selected card:', selectedCard.dataset.vehicleType);
                // Force update data to ensure consistency
                const vehicleType = selectedCard.dataset.vehicleType;
                this.bookingData.vehicleType = vehicleType;
                this.bookingData.vehicle.type = vehicleType;
                
                // Always proceed to next step with services loading
                const nextStep = this.currentStep + 1;
                console.log(`Moving to step ${nextStep} with vehicle type ${vehicleType}`);
                
                // First show the next step, then load services
                this.showStep(nextStep);
                
                // Add debug information
                console.log('After showStep, now loading services');
                console.log('Current vehicleType:', this.bookingData.vehicleType);
                console.log('Current step:', this.currentStep);
                
                // Explicitly load services 
                setTimeout(() => {
                    this.loadServicesByVehicleType();
                }, 500);
                return;
            } else {
                console.log('No vehicle type selected');
                this.showError('Silakan pilih jenis kendaraan terlebih dahulu.');
                return;
            }
        }
        
        // Special handling for step 5 - Payment Method
        if (this.currentStep === 5) {
            console.log('Step 5: Checking payment method selection');
            const selectedCard = document.querySelector('.payment-method-card.selected');
            
            if (selectedCard) {
                console.log('Step 5: Found selected payment method:', selectedCard.dataset.method);
                // Force update data to ensure consistency
                this.bookingData.payment = selectedCard.dataset.method;
            } else {
                console.log('No payment method selected, defaulting to CASH');
                this.bookingData.payment = 'CASH';
                
                // Try to select the CASH card visually
                const cashCard = document.querySelector('.payment-method-card[data-method="CASH"]');
                if (cashCard) {
                    this.selectPaymentMethod(cashCard);
                }
            }
        }
        
        // For other steps, do regular validation
        const isValid = this.validateStep(this.currentStep);
        console.log(`Step ${this.currentStep} validation:`, isValid);
        
        if (!isValid) {
            console.error(`Step ${this.currentStep} validation failed`);
            this.showError('Harap lengkapi semua informasi yang diperlukan.');
            return;
        }
        
        if (this.currentStep < this.maxStep) {
            const nextStep = this.currentStep + 1;
            console.log(`Moving to step ${nextStep}`);
            this.showStep(nextStep);
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
        const titles = ['Select Your Vehicle Type', 'Choose Service', 'Select Schedule', 'Vehicle Details', 'Choose Payment Method', 'Review & Confirmation'];
        document.getElementById('current-step-title').textContent = titles[this.currentStep - 1];
    }
      updateNavigation() {
        const nextBtn = document.getElementById('next-btn');
        const prevBtn = document.getElementById('prev-btn');
        const confirmBtn = document.getElementById('confirm-btn');
        const isValid = this.validateStep(this.currentStep);
        
        console.log(`Navigation update - Step: ${this.currentStep}, Valid: ${isValid}`);
        console.log('Current booking data:', this.bookingData);
        
        // Show/hide buttons based on current step (updated for 6 steps)
        prevBtn.classList.toggle('hidden', this.currentStep <= 1 || this.currentStep === 6);
        nextBtn.classList.toggle('hidden', this.currentStep >= 6);
        confirmBtn.classList.toggle('hidden', this.currentStep !== 6);
        
        // Custom text for next button based on step
        const buttonTexts = {
            1: isValid ? 'Continue to Services <i class="fas fa-arrow-right ml-2"></i>' : 'Select Vehicle Type <i class="fas fa-arrow-right ml-2"></i>',
            2: 'Continue to Schedule <i class="fas fa-arrow-right ml-2"></i>',
            3: 'Continue to Vehicle Details <i class="fas fa-arrow-right ml-2"></i>',
            4: 'Continue to Payment <i class="fas fa-arrow-right ml-2"></i>',
            5: 'Review Booking <i class="fas fa-arrow-right ml-2"></i>'
        };
        
        if (buttonTexts[this.currentStep]) {
            nextBtn.innerHTML = buttonTexts[this.currentStep];
            if (this.currentStep === 1 && isValid) {
                nextBtn.classList.add('bg-blue-700', 'font-medium');
            } else {
                nextBtn.classList.remove('bg-blue-700', 'font-medium');
            }
        }
        
        // Enable/disable next button based on validation
        if (nextBtn) {
            nextBtn.disabled = !isValid;
            nextBtn.classList.toggle('opacity-50', !isValid);
            nextBtn.classList.toggle('cursor-not-allowed', !isValid);
            
            // Additional debugging for step 3 (schedule selection)
            if (this.currentStep === 3) {
                console.log('Step 3 debugging:');
                console.log('Date:', this.bookingData.date);
                console.log('Time:', this.bookingData.time);
                console.log('Date valid:', !!this.bookingData.date);
                console.log('Time valid:', !!this.bookingData.time);
                console.log('Overall valid:', !!this.bookingData.date && !!this.bookingData.time);
            }
        }
    }
    
    updateSummary() {
        // Add vehicle type to summary
        const vehicleTypeElement = document.getElementById('summary-vehicle-type');
        if (vehicleTypeElement) {
            vehicleTypeElement.textContent = this.bookingData.vehicleType || '-';
        }
        
        document.getElementById('summary-service').textContent = this.bookingData.service?.name || '-';
        document.getElementById('summary-duration').textContent = `~${this.bookingData.service?.duration || 0} min`;
        document.getElementById('summary-total').textContent = `Rp ${this.bookingData.total?.toLocaleString('id-ID') || 0}`;
        
        const v = this.bookingData.vehicle;
        document.getElementById('summary-vehicle').textContent = (v.brand && v.model) ? `${v.brand} ${v.model} (${v.licensePlate})` : '-';
        
        // Update payment method based on user selection
        const summaryPaymentElement = document.getElementById('summary-payment');
        if (summaryPaymentElement) {
            console.log('Updating payment summary with:', this.bookingData.payment);
            
            // If no payment method is selected, default to CASH
            if (!this.bookingData.payment) {
                console.log('No payment method selected, defaulting to CASH');
                this.bookingData.payment = 'CASH';
                // Try to select the CASH card visually
                const cashCard = document.querySelector('.payment-method-card[data-method="CASH"]');
                if (cashCard) {
                    cashCard.classList.add('selected', 'border-blue-500', 'bg-blue-50');
                }
            }
            
            // Map payment method code to user-friendly text
            const paymentLabels = {
                'CASH': 'Cash Payment at Location',
                'TRANSFER': 'Bank Transfer',
                'CARD': 'Credit/Debit Card at Location',
                'E_WALLET': 'E-Wallet Payment'
            };
            
            summaryPaymentElement.textContent = paymentLabels[this.bookingData.payment] || this.bookingData.payment;
            
            // Update test form payment method as well (for debugging)
            const testPaymentField = document.getElementById('test-payment');
            if (testPaymentField) {
                testPaymentField.value = this.bookingData.payment;
            }
            
            // Update the main booking form fields as well
            this.updateHiddenFormFields();
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
    }
    
    // New method to update all hidden form fields with current booking data
    updateHiddenFormFields() {
        // Update the main booking form fields
        const updateField = (id, value) => {
            const field = document.getElementById(id);
            if (field) {
                field.value = value || '';
            }
        };
        
        // Update all fields
        updateField('main-serviceId', this.bookingData.service?.id);
        updateField('main-date', this.bookingData.date);
        updateField('main-time', this.bookingData.time);
        updateField('main-brand', this.bookingData.vehicle?.brand);
        updateField('main-model', this.bookingData.vehicle?.model);
        updateField('main-plate', this.bookingData.vehicle?.licensePlate);
        updateField('main-color', this.bookingData.vehicle?.color);
        updateField('main-notes', this.bookingData.specialNotes);
        updateField('main-payment', this.bookingData.payment || 'CASH');
        
        console.log('Updated hidden form fields with current booking data');
    }
    
    async confirmBooking() {
        console.log('Confirm booking started');
        const confirmBtn = document.getElementById('confirm-btn');
        confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Processing...';
        confirmBtn.disabled = true;
        
        // Check if payment method is selected
        if (!this.bookingData.payment) {
            console.error('Payment method not selected');
            this.showError('Please select a payment method first.');
            confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
            confirmBtn.disabled = false;
            return;
        }
        
        // First, check if we're properly logged in
        try {
            console.log('Checking session status...');
            const sessionResponse = await fetch('/customer/test-session');
            const sessionStatus = await sessionResponse.json();
            console.log('Session status:', sessionStatus);
            
            if (!sessionStatus.loggedIn) {
                console.error('Not logged in!');
                this.showError('You need to be logged in to create a booking. Please login and try again.');
                confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
                confirmBtn.disabled = false;
                setTimeout(() => {
                    window.location.href = '/login';
                }, 2000);
                return;
            }
            
            console.log('Logged in as user ID:', sessionStatus.userId);
            
        } catch (e) {
            console.error('Failed to check session:', e);
        }
        
        // Log the booking data before submission
        console.log('Booking data to submit:', this.bookingData);
          // Double check that we have a service ID
        if (!this.bookingData.service || !this.bookingData.service.id || 
            this.bookingData.service.id === 'undefined' || this.bookingData.service.id === 'null') {
            console.error('No service ID found in booking data or service ID is invalid');
            this.showError('Missing service information. Please restart the booking process.');
            confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
            confirmBtn.disabled = false;
            return;
        }
        
        if (!this.bookingData.date || !this.bookingData.time) {
            this.showError('Missing date or time information. Please complete all steps.');
            confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
            confirmBtn.disabled = false;
            return;
        }
        
        try {
            console.log('Preparing booking submission');
            
            // Create a hidden form and submit it (more reliable than fetch in some cases)
            const submissionForm = document.createElement('form');
            submissionForm.method = 'POST';
            submissionForm.action = '/customer/booking/test-create'; // Use the test-create endpoint
            submissionForm.style.display = 'none';
              // Add all form fields
            const addField = (name, value) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = name;
                // Ensure we don't pass "undefined" or "null" as strings
                input.value = (value && value !== 'undefined' && value !== 'null') ? value : '';
                submissionForm.appendChild(input);
            };
            
            // Add all required fields
            addField('serviceId', this.bookingData.service.id);
            addField('date', this.bookingData.date);
            addField('time', this.bookingData.time);
            addField('notes', this.bookingData.specialNotes || '');
            addField('vehicleBrand', this.bookingData.vehicle.brand || '');
            addField('vehicleModel', this.bookingData.vehicle.model || '');
            addField('licensePlate', this.bookingData.vehicle.licensePlate || '');
            addField('vehicleColor', this.bookingData.vehicle.color || '');
            addField('paymentMethod', this.bookingData.payment || 'CASH');
            
            // Log what we're submitting
            console.log('Submitting form with data:', {
                serviceId: this.bookingData.service.id,
                date: this.bookingData.date,
                time: this.bookingData.time,
                notes: this.bookingData.specialNotes,
                vehicleBrand: this.bookingData.vehicle.brand,
                vehicleModel: this.bookingData.vehicle.model,
                licensePlate: this.bookingData.vehicle.licensePlate,
                vehicleColor: this.bookingData.vehicle.color,
                paymentMethod: this.bookingData.payment
            });
            
            // Add form to body and submit it
            document.body.appendChild(submissionForm);
            this.showToast('Submitting your booking...');
            
            // Submit the form
            submissionForm.submit();
            
            // Don't remove the form as we're leaving this page
            return; // Exit function as we're now navigating away
        } catch (error) {
            console.error('Booking error:', error);            
            // Show a more user-friendly error message
            this.showError('Sorry, there was a problem creating your booking. Please try again or contact support.');
            
            // Reset the button state
            confirmBtn.innerHTML = '<i class="fas fa-check mr-2"></i>Confirm Booking';
            confirmBtn.disabled = false;
        }
    }
    
    // Test function for direct booking creation
    async testCreateBooking() {
        console.log('Test booking creation started');
        
        try {
            const response = await fetch('/customer/booking/test-create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                credentials: 'include'
            });
            
            console.log('Test booking response status:', response.status);
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error('Test booking error:', errorText);
                this.showError('Test booking failed');
                return;
            }
            
            const result = await response.json();
            console.log('Test booking result:', result);
            
            if (result.success) {
                this.showToast('Test booking created successfully! ID: ' + result.bookingId);
            } else {
                this.showError('Test booking failed: ' + result.message);
            }
            
        } catch (e) {
            console.error('Test booking error:', e);
            this.showError('Test booking failed');
        }
    }
    
    showError(message) {
        console.error('Error:', message);
        
        // Remove any existing error message
        const existingError = document.getElementById('booking-error-message');
        if (existingError) {
            existingError.remove();
        }
        
        // Create error message element
        const errorDiv = document.createElement('div');
        errorDiv.id = 'booking-error-message';
        errorDiv.className = 'bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4';
        errorDiv.role = 'alert';
        
        errorDiv.innerHTML = `
            <strong class="font-bold">Error! </strong>
            <span class="block sm:inline">${message}</span>
            <span class="absolute top-0 bottom-0 right-0 px-4 py-3">
                <svg class="fill-current h-6 w-6 text-red-500" role="button" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                    <title>Close</title>
                    <path d="M14.348 14.849a1.2 1.2 0 0 1-1.697 0L10 11.819l-2.651 3.029a1.2 1.2 0 1 1-1.697-1.697l2.758-3.15-2.759-3.152a1.2 1.2 0 1 1 1.697-1.697L10 8.183l2.651-3.031a1.2 1.2 0 1 1 1.697 1.697l-2.758 3.152 2.758 3.15a1.2 1.2 0 0 1 0 1.698z"/>
                </svg>
            </span>
        `;
        
        // Step-specific guidance
        if (this.currentStep === 1) {
            errorDiv.innerHTML += `
                <div class="mt-2 text-sm">
                    <strong>Tip:</strong> Silakan klik salah satu jenis kendaraan untuk melanjutkan.
                </div>
            `;
        }
        
        // Find the right location to insert the error
        const stepContent = document.getElementById(`step-${this.currentStep}-content`);
        if (stepContent) {
            stepContent.insertBefore(errorDiv, stepContent.firstChild);
        } else {
            // Fallback to body if step content not found
            document.body.prepend(errorDiv);
        }
        
        // Add close button functionality
        const closeButton = errorDiv.querySelector('svg');
        if (closeButton) {
            closeButton.addEventListener('click', () => {
                errorDiv.remove();
            });
        }
        
        // Auto-hide after 5 seconds
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove();
            }
        }, 5000);
    }

    // Show a toast notification
    showToast(message) {
        console.log('Toast:', message);
        
        // Remove any existing toast
        const existingToast = document.getElementById('booking-toast');
        if (existingToast) {
            existingToast.remove();
        }
        
        // Create toast element
        const toastDiv = document.createElement('div');
        toastDiv.id = 'booking-toast';
        toastDiv.className = 'fixed bottom-4 right-4 bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded shadow-lg z-50';
        
        toastDiv.innerHTML = `
            <div class="flex items-center">
                <svg class="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <span>${message}</span>
            </div>
        `;
        
        document.body.appendChild(toastDiv);
        
        // Animate in
        setTimeout(() => {
            toastDiv.style.transition = 'opacity 0.5s, transform 0.5s';
            toastDiv.style.opacity = '1';
            toastDiv.style.transform = 'translateY(0)';
        }, 10);
        
        // Auto-hide after 3 seconds
        setTimeout(() => {
            if (toastDiv.parentNode) {
                toastDiv.style.opacity = '0';
                toastDiv.style.transform = 'translateY(20px)';
                setTimeout(() => {
                    if (toastDiv.parentNode) {
                        toastDiv.remove();
                    }
                }, 500);
            }
        }, 3000);
    }

    showPaymentMethodSelector() {
        console.log('Showing payment method selector');
        
        // Create modal if it doesn't exist
        let paymentModal = document.getElementById('payment-method-modal');
        if (!paymentModal) {
            paymentModal = document.createElement('div');
            paymentModal.id = 'payment-method-modal';
            paymentModal.className = 'modal fade';
            paymentModal.setAttribute('tabindex', '-1');
            paymentModal.setAttribute('role', 'dialog');
            paymentModal.setAttribute('aria-hidden', 'true');
            
            paymentModal.innerHTML = `
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Select Payment Method</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <div class="payment-methods">
                                <div class="row g-3">
                                    <div class="col-6">
                                        <div class="card payment-method-card" data-method="CASH">
                                            <div class="card-body text-center">
                                                <i class="fas fa-money-bill-wave fa-3x text-success mb-3"></i>
                                                <h6>Cash</h6>
                                                <p class="text-muted small">Pay at the location</p>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-6">
                                        <div class="card payment-method-card" data-method="TRANSFER">
                                            <div class="card-body text-center">
                                                <i class="fas fa-university fa-3x text-primary mb-3"></i>
                                                <h6>Bank Transfer</h6>
                                                <p class="text-muted small">Upload proof later</p>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-6">
                                        <div class="card payment-method-card" data-method="CARD">
                                            <div class="card-body text-center">
                                                <i class="fas fa-credit-card fa-3x text-info mb-3"></i>
                                                <h6>Credit/Debit Card</h6>
                                                <p class="text-muted small">Pay at the location</p>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-6">
                                        <div class="card payment-method-card" data-method="E_WALLET">
                                            <div class="card-body text-center">
                                                <i class="fas fa-wallet fa-3x text-warning mb-3"></i>
                                                <h6>E-Wallet</h6>
                                                <p class="text-muted small">Upload proof later</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn btn-primary" id="payment-method-confirm" disabled>Continue</button>
                        </div>
                    </div>
                </div>
            `;
            
            document.body.appendChild(paymentModal);
            
            // Add event listeners to payment method cards
            document.querySelectorAll('.payment-method-card').forEach(card => {
                card.addEventListener('click', () => {
                    // Remove selected class from all cards
                    document.querySelectorAll('.payment-method-card').forEach(c => c.classList.remove('selected', 'border-primary', 'bg-light'));
                    
                    // Add selected class to clicked card
                    card.classList.add('selected', 'border-primary', 'bg-light');
                    
                    // Enable confirm button
                    document.getElementById('payment-method-confirm').disabled = false;
                    
                    // Store the payment method
                    this.bookingData.payment = card.dataset.method;
                    console.log('Selected payment method:', this.bookingData.payment);
                });
            });
            
            // Add event listener to confirm button
            document.getElementById('payment-method-confirm').addEventListener('click', () => {
                // Hide modal
                bootstrap.Modal.getInstance(paymentModal).hide();
                
                // Continue with booking
                this.confirmBooking();
            });
        }
        
        // Show modal
        const modal = new bootstrap.Modal(paymentModal);
        modal.show();
    }
}

// Initialize booking system
const bookingSystem = new BookingSystem();
