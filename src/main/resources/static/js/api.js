/**
 * API Client & Global Helpers for Hotel Reservation System
 */

const API_BASE = '/api';

const Auth = {
  TOKEN_KEY: 'hotel_auth_token',
  USER_KEY: 'hotel_user_data',

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  getUser() {
    const data = localStorage.getItem(this.USER_KEY);
    try {
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return null;
    }
  },

  setAuth(authResponse) {
    localStorage.setItem(this.TOKEN_KEY, authResponse.token);
    const user = {
      userId: authResponse.userId,
      email: authResponse.email,
      fullName: authResponse.fullName,
      role: authResponse.role
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  },

  clearAuth() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  },

  isLoggedIn() {
    return !!this.getToken();
  },

  isAdmin() {
    const user = this.getUser();
    return user && user.role === 'ROLE_ADMIN';
  }
};

/**
 * Global HTTP Fetch Wrapper
 */
async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {})
  };

  const token = Auth.getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers
  };

  try {
    const response = await fetch(url, config);

    // If 401 Unauthorized, notify and clear auth if session expired
    if (response.status === 401) {
      if (Auth.isLoggedIn()) {
        showToast('Your session has expired. Please log in again.', 'warning');
        Auth.clearAuth();
        setTimeout(() => {
          window.location.href = '/login.html';
        }, 1200);
      }
      const data = await response.json();
      throw new Error(data.message || 'Unauthorized');
    }

    const data = await response.json();

    if (!response.ok) {
      let errorMsg = data.message || `Request failed with status ${response.status}`;
      if (data.errors && typeof data.errors === 'object') {
        const errorList = Object.values(data.errors).join(', ');
        errorMsg += ` (${errorList})`;
      }
      throw new Error(errorMsg);
    }

    return data;
  } catch (error) {
    console.error(`API Error on [${options.method || 'GET'}] ${endpoint}:`, error);
    throw error;
  }
}

/**
 * Toast Notifications
 */
function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let icon = 'fa-info-circle';
  if (type === 'success') icon = 'fa-check-circle';
  if (type === 'error') icon = 'fa-exclamation-circle';
  if (type === 'warning') icon = 'fa-exclamation-triangle';

  toast.innerHTML = `
    <i class="fas ${icon}"></i>
    <div style="flex: 1; font-size: 0.92rem;">${message}</div>
    <span style="cursor: pointer; opacity: 0.7;" onclick="this.parentElement.remove()">&times;</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

/**
 * Formatting Helpers
 */
function formatCurrency(amount) {
  if (amount === null || amount === undefined) return '$0.00';
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    const d = new Date(parts[0], parts[1] - 1, parts[2]);
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }
  return dateStr;
}

/**
 * Global Nav Bar State Renderer
 */
function renderNavbarAuth() {
  const navAuthContainer = document.getElementById('nav-auth-container');
  if (!navAuthContainer) return;

  if (Auth.isLoggedIn()) {
    const user = Auth.getUser();
    const isAdmin = Auth.isAdmin();

    navAuthContainer.innerHTML = `
      <div style="display: flex; align-items: center; gap: 14px;">
        ${isAdmin ? '<a href="/admin.html" class="btn btn-outline btn-sm"><i class="fas fa-chart-line"></i> Admin Portal</a>' : ''}
        <a href="/my-bookings.html" class="btn btn-outline-gold btn-sm"><i class="fas fa-calendar-check"></i> My Bookings</a>
        <span style="font-size: 0.88rem; color: var(--text-muted);"><i class="fas fa-user-circle"></i> ${user?.fullName?.split(' ')[0] || 'Account'}</span>
        <button id="logout-btn" class="btn btn-sm btn-outline" style="border: none; color: var(--danger);"><i class="fas fa-sign-out-alt"></i></button>
      </div>
    `;

    document.getElementById('logout-btn')?.addEventListener('click', () => {
      Auth.clearAuth();
      showToast('Logged out successfully', 'success');
      setTimeout(() => {
        window.location.href = '/index.html';
      }, 500);
    });
  } else {
    navAuthContainer.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <a href="/login.html" class="btn btn-outline btn-sm">Sign In</a>
        <a href="/register.html" class="btn btn-gold btn-sm">Register</a>
      </div>
    `;
  }
}

// Modal Helpers
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('active');
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.remove('active');
}

document.addEventListener('DOMContentLoaded', () => {
  renderNavbarAuth();

  // Close modals on clicking overlay or close button
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        overlay.classList.remove('active');
      }
    });
  });

  document.querySelectorAll('.modal-close, [data-dismiss="modal"]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modal = btn.closest('.modal-overlay');
      if (modal) modal.classList.remove('active');
    });
  });
});
