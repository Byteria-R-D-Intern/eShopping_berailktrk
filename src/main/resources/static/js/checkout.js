// Checkout Page JavaScript

let cartData = null;
let paymentMethods = [];
let selectedPaymentMethod = null;

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    // Check authentication
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/';
        return;
    }

    // Load cart and payment methods
    await Promise.all([
        loadCart(),
        loadPaymentMethods()
    ]);

    // Setup form listeners
    setupFormListeners();

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
            window.location.href = '/cart.html';
            return;
        }

        if (!cartData.items || cartData.items.length === 0) {
            showMessage('Sepetiniz boş', 'error');
            window.location.href = '/cart.html';
            return;
        }

        displayOrderSummary(cartData);
    } catch (error) {
        console.error('Failed to load cart:', error);
        showMessage('Sepet yüklenirken bir hata oluştu', 'error');
    } finally {
        showLoading(false);
    }
}

// Load payment methods
async function loadPaymentMethods() {
    try {
        const response = await API.getPaymentMethods();
        if (response && response.paymentMethods) {
            paymentMethods = response.paymentMethods;
            displayPaymentMethods();
        }
    } catch (error) {
        console.error('Failed to load payment methods:', error);
    }
}

// Display payment methods
function displayPaymentMethods() {
    const container = document.getElementById('paymentMethodsContainer');
    
    if (!paymentMethods || paymentMethods.length === 0) {
        container.innerHTML = '<div class="no-payment-methods">Henüz ödeme yöntemi eklenmemiş. Yeni ödeme yöntemi ekleyin.</div>';
        return;
    }

    container.innerHTML = paymentMethods.map(method => createPaymentMethodHTML(method)).join('');
    
    // Add click listeners
    paymentMethods.forEach(method => {
        const element = document.getElementById(`payment-method-${method.sequenceNumber}`);
        if (element) {
            element.addEventListener('click', () => selectPaymentMethod(method.sequenceNumber));
        }

        // Delete button listener
        const deleteBtn = document.getElementById(`delete-payment-${method.sequenceNumber}`);
        if (deleteBtn) {
            deleteBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                deletePaymentMethod(method.sequenceNumber);
            });
        }
    });

    // Select default payment method if exists
    const defaultMethod = paymentMethods.find(m => m.isDefault);
    if (defaultMethod) {
        selectPaymentMethod(defaultMethod.sequenceNumber);
    } else if (paymentMethods.length > 0) {
        selectPaymentMethod(paymentMethods[0].sequenceNumber);
    }
}

// Create payment method HTML
function createPaymentMethodHTML(method) {
    const cardInfo = method.cardInfo;
    const maskedNumber = cardInfo ? cardInfo.maskedCardNumber : 'N/A';
    const cardType = cardInfo ? cardInfo.cardType : '';
    const expiryDate = cardInfo ? cardInfo.expiryDate : '';
    
    return `
        <div class="payment-method-item" id="payment-method-${method.sequenceNumber}" data-sequence="${method.sequenceNumber}">
            <div class="payment-method-info">
                <div class="payment-method-name">${escapeHtml(method.methodName)}</div>
                <div class="payment-method-details">
                    <span>${maskedNumber}</span>
                    <span>•</span>
                    <span>${expiryDate}</span>
                    <span class="payment-method-type">${method.methodType}</span>
                    ${method.isDefault ? '<span class="payment-method-badge">Varsayılan</span>' : ''}
                </div>
            </div>
            <div class="payment-method-actions">
                <button class="delete-payment-btn" id="delete-payment-${method.sequenceNumber}" title="Sil">🗑️</button>
            </div>
        </div>
    `;
}

// Select payment method
function selectPaymentMethod(sequenceNumber) {
    selectedPaymentMethod = sequenceNumber;
    
    // Update UI
    document.querySelectorAll('.payment-method-item').forEach(item => {
        item.classList.remove('selected');
    });
    
    const selectedElement = document.getElementById(`payment-method-${sequenceNumber}`);
    if (selectedElement) {
        selectedElement.classList.add('selected');
    }
}

// Setup form listeners
function setupFormListeners() {
    // Same as shipping checkbox
    const sameAsShipping = document.getElementById('sameAsShipping');
    const billingForm = document.getElementById('billingAddressForm');
    
    sameAsShipping.addEventListener('change', (e) => {
        billingForm.style.display = e.target.checked ? 'none' : 'block';
    });

    // Card number - only numbers
    const cardNumberInput = document.getElementById('cardNumber');
    if (cardNumberInput) {
        cardNumberInput.addEventListener('input', (e) => {
            // Remove all non-numeric characters
            let value = e.target.value.replace(/\D/g, '');
            // Format with spaces every 4 digits
            value = value.replace(/(.{4})/g, '$1 ').trim();
            e.target.value = value;
            
            // Update card preview
            updateCardPreview(value.replace(/\D/g, ''), e.target.value);
        });
        
        // Prevent non-numeric input
        cardNumberInput.addEventListener('keypress', (e) => {
            if (!/[0-9\s]/.test(e.key) && !['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
                e.preventDefault();
            }
        });
    }

    // Cardholder name - only letters
    const cardholderInput = document.getElementById('cardholderName');
    if (cardholderInput) {
        cardholderInput.addEventListener('input', (e) => {
            // Update card preview
            updateCardPreviewName(e.target.value.toUpperCase() || 'KART SAHIBI');
        });
        
        cardholderInput.addEventListener('keypress', (e) => {
            // Allow letters, spaces, and Turkish characters
            if (!/^[A-Za-zğüşıöçĞÜŞİÖÇ\s]$/.test(e.key) && !['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
                e.preventDefault();
            }
        });
    }

    // CVV - only numbers, small input
    const cvvInput = document.getElementById('cvv');
    if (cvvInput) {
        cvvInput.addEventListener('input', (e) => {
            // Remove all non-numeric characters
            e.target.value = e.target.value.replace(/\D/g, '');
        });
        
        // Prevent non-numeric input
        cvvInput.addEventListener('keypress', (e) => {
            if (!/[0-9]/.test(e.key) && !['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
                e.preventDefault();
            }
        });
    }

    // Populate expiry year dropdown
    populateExpiryYears();

    // Expiry date - update card preview
    const expiryMonthSelect = document.getElementById('expiryMonth');
    const expiryYearSelect = document.getElementById('expiryYear');
    if (expiryMonthSelect && expiryYearSelect) {
        const updateExpiry = () => {
            const month = expiryMonthSelect.value;
            const year = expiryYearSelect.value;
            const expiry = (month && year) ? `${month}/${year}` : 'MM/YY';
            updateCardPreviewExpiry(expiry);
        };
        expiryMonthSelect.addEventListener('change', updateExpiry);
        expiryYearSelect.addEventListener('change', updateExpiry);
    }

    // Order notes character counter
    const orderNotesInput = document.getElementById('orderNotes');
    const orderNotesCharCount = document.getElementById('orderNotesCharCount');
    if (orderNotesInput && orderNotesCharCount) {
        orderNotesInput.addEventListener('input', (e) => {
            const length = e.target.value.length;
            orderNotesCharCount.textContent = length;
            
            // Change color if approaching limit
            if (length > 180) {
                orderNotesCharCount.style.color = '#e74c3c';
            } else if (length > 150) {
                orderNotesCharCount.style.color = '#ffc107';
            } else {
                orderNotesCharCount.style.color = '#666';
            }
        });
    }

}

// Populate expiry year dropdown (current year + 10 years)
function populateExpiryYears() {
    const yearSelect = document.getElementById('expiryYear');
    if (!yearSelect) return;
    
    const currentYear = new Date().getFullYear();
    for (let i = 0; i <= 10; i++) {
        const year = currentYear + i;
        const yearShort = year.toString().substring(2);
        const option = document.createElement('option');
        option.value = yearShort;
        option.textContent = yearShort;
        yearSelect.appendChild(option);
    }
}

// Update card preview
function updateCardPreview(cardNumberDigits, cardNumberFormatted) {
    const previewNumber = document.getElementById('previewCardNumber');
    const previewBrand = document.getElementById('previewCardBrand');
    
    if (!previewNumber || !previewBrand) return;
    
    // Mask card number
    let maskedNumber = '•••• •••• •••• ••••';
    if (cardNumberDigits.length > 0) {
        // Show first 4 digits, mask the rest
        const firstFour = cardNumberDigits.substring(0, 4);
        const remaining = cardNumberDigits.substring(4);
        const masked = '•'.repeat(Math.min(remaining.length, 12));
        
        // Format with spaces every 4 characters
        let formatted = firstFour;
        if (remaining.length > 0) {
            formatted += ' ' + masked.substring(0, 4);
            if (remaining.length > 4) {
                formatted += ' ' + masked.substring(4, 8);
                if (remaining.length > 8) {
                    formatted += ' ' + masked.substring(8, 12);
                }
            }
        }
        maskedNumber = formatted;
    }
    
    previewNumber.textContent = maskedNumber;
    
    // Determine card brand
    const brand = detectCardBrand(cardNumberDigits);
    const brandText = document.getElementById('previewCardBrandText');
    previewBrand.className = 'card-preview-brand';
    if (brand) {
        previewBrand.classList.add(brand.toLowerCase());
        if (brandText) {
            if (brand.toLowerCase() === 'mastercard') {
                brandText.textContent = 'mastercard';
            } else if (brand.toLowerCase() === 'visa') {
                brandText.textContent = '';
            } else if (brand.toLowerCase() === 'amex') {
                brandText.textContent = '';
            } else {
                brandText.textContent = brand.toUpperCase();
            }
        }
    } else {
        if (brandText) brandText.textContent = '';
    }
}

// Update card holder name in preview
function updateCardPreviewName(name) {
    const previewName = document.getElementById('previewCardholderName');
    if (previewName) {
        previewName.textContent = name || 'KART SAHIBI';
    }
}

// Update expiry date in preview
function updateCardPreviewExpiry(expiry) {
    const previewExpiry = document.getElementById('previewExpiry');
    if (previewExpiry) {
        previewExpiry.textContent = expiry || 'MM/YY';
    }
}

// Detect card brand from card number
function detectCardBrand(cardNumber) {
    if (!cardNumber || cardNumber.length === 0) return null;
    
    const firstDigit = cardNumber[0];
    const firstTwoDigits = cardNumber.substring(0, 2);
    const firstFourDigits = cardNumber.substring(0, 4);
    
    // Visa: starts with 4
    if (firstDigit === '4') {
        return 'visa';
    }
    
    // Mastercard: starts with 5, range 51-55
    if (firstDigit === '5' && parseInt(firstTwoDigits) >= 51 && parseInt(firstTwoDigits) <= 55) {
        return 'mastercard';
    }
    
    // American Express: starts with 34 or 37
    if (firstFourDigits === '34' || firstFourDigits === '37') {
        return 'amex';
    }
    
    // Discover: starts with 6
    if (firstDigit === '6') {
        return 'discover';
    }
    
    return null;
}

// Show add payment method form
function showAddPaymentMethodForm() {
    document.getElementById('addPaymentMethodForm').style.display = 'block';
    document.getElementById('addPaymentMethodBtn').style.display = 'none';
}

// Hide add payment method form
function hideAddPaymentMethodForm() {
    document.getElementById('addPaymentMethodForm').style.display = 'none';
    document.getElementById('addPaymentMethodBtn').style.display = 'block';
    document.getElementById('paymentMethodForm').reset();
}

// Handle add payment method
document.getElementById('paymentMethodForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    await handleAddPaymentMethod();
});

async function handleAddPaymentMethod() {

    const methodName = document.getElementById('methodName').value.trim();
    const cardNumber = document.getElementById('cardNumber').value.replace(/\s/g, '');
    const cardholderName = document.getElementById('cardholderName').value.trim();
    const expiryMonth = document.getElementById('expiryMonth').value;
    const expiryYear = document.getElementById('expiryYear').value;
    const cvv = document.getElementById('cvv').value.trim();
    const isDefault = document.getElementById('isDefault').checked;

    // Build expiry date string (MM/YY)
    const expiryDate = expiryMonth && expiryYear ? `${expiryMonth}/${expiryYear}` : '';

    // Validation
    if (!methodName || !cardNumber || !cardholderName || !expiryMonth || !expiryYear || !cvv) {
        showMessage('Lütfen tüm alanları doldurun', 'error');
        return;
    }

    if (cardNumber.length < 13 || cardNumber.length > 19) {
        showMessage('Geçerli bir kart numarası giriniz', 'error');
        return;
    }

    if (!/^[0-9]+$/.test(cardNumber)) {
        showMessage('Kart numarası sadece rakamlardan oluşmalıdır', 'error');
        return;
    }

    if (!/^[A-Za-zğüşıöçĞÜŞİÖÇ\s]+$/.test(cardholderName)) {
        showMessage('Kart üzerindeki isim sadece harflerden oluşmalıdır', 'error');
        return;
    }

    if (cvv.length < 3 || cvv.length > 4) {
        showMessage('CVV 3 veya 4 haneli olmalıdır', 'error');
        return;
    }

    if (!/^[0-9]+$/.test(cvv)) {
        showMessage('CVV sadece rakamlardan oluşmalıdır', 'error');
        return;
    }

    try {
        const result = await API.addPaymentMethod(
            methodName,
            'CREDIT_CARD', // Default to CREDIT_CARD
            cardNumber,
            cardholderName,
            expiryDate,
            cvv,
            isDefault
        );

        if (result) {
            showMessage('Ödeme yöntemi eklendi', 'success');
            hideAddPaymentMethodForm();
            await loadPaymentMethods();
            selectPaymentMethod(result.sequenceNumber);
        } else {
            showMessage('Ödeme yöntemi eklenirken bir hata oluştu', 'error');
        }
    } catch (error) {
        console.error('Add payment method error:', error);
        showMessage('Ödeme yöntemi eklenirken bir hata oluştu', 'error');
    }
}

// Delete payment method
async function deletePaymentMethod(sequenceNumber) {
    if (!confirm('Bu ödeme yöntemini silmek istediğinize emin misiniz?')) {
        return;
    }

    try {
        const success = await API.deletePaymentMethod(sequenceNumber);
        if (success) {
            showMessage('Ödeme yöntemi silindi', 'success');
            await loadPaymentMethods();
            if (selectedPaymentMethod === sequenceNumber) {
                selectedPaymentMethod = null;
            }
        } else {
            showMessage('Ödeme yöntemi silinirken bir hata oluştu', 'error');
        }
    } catch (error) {
        console.error('Delete payment method error:', error);
        showMessage('Ödeme yöntemi silinirken bir hata oluştu', 'error');
    }
}

// Display order summary
function displayOrderSummary(cart) {
    const summaryContent = document.getElementById('orderSummaryContent');
    
    summaryContent.innerHTML = `
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
    `;
}

// Process checkout
async function processCheckout() {
    if (!selectedPaymentMethod) {
        showMessage('Lütfen bir ödeme yöntemi seçin', 'error');
        return;
    }

    // Validate forms
    const shippingAddress = getShippingAddress();
    if (!shippingAddress) {
        showMessage('Lütfen teslimat adresinin tüm gerekli alanlarını doldurun (Şehir, İlçe, Mahalle, Sokak, Bina No)', 'error');
        return;
    }

    let billingAddress = null;
    const sameAsShipping = document.getElementById('sameAsShipping').checked;
    if (!sameAsShipping) {
        billingAddress = getBillingAddress();
        if (!billingAddress) {
            showMessage('Lütfen fatura adresinin tüm gerekli alanlarını doldurun (Şehir, İlçe, Mahalle, Sokak, Bina No)', 'error');
            return;
        }
    }

    const orderNotes = document.getElementById('orderNotes').value.trim();

    showLoading(true);

    try {
        const order = await API.checkout(
            shippingAddress,
            billingAddress,
            selectedPaymentMethod,
            orderNotes,
            null // metadata
        );

        if (order) {
            showMessage('Sipariş oluşturuldu! Ödeme işlemi başlatılıyor...', 'success');
            
            // Initiate payment
            const payment = await API.initiatePayment(order.id, selectedPaymentMethod);
            
            if (payment) {
                showMessage('Ödeme başlatıldı! Siparişler sayfasına yönlendiriliyorsunuz...', 'success');
                setTimeout(() => {
                    window.location.href = '/orders.html';
                }, 2000);
            } else {
                showMessage('Sipariş oluşturuldu ancak ödeme başlatılamadı', 'error');
            }
        } else {
            showMessage('Sipariş oluşturulurken bir hata oluştu', 'error');
        }
    } catch (error) {
        console.error('Checkout error:', error);
        showMessage('Sipariş oluşturulurken bir hata oluştu', 'error');
    } finally {
        showLoading(false);
    }
}

// Get shipping address
function getShippingAddress() {
    const city = document.getElementById('shippingCity').value.trim();
    const district = document.getElementById('shippingDistrict').value.trim();
    const neighborhood = document.getElementById('shippingNeighborhood').value.trim();
    const street = document.getElementById('shippingStreet').value.trim();
    const buildingNo = document.getElementById('shippingBuildingNo').value.trim();
    const apartmentNo = document.getElementById('shippingApartmentNo').value.trim();

    if (!city || !district || !neighborhood || !street || !buildingNo) {
        return null;
    }

    return {
        city,
        district,
        neighborhood,
        street,
        buildingNo,
        apartmentNo: apartmentNo || null,
        country: 'Turkey'
    };
}

// Get billing address
function getBillingAddress() {
    const city = document.getElementById('billingCity').value.trim();
    const district = document.getElementById('billingDistrict').value.trim();
    const neighborhood = document.getElementById('billingNeighborhood').value.trim();
    const street = document.getElementById('billingStreet').value.trim();
    const buildingNo = document.getElementById('billingBuildingNo').value.trim();
    const apartmentNo = document.getElementById('billingApartmentNo').value.trim();

    if (!city || !district || !neighborhood || !street || !buildingNo) {
        return null;
    }

    return {
        city,
        district,
        neighborhood,
        street,
        buildingNo,
        apartmentNo: apartmentNo || null,
        country: 'Turkey'
    };
}

// Show loading spinner
function showLoading(show) {
    const spinner = document.getElementById('loadingSpinner');
    const checkoutContent = document.getElementById('checkoutContent');
    
    if (show) {
        spinner.style.display = 'block';
        if (checkoutContent) checkoutContent.style.display = 'none';
    } else {
        spinner.style.display = 'none';
        if (checkoutContent) checkoutContent.style.display = 'block';
    }
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

