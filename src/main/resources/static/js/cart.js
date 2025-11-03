// Cart Page JavaScript

let cartData = null;

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    // Check authentication
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/';
        return;
    }

    // Load cart
    await loadCart();

    // Update cart badge
    updateCartBadge();
});

// Load cart data
async function loadCart() {
    showLoading(true);
    
    try {
        cartData = await API.getCart();
        
        if (!cartData) {
            showMessage('Sepet yüklenirken bir hata oluştu', 'error');
            return;
        }

        displayCart(cartData);
        updateCartStats(cartData);
    } catch (error) {
        console.error('Failed to load cart:', error);
        showMessage('Sepet yüklenirken bir hata oluştu', 'error');
    } finally {
        showLoading(false);
    }
}

// Display cart
function displayCart(cart) {
    const cartContent = document.getElementById('cartContent');
    const emptyCart = document.getElementById('emptyCart');

    if (!cart.items || cart.items.length === 0) {
        cartContent.style.display = 'none';
        emptyCart.style.display = 'block';
        return;
    }

    cartContent.style.display = 'grid';
    emptyCart.style.display = 'none';

    cartContent.innerHTML = `
        <div class="cart-items" id="cartItems">
            ${cart.items.map(item => createCartItemHTML(item)).join('')}
        </div>
        <div class="cart-summary">
            ${createCartSummaryHTML(cart)}
        </div>
    `;

    // Add event listeners to quantity controls
    attachEventListeners();
}

// Create cart item HTML
function createCartItemHTML(item) {
    return `
        <div class="cart-item" data-sku="${item.productSku}" id="cart-item-${item.productSku}">
            <div class="cart-item-image">📦</div>
            <div class="cart-item-details">
                <h3 class="cart-item-name">${escapeHtml(item.productName)}</h3>
                <p class="cart-item-sku">SKU: ${escapeHtml(item.productSku)}</p>
                <p class="cart-item-price">Birim Fiyat: ${formatPrice(item.unitPrice)}</p>
                <div class="cart-item-actions">
                    <div class="quantity-controls">
                        <button class="quantity-btn" onclick="decreaseQuantity('${item.productSku}')">−</button>
                        <input 
                            type="number" 
                            class="quantity-input" 
                            id="quantity-${item.productSku}"
                            value="${item.quantity}" 
                            min="1"
                            readonly
                        >
                        <button class="quantity-btn" onclick="increaseQuantity('${item.productSku}')">+</button>
                    </div>
                    <button class="remove-btn" onclick="removeItem('${item.productSku}')" title="Sepetten Çıkar">
                        🗑️
                    </button>
                </div>
            </div>
            <div class="cart-item-total">
                ${formatPrice(item.totalPrice)}
            </div>
        </div>
    `;
}

// Create cart summary HTML
function createCartSummaryHTML(cart) {
    return `
        <h2 class="summary-header">Sipariş Özeti</h2>
        <div class="summary-row">
            <span class="summary-label">Toplam Ürün</span>
            <span class="summary-value">${cart.totalItemCount || 0} adet</span>
        </div>
        <div class="summary-row">
            <span class="summary-label">Farklı Ürün</span>
            <span class="summary-value">${cart.uniqueItemCount || 0} çeşit</span>
        </div>
        <div class="summary-row summary-total">
            <span class="summary-label">Toplam Tutar</span>
            <span class="summary-value">${formatPrice(cart.totalAmount || 0)}</span>
        </div>
        <div class="summary-actions">
            <button class="btn btn-primary btn-checkout" onclick="checkout()">
                Ödemeye Geç
            </button>
            <button class="btn btn-secondary btn-clear-cart" onclick="clearCart()">
                Sepeti Temizle
            </button>
        </div>
    `;
}

// Update cart statistics
function updateCartStats(cart) {
    const itemCount = document.getElementById('itemCount');
    if (itemCount) {
        itemCount.textContent = `${cart.totalItemCount || 0} ürün`;
    }
}

// Attach event listeners
function attachEventListeners() {
    // Quantity input change listeners could be added here if needed
}

// Increase quantity
async function increaseQuantity(productSku) {
    const item = cartData.items.find(i => i.productSku === productSku);
    if (!item) return;

    const newQuantity = item.quantity + 1;
    await updateQuantity(productSku, newQuantity);
}

// Decrease quantity
async function decreaseQuantity(productSku) {
    const item = cartData.items.find(i => i.productSku === productSku);
    if (!item) return;

    const newQuantity = Math.max(1, item.quantity - 1);
    await updateQuantity(productSku, newQuantity);
}

// Update quantity
async function updateQuantity(productSku, quantity) {
    const cartItem = document.getElementById(`cart-item-${productSku}`);
    if (cartItem) {
        cartItem.classList.add('loading');
    }

    try {
        const result = await API.updateCartItem(productSku, quantity);
        if (result) {
            // Reload cart to get updated data
            await loadCart();
            showMessage('Sepet güncellendi', 'success');
            updateCartBadge();
        } else {
            showMessage('Sepet güncellenirken bir hata oluştu', 'error');
            if (cartItem) {
                cartItem.classList.remove('loading');
            }
        }
    } catch (error) {
        console.error('Update quantity error:', error);
        showMessage('Sepet güncellenirken bir hata oluştu', 'error');
        if (cartItem) {
            cartItem.classList.remove('loading');
        }
    }
}

// Remove item from cart
async function removeItem(productSku) {
    if (!confirm('Bu ürünü sepetten çıkarmak istediğinize emin misiniz?')) {
        return;
    }

    const cartItem = document.getElementById(`cart-item-${productSku}`);
    if (cartItem) {
        cartItem.classList.add('loading');
    }

    try {
        const success = await API.removeFromCart(productSku);
        if (success) {
            // Reload cart to get updated data
            await loadCart();
            showMessage('Ürün sepetten çıkarıldı', 'success');
            updateCartBadge();
        } else {
            showMessage('Ürün çıkarılırken bir hata oluştu', 'error');
            if (cartItem) {
                cartItem.classList.remove('loading');
            }
        }
    } catch (error) {
        console.error('Remove item error:', error);
        showMessage('Ürün çıkarılırken bir hata oluştu', 'error');
        if (cartItem) {
            cartItem.classList.remove('loading');
        }
    }
}

// Clear cart
async function clearCart() {
    if (!confirm('Sepetinizdeki tüm ürünleri çıkarmak istediğinize emin misiniz?')) {
        return;
    }

    try {
        const success = await API.clearCart();
        if (success) {
            await loadCart();
            showMessage('Sepet temizlendi', 'success');
            updateCartBadge();
        } else {
            showMessage('Sepet temizlenirken bir hata oluştu', 'error');
        }
    } catch (error) {
        console.error('Clear cart error:', error);
        showMessage('Sepet temizlenirken bir hata oluştu', 'error');
    }
}

// Checkout - redirect to checkout page
function checkout() {
    window.location.href = '/checkout.html';
}

// Show loading spinner
function showLoading(show) {
    const spinner = document.getElementById('loadingSpinner');
    const cartContent = document.getElementById('cartContent');
    const emptyCart = document.getElementById('emptyCart');
    
    if (show) {
        spinner.style.display = 'block';
        if (cartContent) cartContent.style.display = 'none';
        if (emptyCart) emptyCart.style.display = 'none';
    } else {
        spinner.style.display = 'none';
    }
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}


