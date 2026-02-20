// Login form handling
document.getElementById('loginForm')?.addEventListener('submit', function(e) {
    e.preventDefault();
    const email = this.email.value;
    const password = this.password.value;
    
    console.log('Attempting login for:', email); // Debug log

    fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            email: email,
            password: password
        })
    })
    .then(response => {
        console.log('Response status:', response.status); // Debug log
        return response.json();
    })
    .then(data => {
        console.log('Login response:', data); // Debug log
        if (data.message === "Login successful") {
            window.location.href = data.redirect;
        } else {
            alert(data.message || "Login failed");
        }
    })
    .catch(error => {
        console.error('Login error:', error);
        alert('Failed to login. Please try again.');
    });
});

// DOM Elements
const menu = document.getElementById('mobile-menu');
const backToTopButton = document.getElementById('back-to-top');
const loginModal = document.getElementById('login-modal');
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
document.getElementById('contact-form')?.addEventListener('submit', function(e) {
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
document.getElementById('login-btn')?.addEventListener('click', () => {
    loginModal.style.display = 'block';
    document.body.style.overflow = 'hidden';
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

// Password toggle functionality
document.querySelectorAll('.togglePassword').forEach(button => {
    button.addEventListener('click', function() {
        const input = this.previousElementSibling;
        const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
        input.setAttribute('type', type);
        
        // Toggle eye icon
        const icon = this.querySelector('i');
        icon.classList.toggle('fa-eye');
        icon.classList.toggle('fa-eye-slash');
    });
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
document.getElementById('loginForm')?.addEventListener('submit', function(e) {
    e.preventDefault();
    
    const formData = {
        email: this.email.value,
        password: this.password.value
    };    fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.message === "Login successful") {
            // Redirect based on role
            window.location.href = data.redirect.startsWith('/') ? data.redirect : '/' + data.redirect;
        } else {
            alert(data.message || 'Login failed');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Failed to login. Please try again.');
    });
});

// Handle register form submission
registerForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    const name = document.getElementById('regName').value;
    const email = document.getElementById('regEmail').value;
    const phone = document.getElementById('regPhone').value;
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;
    
    // Validate password match
    if (password !== confirmPassword) {
        alert('Passwords do not match!');
        return;
    }
    
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

// Login functionality is now handled by anchor tags
