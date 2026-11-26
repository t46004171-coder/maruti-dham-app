import { initializeApp } from "https://www.gstatic.com/firebasejs/11.4.0/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/11.4.0/firebase-auth.js";
import {
  getFirestore,
  collection,
  doc,
  addDoc,
  getDoc,
  setDoc,
  updateDoc,
  onSnapshot,
  query,
  where,
  orderBy,
  limit,
  runTransaction,
  serverTimestamp,
} from "https://www.gstatic.com/firebasejs/11.4.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyB3AXAf6Ag-3vC5NjIrsM-MAX_dzXhrUl8",
  authDomain: "gen-lang-client-0413146166.firebaseapp.com",
  projectId: "gen-lang-client-0413146166",
  storageBucket: "gen-lang-client-0413146166.firebasestorage.app",
  messagingSenderId: "128562258107",
  appId: "1:128562258107:android:ac15d9496e53a5aff3899e",
};

const DATABASE_ID = "ai-studio-android-marutidh-bc57197a-19de-4b00-a083-0583d44833e4";
const AUTHORIZED_OWNER_EMAIL = "tarunkumarjoshi11@gmail.com";

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app, DATABASE_ID);
const googleProvider = new GoogleAuthProvider();

const THEME_PRESETS = {
  NAVY_ROYAL_GOLD: { preset: "NAVY_ROYAL_GOLD", primaryColorHex: "#E5B94C", backgroundColorHex: "#07122B", surfaceColorHex: "#142652", accentColorHex: "#F7D774", textColorHex: "#F8F6F0" },
  WHITE_GOLD: { preset: "WHITE_GOLD", primaryColorHex: "#B8860B", backgroundColorHex: "#FFFFFF", surfaceColorHex: "#F7F3E9", accentColorHex: "#D4AF37", textColorHex: "#1A2238" },
  PREMIUM_DARK: { preset: "PREMIUM_DARK", primaryColorHex: "#F5C451", backgroundColorHex: "#0D0F14", surfaceColorHex: "#1A1F2B", accentColorHex: "#FFE082", textColorHex: "#F3F4F6" },
  CREAM_ROYAL_BLUE: { preset: "CREAM_ROYAL_BLUE", primaryColorHex: "#1E3A8A", backgroundColorHex: "#FFFDF5", surfaceColorHex: "#F3EFE0", accentColorHex: "#C89B2B", textColorHex: "#0F1E46" },
  MODERN_ROYAL_PURPLE: { preset: "MODERN_ROYAL_PURPLE", primaryColorHex: "#E5B94C", backgroundColorHex: "#1A1033", surfaceColorHex: "#291B4E", accentColorHex: "#C084FC", textColorHex: "#FAF5FF" },
};

const DEFAULT_CATEGORIES = ["Tea & Drinks", "Breakfast", "Maggi", "Fast Food", "Indian Food", "Snacks and Desserts"];

const DEFAULT_PRODUCTS = [
  { id: "tea", name: "Desi Tea", nameHindi: "देसी चाय", description: "Freshly brewed hot ginger-cardamom chai.", category: "Tea & Drinks", priceOptions: [{ label: "Regular", price: 10 }, { label: "Special", price: 20 }], available: true, visible: true, foodOrderEnabled: true, deliveryEnabled: true },
  { id: "poha", name: "Poha", nameHindi: "पोहा", description: "Warm flattened rice with peanuts, sev, and lemon.", category: "Breakfast", priceOptions: [{ label: "Half", price: 20 }, { label: "Full", price: 30 }], available: true, visible: true, foodOrderEnabled: true, deliveryEnabled: true },
  { id: "maggi", name: "Maggi", nameHindi: "मैगी", description: "Hot masala Maggi prepared fresh to order.", category: "Maggi", priceOptions: [{ label: "Standard", price: 50 }], available: true, visible: true, foodOrderEnabled: true, deliveryEnabled: true },
  { id: "chole_kulche", name: "Chole Kulche", nameHindi: "छोले कुलचे", description: "Spiced Khatu chole with soft buttered kulche.", category: "Indian Food", priceOptions: [{ label: "Full Plate", price: 50 }], available: true, visible: true, foodOrderEnabled: true, deliveryEnabled: true },
];

const state = {
  lang: "en",
  activeTab: "home",
  adminSubTab: "overview",
  reportPeriod: "DAILY",
  reportStartDate: "",
  reportEndDate: "",
  loginMode: "SELECTOR",
  ownerDeniedMsg: "",
  themePreviewActive: false,
  user: null,
  profile: null,
  isOwner: false,
  shopSettings: {
    shopName: "MARUTI DHAM",
    ownerName: "Tarun Kumar",
    address: "Near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
    mapsQuery: "Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
    shopOpen: true,
    shopControlMode: "OPEN_NOW",
    openingTime: "7:00 AM",
    closingTime: "10:00 PM",
    whatsappNumber: "",
    contactPhone: "",
    contactEmail: "",
    upiId: "",
    cashEnabled: true,
    upiEnabled: false,
    whatsappWebhookUrl: "",
    aboutText: "MARUTI DHAM (Owner: Tarun Kumar) serves fresh tea, snacks, meals, AC/Non-AC rooms, freshen-up suites, and Khatu Shyam Ji local food delivery.",
    privacyPolicy: "MARUTI DHAM collects your name, mobile number, and order details strictly to fulfill your orders.",
    termsText: "Orders are accepted during open shop hours. Cash Only payment applies unless UPI is enabled by owner. Food delivery applies within Khatu Shyam Ji only.",
  },
  siteConfig: {
    logoUrl: "",
    heroBannerUrl: "",
    showAnnouncementsSection: true,
    showDailySpecialsSection: true,
    showRoomsSection: true,
    showDeliverySection: true,
    categories: [...DEFAULT_CATEGORIES],
    draftTheme: { ...THEME_PRESETS.NAVY_ROYAL_GOLD, fontStyle: "DEVOTIONAL_SERIF", layoutVariant: "DEVOTIONAL_CARD" },
    publishedTheme: { ...THEME_PRESETS.NAVY_ROYAL_GOLD, fontStyle: "DEVOTIONAL_SERIF", layoutVariant: "DEVOTIONAL_CARD" },
  },
  deliverySettings: {
    deliveryEnabled: true,
    pickupEnabled: true,
    deliveryCharge: 0,
    packagingCharge: 0,
    serviceAreaName: "Khatu Shyam Ji",
    allowedLocalities: ["Manda Chauraha", "Khatu Shyam Ji Temple Road", "Toran Dwar", "Hospital Chauraha"],
    minimumOrderAmount: 50,
    deliveryNote: "Delivery restricted strictly to Khatu Shyam Ji.",
  },
  announcements: [],
  products: [...DEFAULT_PRODUCTS],
  roomConfigs: [
    { id: "ac", roomType: "AC", title: "AC Room", statusLabel: "AVAILABLE", available: true, availableCount: 3, rate6Hours: 1200, rate12Hours: 2000 },
    { id: "non_ac", roomType: "NON_AC", title: "Non-AC Room", statusLabel: "AVAILABLE", available: true, availableCount: 4, rate6Hours: 800, rate12Hours: 1500 },
  ],
  freshenConfigs: [
    { id: "only_fresh", optionKey: "ONLY_FRESH", title: "Only Fresh", pricePerPerson: 50, available: true },
    { id: "bath_and_fresh", optionKey: "BATH_FRESH", title: "Bath + Fresh", pricePerPerson: 100, available: true },
  ],
  myRequests: [],
  allRequests: [],
  allUsers: [],
  activityLogs: [],
  notifications: [],
  adminRequestsInitialized: false,
  knownAdminRequestIds: new Set(),
  newlyArrivedOrderIds: new Set(),
  lowStockInitialized: false,
  knownLowStockProductIds: new Set(),
  stockFilter: "ALL",
  pickupCart: {},
  deliveryCart: {},
  selectedCategory: "ALL",
  lastSubmitMs: 0,
  unsubscribes: [],
};

const I18N = {
  en: {
    shyamBanner: "JAI SHRI SHYAM • HARE KA SAHARA",
    locationShort: "Near Manda Chauraha, Khatu Shyam Ji",
    chooseLoginTitle: "Select Login Type",
    chooseLoginDesc: "Choose Customer Login to order food & rooms, or Owner Login for business management.",
    customerLoginOption: "1. Customer Login",
    customerLoginSub: "Browse menu, place food & delivery orders, book rooms, and view your receipts.",
    ownerLoginOption: "2. Owner / Admin Login",
    ownerLoginSub: "Restricted to authorized shop owner only. Manage orders, menu, reports & theme.",
    completeProfileTitle: "Complete Customer Profile",
    completeProfileDesc: "Enter your name and 10-digit Indian mobile number to place orders.",
    fullName: "Full Name",
    mobileNumber: "Mobile Number (10 digits)",
    defaultLocality: "Default Area in Khatu Shyam Ji",
    defaultAddress: "Default Address (Optional)",
    consentLabel: "I agree to the MARUTI DHAM Privacy Policy for order fulfillment.",
    saveProfile: "Save & Continue",
    timingsLabel: "Timings (IST)",
    statusLabel: "Shop Status",
    specialsTitle: "Daily Specials & Announcements",
    servicesTitle: "Available Services",
    addressTitle: "Shop Address",
    foodMenuTitle: "Food Menu & Counter Pickup",
  },
  hi: {
    shyamBanner: "जय श्री श्याम • हारे का सहारा",
    locationShort: "मांडा चौराहा के पास, खाटू श्याम जी",
    chooseLoginTitle: "लॉगिन विकल्प चुनें",
    chooseLoginDesc: "ऑर्डर एवं कमरों के लिए ग्राहक लॉगिन चुनें या दुकान प्रबंधन हेतु स्वामी लॉगिन चुनें।",
    customerLoginOption: "1. ग्राहक लॉगिन (Customer Login)",
    customerLoginSub: "मेनू देखें, फ़ूड ऑर्डर व डिलीवरी करें, कमरा बुक करें और रसीद देखें।",
    ownerLoginOption: "2. स्वामी / एडमिन लॉगिन (Owner Login)",
    ownerLoginSub: "केवल अधिकृत दुकान स्वामी के लिए सुरक्षित एडमिन पैनल।",
    completeProfileTitle: "ग्राहक प्रोफ़ाइल पूर्ण करें",
    completeProfileDesc: "ऑर्डर करने के लिए नाम और 10 अंकों का मोबाइल नंबर दर्ज करें।",
    fullName: "पूरा नाम",
    mobileNumber: "मोबाइल नंबर (10 अंक)",
    defaultLocality: "खाटू श्याम जी क्षेत्र",
    defaultAddress: "डिफ़ॉल्ट पता (वैकल्पिक)",
    consentLabel: "मैं ऑर्डर पूरा करने हेतु MARUTI DHAM गोपनीयता नीति से सहमत हूँ।",
    saveProfile: "सहेजें और आगे बढ़ें",
    timingsLabel: "समय (IST)",
    statusLabel: "दुकान स्थिति",
    specialsTitle: "आज के विशेष व सूचनाएँ",
    servicesTitle: "उपलब्ध सेवाएँ",
    addressTitle: "दुकान का पता",
    foodMenuTitle: "फ़ूड मेनू व काउंटर पिकअप",
  },
};

function showToast(msg) {
  const el = document.getElementById("toast");
  el.textContent = msg;
  el.classList.remove("hidden");
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => el.classList.add("hidden"), 3200);
}

function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function parseClockMinutes(str) {
  const m = String(str || "").trim().toUpperCase().match(/^(\d{1,2}):(\d{2})(?:\s*(AM|PM))?$/);
  if (!m) return null;
  let hr = parseInt(m[1], 10);
  const min = parseInt(m[2], 10);
  const ampm = m[3];
  if (ampm === "PM" && hr < 12) hr += 12;
  if (ampm === "AM" && hr === 12) hr = 0;
  return hr * 60 + min;
}

function isShopCurrentlyOpen() {
  const s = state.shopSettings;
  if (s.shopControlMode === "OPEN_NOW") return { isOpen: true, reason: "Open Now" };
  if (s.shopControlMode === "CLOSED_NOW") return { isOpen: false, reason: `Closed by owner. Next opening: ${s.openingTime} IST.` };
  const nowIst = new Date(new Date().toLocaleString("en-US", { timeZone: "Asia/Kolkata" }));
  const nowMins = nowIst.getHours() * 60 + nowIst.getMinutes();
  const openMins = parseClockMinutes(s.openingTime);
  const closeMins = parseClockMinutes(s.closingTime);
  if (openMins != null && closeMins != null) {
    const inside = openMins <= closeMins ? nowMins >= openMins && nowMins < closeMins : nowMins >= openMins || nowMins < closeMins;
    return inside ? { isOpen: true, reason: `Scheduled Open (${s.openingTime} – ${s.closingTime} IST)` } : { isOpen: false, reason: `Closed now. Next opening: ${s.openingTime} IST.` };
  }
  return { isOpen: Boolean(s.shopOpen), reason: s.shopOpen ? "Open" : "Closed" };
}

function formatIst(ts) {
  const d = ts && typeof ts.toDate === "function" ? ts.toDate() : new Date();
  return d.toLocaleString("en-IN", { timeZone: "Asia/Kolkata", day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", hour12: true }) + " IST";
}

function applyLanguage() {
  const dict = I18N[state.lang] || I18N.en;
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.getAttribute("data-i18n");
    if (dict[key]) el.textContent = dict[key];
  });
}

function applySiteThemeAndSections() {
  const theme = (state.isOwner && state.themePreviewActive)
    ? (state.siteConfig.draftTheme || THEME_PRESETS.NAVY_ROYAL_GOLD)
    : (state.siteConfig.publishedTheme || THEME_PRESETS.NAVY_ROYAL_GOLD);

  const root = document.documentElement;
  if (theme.backgroundColorHex) root.style.setProperty("--navy-deep", theme.backgroundColorHex);
  if (theme.surfaceColorHex) {
    root.style.setProperty("--navy-card", theme.surfaceColorHex);
    root.style.setProperty("--navy-surface", theme.surfaceColorHex);
  }
  if (theme.primaryColorHex) root.style.setProperty("--gold", theme.primaryColorHex);
  if (theme.accentColorHex) root.style.setProperty("--gold-bright", theme.accentColorHex);
  if (theme.textColorHex) root.style.setProperty("--cream", theme.textColorHex);

  if (theme.fontStyle === "MODERN_SANS") {
    root.style.setProperty("--font-body", '"Plus Jakarta Sans", "Noto Sans Devanagari", sans-serif');
  } else if (theme.fontStyle === "CLASSIC_ROYAL") {
    root.style.setProperty("--font-body", '"Cinzel", "Noto Sans Devanagari", serif');
  } else {
    root.style.setProperty("--font-body", '"Plus Jakarta Sans", "Noto Sans Devanagari", system-ui, sans-serif');
  }

  document.getElementById("theme-preview-pill")?.classList.toggle("hidden", !(state.isOwner && state.themePreviewActive));

  const logoImg = document.getElementById("brand-logo-img");
  const crest = document.getElementById("brand-crest");
  if (state.siteConfig.logoUrl) {
    logoImg.src = state.siteConfig.logoUrl;
    logoImg.classList.remove("hidden");
    crest.classList.add("hidden");
  } else {
    logoImg.classList.add("hidden");
    crest.classList.remove("hidden");
  }

  const heroImg = document.getElementById("hero-banner-img");
  if (state.siteConfig.heroBannerUrl) {
    heroImg.src = state.siteConfig.heroBannerUrl;
    heroImg.classList.remove("hidden");
  } else {
    heroImg.classList.add("hidden");
  }

  const showAnn = state.siteConfig.showAnnouncementsSection !== false || state.siteConfig.showDailySpecialsSection !== false;
  document.getElementById("home-announcements-wrapper")?.classList.toggle("hidden", !showAnn);
  document.getElementById("home-rooms-tile")?.classList.toggle("hidden", state.siteConfig.showRoomsSection === false);
  document.getElementById("home-delivery-tile")?.classList.toggle("hidden", state.siteConfig.showDeliverySection === false);
}

function renderLoginPortal() {
  const portal = document.getElementById("login-portal-card");
  const signedIn = Boolean(state.user);
  portal.classList.toggle("hidden", signedIn);

  document.getElementById("login-selector-view").classList.toggle("hidden", state.loginMode !== "SELECTOR");
  document.getElementById("login-customer-view").classList.toggle("hidden", state.loginMode !== "CUSTOMER");
  document.getElementById("login-owner-view").classList.toggle("hidden", state.loginMode !== "OWNER");

  const deniedBanner = document.getElementById("owner-denied-banner");
  if (state.ownerDeniedMsg) {
    deniedBanner.textContent = state.ownerDeniedMsg;
    deniedBanner.classList.remove("hidden");
  } else {
    deniedBanner.classList.add("hidden");
  }

  document.getElementById("settings-signout-btn")?.classList.toggle("hidden", !signedIn);
  document.getElementById("auth-btn").textContent = signedIn ? "Sign Out" : "Login";
}

function updateScrollToTopVisibility() {
  const btn = document.getElementById("scroll-to-top-btn");
  if (!btn) return;
  const isLongPage = document.documentElement.scrollHeight > window.innerHeight * 1.15;
  const shouldShow = isLongPage && window.scrollY > 240;
  btn.classList.toggle("hidden", !shouldShow);
}

function switchTab(tab) {
  state.activeTab = tab;
  document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
  document.getElementById(`tab-${tab}`)?.classList.add("active");
  document.querySelectorAll(".nav-item").forEach((b) => {
    const isActive = b.dataset.tab === tab;
    b.classList.toggle("active", isActive);
    if (isActive) b.setAttribute("aria-current", "page");
    else b.removeAttribute("aria-current");
  });
  updateScrollToTopVisibility();
}

function switchAdminSubTab(subTab) {
  state.adminSubTab = subTab;
  document.querySelectorAll(".adm-panel").forEach((el) => el.classList.add("hidden"));
  document.getElementById(`adm-panel-${subTab}`)?.classList.remove("hidden");
  document.querySelectorAll("[data-adm-tab]").forEach((b) => {
    b.classList.toggle("active", b.dataset.admTab === subTab);
  });
  updateScrollToTopVisibility();
}

function populateLocalitySelects() {
  const locs = state.deliverySettings.allowedLocalities || ["Manda Chauraha"];
  ["reg-locality", "deliv-locality"].forEach((id) => {
    const sel = document.getElementById(id);
    if (!sel) return;
    sel.innerHTML = locs.map((l) => `<option value="${escapeHtml(l)}">${escapeHtml(l)}</option>`).join("");
  });
}

function renderPaymentCheckoutVisibility() {
  const upiAllowed = Boolean(state.shopSettings.upiEnabled);

  document.getElementById("pickup-cash-only-badge")?.classList.toggle("hidden", upiAllowed);
  document.getElementById("pickup-payment-wrap")?.classList.toggle("hidden", !upiAllowed);
  if (!upiAllowed) {
    const pSel = document.getElementById("pickup-payment");
    if (pSel) pSel.value = "CASH";
    document.getElementById("pickup-upi-ref-wrap")?.classList.add("hidden");
  }

  document.getElementById("deliv-cash-only-badge")?.classList.toggle("hidden", upiAllowed);
  document.getElementById("deliv-payment-wrap")?.classList.toggle("hidden", !upiAllowed);
  if (!upiAllowed) {
    const dSel = document.getElementById("deliv-payment");
    if (dSel) dSel.value = "CASH";
    document.getElementById("deliv-upi-ref-wrap")?.classList.add("hidden");
  }
}

function renderShopHeaderAndHome() {
  const { isOpen, reason } = isShopCurrentlyOpen();
  const badge = document.getElementById("shop-status-badge");
  badge.textContent = isOpen ? "OPEN" : "CLOSED";
  badge.className = `status-pill ${isOpen ? "status-open" : "status-closed"}`;

  const closedBanner = document.getElementById("shop-closed-banner");
  closedBanner.classList.toggle("hidden", isOpen);
  document.getElementById("shop-closed-reason").textContent = reason;

  document.getElementById("home-about-text").textContent = state.shopSettings.aboutText;
  document.getElementById("home-timings").textContent = `${state.shopSettings.openingTime} – ${state.shopSettings.closingTime}`;
  document.getElementById("home-open-detail").textContent = reason;
  document.getElementById("home-address").textContent = state.shopSettings.address;
  document.getElementById("maps-link").href = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(state.shopSettings.mapsQuery || state.shopSettings.address)}`;

  document.getElementById("settings-privacy-text").textContent = state.shopSettings.privacyPolicy;
  document.getElementById("settings-terms-text").textContent = state.shopSettings.termsText;

  const annContainer = document.getElementById("announcements-list");
  const active = state.announcements.filter((a) => {
    if (!a.active) return false;
    return a.dailySpecial ? state.siteConfig.showDailySpecialsSection !== false : state.siteConfig.showAnnouncementsSection !== false;
  });
  annContainer.innerHTML = active.length
    ? active.map((a) => `<div class="card"><span class="badge-gold">${a.dailySpecial ? "Daily Special" : "Announcement"}</span><h4>${escapeHtml(a.title)}</h4><p class="muted">${escapeHtml(a.message)}</p></div>`).join("")
    : `<div class="card"><p class="muted">Fresh Tea, Poha, Maggi &amp; Chole Kulche prepared daily at MARUTI DHAM.</p></div>`;

  document.getElementById("pickup-availability-note").textContent = state.deliverySettings.pickupEnabled
    ? `Counter Pickup Available • Packaging Fee: ₹${state.deliverySettings.packagingCharge || 0}`
    : "Counter Pickup is temporarily paused by owner.";
  document.getElementById("delivery-area-note").textContent = state.deliverySettings.deliveryEnabled
    ? `Delivering in ${state.deliverySettings.serviceAreaName} • Min Order ₹${state.deliverySettings.minimumOrderAmount} • Delivery Fee ₹${state.deliverySettings.deliveryCharge || 0} + Packaging ₹${state.deliverySettings.packagingCharge || 0}`
    : "Delivery is currently paused by the owner.";

  renderPaymentCheckoutVisibility();
  applySiteThemeAndSections();
}

function cartKey(productId, label) {
  return `${productId}__${label}`;
}

function updateCartQty(cartObj, product, opt, delta) {
  const key = cartKey(product.id, opt.label);
  const existing = cartObj[key] || { name: product.name, variantLabel: opt.label, unitPrice: opt.price, quantity: 0 };
  const nextQty = Math.max(0, existing.quantity + delta);
  if (nextQty === 0) delete cartObj[key];
  else cartObj[key] = { ...existing, quantity: nextQty, lineTotal: nextQty * opt.price };
  renderMenusAndCarts();
}

function renderMenusAndCarts() {
  const visible = state.products.filter((p) => p.visible);
  const configuredCats = (state.siteConfig.categories && state.siteConfig.categories.length)
    ? state.siteConfig.categories
    : DEFAULT_CATEGORIES;
  const productCats = visible.map((p) => p.category).filter(Boolean);
  const categories = ["ALL", ...new Set([...configuredCats, ...productCats])];

  document.getElementById("food-categories").innerHTML = categories
    .map((cat) => `<button type="button" class="cat-chip ${state.selectedCategory === cat ? "active" : ""}" data-cat="${escapeHtml(cat)}">${escapeHtml(cat)}</button>`)
    .join("");
  document.querySelectorAll(".cat-chip[data-cat]").forEach((btn) => {
    btn.onclick = () => { state.selectedCategory = btn.dataset.cat; renderMenusAndCarts(); };
  });

  renderProductGrid("food-menu-grid", visible.filter((p) => p.foodOrderEnabled && (state.selectedCategory === "ALL" || p.category === state.selectedCategory)), state.pickupCart, "pickup");
  renderProductGrid("delivery-menu-grid", visible.filter((p) => p.deliveryEnabled), state.deliveryCart, "delivery");
  renderCartSummary("pickup");
  renderCartSummary("delivery");
}

function renderProductGrid(containerId, products, cartObj, mode) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = products.map((p) => {
    const outOfStock = !p.available || (p.trackStock && (p.stockQuantity || 0) <= 0);
    const variantsHtml = (p.priceOptions || []).map((opt) => {
      const qty = cartObj[cartKey(p.id, opt.label)]?.quantity || 0;
      return `<div class="variant-row">
        <span>${escapeHtml(opt.label)} — <strong>₹${opt.price}</strong></span>
        <div class="qty-controls">
          <button type="button" class="qty-btn" ${outOfStock ? "disabled" : ""} data-mode="${mode}" data-pid="${escapeHtml(p.id)}" data-label="${escapeHtml(opt.label)}" data-delta="-1">-</button>
          <span>${qty}</span>
          <button type="button" class="qty-btn" ${outOfStock ? "disabled" : ""} data-mode="${mode}" data-pid="${escapeHtml(p.id)}" data-label="${escapeHtml(opt.label)}" data-delta="1">+</button>
        </div>
      </div>`;
    }).join("");
    return `<div class="menu-card">
      <div>
        <span class="badge-gold">${escapeHtml(p.category)}</span>
        <h4>${escapeHtml(p.name)} ${p.nameHindi ? `• ${escapeHtml(p.nameHindi)}` : ""}</h4>
        ${p.description ? `<p class="menu-desc">${escapeHtml(p.description)}</p>` : ""}
        ${outOfStock ? `<span class="status-pill status-closed">Out of Stock</span>` : ""}
      </div>
      <div>${variantsHtml}</div>
    </div>`;
  }).join("");

  el.querySelectorAll(".qty-btn").forEach((btn) => {
    btn.onclick = () => {
      const p = state.products.find((x) => x.id === btn.dataset.pid);
      const opt = p?.priceOptions?.find((o) => o.label === btn.dataset.label);
      if (!p || !opt) return;
      const target = btn.dataset.mode === "pickup" ? state.pickupCart : state.deliveryCart;
      updateCartQty(target, p, opt, Number(btn.dataset.delta));
    };
  });
}

function renderCartSummary(mode) {
  const cartObj = mode === "pickup" ? state.pickupCart : state.deliveryCart;
  const items = Object.values(cartObj);
  const subtotal = items.reduce((s, x) => s + x.lineTotal, 0);
  const pkg = items.length ? (state.deliverySettings.packagingCharge || 0) : 0;
  const deliv = mode === "delivery" && items.length ? (state.deliverySettings.deliveryCharge || 0) : 0;
  const grand = subtotal + pkg + deliv;

  if (mode === "pickup") {
    document.getElementById("pickup-cart-items").innerHTML = items.length
      ? items.map((i) => `<div class="request-row"><span>${escapeHtml(i.name)} (${escapeHtml(i.variantLabel)}) x${i.quantity}</span><strong>₹${i.lineTotal}</strong></div>`).join("")
      : `<p class="muted">Cart is empty. Add items above.</p>`;
    document.getElementById("pickup-subtotal").textContent = `₹${subtotal}`;
    document.getElementById("pickup-pkg").textContent = `₹${pkg}`;
    document.getElementById("pickup-grand").textContent = `₹${grand}`;
  } else {
    document.getElementById("delivery-cart-items").innerHTML = items.length
      ? items.map((i) => `<div class="request-row"><span>${escapeHtml(i.name)} (${escapeHtml(i.variantLabel)}) x${i.quantity}</span><strong>₹${i.lineTotal}</strong></div>`).join("")
      : `<p class="muted">Delivery cart is empty.</p>`;
    document.getElementById("deliv-subtotal").textContent = `₹${subtotal}`;
    document.getElementById("deliv-charge").textContent = `₹${deliv}`;
    document.getElementById("deliv-pkg").textContent = `₹${pkg}`;
    document.getElementById("deliv-grand").textContent = `₹${grand}`;
  }
}

function renderRoomsAndFreshen() {
  document.getElementById("room-cards-grid").innerHTML = state.roomConfigs.map((r) => `
    <div class="card">
      <span class="badge-gold">${escapeHtml(r.statusLabel)} (${r.availableCount} rooms)</span>
      <h4>${escapeHtml(r.title)}</h4>
      <p class="muted">6 Hours: ₹${r.rate6Hours} • 12 Hours: ₹${r.rate12Hours}</p>
    </div>`).join("");
  updateRoomFreshenTotal();
}

function updateRoomFreshenTotal() {
  const mode = document.getElementById("rf-mode").value;
  document.getElementById("rf-room-fields").classList.toggle("hidden", mode !== "ROOM");
  document.getElementById("rf-freshen-fields").classList.toggle("hidden", mode !== "FRESHEN");
  let total = 0;
  if (mode === "ROOM") {
    const rType = document.getElementById("rf-room-type").value;
    const dur = Number(document.getElementById("rf-duration").value);
    const rooms = Number(document.getElementById("rf-room-count").value || 1);
    const cfg = state.roomConfigs.find((r) => r.roomType === rType);
    const rateMap = { 1: cfg?.rate1Hour, 2: cfg?.rate2Hours, 3: cfg?.rate3Hours, 4: cfg?.rate4Hours, 5: cfg?.rate5Hours, 6: cfg?.rate6Hours, 12: cfg?.rate12Hours };
    const unit = rateMap[dur] ?? null;
    total = unit != null ? unit * rooms : 0;
    document.getElementById("rf-total").textContent = unit != null ? `₹${total}` : "Confirm on request";
  } else {
    const opt = document.getElementById("rf-freshen-opt").value;
    const people = Number(document.getElementById("rf-people").value || 1);
    const cfg = state.freshenConfigs.find((f) => f.optionKey === opt);
    total = (cfg?.pricePerPerson || (opt === "BATH_FRESH" ? 100 : 50)) * people;
    document.getElementById("rf-total").textContent = `₹${total}`;
  }
  return total;
}

function renderMyRequests() {
  const el = document.getElementById("my-requests-list");
  if (!state.user) {
    el.innerHTML = `<p class="muted">Login with Google to view your live orders &amp; digital receipts.</p>`;
    return;
  }
  el.innerHTML = state.myRequests.length
    ? state.myRequests.map((r) => `
      <div class="request-row">
        <div>
          <strong>${escapeHtml(r.orderNumber || r.id)}</strong> • <span>${escapeHtml(r.status)}</span>
          <div class="muted">${formatIst(r.createdAt)} • ₹${r.grandTotal} (${escapeHtml(r.paymentMethod)})</div>
        </div>
        <button type="button" class="btn-outline-gold view-receipt-btn" data-id="${escapeHtml(r.id)}">Digital Receipt</button>
      </div>`).join("")
    : `<p class="muted">No orders yet.</p>`;

  el.querySelectorAll(".view-receipt-btn").forEach((btn) => {
    btn.onclick = () => {
      const req = state.myRequests.find((x) => x.id === btn.dataset.id);
      if (req) openReceiptModal(req);
    };
  });
}

function openReceiptModal(req) {
  const subtotal = (req.foodTotal || 0) + (req.roomTotal || 0) + (req.freshenUpTotal || 0);
  const lines = [
    "MARUTI DHAM — DIGITAL RECEIPT",
    `Owner: ${state.shopSettings.ownerName} • ${state.shopSettings.address}`,
    "----------------------------------------",
    `Order ID: ${req.orderNumber || req.id}`,
    `Time:     ${formatIst(req.createdAt)}`,
    `Customer: ${req.customerName} (${req.contactPhone})`,
    `Mode:     ${req.fulfillmentType || "PICKUP"} (${req.expectedTime})`,
    `Payment:  ${req.paymentMethod} [${req.paymentStatus}]`,
  ];
  if (req.deliveryAddress) lines.push(`Address:  ${req.deliveryLocality || ""} - ${req.deliveryAddress}`);
  lines.push("----------------------------------------");
  if (Array.isArray(req.items) && req.items.length) {
    lines.push("ITEM | QTY | RATE | TOTAL");
    req.items.forEach((i) => lines.push(`${i.name} (${i.variantLabel}) | ${i.quantity} | ₹${i.unitPrice} | ₹${i.lineTotal}`));
    lines.push("----------------------------------------");
  }
  lines.push(`Subtotal:          ₹${subtotal}`);
  if ((req.deliveryCharge || 0) > 0) lines.push(`Delivery Charge:   ₹${req.deliveryCharge}`);
  if ((req.packagingCharge || 0) > 0) lines.push(`Packaging Charge:  ₹${req.packagingCharge}`);
  lines.push(`Grand Total:       ₹${req.grandTotal}`, "----------------------------------------", "THANK YOU FOR VISITING MARUTI DHAM");

  document.getElementById("receipt-content").textContent = lines.join("\n");
  document.getElementById("receipt-dialog").showModal();
}

function formatOrderStatusBadge(rawStatus) {
  const s = String(rawStatus || "PENDING").toUpperCase();
  const map = {
    PENDING: { label: "Pending", cls: "badge-pending" },
    CONFIRMED: { label: "Confirmed", cls: "badge-confirmed" },
    PREPARING: { label: "Preparing", cls: "badge-preparing" },
    READY: { label: "Ready", cls: "badge-ready" },
    COMPLETED: { label: "Completed", cls: "badge-completed" },
    REJECTED: { label: "Cancelled", cls: "badge-rejected" },
  };
  const info = map[s] || map.PENDING;
  return `<span class="order-status-badge ${info.cls}">${info.label}</span>`;
}

function renderSeparatedOrderRows(containerId, list, emptyMsg) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = list.length ? list.map((r) => `
    <div class="request-row">
      <div>
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:4px;">
          <strong>${escapeHtml(r.orderNumber || r.id)}</strong> — ${escapeHtml(r.customerName)} (${escapeHtml(r.contactPhone)})
          ${formatOrderStatusBadge(r.status)}
        </div>
        <div class="muted">₹${r.grandTotal} • ${escapeHtml(r.paymentMethod)} [${escapeHtml(r.paymentStatus)}]</div>
        ${r.deliveryAddress ? `<div class="muted">Address: ${escapeHtml(r.deliveryLocality || "")} — ${escapeHtml(r.deliveryAddress)}</div>` : ""}
      </div>
      <div class="inline-actions">
        <button type="button" class="btn-outline-gold adm-status-btn" data-id="${escapeHtml(r.id)}" data-status="CONFIRMED">Confirm</button>
        <button type="button" class="btn-outline-gold adm-status-btn" data-id="${escapeHtml(r.id)}" data-status="PREPARING">Preparing</button>
        <button type="button" class="btn-outline-gold adm-status-btn" data-id="${escapeHtml(r.id)}" data-status="READY">Ready</button>
        <button type="button" class="btn-gold adm-status-btn" data-id="${escapeHtml(r.id)}" data-status="COMPLETED">Completed</button>
        ${r.paymentStatus !== "PAID" ? `<button type="button" class="btn-outline-gold adm-paid-btn" data-id="${escapeHtml(r.id)}">Mark Paid</button>` : ""}
        <button type="button" class="btn-outline-danger adm-status-btn" data-id="${escapeHtml(r.id)}" data-status="REJECTED">Cancel</button>
      </div>
    </div>`).join("") : `<p class="muted">${escapeHtml(emptyMsg)}</p>`;

  el.querySelectorAll(".adm-status-btn").forEach((btn) => {
    btn.onclick = async () => {
      const targetOrder = state.allRequests.find((r) => r.id === btn.dataset.id);
      const nextStatus = btn.dataset.status;
      await updateDoc(doc(db, "requests", btn.dataset.id), { status: nextStatus, updatedAt: serverTimestamp() });
      if (
        targetOrder &&
        (targetOrder.status || "PENDING") === "PENDING" &&
        nextStatus !== "PENDING" &&
        nextStatus !== "REJECTED" &&
        (targetOrder.category === "FOOD_REQUEST" || targetOrder.category === "FOOD_DELIVERY_REQUEST")
      ) {
        const orderedByName = {};
        (targetOrder.items || []).forEach((item) => {
          const k = String(item.name || "").trim().toLowerCase();
          if (k && item.quantity > 0) orderedByName[k] = (orderedByName[k] || 0) + Number(item.quantity);
        });
        for (const p of state.products) {
          if (!p.trackStock) continue;
          const orderedQty = orderedByName[String(p.name || "").trim().toLowerCase()] || 0;
          if (orderedQty > 0) {
            const nextQty = Math.max(0, Number(p.stockQuantity || 0) - orderedQty);
            await updateDoc(doc(db, "products", p.id), {
              stockQuantity: nextQty,
              available: nextQty > 0,
              updatedAt: serverTimestamp(),
            });
          }
        }
      }
      showToast(`Order marked ${nextStatus}`);
    };
  });
  el.querySelectorAll(".adm-paid-btn").forEach((btn) => {
    btn.onclick = async () => {
      await updateDoc(doc(db, "requests", btn.dataset.id), { paymentStatus: "PAID", updatedAt: serverTimestamp() });
      showToast("Marked Paid.");
    };
  });
}

function renderAdminCustomers() {
  const el = document.getElementById("admin-customers-list");
  if (!el) return;
  const q = (document.getElementById("adm-cust-search")?.value || "").trim().toLowerCase();
  const filtered = state.allUsers.filter((u) =>
    !q ||
    (u.displayName || "").toLowerCase().includes(q) ||
    (u.phone || "").toLowerCase().includes(q) ||
    (u.email || "").toLowerCase().includes(q)
  );
  el.innerHTML = filtered.map((u) => {
    const orders = state.allRequests.filter((r) => r.userId === (u.userId || u.id));
    const spent = orders.filter((r) => r.status !== "REJECTED").reduce((s, r) => s + (r.grandTotal || 0), 0);
    return `<div class="request-row">
      <div>
        <strong>${escapeHtml(u.displayName || "Customer")}</strong> ${u.deletionRequested ? `<span class="status-pill status-closed">Deletion Req</span>` : ""}
        <div class="muted">Mobile: ${escapeHtml(u.phone || "Not set")} • Email: ${escapeHtml(u.email || "N/A")}</div>
      </div>
      <div><strong>${orders.length} Orders • ₹${spent}</strong></div>
    </div>`;
  }).join("") || `<p class="muted">No customers match.</p>`;
}

function filterAdminOrdersList(list, rawQuery, statusFilter = "ALL") {
  const trimmed = String(rawQuery || "").trim();
  const q = trimmed.toLowerCase();
  const queryDigits = trimmed.replace(/\D/g, "");
  const wantedStatus = String(statusFilter || "ALL").toUpperCase();
  return list.filter((r) => {
    if (wantedStatus !== "ALL" && String(r.status || "PENDING").toUpperCase() !== wantedStatus) {
      return false;
    }
    if (!q) return true;
    const phoneDigits = String(r.contactPhone || "").replace(/\D/g, "");
    return (
      String(r.customerName || "").toLowerCase().includes(q) ||
      String(r.contactPhone || "").toLowerCase().includes(q) ||
      (queryDigits && phoneDigits.includes(queryDigits)) ||
      String(r.orderNumber || "").toLowerCase().includes(q) ||
      String(r.deliveryLocality || "").toLowerCase().includes(q) ||
      String(r.deliveryAddress || "").toLowerCase().includes(q)
    );
  });
}

function renderAdminShopOrders() {
  const shopOrders = state.allRequests.filter((r) => r.category === "FOOD_REQUEST");
  const searchEl = document.getElementById("adm-shop-order-search");
  const statusEl = document.getElementById("adm-shop-order-status");
  const clearEl = document.getElementById("adm-shop-order-clear");
  const q = searchEl ? searchEl.value : "";
  const statusVal = statusEl ? statusEl.value : "ALL";
  if (clearEl) clearEl.classList.toggle("hidden", !q.trim() && statusVal === "ALL");
  const filtered = filterAdminOrdersList(shopOrders, q, statusVal);
  const emptyMsg = shopOrders.length && (q.trim() || statusVal !== "ALL")
    ? "No matching shop orders found for the current search/status filter."
    : "No shop/counter orders yet.";
  renderSeparatedOrderRows("admin-shop-orders-list", filtered, emptyMsg);
}

function renderAdminDeliveryOrders() {
  const delivOrders = state.allRequests.filter((r) => r.category === "FOOD_DELIVERY_REQUEST");
  const searchEl = document.getElementById("adm-deliv-order-search");
  const statusEl = document.getElementById("adm-deliv-order-status");
  const clearEl = document.getElementById("adm-deliv-order-clear");
  const q = searchEl ? searchEl.value : "";
  const statusVal = statusEl ? statusEl.value : "ALL";
  if (clearEl) clearEl.classList.toggle("hidden", !q.trim() && statusVal === "ALL");
  const filtered = filterAdminOrdersList(delivOrders, q, statusVal);
  const emptyMsg = delivOrders.length && (q.trim() || statusVal !== "ALL")
    ? "No matching delivery orders found for the current search/status filter."
    : "No Khatu delivery orders yet.";
  renderSeparatedOrderRows("admin-delivery-orders-list", filtered, emptyMsg);
}

function getIstDateKey(ts) {
  const d = ts && typeof ts.toDate === "function" ? ts.toDate() : (ts instanceof Date ? ts : null);
  if (!d) return "";
  return d.toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
}

function getTodayIstDateKey() {
  return new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
}

function shiftIstDateKey(dateKey, deltaDays) {
  const [y, m, d] = dateKey.split("-").map(Number);
  if (!y || !m || !d) return dateKey;
  return new Date(Date.UTC(y, m - 1, d + deltaDays)).toISOString().slice(0, 10);
}

function formatIstKeyDisplay(dateKey) {
  const [y, m, d] = String(dateKey || "").split("-");
  return (y && m && d) ? `${d}/${m}/${y}` : "";
}

function renderAdminSalesReport() {
  const kpisEl = document.getElementById("adm-report-kpis");
  const productsEl = document.getElementById("adm-report-products");
  const labelEl = document.getElementById("adm-report-range-label");
  const startInput = document.getElementById("adm-report-start");
  const endInput = document.getElementById("adm-report-end");
  const resetBtn = document.getElementById("adm-report-reset");
  if (!kpisEl || !productsEl) return;

  document.querySelectorAll("[data-report-period]").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.reportPeriod === state.reportPeriod);
  });

  const todayKey = getTodayIstDateKey();
  let minKey = todayKey;
  let maxKey = "9999-12-31";
  let rangeText = `Today (${formatIstKeyDisplay(todayKey)})`;

  if (state.reportPeriod === "WEEKLY") {
    minKey = shiftIstDateKey(todayKey, -6);
    rangeText = `Last 7 Days (${formatIstKeyDisplay(minKey)} – ${formatIstKeyDisplay(todayKey)})`;
  } else if (state.reportPeriod === "MONTHLY") {
    minKey = shiftIstDateKey(todayKey, -29);
    rangeText = `Last 30 Days (${formatIstKeyDisplay(minKey)} – ${formatIstKeyDisplay(todayKey)})`;
  } else if (state.reportPeriod === "CUSTOM_RANGE") {
    const rawStart = state.reportStartDate || state.reportEndDate || todayKey;
    const rawEnd = state.reportEndDate || state.reportStartDate || todayKey;
    minKey = rawStart <= rawEnd ? rawStart : rawEnd;
    maxKey = rawStart <= rawEnd ? rawEnd : rawStart;
    if (startInput && startInput.value !== minKey) startInput.value = minKey;
    if (endInput && endInput.value !== maxKey) endInput.value = maxKey;
    rangeText = `Custom Range (${formatIstKeyDisplay(minKey)} – ${formatIstKeyDisplay(maxKey)})`;
  }

  if (resetBtn) {
    const hasCustom = Boolean(state.reportStartDate || state.reportEndDate || state.reportPeriod === "CUSTOM_RANGE");
    resetBtn.classList.toggle("hidden", !hasCustom);
  }
  if (labelEl) labelEl.textContent = `Showing IST Period: ${rangeText}`;

  const scoped = state.allRequests.filter((r) => {
    const key = getIstDateKey(r.createdAt);
    return key && key >= minKey && key <= maxKey;
  });
  const active = scoped.filter((r) => r.status !== "REJECTED");
  const totalSales = active.reduce((s, r) => s + (r.grandTotal || 0), 0);
  const completedOrders = scoped.filter((r) => r.status === "COMPLETED").length;
  const cancelledOrders = scoped.filter((r) => r.status === "REJECTED").length;
  const shopSales = active.filter((r) => r.category === "FOOD_REQUEST").reduce((s, r) => s + (r.grandTotal || 0), 0);
  const deliverySales = active.filter((r) => r.category === "FOOD_DELIVERY_REQUEST").reduce((s, r) => s + (r.grandTotal || 0), 0);
  const roomSales = active.filter((r) => r.category === "ROOM_REQUEST" || r.category === "FRESHEN_UP_REQUEST").reduce((s, r) => s + (r.grandTotal || 0), 0);
  const cashSales = active.filter((r) => r.paymentMethod !== "UPI").reduce((s, r) => s + (r.grandTotal || 0), 0);
  const upiSales = active.filter((r) => r.paymentMethod === "UPI").reduce((s, r) => s + (r.grandTotal || 0), 0);

  kpisEl.innerHTML = `
    <div class="kpi-box"><span>Filtered Revenue</span><strong>₹${totalSales}</strong></div>
    <div class="kpi-box"><span>Total Orders</span><strong>${scoped.length}</strong></div>
    <div class="kpi-box"><span>Completed / Cancelled</span><strong>${completedOrders} / ${cancelledOrders}</strong></div>
    <div class="kpi-box"><span>Shop Counter Sales</span><strong>₹${shopSales}</strong></div>
    <div class="kpi-box"><span>Delivery Sales</span><strong>₹${deliverySales}</strong></div>
    <div class="kpi-box"><span>Rooms &amp; Freshen</span><strong>₹${roomSales}</strong></div>
    <div class="kpi-box"><span>Cash / UPI</span><strong>₹${cashSales} / ₹${upiSales}</strong></div>
  `;

  const productMap = {};
  active.forEach((r) => {
    (r.items || []).forEach((item) => {
      const name = String(item.name || "").trim();
      const qty = Number(item.quantity || 0);
      const rev = Number(item.lineTotal || 0);
      if (name && qty > 0) {
        if (!productMap[name]) productMap[name] = { name, qty: 0, rev: 0 };
        productMap[name].qty += qty;
        productMap[name].rev += rev;
      }
    });
  });
  const sortedProducts = Object.values(productMap).sort((a, b) => (b.qty - a.qty) || (b.rev - a.rev) || a.name.localeCompare(b.name));
  productsEl.innerHTML = sortedProducts.length
    ? sortedProducts.slice(0, 12).map((p) => `
      <div class="request-row">
        <div><strong>${escapeHtml(p.name)}</strong> <span class="muted">(${p.qty} units sold)</span></div>
        <strong>₹${p.rev}</strong>
      </div>`).join("")
    : `<p class="muted">No orders recorded in this date range.</p>`;
}

function renderAdminDashboard() {
  const section = document.getElementById("owner-admin-section");
  section.classList.toggle("hidden", !state.isOwner);
  if (!state.isOwner) return;

  const activeOrders = state.allRequests.filter((r) => r.status !== "REJECTED");
  const shopOrders = state.allRequests.filter((r) => r.category === "FOOD_REQUEST");
  const delivOrders = state.allRequests.filter((r) => r.category === "FOOD_DELIVERY_REQUEST");
  const roomAndFreshen = state.allRequests.filter((r) => r.category === "ROOM_REQUEST" || r.category === "FRESHEN_UP_REQUEST");

  const nowIstDate = new Date().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" });
  const todaysActiveOrders = activeOrders.filter((r) => {
    if (!r.createdAt || typeof r.createdAt.toDate !== "function") return false;
    return r.createdAt.toDate().toLocaleDateString("en-CA", { timeZone: "Asia/Kolkata" }) === nowIstDate;
  });
  const todaysSales = todaysActiveOrders.reduce((s, r) => s + (r.grandTotal || 0), 0);

  const productStatsMap = {};
  activeOrders.forEach((r) => {
    (r.items || []).forEach((item) => {
      const name = String(item.name || "").trim();
      const qty = Number(item.quantity || 0);
      const rev = Number(item.lineTotal || 0);
      if (name && qty > 0) {
        if (!productStatsMap[name]) productStatsMap[name] = { name, qty: 0, rev: 0 };
        productStatsMap[name].qty += qty;
        productStatsMap[name].rev += rev;
      }
    });
  });
  const topProduct = Object.values(productStatsMap).sort((a, b) => (b.qty - a.qty) || (b.rev - a.rev) || a.name.localeCompare(b.name))[0] || null;

  const shopSales = shopOrders.filter((r) => r.status !== "REJECTED").reduce((s, r) => s + (r.grandTotal || 0), 0);
  const delivSales = delivOrders.filter((r) => r.status !== "REJECTED").reduce((s, r) => s + (r.grandTotal || 0), 0);
  const pending = state.allRequests.filter((r) => ["PENDING", "CONFIRMED", "PREPARING", "READY"].includes(r.status)).length;
  const completed = state.allRequests.filter((r) => r.status === "COMPLETED").length;
  const unread = state.notifications.filter((n) => !n.read).length;

  const shopPendingNew = shopOrders.filter((r) => (r.status || "PENDING") === "PENDING").length;
  const delivPendingNew = delivOrders.filter((r) => (r.status || "PENDING") === "PENDING").length;
  const roomsPendingNew = roomAndFreshen.filter((r) => (r.status || "PENDING") === "PENDING").length;
  const totalPendingNew = shopPendingNew + delivPendingNew + roomsPendingNew;
  const activePendingIds = new Set(
    state.allRequests.filter((r) => (r.status || "PENDING") === "PENDING").map((r) => r.id)
  );
  state.newlyArrivedOrderIds.forEach((id) => {
    if (!activePendingIds.has(id)) state.newlyArrivedOrderIds.delete(id);
  });
  const hasNewArrivals = state.newlyArrivedOrderIds.size > 0;

  const newOrdersPill = document.getElementById("admin-new-orders-pill");
  const newOrdersText = document.getElementById("admin-new-orders-text");
  if (newOrdersPill && newOrdersText) {
    newOrdersPill.classList.toggle("hidden", totalPendingNew === 0 && !hasNewArrivals);
    newOrdersPill.classList.toggle("pulse-new", hasNewArrivals);
    newOrdersText.textContent = hasNewArrivals
      ? `${state.newlyArrivedOrderIds.size} New • ${totalPendingNew} Pending`
      : `${totalPendingNew} Pending Order${totalPendingNew === 1 ? "" : "s"}`;
  }

  const syncTabBadge = (elId, count, isFresh) => {
    const el = document.getElementById(elId);
    if (!el) return;
    el.classList.toggle("hidden", count <= 0);
    el.classList.toggle("is-new", Boolean(isFresh && count > 0));
    el.textContent = count > 99 ? "99+" : String(count);
  };
  syncTabBadge("adm-badge-shop", shopPendingNew, hasNewArrivals);
  syncTabBadge("adm-badge-delivery", delivPendingNew, hasNewArrivals);
  syncTabBadge("adm-badge-rooms", roomsPendingNew, hasNewArrivals);

  const navSettingsBadge = document.getElementById("nav-settings-badge");
  if (navSettingsBadge) {
    navSettingsBadge.classList.toggle("hidden", totalPendingNew <= 0);
    navSettingsBadge.textContent = totalPendingNew > 99 ? "99+" : String(totalPendingNew);
  }

  const evaluateLowStockItems = () => {
    return state.products
      .filter((p) => Boolean(p.trackStock))
      .map((p) => {
        const qty = Math.max(0, Number(p.stockQuantity ?? 0));
        const threshold = Math.max(0, Number(p.lowStockThreshold ?? 5));
        const severity = qty <= 0 ? "OUT_OF_STOCK" : (qty <= threshold ? "CRITICAL_LOW" : "HEALTHY");
        return { product: p, qty, threshold, severity };
      })
      .filter((x) => x.severity !== "HEALTHY")
      .sort((a, b) => (a.severity === "OUT_OF_STOCK" ? -1 : 1) - (b.severity === "OUT_OF_STOCK" ? -1 : 1) || a.qty - b.qty);
  };

  const flaggedStock = evaluateLowStockItems();
  const outStockCount = flaggedStock.filter((x) => x.severity === "OUT_OF_STOCK").length;
  const lowStockPill = document.getElementById("admin-low-stock-pill");
  const lowStockText = document.getElementById("admin-low-stock-text");
  if (lowStockPill && lowStockText) {
    lowStockPill.classList.toggle("hidden", flaggedStock.length === 0);
    lowStockPill.classList.toggle("is-critical", outStockCount > 0);
    lowStockText.textContent = `${flaggedStock.length} Low Stock`;
  }
  syncTabBadge("adm-badge-stock", flaggedStock.length, outStockCount > 0);

  document.getElementById("admin-unread-pill").textContent = `${unread} Alerts`;
  document.getElementById("admin-kpis").innerHTML = `
    <div class="kpi-box"><span>Total Today's Sales (IST)</span><strong>₹${todaysSales}</strong></div>
    <div class="kpi-box"><span>Total Pending Orders</span><strong>${pending}</strong></div>
    <div class="kpi-box"><span>Most Popular Product</span><strong>${topProduct ? `${escapeHtml(topProduct.name)} (${topProduct.qty} sold)` : "No orders yet"}</strong></div>
    <div class="kpi-box"><span>Shop Orders (${shopOrders.length})</span><strong>₹${shopSales}</strong></div>
    <div class="kpi-box"><span>Delivery Orders (${delivOrders.length})</span><strong>₹${delivSales}</strong></div>
    <div class="kpi-box"><span>Rooms &amp; Freshen</span><strong>${roomAndFreshen.length}</strong></div>
    <div class="kpi-box"><span>Completed</span><strong>${completed}</strong></div>
    <div class="kpi-box"><span>Customers</span><strong>${state.allUsers.length}</strong></div>
  `;

  const watchlistEl = document.getElementById("admin-low-stock-watchlist");
  if (watchlistEl) {
    watchlistEl.classList.toggle("has-alerts", flaggedStock.length > 0);
    watchlistEl.classList.toggle("has-critical", outStockCount > 0);
    if (!flaggedStock.length) {
      watchlistEl.innerHTML = `
        <div class="admin-header" style="margin-bottom:4px;">
          <h4>Inventory Health (All Good)</h4>
          <button type="button" class="btn-outline-gold adm-jump-stock">Manage Stock</button>
        </div>
        <p class="muted" style="margin:0;">No tracked menu items are at or below their critical threshold.</p>
      `;
    } else {
      watchlistEl.innerHTML = `
        <div class="admin-header" style="margin-bottom:6px;">
          <div>
            <h4>Low-Stock Alerts (${flaggedStock.length} Flagged)</h4>
            <p class="muted" style="margin:2px 0 0;">${outStockCount} out of stock • ${flaggedStock.length - outStockCount} at critical threshold</p>
          </div>
          <button type="button" class="btn-outline-gold adm-jump-stock">Manage Stock</button>
        </div>
        <div class="request-list">
          ${flaggedStock.map((item) => `
            <div class="request-row ${item.severity === "OUT_OF_STOCK" ? "out-of-stock-row" : "low-stock-row"}">
              <div>
                <strong>${escapeHtml(item.product.name)}</strong>
                <span class="order-status-badge ${item.severity === "OUT_OF_STOCK" ? "badge-rejected" : "badge-pending"}">
                  ${item.severity === "OUT_OF_STOCK" ? "OUT OF STOCK" : `LOW STOCK (${item.qty})`}
                </span>
                <div class="muted">Qty: ${item.qty} • Critical Threshold <= ${item.threshold}</div>
              </div>
              <button type="button" class="btn-outline-gold adm-quick-restock" data-id="${escapeHtml(item.product.id)}">+10 Restock</button>
            </div>
          `).join("")}
        </div>
      `;
    }
    watchlistEl.querySelectorAll(".adm-jump-stock").forEach((b) => {
      b.onclick = () => switchAdminSubTab("menu");
    });
    watchlistEl.querySelectorAll(".adm-quick-restock").forEach((b) => {
      b.onclick = async () => {
        const prod = state.products.find((x) => x.id === b.dataset.id);
        if (!prod) return;
        await updateDoc(doc(db, "products", prod.id), {
          stockQuantity: Number(prod.stockQuantity || 0) + 10,
          available: true,
          updatedAt: serverTimestamp(),
        });
        showToast(`${prod.name} restocked +10.`);
      };
    });
  }

  renderAdminShopOrders();
  renderAdminDeliveryOrders();
  renderSeparatedOrderRows("admin-rooms-freshen-list", roomAndFreshen, "No room or freshen-up requests yet.");
  renderAdminCustomers();
  renderAdminSalesReport();

  // Categories
  const cats = state.siteConfig.categories || DEFAULT_CATEGORIES;
  document.getElementById("admin-categories-chips").innerHTML = cats
    .map((c) => `<button type="button" class="cat-chip active adm-remove-cat" data-cat="${escapeHtml(c)}">${escapeHtml(c)} ✕</button>`)
    .join("");
  document.querySelectorAll(".adm-remove-cat").forEach((btn) => {
    btn.onclick = async () => {
      if (cats.length <= 1) return showToast("Keep at least one category.");
      const next = cats.filter((x) => x !== btn.dataset.cat);
      await saveSiteConfigToFirestore({ ...state.siteConfig, categories: next });
      showToast("Category removed.");
    };
  });

  // Payment & Timings form sync
  document.getElementById("adm-cash-enabled").checked = state.shopSettings.cashEnabled !== false;
  document.getElementById("adm-upi-enabled").checked = Boolean(state.shopSettings.upiEnabled);
  document.getElementById("adm-upi-id").value = state.shopSettings.upiId || "";
  document.getElementById("adm-open-time").value = state.shopSettings.openingTime || "7:00 AM";
  document.getElementById("adm-close-time").value = state.shopSettings.closingTime || "10:00 PM";
  document.getElementById("adm-deliv-charge").value = state.deliverySettings.deliveryCharge || 0;
  document.getElementById("adm-pkg-charge").value = state.deliverySettings.packagingCharge || 0;

  // Design form sync
  const draft = state.siteConfig.draftTheme || THEME_PRESETS.NAVY_ROYAL_GOLD;
  document.getElementById("adm-color-primary").value = draft.primaryColorHex || "#E5B94C";
  document.getElementById("adm-color-bg").value = draft.backgroundColorHex || "#07122B";
  document.getElementById("adm-color-surface").value = draft.surfaceColorHex || "#142652";
  document.getElementById("adm-color-accent").value = draft.accentColorHex || "#F7D774";
  document.getElementById("adm-color-text").value = draft.textColorHex || "#F8F6F0";
  document.getElementById("adm-font-style").value = draft.fontStyle || "DEVOTIONAL_SERIF";
  document.getElementById("adm-hero-url").value = state.siteConfig.heroBannerUrl || "";
  document.getElementById("adm-logo-url").value = state.siteConfig.logoUrl || "";
  document.getElementById("adm-show-ann").checked = state.siteConfig.showAnnouncementsSection !== false;
  document.getElementById("adm-show-specials").checked = state.siteConfig.showDailySpecialsSection !== false;
  document.getElementById("adm-show-rooms").checked = state.siteConfig.showRoomsSection !== false;
  document.getElementById("adm-show-delivery").checked = state.siteConfig.showDeliverySection !== false;

  const flaggedIdsSet = new Set(flaggedStock.map((x) => x.product.id));
  const filterAllBtn = document.getElementById("adm-stock-filter-all");
  const filterFlaggedBtn = document.getElementById("adm-stock-filter-flagged");
  if (filterAllBtn) {
    filterAllBtn.textContent = `All Items (${state.products.length})`;
    filterAllBtn.classList.toggle("active", state.stockFilter !== "FLAGGED");
  }
  if (filterFlaggedBtn) {
    filterFlaggedBtn.textContent = `Low Stock Flagged (${flaggedStock.length})`;
    filterFlaggedBtn.classList.toggle("active", state.stockFilter === "FLAGGED");
  }

  const displayedProducts = state.stockFilter === "FLAGGED"
    ? state.products.filter((p) => flaggedIdsSet.has(p.id))
    : state.products;

  document.getElementById("admin-menu-list").innerHTML = displayedProducts.length ? displayedProducts.map((p) => {
    const qty = Math.max(0, Number(p.stockQuantity ?? 20));
    const threshold = Math.max(0, Number(p.lowStockThreshold ?? 5));
    const track = Boolean(p.trackStock);
    const isOut = track && qty <= 0;
    const isLow = track && qty > 0 && qty <= threshold;
    const rowCls = isOut ? "out-of-stock-row" : (isLow ? "low-stock-row" : "");
    const statusBadge = isOut
      ? `<span class="order-status-badge badge-rejected">OUT OF STOCK (0 / min ${threshold})</span>`
      : (isLow
          ? `<span class="order-status-badge badge-pending">LOW STOCK (${qty} / min ${threshold})</span>`
          : (track
              ? `<span class="order-status-badge badge-completed">In Stock (${qty})</span>`
              : `<span class="badge-gold">Unlimited</span>`));

    return `
    <div class="request-row ${rowCls}">
      <div style="flex:1;min-width:220px;">
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:4px;">
          <strong>${escapeHtml(p.name)}</strong>
          <span class="muted">(${escapeHtml(p.category)})</span>
          ${statusBadge}
        </div>
        <div class="inline-actions" style="align-items:center;margin-top:6px;">
          <label class="consent-row" style="margin:0;">
            <input type="checkbox" class="adm-track-stock" data-id="${escapeHtml(p.id)}" ${track ? "checked" : ""} />
            <span>Track Stock</span>
          </label>
          ${track ? `
            <div class="qty-controls">
              <button type="button" class="qty-btn adm-stock-step" data-id="${escapeHtml(p.id)}" data-delta="-1">-</button>
              <strong>${qty}</strong>
              <button type="button" class="qty-btn adm-stock-step" data-id="${escapeHtml(p.id)}" data-delta="1">+</button>
              <button type="button" class="btn-outline-gold adm-stock-step" data-id="${escapeHtml(p.id)}" data-delta="10" style="min-height:34px;padding:4px 10px;">+10</button>
            </div>
            <label style="flex-direction:row;align-items:center;gap:6px;margin:0;">
              <span>Alert &lt;=</span>
              <input type="number" min="0" max="10000" class="stock-threshold-input adm-threshold-input" data-id="${escapeHtml(p.id)}" value="${threshold}" />
            </label>
          ` : ""}
        </div>
      </div>
      <button type="button" class="btn-outline-gold adm-toggle-prod" data-id="${escapeHtml(p.id)}">
        ${p.available ? "Available (Pause)" : "Paused (Enable)"}
      </button>
    </div>`;
  }).join("") : `<p class="muted">No menu items match the selected filter.</p>`;

  document.querySelectorAll(".adm-toggle-prod").forEach((btn) => {
    btn.onclick = async () => {
      const p = state.products.find((x) => x.id === btn.dataset.id);
      if (!p) return;
      await updateDoc(doc(db, "products", p.id), { available: !p.available, updatedAt: serverTimestamp() });
      showToast(`${p.name} updated`);
    };
  });
  document.querySelectorAll(".adm-track-stock").forEach((chk) => {
    chk.onchange = async () => {
      const p = state.products.find((x) => x.id === chk.dataset.id);
      if (!p) return;
      await updateDoc(doc(db, "products", p.id), {
        trackStock: chk.checked,
        lowStockThreshold: Number(p.lowStockThreshold ?? 5),
        updatedAt: serverTimestamp(),
      });
      showToast(`${p.name} stock tracking ${chk.checked ? "enabled" : "disabled"}.`);
    };
  });
  document.querySelectorAll(".adm-stock-step").forEach((btn) => {
    btn.onclick = async () => {
      const p = state.products.find((x) => x.id === btn.dataset.id);
      if (!p) return;
      const next = Math.max(0, Number(p.stockQuantity ?? 0) + Number(btn.dataset.delta || 0));
      await updateDoc(doc(db, "products", p.id), {
        stockQuantity: next,
        available: next > 0,
        updatedAt: serverTimestamp(),
      });
    };
  });
  document.querySelectorAll(".adm-threshold-input").forEach((inp) => {
    inp.onchange = async () => {
      const p = state.products.find((x) => x.id === inp.dataset.id);
      if (!p) return;
      const nextThreshold = Math.min(10000, Math.max(0, Number(inp.value || 0)));
      await updateDoc(doc(db, "products", p.id), {
        lowStockThreshold: nextThreshold,
        updatedAt: serverTimestamp(),
      });
      showToast(`${p.name} critical threshold set to ${nextThreshold}.`);
    };
  });

  document.getElementById("adm-security-session-info").textContent =
    `Verified Owner Session (${state.user?.email || ""}) • Protected by Firebase Auth + /admins RBAC + Zero-Trust Firestore Rules.`;

  document.getElementById("admin-logs-list").innerHTML = state.activityLogs.slice(0, 20).map((l) => `
    <div class="request-row">
      <div><strong>${escapeHtml(l.action)}</strong> (${escapeHtml(l.targetId)})<div class="muted">${escapeHtml(l.previousValue)} → ${escapeHtml(l.newValue)} • ${escapeHtml(l.istTimestampText)}</div></div>
    </div>`).join("") || `<p class="muted">No audit logs recorded yet.</p>`;
}

function readDesignFormSiteConfig() {
  const currentDraft = state.siteConfig.draftTheme || THEME_PRESETS.NAVY_ROYAL_GOLD;
  const draftTheme = {
    preset: currentDraft.preset || "CUSTOM",
    primaryColorHex: document.getElementById("adm-color-primary").value.toUpperCase(),
    backgroundColorHex: document.getElementById("adm-color-bg").value.toUpperCase(),
    surfaceColorHex: document.getElementById("adm-color-surface").value.toUpperCase(),
    accentColorHex: document.getElementById("adm-color-accent").value.toUpperCase(),
    textColorHex: document.getElementById("adm-color-text").value.toUpperCase(),
    fontStyle: document.getElementById("adm-font-style").value,
    layoutVariant: currentDraft.layoutVariant || "DEVOTIONAL_CARD",
  };
  return {
    ...state.siteConfig,
    logoUrl: document.getElementById("adm-logo-url").value.trim(),
    heroBannerUrl: document.getElementById("adm-hero-url").value.trim(),
    showAnnouncementsSection: document.getElementById("adm-show-ann").checked,
    showDailySpecialsSection: document.getElementById("adm-show-specials").checked,
    showRoomsSection: document.getElementById("adm-show-rooms").checked,
    showDeliverySection: document.getElementById("adm-show-delivery").checked,
    draftTheme,
  };
}

async function saveSiteConfigToFirestore(cfg) {
  await setDoc(doc(db, "site_config", "main"), {
    logoUrl: cfg.logoUrl || "",
    heroBannerUrl: cfg.heroBannerUrl || "",
    showAnnouncementsSection: cfg.showAnnouncementsSection !== false,
    showDailySpecialsSection: cfg.showDailySpecialsSection !== false,
    showRoomsSection: cfg.showRoomsSection !== false,
    showDeliverySection: cfg.showDeliverySection !== false,
    categories: (cfg.categories && cfg.categories.length) ? cfg.categories : DEFAULT_CATEGORIES,
    draftTheme: cfg.draftTheme,
    publishedTheme: cfg.publishedTheme,
    updatedAt: serverTimestamp(),
  }, { merge: true });
}

async function submitOrderTransactional(payload) {
  if (!state.user || !state.profile?.registrationCompleted) {
    document.getElementById("registration-card").classList.remove("hidden");
    showToast("Please complete registration first.");
    return null;
  }
  const now = Date.now();
  if (now - state.lastSubmitMs < 3000) {
    showToast("Please wait a moment before submitting again.");
    return null;
  }
  const shopEval = isShopCurrentlyOpen();
  if (!shopEval.isOpen) {
    showToast(shopEval.reason);
    return null;
  }

  // Enforce Cash Only when upiEnabled !== true
  const upiAllowed = Boolean(state.shopSettings.upiEnabled);
  const safeMethod = (upiAllowed && payload.paymentMethod === "UPI") ? "UPI" : "CASH";
  const cleanPayload = {
    ...payload,
    paymentMethod: safeMethod,
    paymentStatus: safeMethod === "UPI" ? "UPI_PENDING_VERIFICATION" : "UNPAID",
  };
  if (safeMethod !== "UPI") delete cleanPayload.upiTransactionRef;

  state.lastSubmitMs = now;
  const counterRef = doc(db, "counters", "orders");
  const reqRef = doc(collection(db, "requests"));
  const orderNumber = await runTransaction(db, async (tx) => {
    const cSnap = await tx.get(counterRef);
    const next = cSnap.exists() ? (cSnap.data().lastNumber || 0) + 1 : 1;
    if (cSnap.exists()) tx.update(counterRef, { lastNumber: next, updatedAt: serverTimestamp() });
    else tx.set(counterRef, { lastNumber: 1, updatedAt: serverTimestamp() });

    const num = `MD-${String(next).padStart(6, "0")}`;
    tx.set(reqRef, {
      ...cleanPayload,
      userId: state.user.uid,
      orderNumber: num,
      customerName: state.profile.displayName,
      contactPhone: state.profile.phone,
      customerEmail: state.user.email || "",
      status: "PENDING",
      whatsappRetryCount: 0,
      whatsappStatus: "NOT_CONFIGURED",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    return num;
  });
  return { id: reqRef.id, orderNumber, ...cleanPayload, customerName: state.profile.displayName, contactPhone: state.profile.phone };
}

function clearSubscriptions() {
  state.unsubscribes.forEach((u) => { try { u(); } catch (_) {} });
  state.unsubscribes = [];
}

function attachFirestoreListeners() {
  clearSubscriptions();
  if (!state.user) return;

  state.unsubscribes.push(
    onSnapshot(doc(db, "shop_settings", "main"), (snap) => {
      if (snap.exists()) state.shopSettings = { ...state.shopSettings, ...snap.data() };
      renderShopHeaderAndHome();
      renderAdminDashboard();
    }),
    onSnapshot(doc(db, "site_config", "main"), (snap) => {
      if (snap.exists()) state.siteConfig = { ...state.siteConfig, ...snap.data() };
      applySiteThemeAndSections();
      renderShopHeaderAndHome();
      renderMenusAndCarts();
      renderAdminDashboard();
    }),
    onSnapshot(doc(db, "delivery_settings", "main"), (snap) => {
      if (snap.exists()) state.deliverySettings = { ...state.deliverySettings, ...snap.data() };
      populateLocalitySelects();
      renderShopHeaderAndHome();
      renderMenusAndCarts();
    }),
    onSnapshot(collection(db, "announcements"), (snap) => {
      state.announcements = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderShopHeaderAndHome();
    }),
    onSnapshot(collection(db, "products"), (snap) => {
      if (!snap.empty) {
        state.products = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
      }
      if (state.isOwner) {
        const flagged = state.products.filter((p) => Boolean(p.trackStock) && Math.max(0, Number(p.stockQuantity ?? 0)) <= Math.max(0, Number(p.lowStockThreshold ?? 5)));
        const flaggedIds = new Set(flagged.map((p) => p.id));
        if (!state.lowStockInitialized) {
          state.knownLowStockProductIds = flaggedIds;
          state.lowStockInitialized = true;
        } else {
          const newlyFlagged = flagged.filter((p) => !state.knownLowStockProductIds.has(p.id));
          state.knownLowStockProductIds = flaggedIds;
          if (newlyFlagged.length) {
            const first = newlyFlagged[0];
            showToast(`LOW STOCK ALERT: ${first.name} (${Math.max(0, Number(first.stockQuantity ?? 0))} left)!`);
          }
        }
      }
      renderMenusAndCarts();
      renderAdminDashboard();
    }),
    onSnapshot(collection(db, "room_configs"), (snap) => {
      if (!snap.empty) state.roomConfigs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderRoomsAndFreshen();
    }),
    onSnapshot(collection(db, "freshen_up_configs"), (snap) => {
      if (!snap.empty) state.freshenConfigs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      updateRoomFreshenTotal();
    }),
    onSnapshot(doc(db, "users", state.user.uid), (snap) => {
      state.profile = snap.exists() ? snap.data() : null;
      const needsReg = !state.profile || !state.profile.registrationCompleted;
      document.getElementById("registration-card").classList.toggle("hidden", !needsReg);
      document.getElementById("request-deletion-btn").classList.toggle("hidden", !state.profile);
      if (state.profile) {
        document.getElementById("reg-name").value = state.profile.displayName || "";
        document.getElementById("reg-phone").value = state.profile.phone || "";
      }
    }),
    onSnapshot(query(collection(db, "requests"), where("userId", "==", state.user.uid), limit(30)), (snap) => {
      state.myRequests = snap.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => (b.createdAt?.seconds || 0) - (a.createdAt?.seconds || 0));
      renderMyRequests();
    })
  );

  if (state.isOwner) {
    state.adminRequestsInitialized = false;
    state.knownAdminRequestIds = new Set();
    state.newlyArrivedOrderIds = new Set();
    state.unsubscribes.push(
      onSnapshot(query(collection(db, "requests"), orderBy("createdAt", "desc"), limit(80)), (snap) => {
        const docs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        if (!state.adminRequestsInitialized) {
          docs.forEach((d) => state.knownAdminRequestIds.add(d.id));
          state.adminRequestsInitialized = true;
        } else {
          const addedPending = [];
          snap.docChanges().forEach((change) => {
            if (change.type === "added" && !state.knownAdminRequestIds.has(change.doc.id)) {
              const data = change.doc.data() || {};
              if ((data.status || "PENDING") === "PENDING") {
                state.newlyArrivedOrderIds.add(change.doc.id);
                addedPending.push({ id: change.doc.id, ...data });
              }
            }
          });
          docs.forEach((d) => state.knownAdminRequestIds.add(d.id));
          if (addedPending.length) {
            const first = addedPending[0];
            showToast(`New Order ${first.orderNumber || ""} from ${first.customerName || "Customer"}!`);
          }
        }
        state.allRequests = docs;
        renderAdminDashboard();
      }),
      onSnapshot(query(collection(db, "users"), limit(100)), (snap) => {
        state.allUsers = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderAdminDashboard();
      }),
      onSnapshot(query(collection(db, "activity_logs"), orderBy("createdAt", "desc"), limit(40)), (snap) => {
        state.activityLogs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderAdminDashboard();
      }),
      onSnapshot(query(collection(db, "notifications"), orderBy("createdAt", "desc"), limit(25)), (snap) => {
        state.notifications = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        renderAdminDashboard();
      })
    );
  }
}

async function handleSignOut() {
  state.loginMode = "SELECTOR";
  state.ownerDeniedMsg = "";
  state.themePreviewActive = false;
  state.adminRequestsInitialized = false;
  state.knownAdminRequestIds.clear();
  state.newlyArrivedOrderIds.clear();
  await signOut(auth);
  showToast("Signed out.");
}

function wireEvents() {
  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.onclick = () => switchTab(btn.dataset.tab);
  });
  document.querySelectorAll("[data-jump-tab]").forEach((btn) => {
    btn.onclick = () => switchTab(btn.dataset.jumpTab);
  });
  document.querySelectorAll("[data-adm-tab]").forEach((btn) => {
    btn.onclick = () => {
      const targetTab = btn.dataset.admTab;
      if (targetTab === "shop-orders" || targetTab === "delivery-orders" || targetTab === "rooms-freshen") {
        state.newlyArrivedOrderIds.clear();
        renderAdminDashboard();
      }
      switchAdminSubTab(targetTab);
    };
  });

  const adminNewOrdersPill = document.getElementById("admin-new-orders-pill");
  if (adminNewOrdersPill) {
    adminNewOrdersPill.onclick = () => {
      const hasShop = state.allRequests.some((r) => r.category === "FOOD_REQUEST" && (r.status || "PENDING") === "PENDING");
      const hasDeliv = state.allRequests.some((r) => r.category === "FOOD_DELIVERY_REQUEST" && (r.status || "PENDING") === "PENDING");
      const target = hasShop ? "shop-orders" : (hasDeliv ? "delivery-orders" : "rooms-freshen");
      state.newlyArrivedOrderIds.clear();
      switchAdminSubTab(target);
      renderAdminDashboard();
    };
  }
  const adminLowStockPill = document.getElementById("admin-low-stock-pill");
  if (adminLowStockPill) {
    adminLowStockPill.onclick = () => {
      state.stockFilter = "FLAGGED";
      switchAdminSubTab("menu");
      renderAdminDashboard();
    };
  }
  const stockFilterAll = document.getElementById("adm-stock-filter-all");
  const stockFilterFlagged = document.getElementById("adm-stock-filter-flagged");
  if (stockFilterAll) {
    stockFilterAll.onclick = () => {
      state.stockFilter = "ALL";
      renderAdminDashboard();
    };
  }
  if (stockFilterFlagged) {
    stockFilterFlagged.onclick = () => {
      state.stockFilter = "FLAGGED";
      renderAdminDashboard();
    };
  }

  const scrollTopBtn = document.getElementById("scroll-to-top-btn");
  if (scrollTopBtn) {
    scrollTopBtn.onclick = () => window.scrollTo({ top: 0, behavior: "smooth" });
  }
  window.addEventListener("scroll", updateScrollToTopVisibility, { passive: true });
  window.addEventListener("resize", updateScrollToTopVisibility, { passive: true });

  document.getElementById("lang-toggle-btn").onclick = () => {
    state.lang = state.lang === "en" ? "hi" : "en";
    applyLanguage();
  };

  // Two-option login portal navigation
  document.getElementById("choose-customer-login-btn").onclick = () => {
    state.loginMode = "CUSTOMER";
    state.ownerDeniedMsg = "";
    renderLoginPortal();
  };
  document.getElementById("choose-owner-login-btn").onclick = () => {
    state.loginMode = "OWNER";
    state.ownerDeniedMsg = "";
    renderLoginPortal();
  };
  document.getElementById("back-from-customer-btn").onclick = () => {
    state.loginMode = "SELECTOR";
    renderLoginPortal();
  };
  document.getElementById("back-from-owner-btn").onclick = () => {
    state.loginMode = "SELECTOR";
    state.ownerDeniedMsg = "";
    renderLoginPortal();
  };

  document.getElementById("customer-google-signin-btn").onclick = async () => {
    state.loginMode = "CUSTOMER";
    await signInWithPopup(auth, googleProvider);
  };
  document.getElementById("owner-google-signin-btn").onclick = async () => {
    state.loginMode = "OWNER";
    state.ownerDeniedMsg = "";
    await signInWithPopup(auth, googleProvider);
  };

  document.getElementById("auth-btn").onclick = async () => {
    if (state.user) await handleSignOut();
    else {
      document.getElementById("login-portal-card")?.scrollIntoView({ behavior: "smooth" });
    }
  };
  document.getElementById("settings-signout-btn").onclick = handleSignOut;
  document.getElementById("admin-signout-btn").onclick = handleSignOut;

  document.getElementById("adm-cust-search").oninput = renderAdminCustomers;
  document.getElementById("adm-shop-order-search").oninput = renderAdminShopOrders;
  document.getElementById("adm-shop-order-status").onchange = renderAdminShopOrders;
  document.getElementById("adm-deliv-order-search").oninput = renderAdminDeliveryOrders;
  document.getElementById("adm-deliv-order-status").onchange = renderAdminDeliveryOrders;
  document.getElementById("adm-shop-order-clear").onclick = () => {
    document.getElementById("adm-shop-order-search").value = "";
    document.getElementById("adm-shop-order-status").value = "ALL";
    renderAdminShopOrders();
  };
  document.getElementById("adm-deliv-order-clear").onclick = () => {
    document.getElementById("adm-deliv-order-search").value = "";
    document.getElementById("adm-deliv-order-status").value = "ALL";
    renderAdminDeliveryOrders();
  };

  document.querySelectorAll("[data-report-period]").forEach((btn) => {
    btn.onclick = () => {
      state.reportPeriod = btn.dataset.reportPeriod;
      if (state.reportPeriod === "CUSTOM_RANGE" && !state.reportStartDate && !state.reportEndDate) {
        const today = getTodayIstDateKey();
        state.reportStartDate = today;
        state.reportEndDate = today;
      }
      renderAdminSalesReport();
    };
  });

  const applyCustomDates = () => {
    const startVal = document.getElementById("adm-report-start")?.value || "";
    const endVal = document.getElementById("adm-report-end")?.value || "";
    if (!startVal && !endVal) {
      state.reportPeriod = "DAILY";
    } else {
      const s = startVal || endVal;
      const e = endVal || startVal;
      state.reportStartDate = s <= e ? s : e;
      state.reportEndDate = s <= e ? e : s;
      state.reportPeriod = "CUSTOM_RANGE";
    }
    renderAdminSalesReport();
  };
  document.getElementById("adm-report-start").onchange = applyCustomDates;
  document.getElementById("adm-report-end").onchange = applyCustomDates;
  document.getElementById("adm-report-apply").onclick = applyCustomDates;
  document.getElementById("adm-report-reset").onclick = () => {
    state.reportPeriod = "DAILY";
    state.reportStartDate = "";
    state.reportEndDate = "";
    document.getElementById("adm-report-start").value = "";
    document.getElementById("adm-report-end").value = "";
    renderAdminSalesReport();
  };

  document.getElementById("pickup-payment").onchange = (e) => {
    document.getElementById("pickup-upi-ref-wrap").classList.toggle("hidden", e.target.value !== "UPI");
  };
  document.getElementById("deliv-payment").onchange = (e) => {
    document.getElementById("deliv-upi-ref-wrap").classList.toggle("hidden", e.target.value !== "UPI");
  };
  ["rf-mode", "rf-room-type", "rf-duration", "rf-room-count", "rf-freshen-opt", "rf-people"].forEach((id) => {
    document.getElementById(id).onchange = updateRoomFreshenTotal;
  });

  document.getElementById("registration-form").onsubmit = async (e) => {
    e.preventDefault();
    if (!state.user) return showToast("Please login first.");
    const name = document.getElementById("reg-name").value.trim();
    const rawPhone = document.getElementById("reg-phone").value.replace(/\D/g, "");
    const tenDigits = rawPhone.length === 12 && rawPhone.startsWith("91") ? rawPhone.slice(2) : rawPhone.slice(-10);
    if (tenDigits.length !== 10 || !/^[6-9]/.test(tenDigits)) {
      return showToast("Enter a valid 10-digit Indian mobile number.");
    }
    const phoneKey = `91${tenDigits}`;
    const locality = document.getElementById("reg-locality").value;
    const address = document.getElementById("reg-address").value.trim();
    const userRef = doc(db, "users", state.user.uid);
    const phoneRef = doc(db, "phone_registry", phoneKey);

    try {
      await runTransaction(db, async (tx) => {
        const pSnap = await tx.get(phoneRef);
        const uSnap = await tx.get(userRef);
        if (pSnap.exists() && pSnap.data().userId !== state.user.uid) {
          throw new Error("Mobile number is already registered with another account.");
        }
        if (!pSnap.exists()) tx.set(phoneRef, { userId: state.user.uid, createdAt: serverTimestamp() });
        const fields = {
          displayName: name,
          email: state.user.email || "",
          phone: tenDigits,
          preferredLanguage: state.lang,
          registrationCompleted: true,
          preferredFulfillment: "PICKUP",
          defaultLocality: locality,
          defaultAddress: address,
          consentAccepted: true,
          updatedAt: serverTimestamp(),
        };
        if (!uSnap.exists()) tx.set(userRef, { userId: state.user.uid, ...fields, deletionRequested: false, createdAt: serverTimestamp() });
        else tx.update(userRef, fields);
      });
      showToast("Profile saved.");
    } catch (err) {
      showToast(err.message || "Registration failed.");
    }
  };

  const orderSuccessTimers = new Map();
  const triggerOrderSummarySuccess = (cardId, bannerId, btnId, orderNumber, defaultBtnLabel) => {
    const prevTimers = orderSuccessTimers.get(cardId);
    if (prevTimers) {
      clearTimeout(prevTimers.btnTimer);
      clearTimeout(prevTimers.bannerTimer);
    }
    const card = document.getElementById(cardId);
    const banner = document.getElementById(bannerId);
    const btn = document.getElementById(btnId);
    if (card) card.classList.add("order-success-active");
    if (banner) {
      const idEl = banner.querySelector(".success-order-id");
      if (idEl) idEl.textContent = orderNumber || "";
      banner.classList.add("hidden");
      void banner.offsetWidth; // restart SVG stroke animations cleanly
      banner.classList.remove("hidden");
    }
    if (btn) {
      btn.classList.add("btn-success-state");
      btn.textContent = "✓ Order Placed";
    }
    const btnTimer = setTimeout(() => {
      if (btn) {
        btn.classList.remove("btn-success-state");
        btn.textContent = defaultBtnLabel;
      }
    }, 2200);
    const bannerTimer = setTimeout(() => {
      if (card) card.classList.remove("order-success-active");
      if (banner) banner.classList.add("hidden");
    }, 4200);
    orderSuccessTimers.set(cardId, { btnTimer, bannerTimer });
  };

  document.getElementById("pickup-order-form").onsubmit = async (e) => {
    e.preventDefault();
    if (!state.deliverySettings.pickupEnabled) return showToast("Pickup is currently disabled by owner.");
    const items = Object.values(state.pickupCart);
    if (!items.length) return showToast("Add at least one food item.");
    const foodTotal = items.reduce((s, x) => s + x.lineTotal, 0);
    const packagingCharge = state.deliverySettings.packagingCharge || 0;
    const upiAllowed = Boolean(state.shopSettings.upiEnabled);
    const payMethod = upiAllowed ? document.getElementById("pickup-payment").value : "CASH";
    const utr = upiAllowed ? document.getElementById("pickup-upi-ref").value.trim() : "";
    if (payMethod === "UPI" && utr.length < 4) return showToast("Enter valid UPI UTR reference.");

    const created = await submitOrderTransactional({
      category: "FOOD_REQUEST",
      items,
      foodTotal,
      roomTotal: 0,
      freshenUpTotal: 0,
      deliveryCharge: 0,
      packagingCharge,
      grandTotal: foodTotal + packagingCharge,
      expectedTime: document.getElementById("pickup-time").value.trim(),
      fulfillmentType: "PICKUP",
      paymentMethod: payMethod,
      ...(utr ? { upiTransactionRef: utr } : {}),
      ...(document.getElementById("pickup-notes").value.trim() ? { notes: document.getElementById("pickup-notes").value.trim() } : {}),
    });
    if (created) {
      state.pickupCart = {};
      renderMenusAndCarts();
      triggerOrderSummarySuccess("pickup-checkout-card", "pickup-order-success", "pickup-submit-btn", created.orderNumber, "Place Food Order");
      showToast(`Order ${created.orderNumber} submitted.`);
      setTimeout(() => openReceiptModal(created), 900);
    }
  };

  document.getElementById("delivery-order-form").onsubmit = async (e) => {
    e.preventDefault();
    if (!state.deliverySettings.deliveryEnabled) return showToast("Delivery is paused by owner.");
    const items = Object.values(state.deliveryCart);
    if (!items.length) return showToast("Add food items for delivery.");
    const foodTotal = items.reduce((s, x) => s + x.lineTotal, 0);
    if (foodTotal < (state.deliverySettings.minimumOrderAmount || 0)) {
      return showToast(`Minimum order for delivery is ₹${state.deliverySettings.minimumOrderAmount}.`);
    }
    const addr = document.getElementById("deliv-address").value.trim();
    const loc = document.getElementById("deliv-locality").value;
    if (/jaipur|sikar|delhi|ajmer|ringas/i.test(addr) && !/khatu/i.test(addr)) {
      return showToast("Delivery is only available within Khatu Shyam Ji.");
    }
    const deliveryCharge = state.deliverySettings.deliveryCharge || 0;
    const packagingCharge = state.deliverySettings.packagingCharge || 0;
    const upiAllowed = Boolean(state.shopSettings.upiEnabled);
    const payMethod = upiAllowed ? document.getElementById("deliv-payment").value : "CASH";
    const utr = upiAllowed ? document.getElementById("deliv-upi-ref").value.trim() : "";
    if (payMethod === "UPI" && utr.length < 4) return showToast("Enter valid UPI UTR reference.");

    const created = await submitOrderTransactional({
      category: "FOOD_DELIVERY_REQUEST",
      items,
      foodTotal,
      roomTotal: 0,
      freshenUpTotal: 0,
      deliveryCharge,
      packagingCharge,
      grandTotal: foodTotal + deliveryCharge + packagingCharge,
      expectedTime: document.getElementById("deliv-time").value.trim(),
      fulfillmentType: "DELIVERY",
      deliveryLocality: loc,
      deliveryAddress: addr,
      paymentMethod: payMethod,
      ...(utr ? { upiTransactionRef: utr } : {}),
    });
    if (created) {
      state.deliveryCart = {};
      renderMenusAndCarts();
      triggerOrderSummarySuccess("delivery-checkout-card", "delivery-order-success", "deliv-submit-btn", created.orderNumber, "Place Delivery Order");
      showToast(`Delivery Order ${created.orderNumber} placed.`);
      setTimeout(() => openReceiptModal(created), 900);
    }
  };

  document.getElementById("room-freshen-form").onsubmit = async (e) => {
    e.preventDefault();
    const mode = document.getElementById("rf-mode").value;
    const people = Number(document.getElementById("rf-people").value || 1);
    const total = updateRoomFreshenTotal();
    const time = document.getElementById("rf-time").value.trim();
    const payload = mode === "ROOM"
      ? {
          category: "ROOM_REQUEST",
          items: [],
          foodTotal: 0,
          roomTotal: total,
          freshenUpTotal: 0,
          deliveryCharge: 0,
          packagingCharge: 0,
          grandTotal: total,
          expectedTime: time,
          fulfillmentType: "PICKUP",
          paymentMethod: "CASH",
          roomType: document.getElementById("rf-room-type").value,
          numberOfRooms: Number(document.getElementById("rf-room-count").value || 1),
          numberOfPeople: people,
          roomDurationHours: Number(document.getElementById("rf-duration").value),
          roomRateConfigured: total > 0,
        }
      : {
          category: "FRESHEN_UP_REQUEST",
          items: [],
          foodTotal: 0,
          roomTotal: 0,
          freshenUpTotal: total,
          deliveryCharge: 0,
          packagingCharge: 0,
          grandTotal: total,
          expectedTime: time,
          fulfillmentType: "PICKUP",
          paymentMethod: "CASH",
          freshenUpOption: document.getElementById("rf-freshen-opt").value,
          numberOfPeople: people,
        };
    const created = await submitOrderTransactional(payload);
    if (created) {
      triggerOrderSummarySuccess("rf-checkout-card", "rf-order-success", "rf-submit-btn", created.orderNumber, "Submit Booking Request");
      showToast(`Request ${created.orderNumber} submitted.`);
      setTimeout(() => openReceiptModal(created), 900);
    }
  };

  document.getElementById("request-deletion-btn").onclick = async () => {
    if (!state.user) return;
    await updateDoc(doc(db, "users", state.user.uid), { deletionRequested: true, updatedAt: serverTimestamp() });
    showToast("Account deletion request recorded.");
  };

  document.getElementById("admin-add-category-form").onsubmit = async (e) => {
    e.preventDefault();
    if (!state.isOwner) return;
    const val = document.getElementById("adm-new-category").value.trim();
    if (!val) return;
    const cats = state.siteConfig.categories || DEFAULT_CATEGORIES;
    if (!cats.includes(val)) {
      await saveSiteConfigToFirestore({ ...state.siteConfig, categories: [...cats, val].slice(0, 20) });
      document.getElementById("adm-new-category").value = "";
      showToast(`Category "${val}" added.`);
    }
  };

  document.getElementById("admin-payment-form").onsubmit = async (e) => {
    e.preventDefault();
    if (!state.isOwner) return;
    const upiEnabled = document.getElementById("adm-upi-enabled").checked;
    const cashEnabled = document.getElementById("adm-cash-enabled").checked || !upiEnabled;
    const upiId = document.getElementById("adm-upi-id").value.trim();
    await updateDoc(doc(db, "shop_settings", "main"), {
      cashEnabled,
      upiEnabled,
      upiId,
      updatedAt: serverTimestamp(),
    });
    showToast(upiEnabled ? "Cash + UPI Payment Enabled" : "Cash Only Payment Active");
  };

  document.querySelectorAll("[data-shop-mode]").forEach((btn) => {
    btn.onclick = async () => {
      if (!state.isOwner) return;
      const mode = btn.dataset.shopMode;
      await updateDoc(doc(db, "shop_settings", "main"), {
        shopControlMode: mode,
        shopOpen: mode !== "CLOSED_NOW",
        updatedAt: serverTimestamp(),
      });
      await addDoc(collection(db, "activity_logs"), {
        action: "SHOP_STATUS_CHANGED",
        targetType: "shop_settings",
        targetId: "main",
        previousValue: state.shopSettings.shopControlMode || "OPEN_NOW",
        newValue: mode,
        performedByEmail: state.user.email || "",
        performedByUid: state.user.uid,
        viaAi: false,
        istTimestampText: formatIst(null),
        createdAt: serverTimestamp(),
      });
      showToast(`Shop status set to ${mode}`);
    };
  });

  document.getElementById("admin-charges-form").onsubmit = async (e) => {
    e.preventDefault();
    if (!state.isOwner) return;
    await updateDoc(doc(db, "shop_settings", "main"), {
      openingTime: document.getElementById("adm-open-time").value.trim(),
      closingTime: document.getElementById("adm-close-time").value.trim(),
      updatedAt: serverTimestamp(),
    });
    await updateDoc(doc(db, "delivery_settings", "main"), {
      deliveryCharge: Number(document.getElementById("adm-deliv-charge").value || 0),
      packagingCharge: Number(document.getElementById("adm-pkg-charge").value || 0),
      updatedAt: serverTimestamp(),
    });
    showToast("Shop timings and charges updated.");
  };

  // Design Studio events
  document.querySelectorAll("[data-preset]").forEach((btn) => {
    btn.onclick = () => {
      const p = THEME_PRESETS[btn.dataset.preset];
      if (!p) return;
      state.siteConfig.draftTheme = { ...state.siteConfig.draftTheme, ...p };
      document.getElementById("adm-color-primary").value = p.primaryColorHex;
      document.getElementById("adm-color-bg").value = p.backgroundColorHex;
      document.getElementById("adm-color-surface").value = p.surfaceColorHex;
      document.getElementById("adm-color-accent").value = p.accentColorHex;
      document.getElementById("adm-color-text").value = p.textColorHex;
    };
  });

  document.getElementById("adm-save-draft-btn").onclick = async () => {
    if (!state.isOwner) return;
    const nextCfg = readDesignFormSiteConfig();
    await saveSiteConfigToFirestore(nextCfg);
    showToast("Design draft saved.");
  };

  document.getElementById("adm-preview-theme-btn").onclick = async () => {
    if (!state.isOwner) return;
    state.siteConfig = readDesignFormSiteConfig();
    state.themePreviewActive = !state.themePreviewActive;
    applySiteThemeAndSections();
    showToast(state.themePreviewActive ? "Live Theme Preview Active" : "Exited Live Preview");
  };

  document.getElementById("adm-publish-theme-btn").onclick = async () => {
    if (!state.isOwner) return;
    const nextCfg = readDesignFormSiteConfig();
    nextCfg.publishedTheme = { ...nextCfg.draftTheme };
    state.themePreviewActive = false;
    await saveSiteConfigToFirestore(nextCfg);
    showToast("Theme published live to App & Website!");
  };

  document.getElementById("close-receipt-btn").onclick = () => document.getElementById("receipt-dialog").close();
  document.getElementById("print-receipt-btn").onclick = () => window.print();
  document.getElementById("copy-receipt-btn").onclick = async () => {
    await navigator.clipboard.writeText(document.getElementById("receipt-content").textContent || "");
    showToast("Receipt copied.");
  };
}

onAuthStateChanged(auth, async (user) => {
  state.user = user;
  if (!user) {
    state.isOwner = false;
    state.profile = null;
    state.adminRequestsInitialized = false;
    state.knownAdminRequestIds.clear();
    state.newlyArrivedOrderIds.clear();
    state.lowStockInitialized = false;
    state.knownLowStockProductIds.clear();
    document.getElementById("nav-settings-badge")?.classList.add("hidden");
    clearSubscriptions();
    document.getElementById("registration-card").classList.add("hidden");
    renderLoginPortal();
    renderAdminDashboard();
    renderMyRequests();
    return;
  }

  let verifiedOwner = Boolean(user.email && user.email.toLowerCase() === AUTHORIZED_OWNER_EMAIL && user.emailVerified);
  if (verifiedOwner) {
    try {
      await setDoc(doc(db, "admins", user.uid), {
        email: user.email.toLowerCase(),
        role: "OWNER",
        updatedAt: serverTimestamp(),
      }, { merge: true });
    } catch (_) {
      verifiedOwner = false;
    }
  } else {
    try {
      const admSnap = await getDoc(doc(db, "admins", user.uid));
      verifiedOwner = admSnap.exists();
    } catch (_) {
      verifiedOwner = false;
    }
  }

  if (state.loginMode === "OWNER" && !verifiedOwner) {
    state.isOwner = false;
    state.ownerDeniedMsg = "Access Denied: This Google account is not authorized for Owner / Admin access.";
    await signOut(auth);
    renderLoginPortal();
    return;
  }

  state.isOwner = verifiedOwner;
  state.ownerDeniedMsg = "";
  renderLoginPortal();
  attachFirestoreListeners();
  if (state.loginMode === "OWNER" && state.isOwner) {
    switchTab("settings");
    document.getElementById("owner-admin-section")?.scrollIntoView({ behavior: "smooth" });
  }
});

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("./sw.js").catch(() => undefined);
}

wireEvents();
applyLanguage();
renderLoginPortal();
populateLocalitySelects();
renderShopHeaderAndHome();
renderMenusAndCarts();
renderRoomsAndFreshen();
renderMyRequests();
