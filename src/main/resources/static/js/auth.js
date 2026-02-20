// Authentication JavaScript Functions

/**
 * Toggle password visibility for input fields
 * @param {string} passwordId - ID of the password input field
 * @param {string} iconId - ID of the toggle icon
 */
function togglePasswordVisibility(passwordId = 'password', iconId = 'passwordIcon') {
    const passwordInput = document.getElementById(passwordId);
    const passwordIcon = document.getElementById(iconId);
    
    if (!passwordInput || !passwordIcon) {
        console.error('Password input or icon not found');
        return;
    }
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        passwordIcon.classList.remove('fa-eye');
        passwordIcon.classList.add('fa-eye-slash');
        passwordIcon.setAttribute('title', 'Hide password');
    } else {
        passwordInput.type = 'password';
        passwordIcon.classList.remove('fa-eye-slash');
        passwordIcon.classList.add('fa-eye');
        passwordIcon.setAttribute('title', 'Show password');
    }
    
    // Add visual feedback
    passwordIcon.style.transform = 'scale(0.9)';
    setTimeout(() => {
        passwordIcon.style.transform = 'scale(1)';
    }, 150);
}

/**
 * Show error message with animation
 * @param {string} message - Error message to display
 * @param {string} elementId - ID of the error element
 */
function showError(message, elementId = 'errorMessage') {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.classList.remove('hidden');
        // Trigger reflow for animation
        errorElement.offsetHeight;
        errorElement.style.animation = 'slideInDown 0.3s ease-out';
    }
}

/**
 * Hide error message
 * @param {string} elementId - ID of the error element
 */
function hideError(elementId = 'errorMessage') {
    const errorElement = document.getElementById(elementId);
    if (errorElement) {
        errorElement.classList.add('hidden');
    }
}

/**
 * Show success message with animation
 * @param {string} message - Success message to display
 * @param {string} elementId - ID of the success element
 */
function showSuccess(message, elementId = 'successMessage') {
    const successElement = document.getElementById(elementId);
    if (successElement) {
        successElement.textContent = message;
        successElement.classList.remove('hidden');
        successElement.style.animation = 'slideInDown 0.3s ease-out';
    }
}

/**
 * Set loading state for submit button
 * @param {HTMLElement} button - Submit button element
 * @param {boolean} loading - Whether to show loading state
 */
function setButtonLoading(button, loading) {
    if (!button) return;
    
    if (loading) {
        button.disabled = true;
        button.innerHTML = '<span class="spinner"></span>Processing...';
        button.style.cursor = 'not-allowed';
    } else {
        button.disabled = false;
        button.style.cursor = 'pointer';
    }
}

/**
 * Validate email format
 * @param {string} email - Email to validate
 * @returns {boolean} - Whether email is valid
 */
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Validate password strength
 * @param {string} password - Password to validate
 * @returns {object} - Validation result with isValid and message
 */
function validatePassword(password) {
    if (password.length < 8) {
        return {
            isValid: false,
            message: 'Password must be at least 8 characters long'
        };
    }
    
    if (!/(?=.*[a-z])/.test(password)) {
        return {
            isValid: false,
            message: 'Password must contain at least one lowercase letter'
        };
    }
    
    if (!/(?=.*[A-Z])/.test(password)) {
        return {
            isValid: false,
            message: 'Password must contain at least one uppercase letter'
        };
    }
    
    if (!/(?=.*\d)/.test(password)) {
        return {
            isValid: false,
            message: 'Password must contain at least one number'
        };
    }
    
    return {
        isValid: true,
        message: 'Password is strong'
    };
}

/**
 * Handle login form submission
 * @param {Event} event - Form submit event
 */
async function handleLogin(event) {
    event.preventDefault();
    
    const form = event.target;
    const submitButton = form.querySelector('button[type="submit"]');
    const originalButtonHTML = submitButton.innerHTML;
    
    // Get form data
    const email = form.email.value.trim();
    const password = form.password.value;
    
    // Validate inputs
    if (!validateEmail(email)) {
        showError('Please enter a valid email address');
        return;
    }
    
    if (!password) {
        showError('Please enter your password');
        return;
    }
    
    try {
        // Set loading state
        setButtonLoading(submitButton, true);
        hideError();
        
        // Make API request
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password }),
            credentials: 'same-origin'
        });

        const data = await response.json();
          if (response.ok) {
            // Show success and redirect
            showSuccess('Login successful! Redirecting...');
            setTimeout(() => {
                window.location.href = data.redirectUrl;
            }, 1000);
            return;
        }
        
        // Handle error
        showError(data.message || 'Invalid email or password');
        
    } catch (error) {
        console.error('Login error:', error);
        showError('Network error. Please check your connection and try again.');
    } finally {
        // Reset button state
        submitButton.disabled = false;
        submitButton.innerHTML = originalButtonHTML;
        submitButton.style.cursor = 'pointer';
    }
}

/**
 * Handle register form submission
 * @param {Event} event - Form submit event
 */
async function handleRegister(event) {
    event.preventDefault();
    
    const form = event.target;
    const submitButton = form.querySelector('button[type="submit"]');
    const originalButtonHTML = submitButton.innerHTML;
    
    // Get form data
    const formData = new FormData(form);
    const name = formData.get('name').trim();
    const email = formData.get('email').trim();
    const password = formData.get('password');
    const confirmPassword = formData.get('confirmPassword');
    
    // Validate inputs
    if (!name) {
        showError('Please enter your full name');
        return;
    }
    
    if (!validateEmail(email)) {
        showError('Please enter a valid email address');
        return;
    }
    
    const passwordValidation = validatePassword(password);
    if (!passwordValidation.isValid) {
        showError(passwordValidation.message);
        return;
    }
    
    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }
    
    try {
        // Set loading state
        setButtonLoading(submitButton, true);
        hideError();
        
        // Make API request
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ name, email, password }),
            credentials: 'same-origin'
        });

        const data = await response.json();
        
        if (response.ok) {
            // Show success and redirect
            showSuccess('Registration successful! Redirecting to login...');
            setTimeout(() => {
                window.location.href = '/login';
            }, 2000);
            return;
        }
        
        // Handle error
        showError(data.message || 'Registration failed. Please try again.');
        
    } catch (error) {
        console.error('Registration error:', error);
        showError('Network error. Please check your connection and try again.');
    } finally {
        // Reset button state
        submitButton.disabled = false;
        submitButton.innerHTML = originalButtonHTML;
        submitButton.style.cursor = 'pointer';
    }
}

/**
 * Initialize authentication page functionality
 */
function initializeAuth() {
    // Initialize form event listeners
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
    
    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
    
    // Initialize password toggle buttons
    const toggleButtons = document.querySelectorAll('.password-toggle');
    toggleButtons.forEach(button => {
        button.addEventListener('click', function() {
            const passwordInput = this.parentElement.querySelector('input[type="password"], input[type="text"]');
            if (passwordInput) {
                const newType = passwordInput.type === 'password' ? 'text' : 'password';
                passwordInput.type = newType;
                
                const icon = this.querySelector('i');
                if (icon) {
                    icon.classList.toggle('fa-eye');
                    icon.classList.toggle('fa-eye-slash');
                }
                
                // Update tooltip
                this.setAttribute('title', newType === 'password' ? 'Show password' : 'Hide password');
            }
        });
    });
    
    // Add input validation on blur
    const emailInputs = document.querySelectorAll('input[type="email"]');
    emailInputs.forEach(input => {
        input.addEventListener('blur', function() {
            if (this.value && !validateEmail(this.value)) {
                this.style.borderColor = '#ef4444';
            } else {
                this.style.borderColor = '';
            }
        });
    });
    
    // Add password strength indicator
    const passwordInputs = document.querySelectorAll('input[type="password"][name="password"]');
    passwordInputs.forEach(input => {
        input.addEventListener('input', function() {
            if (this.value.length > 0) {
                const validation = validatePassword(this.value);
                if (validation.isValid) {
                    this.style.borderColor = '#10b981';
                } else {
                    this.style.borderColor = '#f59e0b';
                }
            } else {
                this.style.borderColor = '';
            }
        });
    });
    
    // Add confirm password validation
    const confirmPasswordInputs = document.querySelectorAll('input[name="confirmPassword"]');
    confirmPasswordInputs.forEach(input => {
        input.addEventListener('blur', function() {
            const passwordInput = document.querySelector('input[name="password"]');
            if (passwordInput && this.value && this.value !== passwordInput.value) {
                this.style.borderColor = '#ef4444';
                showError('Passwords do not match');
            } else {
                this.style.borderColor = '';
                hideError();
            }
        });
    });
    
    // Add phone number validation
    const phoneInputs = document.querySelectorAll('input[type="tel"]');
    phoneInputs.forEach(input => {
        input.addEventListener('input', function() {
            // Only allow numbers
            this.value = this.value.replace(/[^0-9]/g, '');
        });
        
        input.addEventListener('blur', function() {
            if (this.value && (this.value.length < 10 || this.value.length > 13)) {
                this.style.borderColor = '#ef4444';
            } else {
                this.style.borderColor = '';
            }
        });
    });
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', initializeAuth);
