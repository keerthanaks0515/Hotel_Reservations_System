/**
 * Home Page Script (main.js)
 */

document.addEventListener('DOMContentLoaded', () => {
  initSearchForm();
  loadFeaturedRooms();
  loadCategoriesDropdown();
});

function initSearchForm() {
  const checkInInput = document.getElementById('search-checkin');
  const checkOutInput = document.getElementById('search-checkout');
  const guestsInput = document.getElementById('search-guests');
  const categorySelect = document.getElementById('search-category');
  const searchForm = document.getElementById('home-search-form');

  if (!checkInInput || !checkOutInput) return;

  // Set default dates: check-in = today, check-out = tomorrow
  const today = new Date();
  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 1);

  const formatDateYMD = (d) => d.toISOString().split('T')[0];

  checkInInput.min = formatDateYMD(today);
  checkInInput.value = formatDateYMD(today);

  checkOutInput.min = formatDateYMD(tomorrow);
  checkOutInput.value = formatDateYMD(tomorrow);

  // Update check-out min when check-in changes
  checkInInput.addEventListener('change', () => {
    const inDate = new Date(checkInInput.value);
    const nextDay = new Date(inDate);
    nextDay.setDate(inDate.getDate() + 1);
    checkOutInput.min = formatDateYMD(nextDay);
    if (new Date(checkOutInput.value) <= inDate) {
      checkOutInput.value = formatDateYMD(nextDay);
    }
  });

  if (searchForm) {
    searchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const inVal = checkInInput.value;
      const outVal = checkOutInput.value;
      const guestsVal = guestsInput ? guestsInput.value : 1;
      const catVal = categorySelect ? categorySelect.value : '';

      const queryParams = new URLSearchParams({
        checkIn: inVal,
        checkOut: outVal,
        guests: guestsVal
      });

      if (catVal) {
        queryParams.append('categoryId', catVal);
      }

      window.location.href = `/rooms.html?${queryParams.toString()}`;
    });
  }
}

async function loadCategoriesDropdown() {
  const categorySelect = document.getElementById('search-category');
  if (!categorySelect) return;

  try {
    const res = await apiRequest('/categories');
    if (res.success && res.data) {
      categorySelect.innerHTML = '<option value="">All Room Types</option>';
      res.data.forEach(cat => {
        categorySelect.innerHTML += `<option value="${cat.id}">${cat.name}</option>`;
      });
    }
  } catch (e) {
    console.error('Could not load categories:', e);
  }
}

async function loadFeaturedRooms() {
  const container = document.getElementById('featured-rooms-container');
  if (!container) return;

  try {
    const res = await apiRequest('/rooms/featured');
    if (res.success && res.data && res.data.length > 0) {
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
            <h3 class="room-card-title">Room ${room.roomNumber} - ${room.categoryName}</h3>
            <p class="room-card-desc">${room.description || 'Luxury accommodation with premium amenities and bespoke interior design.'}</p>
            <div class="room-meta">
              <span><i class="fas fa-users"></i> Up to ${room.maxOccupancy} Guests</span>
              <span><i class="fas fa-bed"></i> Premium Bedding</span>
            </div>
            <div class="room-amenity-tags">
              ${amenitiesHtml}
            </div>
            <a href="/booking.html?roomId=${room.id}" class="btn btn-primary btn-block">
              Reserve Room <i class="fas fa-arrow-right"></i>
            </a>
          </div>
        `;
        container.appendChild(card);
      });
    } else {
      container.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
          <i class="fas fa-bed"></i>
          <h3>No rooms featured currently</h3>
          <p>Please browse our full catalog.</p>
        </div>
      `;
    }
  } catch (error) {
    console.error('Error loading featured rooms:', error);
    container.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <i class="fas fa-exclamation-triangle"></i>
        <h3>Unable to load rooms</h3>
        <p>Please ensure the backend server is running.</p>
      </div>
    `;
  }
}
