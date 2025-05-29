// DOM Elements
const menu = document.getElementById('mobile-menu');
const backToTopButton = document.getElementById('back-to-top');
const loginModal = document.getElementById('login-modal');
const loginBtn = document.getElementById('login-btn');
const mobileLoginBtn = document.getElementById('mobile-login-btn');
const heroLoginBtn = document.getElementById('hero-login-btn');
const closeBtn = document.querySelector('.close');
const showRegister = document.getElementById('show-register');
const showLogin = document.getElementById('show-login');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const dashboardModal = document.getElementById('dashboard-modal');
const logoutBtn = document.getElementById('logout-btn');
const dashboardBookingForm = document.getElementById('dashboard-booking-form');

// Mobile menu toggle
document.getElementById('menu-toggle').addEventListener('click', function() {
    menu.classList.toggle('hidden');
});

// Back to top button functionality
window.addEventListener('scroll', function() {
    if (window.pageYOffset > 300) {
        backToTopButton.classList.remove('opacity-0', 'invisible');
        backToTopButton.classList.add('opacity-100', 'visible');
    } else {
        backToTopButton.classList.remove('opacity-100', 'visible');
        backToTopButton.classList.add('opacity-0', 'invisible');
    }
});

backToTopButton.addEventListener('click', function() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
});

// Form submission handling
document.getElementById('contact-form').addEventListener('submit', function(e) {
    e.preventDefault();
    alert('Thank you for your message! We will get back to you as soon as possible.');
    this.reset();
});

// Smooth scrolling for navigation links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        
        document.querySelector(this.getAttribute('href')).scrollIntoView({
            behavior: 'smooth'
        });
        
        // Close mobile menu if open
        if (!menu.classList.contains('hidden')) {
            menu.classList.add('hidden');
        }
    });
});

// Login Modal Functionality
[loginBtn, mobileLoginBtn, heroLoginBtn].forEach(btn => {
    btn?.addEventListener('click', () => {
        loginModal.style.display = 'block';
        document.body.style.overflow = 'hidden';
    });
});

// Close modal when clicking X
closeBtn.addEventListener('click', () => {
    loginModal.style.display = 'none';
    dashboardModal.style.display = 'none';
    document.body.style.overflow = 'auto';
});

// Close modal when clicking outside
window.addEventListener('click', (e) => {
    if (e.target === loginModal) {
        loginModal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
    if (e.target === dashboardModal) {
        dashboardModal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
});

// Toggle between login and register forms
showRegister?.addEventListener('click', (e) => {
    e.preventDefault();
    loginForm.classList.add('hidden');
    registerForm.classList.remove('hidden');
});

showLogin?.addEventListener('click', (e) => {
    e.preventDefault();
    registerForm.classList.add('hidden');
    loginForm.classList.remove('hidden');
});

// Handle login form submission
loginForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    
    // In a real app, you would validate credentials with a server
    // For demo purposes, we'll just simulate a successful login
    simulateLogin(email);
});

// Handle register form submission
registerForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = document.getElementById('register-name').value;
    const email = document.getElementById('register-email').value;
    const phone = document.getElementById('register-phone').value;
    const password = document.getElementById('register-password').value;
    
    // In a real app, you would send this data to a server
    // For demo purposes, we'll just simulate registration and login
    simulateLogin(email, name);
});

// Handle dashboard booking form
dashboardBookingForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    alert('Your appointment has been booked successfully!');
    dashboardModal.style.display = 'none';
    document.body.style.overflow = 'auto';
});

// Handle logout
logoutBtn?.addEventListener('click', () => {
    dashboardModal.style.display = 'none';
    document.body.style.overflow = 'auto';
});

// Simulate login function
function simulateLogin(email, name = 'Customer') {
    // Close login modal
    loginModal.style.display = 'none';
    
    // Update user info in dashboard
    document.getElementById('user-name').textContent = name;
    document.getElementById('user-email').textContent = email;
    
    // Change login button to show user is logged in
    loginBtn.innerHTML = '<i class="fas fa-user-circle mr-2"></i>My Account';
    loginBtn.classList.remove('bg-blue-600');
    loginBtn.classList.add('bg-green-600');
    
    // Open dashboard modal
    dashboardModal.style.display = 'block';
}

// Change all "Book Now" buttons to "Login to Book"
document.querySelectorAll('.service-card button').forEach(btn => {
    btn.addEventListener('click', () => {
        loginModal.style.display = 'block';
        document.body.style.overflow = 'hidden';
    });
});
