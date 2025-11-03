// Products Page JavaScript

let currentProducts = [];

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    // Check authentication
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/';
        return;
    }

    // Load products
    await loadProducts();

    // Search functionality
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');

    searchBtn.addEventListener('click', handleSearch);
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            handleSearch();
        }
    });

    // Sort functionality
    const sortSelect = document.getElementById('sortSelect');
    sortSelect.addEventListener('change', handleSort);

    // Update cart badge
    updateCartBadge();
});

// Load all products
async function loadProducts() {
    showLoading(true);
    
    try {
        currentProducts = await API.getProducts();
        displayProducts(currentProducts);
    } catch (error) {
        console.error('Failed to load products:', error);
        showMessage('Ürünler yüklenirken bir hata oluştu', 'error');
    } finally {
        showLoading(false);
    }
}

// Search products
async function handleSearch() {
    const searchInput = document.getElementById('searchInput');
    const searchTerm = searchInput.value.trim();

    if (!searchTerm) {
        await loadProducts();
        return;
    }

    showLoading(true);

    try {
        currentProducts = await API.searchProducts(searchTerm);
        displayProducts(currentProducts);
    } catch (error) {
        console.error('Search failed:', error);
        showMessage('Arama sırasında bir hata oluştu', 'error');
    } finally {
        showLoading(false);
    }
}

// Display products
function displayProducts(products) {
    const grid = document.getElementById('productsGrid');
    const emptyState = document.getElementById('emptyState');

    if (products.length === 0) {
        grid.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    grid.style.display = 'grid';
    emptyState.style.display = 'none';

    // Apply current sort if any
    const sortedProducts = applySorting(products, document.getElementById('sortSelect').value);
    grid.innerHTML = sortedProducts.map(product => createProductCard(product)).join('');
}

// Handle sort change
function handleSort() {
    // Re-display products with current sort selection
    displayProducts([...currentProducts]);
}

// Apply sorting to products array
function applySorting(products, sortOption) {
    const sorted = [...products]; // Create a copy to avoid mutating original array

    switch(sortOption) {
        case 'price-asc':
            sorted.sort((a, b) => {
                const priceA = parseFloat(a.price) || 0;
                const priceB = parseFloat(b.price) || 0;
                return priceA - priceB;
            });
            break;

        case 'price-desc':
            sorted.sort((a, b) => {
                const priceA = parseFloat(a.price) || 0;
                const priceB = parseFloat(b.price) || 0;
                return priceB - priceA;
            });
            break;

        case 'name-asc':
            sorted.sort((a, b) => {
                const nameA = (a.name || '').toLowerCase();
                const nameB = (b.name || '').toLowerCase();
                return nameA.localeCompare(nameB, 'tr');
            });
            break;

        case 'name-desc':
            sorted.sort((a, b) => {
                const nameA = (a.name || '').toLowerCase();
                const nameB = (b.name || '').toLowerCase();
                return nameB.localeCompare(nameA, 'tr');
            });
            break;

        case 'default':
        default:
            // Sort by creation date (newest first) if available
            sorted.sort((a, b) => {
                const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
                const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
                return dateB - dateA; // Newest first
            });
            break;
    }

    return sorted;
}

// Create product card HTML
function createProductCard(product) {
    const stockInfo = getStockInfo(product);
    const stockClass = stockInfo.class;
    const stockText = stockInfo.text;

    return `
        <div class="product-card" data-sku="${product.sku}">
            <div class="product-image">📦</div>
            <div class="product-info">
                <h3 class="product-name">${escapeHtml(product.name)}</h3>
                <p class="product-description">${escapeHtml(product.description || '')}</p>
                <div class="stock-info ${stockClass}">${stockText}</div>
                <div class="product-price">${formatPrice(product.price, product.currency)}</div>
                <div class="product-actions" id="actions-${product.sku}">
                    ${createProductActions(product)}
                </div>
            </div>
        </div>
    `;
}

// Get stock information
function getStockInfo(product) {
    if (product.availableStock === undefined || product.availableStock === null) {
        return { class: '', text: '' };
    }

    if (product.availableStock === 0) {
        return { class: 'out-of-stock', text: 'Stokta yok' };
    }

    if (product.availableStock < 5) {
        return { class: 'low-stock', text: `Son ${product.availableStock} adet!` };
    }

    return { class: 'in-stock', text: `Stokta ${product.availableStock} adet` };
}

// Create product actions (add to cart button or quantity controls)
function createProductActions(product) {
    return `
        <button class="btn btn-primary btn-add-to-cart" onclick="addToCart('${product.sku}', 1)">
            Sepete Ekle
        </button>
    `;
}

// Add to cart
async function addToCart(productSku, quantity) {
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/';
        return;
    }

    try {
        const result = await API.addToCart(productSku, quantity);
        if (result) {
            showMessage('Ürün sepete eklendi!', 'success');
            updateCartBadge();
            // Update UI to show quantity controls if needed
        } else {
            showMessage('Ürün sepete eklenirken bir hata oluştu', 'error');
        }
    } catch (error) {
        console.error('Add to cart error:', error);
        
        if (error.message && error.message.includes('401')) {
            TokenManager.removeToken();
            window.location.href = '/';
            return;
        }

        showMessage('Ürün sepete eklenirken bir hata oluştu', 'error');
    }
}

// Show loading spinner
function showLoading(show) {
    const spinner = document.getElementById('loadingSpinner');
    const grid = document.getElementById('productsGrid');
    
    if (show) {
        spinner.style.display = 'block';
        grid.style.display = 'none';
    } else {
        spinner.style.display = 'none';
        grid.style.display = 'grid';
    }
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

