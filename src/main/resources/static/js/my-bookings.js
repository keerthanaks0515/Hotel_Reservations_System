/**
 * Customer Booking History Script (my-bookings.js)
 */

let userBookings = [];
let currentFilter = 'ALL';
let cancelTargetId = null;

document.addEventListener('DOMContentLoaded', () => {
  if (!Auth.isLoggedIn()) {
    showToast('Please sign in to view your reservations.', 'info');
    setTimeout(() => { window.location.href = '/login.html'; }, 800);
    return;
  }

  initTabs();
  loadUserBookings();

  document.getElementById('confirm-cancel-btn')?.addEventListener('click', executeCancellation);
});

function initTabs() {
  const tabs = document.querySelectorAll('.filter-tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentFilter = tab.getAttribute('data-status');
      renderBookings();
    });
  });
}

async function loadUserBookings() {
  const container = document.getElementById('bookings-container');
  if (!container) return;

  container.innerHTML = `
    <div style="text-align: center; padding: 60px 0;">
      <i class="fas fa-spinner fa-spin" style="font-size: 2.2rem; color: var(--gold);"></i>
      <p style="margin-top: 14px; color: var(--text-muted);">Fetching your reservation history...</p>
    </div>
  `;

  try {
    const res = await apiRequest('/reservations/my');
    if (res.success && res.data) {
      userBookings = res.data;
      renderBookings();
    }
  } catch (err) {
    container.innerHTML = `
      <div class="empty-state">
        <i class="fas fa-exclamation-triangle"></i>
        <h3>Failed to load bookings</h3>
        <p>${err.message}</p>
      </div>
    `;
  }
}

function renderBookings() {
  const container = document.getElementById('bookings-container');
  if (!container) return;

  let filtered = userBookings;
  if (currentFilter === 'UPCOMING') {
    filtered = userBookings.filter(b => b.status === 'CONFIRMED' || b.status === 'PENDING');
  } else if (currentFilter === 'COMPLETED') {
    filtered = userBookings.filter(b => b.status === 'CHECKED_IN' || b.status === 'CHECKED_OUT');
  } else if (currentFilter === 'CANCELLED') {
    filtered = userBookings.filter(b => b.status === 'CANCELLED');
  }

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <i class="fas fa-calendar-times"></i>
        <h3>No reservations found</h3>
        <p>You have no reservations matching this filter.</p>
        <a href="/rooms.html" class="btn btn-gold btn-sm" style="margin-top: 16px;">Browse Available Rooms</a>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  filtered.forEach(booking => {
    const statusClass = `badge-${booking.status.toLowerCase()}`;
    const isCancellable = (booking.status === 'CONFIRMED' || booking.status === 'PENDING');

    const card = document.createElement('div');
    card.className = 'booking-item-card';
    card.style = `
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 24px;
      margin-bottom: 20px;
      box-shadow: var(--shadow-sm);
      display: grid;
      grid-template-columns: auto 1fr auto;
      gap: 24px;
      align-items: center;
    `;

    card.innerHTML = `
      <img src="${booking.room?.imageUrl || 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80'}"
           alt="Room ${booking.room?.roomNumber}"
           style="width: 140px; height: 110px; object-fit: cover; border-radius: var(--radius-sm);">

      <div>
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 6px;">
          <h3 style="font-size: 1.25rem;">Room ${booking.room?.roomNumber} - ${booking.room?.categoryName}</h3>
          <span class="badge ${statusClass}">${booking.status}</span>
        </div>
        <div style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 8px;">
          <span style="font-weight: 600; color: var(--primary);">Reference:</span> ${booking.bookingReference} &bull;
          <span style="font-weight: 600; color: var(--primary);">Reserved On:</span> ${formatDate(booking.createdAt?.split('T')[0])}
        </div>
        <div style="display: flex; gap: 20px; font-size: 0.88rem; color: var(--text-main);">
          <span><i class="fas fa-calendar-alt" style="color: var(--gold);"></i> <strong>Check-in:</strong> ${formatDate(booking.checkInDate)}</span>
          <span><i class="fas fa-calendar-check" style="color: var(--gold);"></i> <strong>Check-out:</strong> ${formatDate(booking.checkOutDate)}</span>
          <span><i class="fas fa-moon" style="color: var(--gold);"></i> ${booking.nights} night(s)</span>
          <span><i class="fas fa-users" style="color: var(--gold);"></i> ${booking.numberOfGuests} guests</span>
        </div>
        ${booking.cancellationReason ? `<p style="font-size: 0.85rem; color: var(--danger); margin-top: 8px;"><i class="fas fa-ban"></i> Reason: ${booking.cancellationReason}</p>` : ''}
      </div>

      <div style="text-align: right; display: flex; flex-direction: column; gap: 10px; justify-content: center;">
        <div style="font-size: 1.35rem; font-weight: 700; color: var(--primary);">
          ${formatCurrency(booking.totalAmount)}
        </div>
        <div style="display: flex; gap: 8px;">
          <button class="btn btn-outline btn-sm" onclick="viewVoucher('${booking.bookingReference}')">
            <i class="fas fa-ticket-alt"></i> Details
          </button>
          ${isCancellable ? `
            <button class="btn btn-danger btn-sm" onclick="promptCancel(${booking.id}, '${booking.bookingReference}')">
              Cancel
            </button>
          ` : ''}
        </div>
      </div>
    `;

    container.appendChild(card);
  });
}

function promptCancel(reservationId, bookingReference) {
  cancelTargetId = reservationId;
  const refDisplay = document.getElementById('cancel-modal-ref');
  if (refDisplay) refDisplay.textContent = bookingReference;
  document.getElementById('cancellation-reason').value = '';
  openModal('cancel-confirm-modal');
}

async function executeCancellation() {
  if (!cancelTargetId) return;

  const reason = document.getElementById('cancellation-reason')?.value || 'Guest requested cancellation';
  const confirmBtn = document.getElementById('confirm-cancel-btn');

  if (confirmBtn) {
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cancelling...';
  }

  try {
    const res = await apiRequest(`/reservations/${cancelTargetId}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });

    if (res.success) {
      showToast('Reservation cancelled successfully.', 'success');
      closeModal('cancel-confirm-modal');
      await loadUserBookings();
    }
  } catch (error) {
    showToast(error.message, 'error');
  } finally {
    if (confirmBtn) {
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = 'Confirm Cancellation';
    }
  }
}

async function viewVoucher(bookingReference) {
  try {
    const res = await apiRequest(`/reservations/lookup/${bookingReference}`);
    if (res.success && res.data) {
      const b = res.data;
      const modalBody = document.getElementById('voucher-modal-content');
      if (modalBody) {
        modalBody.innerHTML = `
          <div style="text-align: center; border-bottom: 2px dashed var(--border-color); padding-bottom: 16px; margin-bottom: 16px;">
            <i class="fas fa-hotel" style="font-size: 2.2rem; color: var(--gold);"></i>
            <h2 style="margin: 8px 0 4px;">Grand Heritage Hotel</h2>
            <p style="font-size: 0.85rem; color: var(--text-muted);">Official Reservation Voucher</p>
            <div style="margin-top: 10px; display: inline-block; background: var(--gold-light); padding: 4px 14px; border-radius: 4px; font-weight: 700; color: var(--primary);">
              BOOKING REF: ${b.bookingReference}
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px; font-size: 0.9rem; margin-bottom: 16px;">
            <div><strong>Guest Name:</strong> ${b.guestName}</div>
            <div><strong>Contact Email:</strong> ${b.guestEmail}</div>
            <div><strong>Room Number:</strong> ${b.room?.roomNumber} (${b.room?.categoryName})</div>
            <div><strong>Total Guests:</strong> ${b.numberOfGuests}</div>
            <div><strong>Check-In Date:</strong> ${formatDate(b.checkInDate)}</div>
            <div><strong>Check-Out Date:</strong> ${formatDate(b.checkOutDate)}</div>
            <div><strong>Nights:</strong> ${b.nights} night(s)</div>
            <div><strong>Booking Status:</strong> <span class="badge badge-${b.status.toLowerCase()}">${b.status}</span></div>
          </div>

          <div style="background: var(--bg-main); padding: 14px; border-radius: var(--radius-sm); margin-bottom: 16px; font-size: 0.9rem;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
              <span>Room Rate (${b.nights} nights @ ${formatCurrency(b.roomRate)}):</span>
              <span>${formatCurrency(b.subtotal)}</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
              <span>Taxes & Fees (12%):</span>
              <span>${formatCurrency(b.taxAmount)}</span>
            </div>
            <div style="display: flex; justify-content: space-between; font-weight: 700; font-size: 1.05rem; border-top: 1px solid var(--border-color); padding-top: 6px;">
              <span>Total Amount:</span>
              <span style="color: var(--primary);">${formatCurrency(b.totalAmount)}</span>
            </div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 6px;">
              Payment Reference: ${b.paymentReference || 'N/A'} (${b.paymentStatus || 'PENDING'})
            </div>
          </div>

          <div style="text-align: center;">
            <button class="btn btn-outline btn-sm" onclick="window.print()">
              <i class="fas fa-print"></i> Print Voucher
            </button>
          </div>
        `;
        openModal('voucher-modal');
      }
    }
  } catch (err) {
    showToast(`Could not load reservation: ${err.message}`, 'error');
  }
}
