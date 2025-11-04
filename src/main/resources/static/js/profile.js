// Profile Page JavaScript

// JWT Token'dan kullanıcı bilgilerini çıkar
function parseJwtToken(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (error) {
        console.error('JWT token parse error:', error);
        return null;
    }
}

// Sayfa yüklendiğinde
document.addEventListener('DOMContentLoaded', async () => {
    // Authentication kontrolü
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/';
        return;
    }

    // Sepet sayısını güncelle
    updateCartBadge();

    // Profil bilgilerini yükle
    await loadProfileInfo();
    
    // Adres bilgilerini yükle
    await loadAddresses();
    
    // Ödeme yöntemlerini yükle
    await loadPaymentMethods();
    
    // Siparişleri yükle
    await loadOrders();
});

// Sepet rozetini güncelle
async function updateCartBadge() {
    const badge = document.getElementById('cartBadge');
    if (badge) {
        const count = await API.getCartCount();
        badge.textContent = count || 0;
    }
}

// Profil bilgilerini yükle
async function loadProfileInfo() {
    const token = TokenManager.getToken();
    if (!token) {
        return;
    }

    const payload = parseJwtToken(token);
    if (!payload) {
        return;
    }

    // JWT token'dan bilgileri al
    const email = payload.email || '-';
    const role = payload.role || 'CUSTOMER';
    const roleText = role === 'ADMIN' ? 'Yönetici' : 'Müşteri';

    // Email'i göster
    const emailElement = document.getElementById('userEmail');
    if (emailElement) {
        emailElement.textContent = email;
    }

    // Rolü göster
    const roleElement = document.getElementById('userRole');
    if (roleElement) {
        roleElement.textContent = roleText;
    }

    // Üyelik tarihi - JWT'de yoksa siparişlerden en eski tarihi al
    const createdAtElement = document.getElementById('userCreatedAt');
    if (createdAtElement) {
        // Şimdilik token'dan alınamadığı için siparişlerden alacağız
        // Veya "Bilinmiyor" yazabiliriz
        createdAtElement.textContent = 'Yükleniyor...';
    }
}

// Adres bilgilerini yükle (Order'lardan)
async function loadAddresses() {
    const container = document.getElementById('addressesContainer');
    if (!container) {
        return;
    }

    try {
        const orders = await API.getOrders();
        if (!orders || orders.length === 0) {
            container.innerHTML = '<p class="no-data">Henüz kayıtlı adres bulunmuyor</p>';
            return;
        }

        // Unique adresleri çıkar (shipping ve billing)
        const addresses = new Map();
        
        orders.forEach(order => {
            // Shipping address
            if (order.shippingAddress) {
                const addressKey = JSON.stringify(order.shippingAddress);
                if (!addresses.has(addressKey)) {
                    addresses.set(addressKey, {
                        type: 'shipping',
                        address: order.shippingAddress
                    });
                }
            }
            
            // Billing address
            if (order.billingAddress) {
                const addressKey = JSON.stringify(order.billingAddress);
                if (!addresses.has(addressKey)) {
                    addresses.set(addressKey, {
                        type: 'billing',
                        address: order.billingAddress
                    });
                }
            }
        });

        if (addresses.size === 0) {
            container.innerHTML = '<p class="no-data">Henüz kayıtlı adres bulunmuyor</p>';
            return;
        }

        // Adresleri göster
        let html = '<div class="address-list">';
        addresses.forEach((addressData) => {
            const address = addressData.address;
            const type = addressData.type;
            const typeText = type === 'shipping' ? 'Teslimat Adresi' : 'Fatura Adresi';
            
            html += `
                <div class="address-item">
                    <div class="address-item-header">
                        <span class="address-type ${type}">${typeText}</span>
                    </div>
                    <div class="address-details">
                        ${address.city ? `<p><strong>Şehir:</strong> ${address.city}</p>` : ''}
                        ${address.district ? `<p><strong>İlçe:</strong> ${address.district}</p>` : ''}
                        ${address.neighborhood ? `<p><strong>Mahalle:</strong> ${address.neighborhood}</p>` : ''}
                        ${address.street ? `<p><strong>Sokak:</strong> ${address.street}</p>` : ''}
                        ${address.buildingNo ? `<p><strong>Bina No:</strong> ${address.buildingNo}</p>` : ''}
                        ${address.apartmentNo ? `<p><strong>Daire No:</strong> ${address.apartmentNo}</p>` : ''}
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;

    } catch (error) {
        console.error('Adres yükleme hatası:', error);
        container.innerHTML = '<p class="no-data">Adresler yüklenirken bir hata oluştu</p>';
    }
}

// Ödeme yöntemlerini yükle
async function loadPaymentMethods() {
    const container = document.getElementById('paymentMethodsContainer');
    if (!container) {
        return;
    }

    try {
        const methods = await API.getPaymentMethods();
        if (!methods || methods.length === 0) {
            container.innerHTML = '<p class="no-data">Henüz ödeme yöntemi eklenmemiş</p>';
            return;
        }

        let html = '<div class="payment-methods-list">';
        methods.forEach((method) => {
            const cardNumber = method.maskedCardNumber || '**** **** **** ****';
            const cardType = method.cardType || 'Bilinmiyor';
            const isDefault = method.isDefault || false;
            
            html += `
                <div class="payment-method-item">
                    <div class="payment-method-info">
                        <div class="payment-method-name">${method.methodName || 'İsimsiz'}</div>
                        <div class="payment-method-details">
                            <span>${cardNumber}</span>
                            <span class="payment-method-type">${cardType}</span>
                            ${isDefault ? '<span class="payment-method-badge">Varsayılan</span>' : ''}
                        </div>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;

    } catch (error) {
        console.error('Ödeme yöntemleri yükleme hatası:', error);
        container.innerHTML = '<p class="no-data">Ödeme yöntemleri yüklenirken bir hata oluştu</p>';
    }
}

// Siparişleri yükle
async function loadOrders() {
    const container = document.getElementById('ordersContainer');
    if (!container) {
        return;
    }

    try {
        const orders = await API.getOrders();
        if (!orders || orders.length === 0) {
            container.innerHTML = '<p class="no-data">Henüz sipariş bulunmuyor</p>';
            
            // Üyelik tarihini ayarla (sipariş yoksa token'dan alınamıyor)
            const createdAtElement = document.getElementById('userCreatedAt');
            if (createdAtElement) {
                createdAtElement.textContent = 'Bilinmiyor';
            }
            return;
        }

        // Üyelik tarihini en eski sipariş tarihinden al (yaklaşık)
        const createdAtElement = document.getElementById('userCreatedAt');
        if (createdAtElement && orders.length > 0) {
            const oldestOrder = orders[orders.length - 1];
            if (oldestOrder.createdAt) {
                const date = new Date(oldestOrder.createdAt);
                createdAtElement.textContent = date.toLocaleDateString('tr-TR', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                });
            }
        }

        let html = '<div class="orders-list">';
        orders.forEach((order) => {
            const orderNumber = order.orderNumber || 'N/A';
            const orderDate = order.createdAt ? new Date(order.createdAt).toLocaleDateString('tr-TR', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            }) : 'Bilinmiyor';
            const status = order.status || 'PENDING';
            const statusText = getOrderStatusText(status);
            const statusClass = status.toLowerCase();
            const totalAmount = order.totalAmount || 0;
            const currency = order.currency || 'TRY';
            
            // Sipariş detayları
            const itemCount = order.orderItems ? order.orderItems.length : 0;
            
            html += `
                <div class="order-item" onclick="viewOrderDetail('${order.id}')">
                    <div class="order-header">
                        <div>
                            <div class="order-number">Sipariş #${orderNumber}</div>
                            <div class="order-date">${orderDate}</div>
                        </div>
                        <div>
                            <span class="order-status ${statusClass}">${statusText}</span>
                        </div>
                    </div>
                    <div class="order-details">
                        <div class="order-detail-item">
                            <div class="order-detail-label">Ürün Sayısı</div>
                            <div class="order-detail-value">${itemCount} ürün</div>
                        </div>
                        <div class="order-detail-item">
                            <div class="order-detail-label">Ödeme Durumu</div>
                            <div class="order-detail-value">${getPaymentStatusText(order.paymentStatus)}</div>
                        </div>
                    </div>
                    <div class="order-total">
                        <span class="order-total-label">Toplam</span>
                        <span class="order-total-value">${formatPrice(totalAmount, currency)}</span>
                    </div>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;

    } catch (error) {
        console.error('Sipariş yükleme hatası:', error);
        container.innerHTML = '<p class="no-data">Siparişler yüklenirken bir hata oluştu</p>';
    }
}

// Sipariş durumu metnini al
function getOrderStatusText(status) {
    const statusMap = {
        'PENDING': 'Beklemede',
        'PROCESSING': 'İşleniyor',
        'SHIPPED': 'Kargoda',
        'DELIVERED': 'Teslim Edildi',
        'CANCELLED': 'İptal Edildi'
    };
    return statusMap[status] || status;
}

// Ödeme durumu metnini al
function getPaymentStatusText(status) {
    const statusMap = {
        'NONE': 'Ödenmedi',
        'PENDING': 'Beklemede',
        'COMPLETED': 'Ödendi',
        'FAILED': 'Başarısız',
        'REFUNDED': 'İade Edildi'
    };
    return statusMap[status] || status;
}

// Sipariş detayını görüntüle
function viewOrderDetail(orderId) {
    // Şimdilik sipariş detay sayfası yok, console'a yazdır
    console.log('Sipariş detayı:', orderId);
    // İleride sipariş detay sayfası eklendiğinde buraya yönlendirme yapılabilir
    // window.location.href = `/orders.html?id=${orderId}`;
}


