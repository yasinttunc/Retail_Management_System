/* ============================================================
   Retail Management System — frontend logic
   Talks to the Spring Boot REST API. No business logic here:
   every rule lives in the backend; this file only renders data
   and issues HTTP calls to the documented endpoints.
   ============================================================ */

// If the frontend is served separately (e.g. python http.server),
// set this to the backend URL, e.g. "http://localhost:8080".
// Empty string => same origin (served by Spring Boot /static).
const apiURL = "";

/* ---------------- tiny helpers ---------------- */
const $  = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const money = (n) => "$" + Number(n || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const esc = (s) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

async function api(path, opts = {}) {
  const res = await fetch(apiURL + path, {
    headers: { "Content-Type": "application/json" },
    ...opts,
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) throw new Error((data && (data.message || data.Error)) || `Request failed (${res.status})`);
  return data;
}

function toast(title, msg = "", type = "info") {
  const el = document.createElement("div");
  el.className = "toast " + (type === "ok" ? "ok" : type === "err" ? "err" : "");
  el.innerHTML = `<div><div class="t-title">${esc(title)}</div>${msg ? `<div class="t-msg">${esc(msg)}</div>` : ""}</div>`;
  $("#toasts").appendChild(el);
  setTimeout(() => { el.style.opacity = "0"; el.style.transform = "translateX(20px)"; el.style.transition = "all .3s"; }, 3200);
  setTimeout(() => el.remove(), 3600);
}

function stars(rating) {
  const r = Math.max(0, Math.min(5, Math.round(rating || 0)));
  return `<span class="stars">${"★".repeat(r)}<span class="off">${"★".repeat(5 - r)}</span></span>`;
}
function stockBadge(n) {
  if (n <= 0) return `<span class="badge badge-danger"><span class="dot"></span>Out of stock</span>`;
  if (n < 10) return `<span class="badge badge-warn"><span class="dot"></span>Low</span>`;
  return `<span class="badge badge-ok"><span class="dot"></span>In stock</span>`;
}

/* ---------------- shared state ---------------- */
const state = {
  stores: [],
  products: [],
  activeStore: null,
  inventory: [],   // for active store
  cart: {},        // productId -> {product, qty}
};

/* ---------------- navigation ---------------- */
const PAGES = {
  dashboard: ["Dashboard", "Real-time overview of stores, catalog and orders", loadDashboard],
  products:  ["Products", "Browse, search and manage the product catalog", loadProducts],
  inventory: ["Inventory", "Stock levels for the active store", loadInventory],
  stores:    ["Stores", "Physical locations in the network", loadStores],
  order:     ["Place Order", "Build a cart and check out", loadOrder],
  reviews:   ["Reviews", "Customer feedback per product", loadReviews],
};

function navigate(key) {
  $$(".nav-item").forEach((n) => n.classList.toggle("active", n.dataset.nav === key));
  $$(".section").forEach((s) => s.classList.remove("active"));
  $("#sec-" + key).classList.add("active");
  const [title, sub, loader] = PAGES[key];
  $("#pageTitle").textContent = title;
  $("#pageSub").textContent = sub;
  loader && loader();
}
$$(".nav-item").forEach((n) => n.addEventListener("click", () => navigate(n.dataset.nav)));

/* ---------------- bootstrap ---------------- */
async function boot() {
  if (apiURL) $("#apiHost").textContent = apiURL.replace(/^https?:\/\//, "");
  try {
    await loadStoresList();
    await loadProductsList();
    populateStorePicker();
    populateProductSelects();
    populateCategoryFilters();
    navigate("dashboard");
  } catch (e) {
    toast("Could not reach the backend", e.message, "err");
    navigate("dashboard");
  }
}

async function loadStoresList() {
  // No "list all stores" endpoint is defined in the API, so we probe
  // sequential store IDs via /store/validate/{id}. Adjust range as needed.
  const found = [];
  const ids = Array.from({ length: 12 }, (_, i) => i + 1);
  await Promise.all(ids.map(async (id) => {
    try {
      const ok = await api(`/store/validate/${id}`);
      if (ok === true) {
        // pull store name via any product-less lookup fallback
        found.push({ id, name: `Store #${id}`, address: "" });
      }
    } catch { /* ignore */ }
  }));
  found.sort((a, b) => a.id - b.id);
  state.stores = found;
  state.activeStore = found[0]?.id ?? null;
}

async function loadProductsList() {
  const data = await api("/products/all");           // listProduct -> { products: [...] }
  state.products = (data && data.products) || [];
}

/* ---------------- pickers / selects ---------------- */
function populateStorePicker() {
  const sel = $("#storeSelect");
  sel.innerHTML = state.stores.map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join("")
    || `<option value="">No stores</option>`;
  if (state.activeStore) sel.value = state.activeStore;
  sel.onchange = () => { state.activeStore = Number(sel.value); refreshActiveStoreViews(); };
}

function populateProductSelects() {
  const opts = state.products.map((p) => `<option value="${p.id}">${esc(p.name)}</option>`).join("");
  ["#invProductSelect", "#reviewProductSelect"].forEach((s) => { const el = $(s); if (el) el.innerHTML = opts; });
  const st = $("#invStoreSelect");
  if (st) st.innerHTML = state.stores.map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join("");
}

function populateCategoryFilters() {
  const cats = [...new Set(state.products.map((p) => p.category).filter(Boolean))].sort();
  $("#prodCategory").innerHTML = `<option value="">All categories</option>` + cats.map((c) => `<option>${esc(c)}</option>`).join("");
  $("#catList").innerHTML = cats.map((c) => `<option value="${esc(c)}">`).join("");
}

function refreshActiveStoreViews() {
  const active = $(".section.active")?.id?.replace("sec-", "");
  if (["dashboard", "inventory", "order"].includes(active)) PAGES[active][2]();
}

/* ---------------- Dashboard ---------------- */
async function loadDashboard() {
  $("#kpiProducts").textContent = state.products.length;
  $("#kpiStores").textContent = state.stores.length;
  if (!state.activeStore) return;
  let inv = [];
  try { inv = await getStoreInventory(state.activeStore); } catch { /* */ }
  const totalStock = inv.reduce((a, p) => a + (p.stockLevel ?? p.stock ?? 0), 0);
  const low = inv.filter((p) => (p.stockLevel ?? 0) < 10);
  $("#kpiStock").textContent = totalStock.toLocaleString();
  $("#kpiStockSub").textContent = storeName(state.activeStore);
  $("#kpiLow").textContent = low.length;

  // category bars
  const byCat = {};
  inv.forEach((p) => { byCat[p.category] = (byCat[p.category] || 0) + (p.stockLevel ?? 0); });
  const entries = Object.entries(byCat).sort((a, b) => b[1] - a[1]);
  const max = Math.max(1, ...entries.map((e) => e[1]));
  $("#catBars").innerHTML = entries.length ? entries.map(([c, v]) => `
    <div class="row2"><div class="muted">${esc(c)}</div>
    <div class="track"><div class="fill" style="width:${(v / max * 100).toFixed(1)}%"></div></div>
    <div class="val">${v}</div></div>`).join("") : `<div class="empty">No inventory for this store.</div>`;

  $("#lowStockBody").innerHTML = low.length ? low.sort((a, b) => a.stockLevel - b.stockLevel).map((p) => `
    <tr><td class="cell-strong">${esc(p.name)}</td><td><span class="badge badge-cat">${esc(p.category)}</span></td>
    <td>${stockBadge(p.stockLevel)} <span class="muted">${p.stockLevel}</span></td></tr>`).join("")
    : `<tr><td colspan="3" class="empty" style="padding:24px">All products well stocked 🎉</td></tr>`;
}

/* inventory for a store — endpoint returns products enriched with stock */
async function getStoreInventory(storeId) {
  const data = await api(`/inventory/${storeId}`);   // -> { products: [...] }
  return (data && (data.products || data.product)) || [];
}
const storeName = (id) => state.stores.find((s) => s.id === Number(id))?.name || `Store #${id}`;

/* ---------------- Products ---------------- */
let prodFilterTimer;
async function loadProducts() {
  await renderProducts();
  $("#prodSearch").oninput = () => { clearTimeout(prodFilterTimer); prodFilterTimer = setTimeout(renderProducts, 250); };
  $("#prodCategory").onchange = renderProducts;
}
async function renderProducts() {
  const q = $("#prodSearch").value.trim().toLowerCase();
  const cat = $("#prodCategory").value;
  let list = state.products;
  if (cat) list = list.filter((p) => p.category === cat);
  if (q) list = list.filter((p) => (p.name || "").toLowerCase().includes(q));
  $("#prodCount").textContent = `${list.length} of ${state.products.length}`;
  $("#productsBody").innerHTML = list.length ? list.map((p) => `
    <tr>
      <td class="cell-strong">${esc(p.name)}</td>
      <td><span class="badge badge-cat">${esc(p.category)}</span></td>
      <td class="muted">${esc(p.sku)}</td>
      <td class="cell-strong">${money(p.price)}</td>
      <td style="text-align:right"><button class="btn btn-danger btn-sm" data-del="${p.id}">Delete</button></td>
    </tr>`).join("") : `<tr><td colspan="5" class="empty">No products match your filters.</td></tr>`;
  $$("#productsBody [data-del]").forEach((b) => b.onclick = () => deleteProduct(b.dataset.del));
}

$("#addProductForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target;
  const body = {
    name: f.name.value.trim(),
    category: f.category.value.trim(),
    price: parseFloat(f.price.value),
    sku: f.sku.value.trim(),
  };
  try {
    const r = await api("/products", { method: "POST", body: JSON.stringify(body) });
    toast("Product added", r.message || "", "ok");
    f.reset();
    await loadProductsList(); populateProductSelects(); populateCategoryFilters(); renderProducts();
  } catch (err) { toast("Could not add product", err.message, "err"); }
});

async function deleteProduct(id) {
  if (!confirm("Delete this product and its inventory entries?")) return;
  try {
    const r = await api(`/products/${id}`, { method: "DELETE" });
    toast("Product deleted", r.message || "", "ok");
    await loadProductsList(); populateProductSelects(); populateCategoryFilters(); renderProducts();
  } catch (err) { toast("Could not delete", err.message, "err"); }
}

/* ---------------- Inventory ---------------- */
let invSearchTimer;
async function loadInventory() {
  $("#invStoreName").textContent = storeName(state.activeStore);
  $("#invSearch").oninput = () => { clearTimeout(invSearchTimer); invSearchTimer = setTimeout(renderInventory, 250); };
  await renderInventory();
}
async function renderInventory() {
  const body = $("#inventoryBody");
  body.innerHTML = `<tr><td colspan="5"><div class="center-load"><span class="loading"></span></div></td></tr>`;
  if (!state.activeStore) { body.innerHTML = `<tr><td colspan="5" class="empty">No active store.</td></tr>`; return; }
  let inv = [];
  try { inv = await getStoreInventory(state.activeStore); } catch (e) { body.innerHTML = `<tr><td colspan="5" class="empty">${esc(e.message)}</td></tr>`; return; }
  const q = $("#invSearch").value.trim().toLowerCase();
  if (q) inv = inv.filter((p) => (p.name || "").toLowerCase().includes(q));
  state.inventory = inv;
  body.innerHTML = inv.length ? inv.map((p) => `
    <tr>
      <td class="cell-strong">${esc(p.name)}</td>
      <td><span class="badge badge-cat">${esc(p.category)}</span></td>
      <td>${money(p.price)}</td>
      <td class="cell-strong">${p.stockLevel ?? "—"}</td>
      <td>${stockBadge(p.stockLevel ?? 0)}</td>
    </tr>`).join("") : `<tr><td colspan="5" class="empty">No inventory for ${esc(storeName(state.activeStore))}.</td></tr>`;
}

$("#addInventoryForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target;
  const productId = Number(f.productId.value);
  const storeId = Number(f.storeId.value);
  const product = state.products.find((p) => p.id === productId);
  const store = state.stores.find((s) => s.id === storeId);
  const body = { product, store, stockLevel: parseInt(f.stockLevel.value, 10) };
  try {
    const r = await api("/inventory", { method: "POST", body: JSON.stringify(body) });
    toast("Inventory saved", r.message || "", "ok");
    f.reset();
    if (storeId === state.activeStore) renderInventory();
  } catch (err) { toast("Could not save inventory", err.message, "err"); }
});

/* ---------------- Stores ---------------- */
async function loadStores() {
  $("#storesBody").innerHTML = state.stores.length ? state.stores.map((s) => `
    <tr>
      <td class="muted">#${s.id}</td>
      <td class="cell-strong">${esc(s.name)}</td>
      <td class="muted">${esc(s.address || "—")}</td>
      <td><span class="badge badge-ok"><span class="dot"></span>Active</span></td>
    </tr>`).join("") : `<tr><td colspan="4" class="empty">No stores yet — add your first store.</td></tr>`;
}

$("#addStoreForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.target;
  const body = { name: f.name.value.trim(), address: f.address.value.trim() };
  try {
    const r = await api("/store", { method: "POST", body: JSON.stringify(body) });
    toast("Store created", r.message || "", "ok");
    f.reset();
    await loadStoresList(); populateStorePicker(); populateProductSelects(); loadStores();
  } catch (err) { toast("Could not add store", err.message, "err"); }
});

/* ---------------- Place Order ---------------- */
async function loadOrder() {
  $("#orderStoreName").textContent = storeName(state.activeStore);
  const body = $("#orderProductsBody");
  body.innerHTML = `<tr><td colspan="4"><div class="center-load"><span class="loading"></span></div></td></tr>`;
  let inv = [];
  try { inv = await getStoreInventory(state.activeStore); } catch { /* */ }
  body.innerHTML = inv.length ? inv.map((p) => `
    <tr>
      <td class="cell-strong">${esc(p.name)}<div class="muted" style="font-size:12px">${esc(p.category)}</div></td>
      <td>${money(p.price)}</td>
      <td>${stockBadge(p.stockLevel ?? 0)} <span class="muted">${p.stockLevel ?? 0}</span></td>
      <td style="text-align:right"><button class="btn btn-ghost btn-sm" data-add="${p.id}" ${(p.stockLevel ?? 0) <= 0 ? "disabled" : ""}>Add</button></td>
    </tr>`).join("") : `<tr><td colspan="4" class="empty">No products available at this store.</td></tr>`;
  $$("#orderProductsBody [data-add]").forEach((b) => b.onclick = () => addToCart(Number(b.dataset.add), inv));
  renderCart();
}

function addToCart(productId, inv) {
  const product = inv.find((p) => p.id === productId) || state.products.find((p) => p.id === productId);
  if (!product) return;
  const line = state.cart[productId] || { product, qty: 0 };
  line.qty += 1;
  state.cart[productId] = line;
  renderCart();
}
function changeQty(productId, delta) {
  const line = state.cart[productId]; if (!line) return;
  line.qty += delta;
  if (line.qty <= 0) delete state.cart[productId];
  renderCart();
}
function renderCart() {
  const lines = Object.values(state.cart);
  const wrap = $("#cartLines");
  if (!lines.length) {
    wrap.innerHTML = `<div class="empty"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/></svg><div>Your cart is empty. Add products from the left.</div></div>`;
  } else {
    wrap.innerHTML = lines.map(({ product, qty }) => `
      <div class="cart-line">
        <div class="name">${esc(product.name)}<div class="muted" style="font-size:12px">${money(product.price)} each</div></div>
        <div class="qty"><button data-dec="${product.id}">−</button><span>${qty}</span><button data-inc="${product.id}">+</button></div>
        <div class="cell-strong" style="width:80px;text-align:right">${money(product.price * qty)}</div>
      </div>`).join("");
    $$("#cartLines [data-inc]").forEach((b) => b.onclick = () => changeQty(Number(b.dataset.inc), 1));
    $$("#cartLines [data-dec]").forEach((b) => b.onclick = () => changeQty(Number(b.dataset.dec), -1));
  }
  $("#cartTotal").textContent = money(cartTotal());
}
const cartTotal = () => Object.values(state.cart).reduce((a, l) => a + l.product.price * l.qty, 0);

$("#placeOrderForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const lines = Object.values(state.cart);
  if (!lines.length) { toast("Cart is empty", "Add at least one product.", "err"); return; }
  const f = e.target;
  // PlaceOrderRequestDTO expected by the backend:
  const payload = {
    name: f.name.value.trim(),
    email: f.email.value.trim(),
    phone: f.phone.value.trim(),
    storeId: state.activeStore,
    totalPrice: Number(cartTotal().toFixed(2)),
    purchaseProduct: lines.map(({ product, qty }) => ({ productId: product.id, quantity: qty, price: product.price })),
  };
  const btn = $("#placeOrderBtn"); btn.disabled = true;
  try {
    const r = await api("/store/placeOrder", { method: "POST", body: JSON.stringify(payload) });
    toast("Order placed", r.message || "Order placed successfully", "ok");
    state.cart = {}; f.reset(); loadOrder();
  } catch (err) { toast("Order failed", err.message, "err"); }
  finally { btn.disabled = false; }
});

/* ---------------- Reviews ---------------- */
async function loadReviews() { /* selects already populated */ }
$("#loadReviewsBtn").addEventListener("click", fetchReviews);
$("#reviewProductSelect").addEventListener("change", fetchReviews);

async function fetchReviews() {
  const productId = Number($("#reviewProductSelect").value);
  const storeId = state.activeStore;
  if (!productId || !storeId) return;
  const list = $("#reviewsList");
  list.innerHTML = `<div class="center-load"><span class="loading"></span></div>`;
  try {
    const data = await api(`/reviews/${storeId}/${productId}`);   // -> { reviews: [...] }
    const reviews = (data && data.reviews) || [];
    const pname = state.products.find((p) => p.id === productId)?.name || `Product #${productId}`;
    $("#reviewMeta").textContent = `${pname} · ${storeName(storeId)}`;
    if (!reviews.length) { list.innerHTML = `<div class="empty">No reviews yet for this product at this store.</div>`; $("#reviewSummary").innerHTML = ""; return; }
    const avg = reviews.reduce((a, r) => a + (r.rating || 0), 0) / reviews.length;
    $("#reviewSummary").innerHTML = `
      <div style="text-align:center">
        <div style="font-size:38px;font-weight:720">${avg.toFixed(1)}</div>
        ${stars(avg)}
        <div class="muted" style="margin-top:6px">${reviews.length} review${reviews.length > 1 ? "s" : ""}</div>
      </div>`;
    list.innerHTML = reviews.map((r) => {
      const name = r.customerName || "Unknown";
      const initials = name.split(" ").map((w) => w[0]).slice(0, 2).join("").toUpperCase();
      return `<div class="review">
        <div class="who"><div class="avatar">${esc(initials || "?")}</div>
          <div><div class="cell-strong">${esc(name)}</div>${stars(r.rating)}</div></div>
        <div>${esc(r.comment || "")}</div>
      </div>`;
    }).join("");
  } catch (err) { list.innerHTML = `<div class="empty">${esc(err.message)}</div>`; }
}

/* ---------------- go ---------------- */
boot();
