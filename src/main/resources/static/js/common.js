// Common JavaScript Functions - API helpers, token management, etc.

const API_BASE_URL = window.location.origin;

// Token Management
const TokenManager = {
    getToken: () => {
        return localStorage.getItem('jwt_token');
    },

    saveToken: (token) => {
        localStorage.setItem('jwt_token', token);
    },

    removeToken: () => {
        localStorage.removeItem('jwt_token');
    },

    isAuthenticated: () => {
        return !!TokenManager.getToken();
    }
};

// API Request Helper
async function apiRequest(url, options = {}) {
    const token = TokenManager.getToken();
    
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        }
    };

    // Add authorization header if token exists
    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`;
    }

    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    };

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, finalOptions);
        return response;
    } catch (error) {
        console.error('API request error:', error);
        throw error;
    }
}

// API Helper Functions
const API = {
    // Auth
    login: async (email, password) => {
        const response = await apiRequest('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        if (response.ok) {
            const token = await response.text();
            TokenManager.saveToken(token);
            return { success: true, token };
        }
        return { success: false, status: response.status };
    },

    register: async (email, password, confirmPassword) => {
        const response = await apiRequest('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({ email, password, confirmPassword })
        });
        if (response.ok || response.status === 201) {
            const token = await response.text();
            TokenManager.saveToken(token);
            return { success: true, token };
        }
        return { success: false, status: response.status };
    },

    logout: () => {
        TokenManager.removeToken();
        window.location.href = '/';
    },

    // Products
    getProducts: async () => {
        const response = await apiRequest('/api/products');
        if (response.ok) {
            return await response.json();
        }
        return [];
    },

    searchProducts: async (name) => {
        const response = await apiRequest(`/api/products/search?name=${encodeURIComponent(name)}`);
        if (response.ok) {
            return await response.json();
        }
        return [];
    },

    getProductBySku: async (sku) => {
        const response = await apiRequest(`/api/products/${sku}`);
        if (response.ok) {
            return await response.json();
        }
        return null;
    },

    // Cart
    getCart: async () => {
        const response = await apiRequest('/api/cart');
        if (response.ok) {
            return await response.json();
        }
        if (response.status === 401) {
            TokenManager.removeToken();
            window.location.href = '/';
            return null;
        }
        return null;
    },

    addToCart: async (productSku, quantity) => {
        const response = await apiRequest('/api/cart/add', {
            method: 'POST',
            body: JSON.stringify({ productSku, quantity })
        });
        if (response.ok) {
            return await response.json();
        }
        return null;
    },

    updateCartItem: async (productSku, quantity) => {
        const response = await apiRequest('/api/cart/update', {
            method: 'PUT',
            body: JSON.stringify({ productSku, quantity })
        });
        if (response.ok) {
            return await response.json();
        }
        return null;
    },

    removeFromCart: async (productSku, quantity) => {
        const url = quantity 
            ? `/api/cart/remove/${productSku}?quantity=${quantity}`
            : `/api/cart/remove/${productSku}`;
        const response = await apiRequest(url, {
            method: 'DELETE'
        });
        return response.ok;
    },

    clearCart: async () => {
        const response = await apiRequest('/api/cart/clear', {
            method: 'DELETE'
        });
        return response.ok;
    },

    getCartCount: async () => {
        const response = await apiRequest('/api/cart/count');
        if (response.ok) {
            return await response.text().then(text => parseInt(text) || 0);
        }
        return 0;
    }
};

// Format Price
function formatPrice(price, currency = 'TRY') {
    return new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency: currency
    }).format(price);
}

// Show Message
function showMessage(message, type = 'info', duration = 3000) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;
    
    document.body.insertBefore(messageDiv, document.body.firstChild);
    
    setTimeout(() => {
        messageDiv.remove();
    }, duration);
}

// Check Authentication
function checkAuth() {
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/';
        return false;
    }
    return true;
}

// Update Cart Badge
async function updateCartBadge() {
    if (!TokenManager.isAuthenticated()) {
        const badge = document.getElementById('cartBadge');
        if (badge) badge.style.display = 'none';
        return;
    }

    try {
        const count = await API.getCartCount();
        const badge = document.getElementById('cartBadge');
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'inline' : 'none';
        }
    } catch (error) {
        console.error('Failed to update cart badge:', error);
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    updateCartBadge();
});

