/**
 * Administrator Portal Script (admin.js)
 */

let revenueChartInstance = null;
let bookingsChartInstance = null;
let currentAdminRooms = [];
let categoriesCache = [];
let amenitiesCache = [];

document.addEventListener('DOMContentLoaded', () => {
  if (!Auth.isLoggedIn() || !Auth.isAdmin()) {
    showToast('Administrator privileges required. Please sign in as an admin.', 'error');
    setTimeout(() => { window.location.href = '/login.html'; }, 1000);
    return;
  }

  initAdminNavigation();
  loadDashboardData();
  loadMetadataCache();
  loadRoomsTable();
  loadReservationsTable();

  // Wire forms
  document.getElementById('add-room-form')?.addEventListener('submit', handleAddRoom);
  document.getElementById('edit-room-form')?.addEventListener('submit', handleEditRoom);
  document.getElementById('admin-res-search')?.addEventListener('input', debounce(loadReservationsTable, 300));
  document.getElementById('admin-res-filter-status')?.addEventListener('change', loadReservationsTable);
  document.getElementById('export-csv-btn')?.addEventListener('click', handleExportCsv);
});

function initAdminNavigation() {
  const tabs = document.querySelectorAll('.admin-tab-btn');
  const sections = document.querySelectorAll('.admin-view-section');

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      sections.forEach(s => s.classList.add('hidden'));

      tab.classList.add('active');
      const targetId = tab.getAttribute('data-target');
      document.getElementById(targetId)?.classList.remove('hidden');
    });
  });
}

async function loadMetadataCache() {
  try {
    const [catRes, amenRes] = await Promise.all([
      apiRequest('/categories'),
      apiRequest('/categories/amenities')
    ]);
    if (catRes.success) categoriesCache = catRes.data;
    if (amenRes.success) amenitiesCache = amenRes.data;

    populateCategoryDropdowns();
    populateAmenityCheckboxes('add-room-amenities');
    populateAmenityCheckboxes('edit-room-amenities');
  } catch (e) {
    console.error('Metadata cache error:', e);
  }
}

function populateCategoryDropdowns() {
  const addCat = document.getElementById('add-room-category');
  const editCat = document.getElementById('edit-room-category');

  const options = categoriesCache.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
  if (addCat) addCat.innerHTML = options;
  if (editCat) editCat.innerHTML = options;
}

function populateAmenityCheckboxes(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  container.innerHTML = amenitiesCache.map(a => `
    <label style="display: inline-flex; align-items: center; gap: 6px; font-size: 0.85rem; margin-right: 12px; margin-bottom: 8px;">
      <input type="checkbox" value="${a.id}" class="amenity-checkbox"> ${a.name}
    </label>
  `).join('');
}

async function loadDashboardData() {
  try {
    const res = await apiRequest('/admin/dashboard/stats');
    if (res.success && res.data) {
      const stats = res.data;

      // Update KPI counters
      document.getElementById('kpi-total-rooms').textContent = stats.totalRooms;
      document.getElementById('kpi-available-rooms').textContent = stats.availableRooms;
      document.getElementById('kpi-occupied-rooms').textContent = stats.occupiedRooms;
      document.getElementById('kpi-total-reservations').textContent = stats.totalReservations;
      document.getElementById('kpi-confirmed-res').textContent = stats.confirmedReservations;
      document.getElementById('kpi-revenue').textContent = formatCurrency(stats.totalRevenue);

      renderCharts(stats.monthlyRevenue, stats.dailyReservations);
    }
  } catch (err) {
    showToast(`Error loading dashboard metrics: ${err.message}`, 'error');
  }
}

function renderCharts(monthlyRevenueData, dailyReservationsData) {
  // Chart 1: Monthly Revenue
  const revCtx = document.getElementById('monthlyRevenueChart')?.getContext('2d');
  if (revCtx && monthlyRevenueData && window.Chart) {
    if (revenueChartInstance) revenueChartInstance.destroy();

    revenueChartInstance = new Chart(revCtx, {
      type: 'bar',
      data: {
        labels: Object.keys(monthlyRevenueData),
        datasets: [{
          label: 'Revenue ($)',
          data: Object.values(monthlyRevenueData),
          backgroundColor: 'rgba(197, 168, 128, 0.85)',
          borderColor: '#c5a880',
          borderWidth: 1,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (val) => '$' + val
            }
          }
        }
      }
    });
  }

  // Chart 2: Daily Bookings
  const bookCtx = document.getElementById('dailyBookingsChart')?.getContext('2d');
  if (bookCtx && dailyReservationsData && window.Chart) {
    if (bookingsChartInstance) bookingsChartInstance.destroy();

    bookingsChartInstance = new Chart(bookCtx, {
      type: 'line',
      data: {
        labels: Object.keys(dailyReservationsData),
        datasets: [{
          label: 'Bookings Created',
          data: Object.values(dailyReservationsData),
          borderColor: '#1a2b49',
          backgroundColor: 'rgba(26, 43, 73, 0.1)',
          fill: true,
          tension: 0.3,
          pointRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1 }
          }
        }
      }
    });
  }
}

// Room Management
async function loadRoomsTable() {
  const tbody = document.getElementById('admin-rooms-tbody');
  if (!tbody) return;

  try {
    const res = await apiRequest('/admin/rooms');
    if (res.success && res.data) {
      currentAdminRooms = res.data;
      tbody.innerHTML = '';

      if (res.data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No rooms configured.</td></tr>`;
        return;
      }

      res.data.forEach(room => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td><strong>${room.roomNumber}</strong></td>
          <td>${room.categoryName}</td>
          <td>${formatCurrency(room.pricePerNight)}</td>
          <td>${room.maxOccupancy} Guests</td>
          <td>
            <label style="cursor: pointer; display: inline-flex; align-items: center; gap: 6px;">
              <input type="checkbox" ${room.active ? 'checked' : ''} onchange="toggleRoomActive(${room.id}, this.checked)">
              <span style="font-size: 0.85rem; color: ${room.active ? 'var(--success)' : 'var(--danger)'};">
                ${room.active ? 'Active' : 'Inactive'}
              </span>
            </label>
          </td>
          <td>
            <div style="display: flex; gap: 8px;">
              <button class="btn btn-outline btn-sm" onclick="openEditRoomModal(${room.id})"><i class="fas fa-edit"></i></button>
              <button class="btn btn-danger btn-sm" onclick="deleteRoom(${room.id})"><i class="fas fa-trash"></i></button>
            </div>
          </td>
        `;
        tbody.appendChild(tr);
      });
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" style="color: var(--danger); text-align: center;">${err.message}</td></tr>`;
  }
}

async function handleAddRoom(e) {
  e.preventDefault();
  const roomNumber = document.getElementById('add-room-number')?.value;
  const categoryId = document.getElementById('add-room-category')?.value;
  const pricePerNight = document.getElementById('add-room-price')?.value;
  const maxOccupancy = document.getElementById('add-room-occupancy')?.value;
  const description = document.getElementById('add-room-desc')?.value;
  const imageUrl = document.getElementById('add-room-image')?.value;

  const checkedAmenities = Array.from(document.querySelectorAll('#add-room-amenities .amenity-checkbox:checked'))
    .map(cb => parseInt(cb.value, 10));

  try {
    const payload = {
      roomNumber,
      categoryId: parseInt(categoryId, 10),
      pricePerNight: parseFloat(pricePerNight),
      maxOccupancy: parseInt(maxOccupancy, 10),
      active: true,
      description,
      imageUrl,
      amenityIds: checkedAmenities
    };

    const res = await apiRequest('/admin/rooms', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.success) {
      showToast('Room created successfully', 'success');
      closeModal('add-room-modal');
      document.getElementById('add-room-form')?.reset();
      loadRoomsTable();
      loadDashboardData();
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openEditRoomModal(roomId) {
  const room = currentAdminRooms.find(r => r.id === roomId);
  if (!room) return;

  document.getElementById('edit-room-id').value = room.id;
  document.getElementById('edit-room-number').value = room.roomNumber;
  document.getElementById('edit-room-category').value = room.categoryId;
  document.getElementById('edit-room-price').value = room.pricePerNight;
  document.getElementById('edit-room-occupancy').value = room.maxOccupancy;
  document.getElementById('edit-room-desc').value = room.description || '';
  document.getElementById('edit-room-image').value = room.imageUrl || '';

  // Match amenities
  document.querySelectorAll('#edit-room-amenities .amenity-checkbox').forEach(cb => {
    const amenName = cb.parentElement.textContent.trim();
    cb.checked = (room.amenities || []).includes(amenName);
  });

  openModal('edit-room-modal');
}

async function handleEditRoom(e) {
  e.preventDefault();
  const id = document.getElementById('edit-room-id')?.value;
  const roomNumber = document.getElementById('edit-room-number')?.value;
  const categoryId = document.getElementById('edit-room-category')?.value;
  const pricePerNight = document.getElementById('edit-room-price')?.value;
  const maxOccupancy = document.getElementById('edit-room-occupancy')?.value;
  const description = document.getElementById('edit-room-desc')?.value;
  const imageUrl = document.getElementById('edit-room-image')?.value;

  const checkedAmenities = Array.from(document.querySelectorAll('#edit-room-amenities .amenity-checkbox:checked'))
    .map(cb => parseInt(cb.value, 10));

  try {
    const payload = {
      roomNumber,
      categoryId: parseInt(categoryId, 10),
      pricePerNight: parseFloat(pricePerNight),
      maxOccupancy: parseInt(maxOccupancy, 10),
      active: true,
      description,
      imageUrl,
      amenityIds: checkedAmenities
    };

    const res = await apiRequest(`/admin/rooms/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });

    if (res.success) {
      showToast('Room updated successfully', 'success');
      closeModal('edit-room-modal');
      loadRoomsTable();
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function toggleRoomActive(id, active) {
  try {
    await apiRequest(`/admin/rooms/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active })
    });
    showToast(`Room is now ${active ? 'Active' : 'Inactive'}`, 'info');
    loadRoomsTable();
    loadDashboardData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteRoom(id) {
  if (!confirm('Are you sure you want to delete this room? This cannot be undone.')) return;

  try {
    await apiRequest(`/admin/rooms/${id}`, { method: 'DELETE' });
    showToast('Room deleted successfully', 'success');
    loadRoomsTable();
    loadDashboardData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Reservation Management
async function loadReservationsTable() {
  const tbody = document.getElementById('admin-res-tbody');
  if (!tbody) return;

  const search = document.getElementById('admin-res-search')?.value;
  const status = document.getElementById('admin-res-filter-status')?.value;

  const params = new URLSearchParams();
  if (search) params.append('search', search);
  if (status) params.append('status', status);

  try {
    const res = await apiRequest(`/admin/reservations?${params.toString()}`);
    if (res.success && res.data) {
      tbody.innerHTML = '';

      if (res.data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No reservations found.</td></tr>`;
        return;
      }

      res.data.forEach(r => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td><strong>${r.bookingReference}</strong></td>
          <td>${r.guestName}<br><span style="font-size: 0.8rem; color: var(--text-muted);">${r.guestEmail}</span></td>
          <td>Room ${r.room?.roomNumber} (${r.room?.categoryName})</td>
          <td>${formatDate(r.checkInDate)} - ${formatDate(r.checkOutDate)}<br><span style="font-size: 0.8rem; color: var(--text-muted);">${r.nights} nights</span></td>
          <td><strong>${formatCurrency(r.totalAmount)}</strong></td>
          <td><span class="badge badge-${r.status.toLowerCase()}">${r.status}</span></td>
          <td>
            <select class="form-control" style="padding: 4px 8px; font-size: 0.85rem;" onchange="updateReservationStatus(${r.id}, this.value)">
              <option value="" disabled selected>Update Status</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="CHECKED_IN">CHECKED_IN</option>
              <option value="CHECKED_OUT">CHECKED_OUT</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </td>
        `;
        tbody.appendChild(tr);
      });
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="color: var(--danger); text-align: center;">${err.message}</td></tr>`;
  }
}

async function updateReservationStatus(id, newStatus) {
  if (!newStatus) return;

  try {
    const res = await apiRequest(`/admin/reservations/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status: newStatus, reason: 'Updated via Admin Console' })
    });

    if (res.success) {
      showToast(`Status updated to ${newStatus}`, 'success');
      loadReservationsTable();
      loadDashboardData();
    }
  } catch (err) {
    showToast(err.message, 'error');
    loadReservationsTable();
  }
}

async function handleExportCsv() {
  try {
    const token = Auth.getToken();
    const res = await fetch('/api/admin/reports/reservations/export-csv', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (!res.ok) throw new Error('Failed to download report');

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `hotel_reservations_${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    showToast('Reservation report exported to CSV successfully', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}
