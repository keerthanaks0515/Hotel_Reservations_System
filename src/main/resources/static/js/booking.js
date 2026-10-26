/**
 * Grand Heritage - Premium Hotel Checkout & Payment Flow (booking.js)
 * 
 * Modular implementation providing:
 * - Real-time price and tax estimation
 * - Dynamic 2-column luxury checkout layout
 * - Interactive payment methods (Credit Card, UPI/QR, Pay at Hotel)
 * - Live virtual credit card preview and auto-formatting
 * - Comprehensive client-side validation
 * - Simulated bank processing animation and outcome modals
 * - Discrete Evaluator/Demo controls for testing success and decline scenarios
 */

// Global State
let selectedRoom = null;
let currentCreatedReservation = null;
let selectedPaymentMethod = 'SIMULATED_CARD'; // 'SIMULATED_CARD' | 'SIMULATED_UPI' | 'PAY_AT_CHECKIN'
let selectedUpiMode = 'id'; // 'id' | 'qr'
let lastPaymentResponse = null;

document.addEventListener('DOMContentLoaded', () => {
  initBookingPage();
});

/**
 * 1. Page Initialization
 */
async function initBookingPage() {
  const urlParams = new URLSearchParams(window.location.search);
  const roomId = urlParams.get('roomId');
  const checkIn = urlParams.get('checkIn');
  const checkOut = urlParams.get('checkOut');
  const guests = urlParams.get('guests');

  if (!roomId) {
    showToast('No room selected. Redirecting to room catalog.', 'warning');
    setTimeout(() => { window.location.href = '/rooms.html'; }, 1500);
    return;
  }

  // Pre-fill user profile if logged in
  if (Auth.isLoggedIn()) {
    const user = Auth.getUser();
    if (user) {
      const nameInput = document.getElementById('guest-name');
      const emailInput = document.getElementById('guest-email');
      if (nameInput && !nameInput.value) nameInput.value = user.fullName || '';
      if (emailInput && !emailInput.value) emailInput.value = user.email || '';
    }
  }

  // Set default dates if not provided: Check-in = Today, Check-out = Tomorrow
  const today = new Date();
  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 1);
  const formatDateYMD = (d) => d.toISOString().split('T')[0];

  const checkInInput = document.getElementById('book-checkin');
  const checkOutInput = document.getElementById('book-checkout');
  const guestsInput = document.getElementById('book-guests');

  if (checkInInput) checkInInput.value = checkIn || formatDateYMD(today);
  if (checkOutInput) checkOutInput.value = checkOut || formatDateYMD(tomorrow);
  if (guestsInput && guests) guestsInput.value = guests;

  // Listeners for live stay calculations
  checkInInput?.addEventListener('change', () => {
    const inDate = new Date(checkInInput.value);
    const nextDay = new Date(inDate);
    nextDay.setDate(inDate.getDate() + 1);
    checkOutInput.min = formatDateYMD(nextDay);
    if (new Date(checkOutInput.value) <= inDate) {
      checkOutInput.value = formatDateYMD(nextDay);
    }
    updatePriceSummary();
  });
  checkOutInput?.addEventListener('change', updatePriceSummary);
  guestsInput?.addEventListener('change', updatePriceSummary);

  // Load Room Data
  await loadRoomDetails(roomId);

  // Initialize Card Inputs & Live Formatter
  initCardInputFormatters();

  // Wire booking submission form
  document.getElementById('booking-form')?.addEventListener('submit', handleBookingSubmit);

  // Wire payment submission form
  document.getElementById('simulated-payment-form')?.addEventListener('submit', handlePaymentSubmit);
}

/**
 * 2. Room Details & Price Calculations
 */
async function loadRoomDetails(roomId) {
  try {
    const res = await apiRequest(`/rooms/${roomId}`);
    if (res.success && res.data) {
      selectedRoom = res.data;
      renderRoomSummary(selectedRoom);
      updatePriceSummary();
    }
  } catch (err) {
    showToast(`Error loading room: ${err.message}`, 'error');
  }
}

function renderRoomSummary(room) {
  const imgEl = document.getElementById('summary-room-img');
  const titleEl = document.getElementById('summary-room-title');
  const catEl = document.getElementById('summary-room-category');
  const rateEl = document.getElementById('summary-room-rate');
  const guestsInput = document.getElementById('book-guests');

  if (imgEl) imgEl.src = room.imageUrl || 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80';
  if (titleEl) titleEl.textContent = `Room ${room.roomNumber}`;
  if (catEl) catEl.textContent = room.categoryName;
  if (rateEl) rateEl.textContent = `${formatCurrency(room.pricePerNight)} / night`;

  if (guestsInput) {
    guestsInput.max = room.maxOccupancy;
    if (parseInt(guestsInput.value, 10) > room.maxOccupancy) {
      guestsInput.value = room.maxOccupancy;
    }
  }
}

function updatePriceSummary() {
  if (!selectedRoom) return;

  const inVal = document.getElementById('book-checkin')?.value;
  const outVal = document.getElementById('book-checkout')?.value;

  if (!inVal || !outVal) return;

  const inDate = new Date(inVal);
  const outDate = new Date(outVal);

  const diffTime = outDate.getTime() - inDate.getTime();
  const nights = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  const nightsDisplay = document.getElementById('summary-nights');
  const subtotalDisplay = document.getElementById('summary-subtotal');
  const taxDisplay = document.getElementById('summary-tax');
  const totalDisplay = document.getElementById('summary-total');

  if (nights <= 0) {
    if (nightsDisplay) nightsDisplay.textContent = 'Invalid dates';
    if (subtotalDisplay) subtotalDisplay.textContent = '$0.00';
    if (taxDisplay) taxDisplay.textContent = '$0.00';
    if (totalDisplay) totalDisplay.textContent = '$0.00';
    return;
  }

  const subtotal = selectedRoom.pricePerNight * nights;
  const tax = subtotal * 0.12; // 12% Hotel Tax
  const total = subtotal + tax;

  if (nightsDisplay) nightsDisplay.textContent = `${nights} night${nights > 1 ? 's' : ''}`;
  if (subtotalDisplay) subtotalDisplay.textContent = formatCurrency(subtotal);
  if (taxDisplay) taxDisplay.textContent = formatCurrency(tax);
  if (totalDisplay) totalDisplay.textContent = formatCurrency(total);
}

/**
 * 3. Stepper Progress Visual Updater
 */
function updateStepper(step) {
  const steps = ['room', 'guest', 'review', 'payment', 'confirm'];
  const stepElements = {
    room: document.getElementById('step-nav-room'),
    guest: document.getElementById('step-nav-guest'),
    review: document.getElementById('step-nav-review'),
    payment: document.getElementById('step-nav-payment'),
    confirm: document.getElementById('step-nav-confirm')
  };

  const dividers = {
    rg: document.getElementById('div-room-guest'),
    gr: document.getElementById('div-guest-review'),
    rp: document.getElementById('div-review-payment'),
    pc: document.getElementById('div-payment-confirm')
  };

  if (step === 1) {
    // Guest Details active
    stepElements.room?.classList.add('completed');
    stepElements.guest?.classList.add('active');
    stepElements.guest?.classList.remove('completed');
    stepElements.review?.classList.remove('active', 'completed');
    stepElements.payment?.classList.remove('active', 'completed');
    stepElements.confirm?.classList.remove('active', 'completed');
    if (dividers.rg) dividers.rg.classList.add('completed');
    if (dividers.gr) dividers.gr.classList.remove('completed');
    if (dividers.rp) dividers.rp.classList.remove('completed');
    if (dividers.pc) dividers.pc.classList.remove('completed');
  } else if (step === 2) {
    // Payment active (Review & Details completed)
    stepElements.room?.classList.add('completed');
    stepElements.guest?.classList.add('completed');
    stepElements.review?.classList.add('completed');
    stepElements.payment?.classList.add('active');
    stepElements.payment?.classList.remove('completed');
    stepElements.confirm?.classList.remove('active', 'completed');
    if (dividers.rg) dividers.rg.classList.add('completed');
    if (dividers.gr) dividers.gr.classList.add('completed');
    if (dividers.rp) dividers.rp.classList.add('completed');
    if (dividers.pc) dividers.pc.classList.remove('completed');
  } else if (step === 3) {
    // Confirmation all completed
    steps.forEach(s => {
      stepElements[s]?.classList.remove('active');
      stepElements[s]?.classList.add('completed');
    });
    Object.values(dividers).forEach(d => d?.classList.add('completed'));
  }
}

/**
 * 4. Step 1: Submit Guest Details and Hold Reservation
 */
async function handleBookingSubmit(e) {
  e.preventDefault();

  if (!Auth.isLoggedIn()) {
    showToast('Please sign in to confirm your reservation.', 'warning');
    openModal('login-required-modal');
    return;
  }

  if (!selectedRoom) return;

  const checkIn = document.getElementById('book-checkin')?.value;
  const checkOut = document.getElementById('book-checkout')?.value;
  const guests = parseInt(document.getElementById('book-guests')?.value || '1', 10);
  const guestName = document.getElementById('guest-name')?.value.trim();
  const guestEmail = document.getElementById('guest-email')?.value.trim();
  const guestPhone = document.getElementById('guest-phone')?.value.trim();
  const specialRequests = document.getElementById('guest-requests')?.value.trim();

  if (!guestName || !guestEmail) {
    showToast('Please enter your full name and contact email.', 'warning');
    return;
  }

  const submitBtn = document.getElementById('create-booking-btn');
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Securing Your Room...';
  }

  try {
    const payload = {
      roomId: selectedRoom.id,
      checkInDate: checkIn,
      checkOutDate: checkOut,
      numberOfGuests: guests,
      guestName,
      guestEmail,
      guestPhone,
      specialRequests
    };

    const res = await apiRequest('/reservations', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.success && res.data) {
      currentCreatedReservation = res.data;
      showToast('Room held successfully! Please finalize your payment method.', 'success');
      showPaymentStep(res.data);
    }
  } catch (error) {
    showToast(error.message, 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Proceed to Payment <i class="fas fa-arrow-right"></i>';
    }
  }
}

/**
 * 5. Step 2: Show 2-Column Payment Experience
 */
function showPaymentStep(reservation) {
  document.getElementById('booking-step-form')?.classList.add('hidden');
  document.getElementById('booking-step-payment')?.classList.remove('hidden');

  // Update Stepper
  updateStepper(2);

  // Populate Left Column Completed Steps Checklist
  const summaryGuest = document.getElementById('pay-summary-guest');
  const summaryDates = document.getElementById('pay-summary-dates');
  const summaryRoom = document.getElementById('pay-summary-room');

  if (summaryGuest) {
    summaryGuest.textContent = `${reservation.guestName} (${reservation.guestEmail})`;
  }
  if (summaryDates) {
    summaryDates.textContent = `${formatDate(reservation.checkInDate)} → ${formatDate(reservation.checkOutDate)} (${reservation.nights} night${reservation.nights > 1 ? 's' : ''}, ${reservation.numberOfGuests} guest${reservation.numberOfGuests > 1 ? 's' : ''})`;
  }
  if (summaryRoom) {
    summaryRoom.textContent = `Room ${reservation.room.roomNumber} — ${reservation.room.categoryName}`;
  }

  // Populate Right Column Sticky Summary
  const badgeRef = document.getElementById('pay-booking-ref-badge');
  const payImg = document.getElementById('pay-room-img');
  const payTitle = document.getElementById('pay-room-title');
  const payCat = document.getElementById('pay-room-category');
  const payRate = document.getElementById('pay-room-rate');
  const payCheckIn = document.getElementById('pay-summary-checkin');
  const payCheckOut = document.getElementById('pay-summary-checkout');
  const payNights = document.getElementById('pay-summary-nights');
  const payGuests = document.getElementById('pay-summary-guestcount');
  const paySubtotal = document.getElementById('pay-summary-subtotal');
  const payTax = document.getElementById('pay-summary-tax');
  const payTotal = document.getElementById('pay-summary-total');

  if (badgeRef) badgeRef.textContent = reservation.bookingReference;
  if (payImg) payImg.src = reservation.room.imageUrl || 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80';
  if (payTitle) payTitle.textContent = `Room ${reservation.room.roomNumber}`;
  if (payCat) payCat.textContent = reservation.room.categoryName;
  if (payRate) payRate.textContent = `${formatCurrency(reservation.room.pricePerNight)} / night`;

  if (payCheckIn) payCheckIn.textContent = formatDate(reservation.checkInDate);
  if (payCheckOut) payCheckOut.textContent = formatDate(reservation.checkOutDate);
  if (payNights) payNights.textContent = `${reservation.nights} night${reservation.nights > 1 ? 's' : ''}`;
  if (payGuests) payGuests.textContent = `${reservation.numberOfGuests} guest${reservation.numberOfGuests > 1 ? 's' : ''}`;

  const subtotalVal = (reservation.totalAmount / 1.12);
  const taxVal = reservation.totalAmount - subtotalVal;
  if (paySubtotal) paySubtotal.textContent = formatCurrency(subtotalVal);
  if (payTax) payTax.textContent = formatCurrency(taxVal);
  if (payTotal) payTotal.textContent = formatCurrency(reservation.totalAmount);

  // Sync amount across payment panels
  const qrAmount = document.getElementById('qr-display-amount');
  const hotelAmount = document.getElementById('hotel-pay-amount');
  if (qrAmount) qrAmount.textContent = formatCurrency(reservation.totalAmount);
  if (hotelAmount) hotelAmount.textContent = formatCurrency(reservation.totalAmount);

  // Update primary button text
  const totalStr = currentCreatedReservation ? formatCurrency(currentCreatedReservation.totalAmount) : '$0.00';
  const payBtnLabel = document.getElementById('pay-button-label');
  if (payBtnLabel) {
    if (selectedPaymentMethod === 'PAY_AT_CHECKIN') {
      payBtnLabel.innerHTML = 'Confirm Reservation &rarr;';
    } else {
      payBtnLabel.innerHTML = `Pay ${formatCurrency(reservation.totalAmount)} &rarr;`;
    }
  }

  // Set cardholder default name
  const cardHolderInput = document.getElementById('card-holder');
  if (cardHolderInput && reservation.guestName) {
    cardHolderInput.value = reservation.guestName;
    updateCardPreview();
  }

  // Set default method to Credit Card
  selectPaymentMethod('SIMULATED_CARD');

  window.scrollTo({ top: 120, behavior: 'smooth' });
}

/**
 * 6. Payment Method Switching
 */
function selectPaymentMethod(method) {
  selectedPaymentMethod = method;

  // Toggle card active states
  const cards = document.querySelectorAll('.payment-method-card');
  cards.forEach(card => {
    const isSelected = card.getAttribute('data-method') === method;
    if (isSelected) {
      card.classList.add('active');
      card.setAttribute('aria-checked', 'true');
      card.querySelector('.custom-radio')?.classList.add('checked');
    } else {
      card.classList.remove('active');
      card.setAttribute('aria-checked', 'false');
      card.querySelector('.custom-radio')?.classList.remove('checked');
    }
  });

  // Toggle content panels
  const cardSection = document.getElementById('method-section-card');
  const upiSection = document.getElementById('method-section-upi');
  const hotelSection = document.getElementById('method-section-hotel');
  const payBtnLabel = document.getElementById('pay-button-label');

  if (cardSection) cardSection.classList.toggle('hidden', method !== 'SIMULATED_CARD');
  if (upiSection) upiSection.classList.toggle('hidden', method !== 'SIMULATED_UPI');
  if (hotelSection) hotelSection.classList.toggle('hidden', method !== 'PAY_AT_CHECKIN');

  // Update primary button text
  const totalStr = currentCreatedReservation ? formatCurrency(currentCreatedReservation.totalAmount) : '$0.00';
  if (payBtnLabel) {
    if (method === 'PAY_AT_CHECKIN') {
      payBtnLabel.innerHTML = 'Confirm Reservation &rarr;';
    } else {
      payBtnLabel.innerHTML = `Pay ${totalStr} &rarr;`;
    }
  }

  // Clear previous validation error highlights
  clearValidationErrors();
}

/**
 * 7. UPI Mode Switcher (UPI ID vs QR Code)
 */
function switchUpiMode(mode) {
  selectedUpiMode = mode;
  const btnId = document.getElementById('btn-upi-id');
  const btnQr = document.getElementById('btn-upi-qr');
  const containerId = document.getElementById('upi-id-container');
  const containerQr = document.getElementById('upi-qr-container');

  if (mode === 'id') {
    btnId?.classList.add('active');
    btnQr?.classList.remove('active');
    containerId?.classList.remove('hidden');
    containerQr?.classList.add('hidden');
  } else {
    btnQr?.classList.add('active');
    btnId?.classList.remove('active');
    containerQr?.classList.remove('hidden');
    containerId?.classList.add('hidden');
  }
}

/**
 * 8. Card Input Formatting & Live Virtual Card Sync
 */
function initCardInputFormatters() {
  const numberInput = document.getElementById('card-number');
  const holderInput = document.getElementById('card-holder');
  const expiryInput = document.getElementById('card-expiry');
  const cvvInput = document.getElementById('card-cvv');

  // Format Card Number with 4-digit spacing
  numberInput?.addEventListener('input', (e) => {
    let val = e.target.value.replace(/\D/g, '').substring(0, 16);
    val = val.replace(/(.{4})/g, '$1 ').trim();
    e.target.value = val;
    updateCardPreview();
    detectCardBrand(val);
    validateSingleField('card-number');
  });

  // Cardholder Name live sync
  holderInput?.addEventListener('input', () => {
    updateCardPreview();
    validateSingleField('card-holder');
  });

  // Expiry Date Auto Slash MM/YY
  expiryInput?.addEventListener('input', (e) => {
    let val = e.target.value.replace(/\D/g, '').substring(0, 4);
    if (val.length >= 2) {
      val = val.substring(0, 2) + '/' + val.substring(2);
    }
    e.target.value = val;
    updateCardPreview();
    validateSingleField('card-expiry');
  });

  // CVV digits only
  cvvInput?.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').substring(0, 4);
    validateSingleField('card-cvv');
  });

  // UPI Input validation on blur/input
  document.getElementById('upi-id-input')?.addEventListener('input', () => {
    validateSingleField('upi-id');
  });
}

function updateCardPreview() {
  const numVal = document.getElementById('card-number')?.value.trim();
  const holderVal = document.getElementById('card-holder')?.value.trim();
  const expVal = document.getElementById('card-expiry')?.value.trim();

  const previewNum = document.getElementById('preview-card-number');
  const previewHolder = document.getElementById('preview-card-holder');
  const previewExp = document.getElementById('preview-card-expiry');

  if (previewNum) {
    if (numVal) {
      // Mask middle digits: 4521 •••• •••• 5678
      const raw = numVal.replace(/\s+/g, '');
      if (raw.length <= 4) {
        previewNum.textContent = numVal;
      } else {
        const first4 = raw.substring(0, 4);
        const last4 = raw.length > 12 ? raw.substring(raw.length - 4) : '••••';
        previewNum.textContent = `${first4} •••• •••• ${last4}`;
      }
    } else {
      previewNum.textContent = '•••• •••• •••• 4521';
    }
  }

  if (previewHolder) {
    previewHolder.textContent = holderVal ? holderVal.toUpperCase() : 'JOHN DOE';
  }

  if (previewExp) {
    previewExp.textContent = expVal || '12/28';
  }
}

function detectCardBrand(cardNumberWithSpaces) {
  const digits = cardNumberWithSpaces.replace(/\D/g, '');
  const brandBadge = document.getElementById('preview-brand-badge');
  const typeIcon = document.getElementById('card-type-icon');

  let brandIcon = '<i class="fab fa-cc-visa"></i>';
  let color = '#1a1f71';

  if (/^5[1-5]/.test(digits) || /^2[2-7]/.test(digits)) {
    brandIcon = '<i class="fab fa-cc-mastercard"></i>';
    color = '#eb001b';
  } else if (/^3[47]/.test(digits)) {
    brandIcon = '<i class="fab fa-cc-amex"></i>';
    color = '#006fcf';
  } else if (/^(60|65|35)/.test(digits)) {
    brandIcon = '<span style="font-weight: 800; font-size: 1rem; color: #097939;">RuPay</span>';
    color = '#097939';
  } else if (/^4/.test(digits)) {
    brandIcon = '<i class="fab fa-cc-visa"></i>';
    color = '#1a1f71';
  }

  if (brandBadge) brandBadge.innerHTML = brandIcon;
  if (typeIcon) {
    typeIcon.innerHTML = brandIcon;
    typeIcon.style.color = color;
  }
}

/**
 * 9. Input Validation
 */
function validateSingleField(fieldId) {
  if (fieldId === 'card-holder') {
    const el = document.getElementById('card-holder');
    const err = document.getElementById('error-card-holder');
    const isValid = el && el.value.trim().length >= 2;
    toggleFieldState(el, err, isValid);
    return isValid;
  }
  if (fieldId === 'card-number') {
    const el = document.getElementById('card-number');
    const err = document.getElementById('error-card-number');
    const digits = el ? el.value.replace(/\D/g, '') : '';
    const isValid = digits.length >= 15 && digits.length <= 16;
    toggleFieldState(el, err, isValid);
    return isValid;
  }
  if (fieldId === 'card-expiry') {
    const el = document.getElementById('card-expiry');
    const err = document.getElementById('error-card-expiry');
    let isValid = false;
    if (el && el.value.includes('/')) {
      const parts = el.value.split('/');
      const month = parseInt(parts[0], 10);
      const year = parseInt(parts[1], 10);
      const currentYear = new Date().getFullYear() % 100;
      isValid = month >= 1 && month <= 12 && year >= currentYear && year <= (currentYear + 15);
    }
    toggleFieldState(el, err, isValid);
    return isValid;
  }
  if (fieldId === 'card-cvv') {
    const el = document.getElementById('card-cvv');
    const err = document.getElementById('error-card-cvv');
    const digits = el ? el.value.replace(/\D/g, '') : '';
    const isValid = digits.length >= 3 && digits.length <= 4;
    toggleFieldState(el, err, isValid);
    return isValid;
  }
  if (fieldId === 'upi-id') {
    const el = document.getElementById('upi-id-input');
    const err = document.getElementById('error-upi-id');
    const val = el ? el.value.trim() : '';
    const isValid = val.includes('@') && val.split('@')[0].length >= 2 && val.split('@')[1].length >= 2;
    toggleFieldState(el, err, isValid);
    return isValid;
  }
  return true;
}

function toggleFieldState(inputEl, errorEl, isValid) {
  if (!inputEl) return;
  if (isValid) {
    inputEl.classList.remove('input-invalid');
    inputEl.classList.add('input-valid');
    if (errorEl) errorEl.classList.remove('visible');
  } else {
    inputEl.classList.add('input-invalid');
    inputEl.classList.remove('input-valid');
    if (errorEl) errorEl.classList.add('visible');
  }
}

function clearValidationErrors() {
  document.querySelectorAll('.form-control, .modern-input').forEach(el => {
    el.classList.remove('input-invalid', 'input-valid');
  });
  document.querySelectorAll('.field-error-msg').forEach(el => {
    el.classList.remove('visible');
  });
}

function validateAllFieldsForCurrentMethod() {
  if (selectedPaymentMethod === 'PAY_AT_CHECKIN') {
    return true; // No credentials required
  }

  if (selectedPaymentMethod === 'SIMULATED_UPI') {
    if (selectedUpiMode === 'qr') return true; // QR is visual scan
    return validateSingleField('upi-id');
  }

  // Credit Card validations
  const v1 = validateSingleField('card-holder');
  const v2 = validateSingleField('card-number');
  const v3 = validateSingleField('card-expiry');
  const v4 = validateSingleField('card-cvv');

  return v1 && v2 && v3 && v4;
}

/**
 * 10. Step 2: Payment Execution & Processing Simulation Modal
 */
async function handlePaymentSubmit(e) {
  e.preventDefault();
  if (!currentCreatedReservation) {
    showToast('No active reservation to pay for.', 'error');
    return;
  }

  // 1. Client-side Validation
  if (!validateAllFieldsForCurrentMethod()) {
    showToast('Please correct the highlighted fields before proceeding.', 'warning');
    return;
  }

  // 2. Open Animated Processing Modal (Requirement 8)
  const modal = document.getElementById('payment-processing-modal');
  const stateProcessing = document.getElementById('modal-state-processing');
  const stateSuccess = document.getElementById('modal-state-success');
  const stateFailure = document.getElementById('modal-state-failure');

  if (modal) modal.classList.add('active');
  if (stateProcessing) stateProcessing.classList.remove('hidden');
  if (stateSuccess) stateSuccess.classList.add('hidden');
  if (stateFailure) stateFailure.classList.add('hidden');

  // Prevent duplicate clicks on button
  const payBtn = document.getElementById('submit-payment-btn');
  if (payBtn) payBtn.disabled = true;

  // 3. Read test scenario (from Discrete Evaluator Mode panel)
  const isSuccess = document.querySelector('input[name="sim-scenario"]:checked')?.value === 'success';

  // Extract dummy mock values for demonstration
  const cardDigits = document.getElementById('card-number')?.value.replace(/\D/g, '') || '4521';
  const dummyLast4 = cardDigits.slice(-4);
  const dummyUpi = document.getElementById('upi-id-input')?.value.trim() || 'demo@upi';

  try {
    // Simulated bank network latency (1.4 seconds) for realistic financial handshake
    await new Promise(resolve => setTimeout(resolve, 1400));

    const payload = {
      bookingReference: currentCreatedReservation.bookingReference,
      paymentMethod: selectedPaymentMethod,
      simulateSuccess: isSuccess,
      dummyCardLast4: dummyLast4,
      dummyUpiId: dummyUpi
    };

    const res = await apiRequest('/payments/simulate', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.success && res.data) {
      lastPaymentResponse = res.data;

      if (isSuccess) {
        // Transition to Success Modal State
        stateProcessing?.classList.add('hidden');
        stateSuccess?.classList.remove('hidden');

        const successRef = document.getElementById('modal-success-booking-ref');
        const successPayRef = document.getElementById('modal-success-pay-ref');
        const successAmount = document.getElementById('modal-success-amount');
        const successRoom = document.getElementById('modal-success-room');
        const successDates = document.getElementById('modal-success-dates');
        const successDuration = document.getElementById('modal-success-duration');

        if (successRef) successRef.textContent = res.data.bookingReference;
        if (successPayRef) successPayRef.textContent = res.data.paymentReference || 'PAY-SIM-APPROVED';
        if (successAmount) successAmount.textContent = formatCurrency(res.data.totalAmount);

        if (successRoom) {
          const rm = res.data.room || selectedRoom;
          successRoom.textContent = rm ? `Room ${rm.roomNumber} \u2014 ${rm.categoryName}` : 'Grand Heritage Suite';
        }

        if (successDates) {
          successDates.innerHTML = `${formatDate(res.data.checkInDate)} &rarr; ${formatDate(res.data.checkOutDate)}`;
        }

        if (successDuration) {
          const gCount = res.data.numberOfGuests || (currentCreatedReservation ? currentCreatedReservation.numberOfGuests : 1);
          const nCount = res.data.nights || (currentCreatedReservation ? currentCreatedReservation.nights : 1);
          successDuration.innerHTML = `${gCount} Guest${gCount > 1 ? 's' : ''} &bull; ${nCount} Night${nCount > 1 ? 's' : ''}`;
        }

        showToast('Payment Approved! Reservation is now CONFIRMED.', 'success');
      } else {
        // Transition to Failure Modal State
        stateProcessing?.classList.add('hidden');
        stateFailure?.classList.remove('hidden');
        showToast('Transaction declined by simulated issuer. Reservation is PENDING.', 'warning');
      }
    }
  } catch (error) {
    stateProcessing?.classList.add('hidden');
    stateFailure?.classList.remove('hidden');
    showToast(error.message, 'error');
  } finally {
    if (payBtn) payBtn.disabled = false;
  }
}

/**
 * 11. Modal Outcome Actions
 */
function viewReservationVoucher() {
  closeProcessingModal();
  if (lastPaymentResponse) {
    showConfirmationStep(lastPaymentResponse);
  }
}

function downloadVoucherDirectly() {
  closeProcessingModal();
  if (lastPaymentResponse) {
    showConfirmationStep(lastPaymentResponse);
    setTimeout(() => {
      window.print();
    }, 400);
  }
}

function retryPayment() {
  closeProcessingModal();
  showToast('You can now re-attempt the payment or select another method.', 'info');
  // Auto-focus relevant field
  if (selectedPaymentMethod === 'SIMULATED_CARD') {
    document.getElementById('card-number')?.focus();
  } else if (selectedPaymentMethod === 'SIMULATED_UPI') {
    document.getElementById('upi-id-input')?.focus();
  }
}

function chooseAnotherMethod() {
  closeProcessingModal();
  selectPaymentMethod('PAY_AT_CHECKIN');
  showToast('Switched to Pay at Hotel. Confirm when ready.', 'info');
}

function closeProcessingModal() {
  document.getElementById('payment-processing-modal')?.classList.remove('active');
}

/**
 * 12. Step 3: Full Printable Confirmation Voucher View
 */
function showConfirmationStep(reservation) {
  document.getElementById('booking-step-payment')?.classList.add('hidden');
  document.getElementById('booking-step-confirmation')?.classList.remove('hidden');

  // Update Stepper to 100% complete
  updateStepper(3);

  const confRef = document.getElementById('conf-reference');
  const confGuest = document.getElementById('conf-guest');
  const confRoom = document.getElementById('conf-room');
  const confDates = document.getElementById('conf-dates');
  const confTotal = document.getElementById('conf-total');
  const confPaymentRef = document.getElementById('conf-pay-ref');

  if (confRef) confRef.textContent = reservation.bookingReference;
  if (confGuest) confGuest.textContent = `${reservation.guestName} (${reservation.guestEmail})`;
  if (confRoom) confRoom.textContent = `Room ${reservation.room.roomNumber} - ${reservation.room.categoryName}`;
  if (confDates) confDates.textContent = `${formatDate(reservation.checkInDate)} to ${formatDate(reservation.checkOutDate)} (${reservation.nights} night${reservation.nights > 1 ? 's' : ''})`;
  if (confTotal) confTotal.textContent = formatCurrency(reservation.totalAmount);
  if (confPaymentRef) confPaymentRef.textContent = reservation.paymentReference || 'PAY-SIMULATED';

  window.scrollTo({ top: 100, behavior: 'smooth' });
}

/**
 * 13. Back to Step 1 (Guest Details)
 */
function backToGuestDetails() {
  document.getElementById('booking-step-payment')?.classList.add('hidden');
  document.getElementById('booking-step-form')?.classList.remove('hidden');
  updateStepper(1);
  window.scrollTo({ top: 120, behavior: 'smooth' });
}

/**
 * 14. Developer / Evaluator Mode Controls (Requirement 11)
 */
function toggleEvaluatorPanel() {
  const panel = document.getElementById('evaluator-content-panel');
  const chevron = document.getElementById('evaluator-chevron');
  const statusLabel = document.getElementById('evaluator-status-label');
  const toggleBtn = document.querySelector('.evaluator-toggle-btn');
  if (!panel) return;

  const isHidden = panel.classList.contains('hidden');
  if (isHidden) {
    panel.classList.remove('hidden');
    if (chevron) chevron.style.transform = 'rotate(180deg)';
    if (statusLabel) {
      statusLabel.textContent = 'ON';
      statusLabel.classList.add('active');
    }
    if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'true');
  } else {
    panel.classList.add('hidden');
    if (chevron) chevron.style.transform = 'rotate(0deg)';
    if (statusLabel) {
      statusLabel.textContent = 'OFF';
      statusLabel.classList.remove('active');
    }
    if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
  }
}

function quickFillUpi() {
  selectPaymentMethod('SIMULATED_UPI');
  switchUpiMode('id');
  const upiInput = document.getElementById('upi-id-input');
  if (upiInput) {
    upiInput.value = 'demo.guest@okhdfcbank';
  }
  clearValidationErrors();
  showToast('Loaded valid demo UPI ID.', 'info');
}

function quickFillCard(type) {
  const numEl = document.getElementById('card-number');
  const expEl = document.getElementById('card-expiry');
  const cvvEl = document.getElementById('card-cvv');
  const holderEl = document.getElementById('card-holder');

  if (holderEl && !holderEl.value) holderEl.value = 'Eleanor Martinez';
  if (expEl) expEl.value = '12/28';
  if (cvvEl) cvvEl.value = '888';

  if (type === 'visa' && numEl) {
    numEl.value = '4521 8899 1234 5678';
  } else if (type === 'mastercard' && numEl) {
    numEl.value = '5424 9911 2233 4455';
  } else if (type === 'rupay' && numEl) {
    numEl.value = '6071 8822 3344 5566';
  }

  updateCardPreview();
  detectCardBrand(numEl ? numEl.value : '');
  clearValidationErrors();
  showToast(`Loaded valid ${type.toUpperCase()} demo card details.`, 'info');
}

/**
 * 15. Quick Login Modal Handler
 */
function quickLoginFromBooking(email, password) {
  apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  }).then(res => {
    if (res.success) {
      Auth.setAuth(res.data);
      renderNavbarAuth();
      closeModal('login-required-modal');
      showToast('Signed in successfully! You can now complete your booking.', 'success');
      const nameInput = document.getElementById('guest-name');
      const emailInput = document.getElementById('guest-email');
      if (nameInput) nameInput.value = res.data.fullName;
      if (emailInput) emailInput.value = res.data.email;
    }
  }).catch(err => {
    showToast(`Login failed: ${err.message}`, 'error');
  });
}
