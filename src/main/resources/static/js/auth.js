// API Base URL
const API_BASE_URL = window.location.origin;

// Tab Switching
const loginTab = document.getElementById('loginTab');
const registerTab = document.getElementById('registerTab');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');

loginTab.addEventListener('click', () => {
    switchTab('login');
});

registerTab.addEventListener('click', () => {
    switchTab('register');
});

function switchTab(tab) {
    if (tab === 'login') {
        loginTab.classList.add('active');
        registerTab.classList.remove('active');
        loginForm.classList.add('active');
        registerForm.classList.remove('active');
        clearMessages();
    } else {
        registerTab.classList.add('active');
        loginTab.classList.remove('active');
        registerForm.classList.add('active');
        loginForm.classList.remove('active');
        clearMessages();
    }
}

// Clear error messages and form messages
function clearMessages() {
    document.querySelectorAll('.error-message').forEach(el => el.textContent = '');
    document.querySelectorAll('.message').forEach(el => {
        el.classList.remove('success', 'error');
        el.style.display = 'none';
    });
}

// Clear input errors
function clearInputErrors(formId) {
    const form = document.getElementById(formId);
    form.querySelectorAll('.error-message').forEach(el => el.textContent = '');
    form.querySelectorAll('input').forEach(input => input.classList.remove('error'));
}

// Show error message
function showError(elementId, message) {
    const errorElement = document.getElementById(elementId);
    errorElement.textContent = message;
    const input = errorElement.previousElementSibling;
    if (input && input.tagName === 'INPUT') {
        input.classList.add('error');
    }
}

// Show form message
function showMessage(formType, message, type = 'error') {
    const messageElement = document.getElementById(`${formType}Message`);
    messageElement.textContent = message;
    messageElement.classList.remove('success', 'error');
    messageElement.classList.add(type);
    messageElement.style.display = 'block';
    
    if (type === 'success') {
        setTimeout(() => {
            messageElement.style.display = 'none';
        }, 3000);
    }
}

// Set loading state
function setLoading(buttonId, isLoading) {
    const button = document.getElementById(buttonId);
    const btnText = button.querySelector('.btn-text');
    const btnLoader = button.querySelector('.btn-loader');
    
    if (isLoading) {
        button.disabled = true;
        btnText.style.display = 'none';
        btnLoader.style.display = 'inline-block';
    } else {
        button.disabled = false;
        btnText.style.display = 'inline';
        btnLoader.style.display = 'none';
    }
}

// Save token to localStorage
function saveToken(token) {
    localStorage.setItem('jwt_token', token);
}

// Get token from localStorage
function getToken() {
    return localStorage.getItem('jwt_token');
}

// Check if user is already logged in
function checkAuth() {
    const token = getToken();
    if (token) {
        // Redirect to main page if token exists
        // For now, we'll just log it
        console.log('User already authenticated');
        // You can redirect here: window.location.href = '/dashboard.html';
    }
}

// Login Form Handler
const loginFormElement = document.getElementById('loginFormElement');
loginFormElement.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearInputErrors('loginFormElement');
    
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;
    
    // Client-side validation
    if (!email) {
        showError('loginEmailError', 'Email boş olamaz');
        return;
    }
    
    if (!isValidEmail(email)) {
        showError('loginEmailError', 'Geçerli bir email adresi giriniz');
        return;
    }
    
    if (!password) {
        showError('loginPasswordError', 'Şifre boş olamaz');
        return;
    }
    
    setLoading('loginBtn', true);
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });
        
        if (response.ok) {
            const token = await response.text(); // Token is returned as plain text
            saveToken(token);
            showMessage('login', 'Giriş başarılı! Yönlendiriliyorsunuz...', 'success');
            
            // Redirect after successful login
            setTimeout(() => {
                window.location.href = '/products.html'; // or your main page
            }, 1500);
        } else {
            let errorMessage = 'Giriş başarısız';
            
            if (response.status === 401) {
                errorMessage = 'Email veya şifre hatalı';
            } else if (response.status === 403) {
                errorMessage = 'Hesap kilitli veya aktif değil';
            } else {
                try {
                    const errorText = await response.text();
                    if (errorText) {
                        errorMessage = errorText;
                    }
                } catch (e) {
                    // If response is not readable text
                }
            }
            
            showMessage('login', errorMessage, 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('login', 'Bağlantı hatası. Lütfen tekrar deneyin.', 'error');
    } finally {
        setLoading('loginBtn', false);
    }
});

// Register Form Handler
const registerFormElement = document.getElementById('registerFormElement');
registerFormElement.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearInputErrors('registerFormElement');
    
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    // Client-side validation
    if (!email) {
        showError('registerEmailError', 'Email boş olamaz');
        return;
    }
    
    if (!isValidEmail(email)) {
        showError('registerEmailError', 'Geçerli bir email adresi giriniz');
        return;
    }
    
    if (!password) {
        showError('registerPasswordError', 'Şifre boş olamaz');
        return;
    }
    
    if (password.length < 8) {
        showError('registerPasswordError', 'Şifre en az 8 karakter olmalıdır');
        return;
    }
    
    if (!confirmPassword) {
        showError('confirmPasswordError', 'Şifre tekrarı boş olamaz');
        return;
    }
    
    if (password !== confirmPassword) {
        showError('confirmPasswordError', 'Şifreler eşleşmiyor');
        return;
    }
    
    setLoading('registerBtn', true);
    
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                email: email,
                password: password,
                confirmPassword: confirmPassword
            })
        });
        
        if (response.ok || response.status === 201) {
            const token = await response.text(); // Token is returned as plain text
            saveToken(token);
            showMessage('register', 'Kayıt başarılı! Yönlendiriliyorsunuz...', 'success');
            
            // Clear form
            registerFormElement.reset();
            
            // Switch to login tab or redirect
            setTimeout(() => {
                switchTab('login');
                // Or redirect: window.location.href = '/products.html';
            }, 1500);
        } else {
            let errorMessage = 'Kayıt başarısız';
            
            if (response.status === 400) {
                errorMessage = 'Validation hatası veya email zaten kullanımda';
                try {
                    const errorText = await response.text();
                    if (errorText && errorText.length < 200) {
                        errorMessage = errorText;
                    }
                } catch (e) {
                    // If response is not readable text
                }
            } else {
                try {
                    const errorText = await response.text();
                    if (errorText && errorText.length < 200) {
                        errorMessage = errorText;
                    }
                } catch (e) {
                    // If response is not readable text
                }
            }
            
            showMessage('register', errorMessage, 'error');
        }
    } catch (error) {
        console.error('Register error:', error);
        showMessage('register', 'Bağlantı hatası. Lütfen tekrar deneyin.', 'error');
    } finally {
        setLoading('registerBtn', false);
    }
});

// Email validation helper
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Initialize - check if user is already authenticated
checkAuth();

