/**
 * Room Catalog & Availability Search Script (rooms.js)
 */

let currentRoomsData = [];

document.addEventListener('DOMContentLoaded', () => {
  initFilterControls();
  loadCategories();
  applyInitialUrlParams();
  searchAvailableRooms();
});

function initFilterControls() {
  const checkInInput = document.getElementById('filter-checkin');
  const checkOutInput = document.getElementById('filter-checkout');
  const guestsInput = document.getElementById('filter-guests');
  const categorySelect = document.getElementById('filter-category');
  const priceSlider = document.getElementById('filter-price');
  const priceDisplay = document.getElementById('price-display');
  const filterForm = document.getElementById('rooms-filter-form');

  const today = new Date();
  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 1);

  const formatDateYMD = (d) => d.toISOString().split('T')[0];

  if (checkInInput && checkOutInput) {
    checkInInput.min = formatDateYMD(today);
    checkOutInput.min = formatDateYMD(tomorrow);

    checkInInput.addEventListener('change', () => {
      const inDate = new Date(checkInInput.value);
      const nextDay = new Date(inDate);
      nextDay.setDate(inDate.getDate() + 1);
      checkOutInput.min = formatDateYMD(nextDay);
      if (new Date(checkOutInput.value) <= inDate) {
        checkOutInput.value = formatDateYMD(nextDay);
      }
    });
  }

  if (priceSlider && priceDisplay) {
    priceSlider.addEventListener('input', () => {
      priceDisplay.textContent = `$${priceSlider.value}`;
    });
  }

  if (filterForm) {
    filterForm.addEventListener('submit', (e) => {
      e.preventDefault();
      searchAvailableRooms();
    });
  }

  document.getElementById('reset-filters-btn')?.addEventListener('click', () => {
    checkInInput.value = formatDateYMD(today);
    checkOutInput.value = formatDateYMD(tomorrow);
    if (guestsInput) guestsInput.value = '1';
    if (categorySelect) categorySelect.value = '';
    if (priceSlider) {
      priceSlider.value = '500';
      if (priceDisplay) priceDisplay.textContent = '$500';
    }
    searchAvailableRooms();
  });
}

function applyInitialUrlParams() {
  const params = new URLSearchParams(window.location.search);
  const checkIn = params.get('checkIn');
  const checkOut = params.get('checkOut');
  const guests = params.get('guests');
  const cat = params.get('categoryId');

  const today = new Date();
  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 1);
  const formatDateYMD = (d) => d.toISOString().split('T')[0];

  const checkInInput = document.getElementById('filter-checkin');
  const checkOutInput = document.getElementById('filter-checkout');
  const guestsInput = document.getElementById('filter-guests');
  const categorySelect = document.getElementById('filter-category');

  if (checkInInput) checkInInput.value = checkIn || formatDateYMD(today);
  if (checkOutInput) checkOutInput.value = checkOut || formatDateYMD(tomorrow);
  if (guestsInput && guests) guestsInput.value = guests;
  if (categorySelect && cat) categorySelect.value = cat;
}

async function loadCategories() {
  const select = document.getElementById('filter-category');
  if (!select) return;

  try {
    const res = await apiRequest('/categories');
    if (res.success && res.data) {
      const currentVal = select.value;
      select.innerHTML = '<option value="">All Categories</option>';
      res.data.forEach(c => {
        select.innerHTML += `<option value="${c.id}">${c.name}</option>`;
      });
      if (currentVal) select.value = currentVal;
    }
  } catch (err) {
    console.error('Failed to load categories:', err);
  }
}

async function searchAvailableRooms() {
  const container = document.getElementById('rooms-list-container');
  const resultsCount = document.getElementById('results-count');
  if (!container) return;

  const checkIn = document.getElementById('filter-checkin')?.value;
  const checkOut = document.getElementById('filter-checkout')?.value;
  const guests = document.getElementById('filter-guests')?.value || 1;
  const categoryId = document.getElementById('filter-category')?.value;
  const maxPrice = document.getElementById('filter-price')?.value;

  container.innerHTML = `
    <div style="grid-column: 1 / -1; text-align: center; padding: 50px 0;">
      <i class="fas fa-spinner fa-spin" style="font-size: 2.2rem; color: var(--gold);"></i>
      <p style="margin-top: 14px; color: var(--text-muted);">Verifying live room availability...</p>
    </div>
  `;

  try {
    const params = new URLSearchParams();
    if (checkIn) params.append('checkInDate', checkIn);
    if (checkOut) params.append('checkOutDate', checkOut);
    if (guests) params.append('guests', guests);
    if (categoryId) params.append('categoryId', categoryId);
    if (maxPrice && maxPrice < 500) params.append('maxPrice', maxPrice);

    const res = await apiRequest(`/rooms/search?${params.toString()}`);

    if (res.success && res.data) {
      currentRoomsData = res.data;
      if (resultsCount) resultsCount.textContent = `${res.data.length} rooms available`;

      if (res.data.length === 0) {
        container.innerHTML = `
          <div class="empty-state" style="grid-column: 1 / -1;">
            <i class="fas fa-calendar-times"></i>
            <h3>No Rooms Available For Selected Dates</h3>
            <p>All rooms matching your criteria are booked for ${formatDate(checkIn)} to ${formatDate(checkOut)}. Please adjust your dates or guest capacity.</p>
          </div>
        `;
        return;
      }

      container.innerHTML = '';
      res.data.forEach(room => {
        const amenitiesHtml = (room.amenities || [])
          .slice(0, 3)
          .map(a => `<span class="amenity-tag">${a}</span>`)
          .join('');

        const card = document.createElement('div');
        card.className = 'room-card';
        card.innerHTML = `
          <div class="room-card-img">
            <img src="${room.imageUrl || 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80'}" alt="Room ${room.roomNumber}">
            <div class="room-category-badge">${room.categoryName}</div>
            <div class="room-price-tag">${formatCurrency(room.pricePerNight)}<span>/night</span></div>
          </div>
          <div class="room-card-body">
            <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 6px;">
              <h3 class="room-card-title">Room ${room.roomNumber}</h3>
              <span style="font-size: 0.85rem; color: var(--gold-hover); font-weight: 600;">${room.categoryName}</span>
            </div>
            <p class="room-card-desc">${room.description || 'Luxury accommodation with modern hospitality appointments.'}</p>
            <div class="room-meta">
              <span><i class="fas fa-users"></i> Max ${room.maxOccupancy} Guests</span>
              <span><i class="fas fa-check-circle" style="color: var(--success);"></i> Available</span>
            </div>
            <div class="room-amenity-tags">
              ${amenitiesHtml}
            </div>
            <div style="display: flex; gap: 10px; margin-top: auto;">
              <button class="btn btn-outline btn-sm" onclick="viewRoomDetails(${room.id})" style="flex: 1;">
                <i class="fas fa-info-circle"></i> Details
              </button>
              <a href="/booking.html?roomId=${room.id}&checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}" class="btn btn-gold btn-sm" style="flex: 1;">
                Book Now <i class="fas fa-arrow-right"></i>
              </a>
            </div>
          </div>
        `;
        container.appendChild(card);
      });
    }
  } catch (error) {
    container.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <i class="fas fa-exclamation-triangle"></i>
        <h3>Search Error</h3>
        <p>${error.message}</p>
      </div>
    `;
  }
}

function viewRoomDetails(roomId) {
  const room = currentRoomsData.find(r => r.id === roomId);
  if (!room) return;

  const modalImg = document.getElementById('modal-room-img');
  const modalTitle = document.getElementById('modal-room-title');
  const modalCategory = document.getElementById('modal-room-category');
  const modalPrice = document.getElementById('modal-room-price');
  const modalOccupancy = document.getElementById('modal-room-occupancy');
  const modalDesc = document.getElementById('modal-room-desc');
  const modalAmenities = document.getElementById('modal-room-amenities');
  const modalBookBtn = document.getElementById('modal-book-btn');

  const checkIn = document.getElementById('filter-checkin')?.value || '';
  const checkOut = document.getElementById('filter-checkout')?.value || '';
  const guests = document.getElementById('filter-guests')?.value || '1';

  if (modalImg) modalImg.src = room.imageUrl || 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80';
  if (modalTitle) modalTitle.textContent = `Room ${room.roomNumber}`;
  if (modalCategory) modalCategory.textContent = room.categoryName;
  if (modalPrice) modalPrice.textContent = `${formatCurrency(room.pricePerNight)} / night`;
  if (modalOccupancy) modalOccupancy.textContent = `Up to ${room.maxOccupancy} Guests`;
  if (modalDesc) modalDesc.textContent = room.description || 'Full-service luxury suite equipped with bespoke amenities and refined comfort.';

  if (modalAmenities) {
    modalAmenities.innerHTML = (room.amenities || []).map(a => `
      <li style="display: flex; align-items: center; gap: 8px; font-size: 0.9rem; margin-bottom: 6px;">
        <i class="fas fa-check" style="color: var(--gold);"></i> ${a}
      </li>
    `).join('');
  }

  if (modalBookBtn) {
    modalBookBtn.href = `/booking.html?roomId=${room.id}&checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}`;
  }

  openModal('room-detail-modal');
}
