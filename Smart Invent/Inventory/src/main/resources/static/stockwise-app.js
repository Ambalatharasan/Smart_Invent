const state = {
  token: sessionStorage.getItem("stockwise.token") || "",
  user: JSON.parse(sessionStorage.getItem("stockwise.user") || "null"),
  inventory: [],
  categories: [],
  managers: [],
  restock: null,
  orders: [],
  activity: [],
  dashboard: {
    summary: null,
    inventoryStatus: null,
    categoryStock: [],
    monthlyRestock: [],
    inventoryValueTrend: [],
    lowStockWatchlist: [],
    recentActivity: [],
  },
  charts: {},
  view: "dashboard",
  selectedManagerId: null,
  statusFilter: "",
  categoryFilter: "",
  search: "",
  activityTypeFilter: "",
  activityDateFilter: "",
  itemDialogMode: "create",
  editingItemId: null,
};

const LOGIN_PATH = "/login.html";
const THEME_KEY = "stockwise.theme";
const OTHER_CATEGORY_VALUE = "__other";
const DEFAULT_CATEGORIES = [
  "Grocery",
  "Apparel",
  "Body Care",
  "Home Goods",
  "Lifestyle",
  "Stationery",
  "Electronics",
  "Hardware",
  "Medical",
];

const els = {
  sidebarToggle: document.querySelector("#sidebarToggle"),
  sidebarBackdrop: document.querySelector("#sidebarBackdrop"),
  sessionUser: document.querySelector("#sessionUser"),
  profileName: document.querySelector("#profileName"),
  profileMenuName: document.querySelector("#profileMenuName"),
  profileMenuEmail: document.querySelector("#profileMenuEmail"),
  profileMenuButton: document.querySelector("#profileMenuButton"),
  profileMenu: document.querySelector("#profileMenu"),
  logoutButton: document.querySelector("#logoutButton"),
  profileLogoutButton: document.querySelector("#profileLogoutButton"),
  themeToggle: document.querySelector("#themeToggle"),
  settingsThemeToggle: document.querySelector("#settingsThemeToggle"),
  refreshButton: document.querySelector("#refreshButton"),
  openAddItem: document.querySelector("#openAddItem"),
  metricGrid: document.querySelector("#metricGrid"),
  apiStatus: document.querySelector("#apiStatus"),
  settingsTheme: document.querySelector("#settingsTheme"),
  settingsApiStatus: document.querySelector("#settingsApiStatus"),
  dashboardLowStockRows: document.querySelector("#dashboardLowStockRows"),
  recentActivityRows: document.querySelector("#recentActivityRows"),
  inventoryRows: document.querySelector("#inventoryRows"),
  inventorySearch: document.querySelector("#inventorySearch"),
  statusFilter: document.querySelector("#statusFilter"),
  categoryFilter: document.querySelector("#categoryFilter"),
  restockBudget: document.querySelector("#restockBudget"),
  restockCards: document.querySelector("#restockCards"),
  orderList: document.querySelector("#orderList"),
  managerList: document.querySelector("#managerList"),
  managerListCount: document.querySelector("#managerListCount"),
  managerDetail: document.querySelector("#managerDetail"),
  activityRows: document.querySelector("#activityRows"),
  activityTypeButtons: document.querySelectorAll("[data-activity-type]"),
  activityDateButtons: document.querySelectorAll("[data-activity-date]"),
  activityForm: document.querySelector("#activityForm"),
  activityNote: document.querySelector("#activityNote"),
  itemDialog: document.querySelector("#itemDialog"),
  itemDialogTitle: document.querySelector("#itemDialogTitle"),
  itemForm: document.querySelector("#itemForm"),
  itemNumInput: document.querySelector("#itemNumInput"),
  itemCategory: document.querySelector("#itemCategory"),
  customCategoryWrap: document.querySelector("#customCategoryWrap"),
  customCategory: document.querySelector("#customCategory"),
  itemManager: document.querySelector("#itemManager"),
  saveItemButton: document.querySelector("#saveItemButton"),
  closeItemDialog: document.querySelector("#closeItemDialog"),
  cancelItem: document.querySelector("#cancelItem"),
  toast: document.querySelector("#toast"),
};

const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });
const dateFmt = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" });
const dateTimeFmt = new Intl.DateTimeFormat("en-US", {
  month: "short",
  day: "numeric",
  year: "numeric",
  hour: "numeric",
  minute: "2-digit",
});

const activityLabels = {
  ITEM_CREATED: "Item Created",
  ITEM_UPDATED: "Item Updated",
  STOCK_ADJUSTED: "Stock Adjusted",
  RESTOCK_ORDERED: "Restock Ordered",
  RESTOCK_RECEIVED: "Restock Received",
  MANAGER_UPDATED: "Manager Updated",
  NOTE: "Note",
};

function formatMoney(value) {
  return money.format(Number(value || 0));
}

function formatDate(value) {
  if (!value) return "None";
  return dateFmt.format(new Date(value));
}

function formatDateTime(value) {
  if (!value) return "None";
  return dateTimeFmt.format(new Date(value));
}

function formatActivityType(type) {
  return activityLabels[type] || String(type || "").replaceAll("_", " ");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function normalizeStatus(status) {
  const value = String(status || "").toUpperCase();
  if (value === "LOW STOCK" || value === "LOW") return "LOW";
  if (value === "OUT OF STOCK" || value === "OUT") return "OUT";
  return value || "HEALTHY";
}

function statusLabel(status) {
  const value = normalizeStatus(status);
  if (value === "HEALTHY") return "Healthy";
  if (value === "LOW") return "Low";
  if (value === "OUT") return "Out";
  return formatActivityType(value);
}

function statusBadge(status) {
  const value = normalizeStatus(status);
  const styles = {
    HEALTHY: "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200",
    LOW: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200",
    OUT: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200",
  };
  return `<span class="status-badge ${styles[value] || "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200"}">${statusLabel(value)}</span>`;
}

function orderStatusBadge(status) {
  const value = String(status || "DRAFT").toUpperCase();
  const styles = {
    DRAFT: "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200",
    SUBMITTED: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200",
    RECEIVED: "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200",
    CANCELLED: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200",
  };
  return `<span class="status-badge ${styles[value] || styles.DRAFT}">${formatActivityType(value)}</span>`;
}

function emptyState(message, colspan = 1) {
  return `
    <tr>
      <td colspan="${colspan}">
        <div class="empty-state">
          <span class="empty-state-icon" aria-hidden="true">i</span>
          <strong>No data available</strong>
          <span>${escapeHtml(message)}</span>
        </div>
      </td>
    </tr>
  `;
}

function showToast(message) {
  if (!els.toast) return;
  els.toast.textContent = message;
  els.toast.classList.remove("hidden");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    els.toast.classList.add("hidden");
  }, 3000);
}

function redirectToLogin() {
  sessionStorage.removeItem("stockwise.token");
  sessionStorage.removeItem("stockwise.user");
  window.location.replace(LOGIN_PATH);
}

function currentTheme() {
  return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

function applyTheme(theme) {
  const nextTheme = theme === "dark" ? "dark" : "light";
  document.documentElement.dataset.theme = nextTheme;
  document.documentElement.classList.toggle("dark", nextTheme === "dark");
  localStorage.setItem(THEME_KEY, nextTheme);
  els.themeToggle?.setAttribute("aria-label", nextTheme === "dark" ? "Use light theme" : "Use dark theme");
  if (els.settingsTheme) {
    els.settingsTheme.textContent = nextTheme === "dark" ? "Dark" : "Light";
  }
  if (state.view === "dashboard") {
    renderCharts();
  }
}

function setSidebarOpen(open) {
  document.body.classList.toggle("sidebar-open", open);
  els.sidebarToggle?.setAttribute("aria-expanded", String(open));
  els.sidebarToggle?.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
}

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    try {
      const error = await response.json();
      message = error.message || message;
      if (error.fields) {
        message = Object.values(error.fields).find(Boolean) || message;
      }
    } catch {
      // Keep the status-based fallback.
    }
    const error = new Error(response.status === 401 ? "Login required. Please sign in again." : message);
    error.status = response.status;
    throw error;
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function loadAll() {
  if (!state.token) {
    redirectToLogin();
    return;
  }
  try {
    const [
      inventory,
      managers,
      categories,
      restock,
      orders,
      activity,
      summary,
      inventoryStatus,
      categoryStock,
      monthlyRestock,
      inventoryValueTrend,
      lowStockWatchlist,
      recentActivity,
    ] = await Promise.all([
      api("/api/inventory"),
      api("/api/managers"),
      api("/api/categories"),
      api("/api/restock/summary"),
      api("/api/restock/orders"),
      api("/api/activity"),
      api("/api/dashboard/summary"),
      api("/api/dashboard/inventory-status"),
      api("/api/dashboard/category-stock"),
      api("/api/dashboard/monthly-restock"),
      api("/api/dashboard/inventory-value-trend"),
      api("/api/dashboard/low-stock-watchlist"),
      api("/api/dashboard/recent-activity"),
    ]);

    state.inventory = inventory;
    state.managers = managers;
    state.categories = categories;
    state.restock = restock;
    state.orders = orders;
    state.activity = activity;
    state.dashboard = {
      summary,
      inventoryStatus,
      categoryStock,
      monthlyRestock,
      inventoryValueTrend,
      lowStockWatchlist,
      recentActivity,
    };
    render();
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      redirectToLogin();
      return;
    }
    showToast(error.message);
  }
}

function render() {
  renderSession();
  renderMetrics();
  renderDashboard();
  renderInventory();
  renderRestock();
  renderManagers();
  renderActivity();
  renderSettings();
}

function renderSession() {
  const label = state.user ? `${state.user.name} (${state.user.role})` : "Not connected";
  els.sessionUser.textContent = label;
  if (els.profileName) {
    els.profileName.textContent = state.user?.name || "User";
  }
  if (els.profileMenuName) {
    els.profileMenuName.textContent = state.user?.name || "Smart Invent User";
  }
  if (els.profileMenuEmail) {
    els.profileMenuEmail.textContent = state.user?.email || "";
  }
  els.apiStatus.textContent = state.token ? "Connected" : "Waiting for login";
  els.settingsApiStatus.textContent = state.token ? "Connected" : "Not connected";
}

function renderMetrics() {
  if (!els.metricGrid) return;
  const summary = state.dashboard.summary || deriveSummary();
  const cards = [
    ["Units on Hand", summary.unitsOnHand, `${state.inventory.length} tracked products`],
    ["Low Stock Items", summary.lowStockItems, "At or below reorder level"],
    ["Out of Stock Items", summary.outOfStockItems, "Products blocking sales"],
    ["Inventory Value", formatMoney(summary.inventoryValue), "Cost basis on hand"],
    ["Restock Budget", formatMoney(summary.restockBudget), "Suggested order cost"],
    ["Active Managers", summary.activeManagers, "Department owners"],
  ];

  els.metricGrid.innerHTML = cards
    .map(([label, value, note]) => `
      <article class="metric-card">
        <span>${escapeHtml(label)}</span>
        <strong>${escapeHtml(value)}</strong>
        <small>${escapeHtml(note)}</small>
      </article>
    `)
    .join("");
  els.metricGrid.hidden = state.view !== "dashboard";
}

function deriveSummary() {
  const unitsOnHand = state.inventory.reduce((sum, item) => sum + item.quantity, 0);
  const lowStockItems = state.inventory.filter((item) => item.status === "LOW").length;
  const outOfStockItems = state.inventory.filter((item) => item.status === "OUT").length;
  const inventoryValue = state.inventory.reduce((sum, item) => sum + Number(item.inventoryValue || 0), 0);
  const restockBudget = state.restock?.estimatedBudget || 0;
  return {
    unitsOnHand,
    lowStockItems,
    outOfStockItems,
    inventoryValue,
    restockBudget,
    activeManagers: state.managers.length,
  };
}

function renderDashboard() {
  const watchRows = state.dashboard.lowStockWatchlist || [];
  els.dashboardLowStockRows.innerHTML = watchRows.length
    ? watchRows.map((item) => `
        <tr>
          <td><strong>${escapeHtml(item.itemName)}</strong></td>
          <td>${escapeHtml(item.sku)}</td>
          <td>${item.quantity}</td>
          <td>${item.reorderLevel}</td>
          <td>${statusBadge(item.status)}</td>
        </tr>
      `).join("")
    : emptyState("No urgent restock items right now.", 5);

  const recent = state.dashboard.recentActivity || [];
  els.recentActivityRows.innerHTML = recent.length
    ? recent.map((entry) => `
        <article class="rounded-xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <span class="soft-badge bg-cyan-100 text-cyan-800 dark:bg-cyan-900/40 dark:text-cyan-200">${formatActivityType(entry.type)}</span>
              <p class="mt-3 font-semibold text-gray-900 dark:text-white">${escapeHtml(entry.activity)}</p>
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">${escapeHtml(entry.loggedBy)}</p>
            </div>
            <time class="shrink-0 text-xs font-bold text-gray-500 dark:text-gray-400">${formatDateTime(entry.loggedDate)}</time>
          </div>
        </article>
      `).join("")
    : `<p class="empty-state">No recent activity is available.</p>`;

  renderCharts();
}

function destroyCharts() {
  Object.values(state.charts).forEach((chart) => {
    try {
      chart.destroy();
    } catch {
      // ApexCharts can throw if a container was removed during resize.
    }
  });
  state.charts = {};
}

function renderCharts() {
  if (state.view !== "dashboard") {
    destroyCharts();
    return;
  }
  destroyCharts();
  const chartIds = ["inventoryStatusChart", "categoryStockChart", "monthlyRestockChart", "inventoryValueChart"];
  if (!window.ApexCharts) {
    chartIds.forEach((id) => showChartEmpty(id, "Chart library is loading. Refresh if charts do not appear."));
    return;
  }

  const mode = currentTheme();
  const isDark = mode === "dark";
  const chartHeight = window.matchMedia("(max-width: 767px)").matches ? 280 : 315;
  const textColor = isDark ? "#d1d5db" : "#374151";
  const gridColor = isDark ? "#374151" : "#e5e7eb";
  const baseOptions = {
    chart: {
      background: "transparent",
      toolbar: { show: false },
      fontFamily: "Inter, Segoe UI, Arial, sans-serif",
      foreColor: textColor,
    },
    theme: { mode },
    grid: { borderColor: gridColor },
    tooltip: { theme: mode },
    dataLabels: { style: { fontFamily: "Inter, Segoe UI, Arial, sans-serif" } },
  };

  const status = state.dashboard.inventoryStatus || { healthy: 0, lowStock: 0, outOfStock: 0 };
  const statusSeries = [status.healthy || 0, status.lowStock || 0, status.outOfStock || 0];
  createChart("inventoryStatusChart", statusSeries.some(Boolean), {
    ...baseOptions,
    chart: { ...baseOptions.chart, type: "donut", height: chartHeight },
    labels: ["Healthy", "Low Stock", "Out of Stock"],
    series: statusSeries,
    colors: ["#16a34a", "#f59e0b", "#dc2626"],
    legend: { position: "bottom", labels: { colors: textColor } },
    stroke: { colors: [isDark ? "#1f2937" : "#ffffff"] },
    plotOptions: {
      pie: {
        donut: {
          size: "68%",
          labels: {
            show: true,
            value: {
              color: textColor,
            },
            total: {
              show: true,
              label: "Items",
              color: textColor,
            },
          },
        },
      },
    },
  });

  const categoryStock = state.dashboard.categoryStock || [];
  createChart("categoryStockChart", categoryStock.some((entry) => Number(entry.quantity) > 0), {
    ...baseOptions,
    chart: { ...baseOptions.chart, type: "bar", height: chartHeight },
    series: [{ name: "Quantity", data: categoryStock.map((entry) => Number(entry.quantity || 0)) }],
    xaxis: {
      categories: categoryStock.map((entry) => entry.category),
      labels: { style: { colors: textColor } },
    },
    yaxis: { labels: { style: { colors: textColor } } },
    colors: ["#0891b2"],
    plotOptions: { bar: { borderRadius: 6, columnWidth: "46%" } },
  });

  const monthlyRestock = state.dashboard.monthlyRestock || [];
  createChart("monthlyRestockChart", monthlyRestock.some((entry) => Number(entry.quantity) > 0), {
    ...baseOptions,
    chart: { ...baseOptions.chart, type: "area", height: chartHeight },
    series: [{ name: "Restock Quantity", data: monthlyRestock.map((entry) => Number(entry.quantity || 0)) }],
    xaxis: {
      categories: monthlyRestock.map((entry) => entry.month),
      labels: { style: { colors: textColor } },
    },
    yaxis: { labels: { style: { colors: textColor } } },
    colors: ["#2563eb"],
    stroke: { curve: "smooth", width: 3 },
    fill: { type: "gradient", gradient: { opacityFrom: 0.35, opacityTo: 0.02 } },
  });

  const inventoryValueTrend = state.dashboard.inventoryValueTrend || [];
  createChart("inventoryValueChart", inventoryValueTrend.some((entry) => Number(entry.value) > 0), {
    ...baseOptions,
    chart: { ...baseOptions.chart, type: "area", height: chartHeight },
    series: [{ name: "Inventory Value", data: inventoryValueTrend.map((entry) => Number(entry.value || 0)) }],
    xaxis: {
      categories: inventoryValueTrend.map((entry) => entry.month),
      labels: { style: { colors: textColor } },
    },
    yaxis: {
      labels: {
        style: { colors: textColor },
        formatter: (value) => formatMoney(value),
      },
    },
    colors: ["#10b981"],
    stroke: { curve: "smooth", width: 3 },
    fill: { type: "gradient", gradient: { opacityFrom: 0.34, opacityTo: 0.03 } },
    tooltip: {
      theme: mode,
      y: {
        formatter: (value) => formatMoney(value),
      },
    },
  });
}

function createChart(id, hasData, options) {
  const element = document.querySelector(`#${id}`);
  if (!element) return;
  if (!hasData) {
    showChartEmpty(id, "No chart data is available yet.");
    return;
  }
  element.innerHTML = "";
  const chart = new ApexCharts(element, options);
  state.charts[id] = chart;
  chart.render().catch(() => {
    delete state.charts[id];
    showChartEmpty(id, "Chart could not be rendered.");
  });
}

function showChartEmpty(id, message) {
  const element = document.querySelector(`#${id}`);
  if (!element) return;
  element.innerHTML = `<div class="chart-empty">${escapeHtml(message)}</div>`;
}

function categories() {
  return [...new Set([...DEFAULT_CATEGORIES, ...state.categories, ...state.inventory.map((item) => item.category)].filter(Boolean))]
    .sort((a, b) => a.localeCompare(b));
}

function updateCategoryFilterOptions() {
  if (!els.categoryFilter) return;
  const selected = state.categoryFilter;
  els.categoryFilter.innerHTML = `<option value="">All categories</option>${categories()
    .map((category) => `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`)
    .join("")}`;
  els.categoryFilter.value = selected;
}

function filteredInventory() {
  const search = state.search.toLowerCase();
  return state.inventory
    .filter((item) => !state.statusFilter || item.status === state.statusFilter)
    .filter((item) => !state.categoryFilter || item.category === state.categoryFilter)
    .filter((item) => {
      if (!search) return true;
      return [item.sku, item.name, item.category, item.supplier, item.location]
        .join(" ")
        .toLowerCase()
        .includes(search);
    });
}

function renderInventory() {
  updateCategoryFilterOptions();
  const rows = filteredInventory();
  els.inventoryRows.innerHTML = rows.length ? rows.map((item) => `
    <tr>
      <td><strong>${escapeHtml(item.sku)}</strong></td>
      <td>
        <strong class="text-gray-900 dark:text-white">${escapeHtml(item.name)}</strong>
        <span class="mt-1 block text-xs text-gray-500 dark:text-gray-400">${escapeHtml(item.supplier)} - ${escapeHtml(item.location)}</span>
      </td>
      <td>${escapeHtml(item.category)}</td>
      <td>${item.quantity}</td>
      <td>${item.reorderPoint}</td>
      <td>${statusBadge(item.status)}</td>
      <td>${formatDate(item.lastRestockDate)}</td>
      <td>${item.lastRestockQuantity || 0}</td>
      <td>
        <div class="action-menu">
          <button class="btn-secondary action-menu-button" type="button" data-action-menu="${item.id}" aria-haspopup="menu" aria-expanded="false">
            Actions
            <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="m6 8 4 4 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path></svg>
          </button>
          <div class="action-menu-panel" data-action-panel="${item.id}" role="menu">
            <button type="button" data-view-item="${item.id}" role="menuitem">View details</button>
            <button type="button" data-edit="${item.id}" role="menuitem">Edit item</button>
            <button type="button" data-receive="${item.id}" role="menuitem">Receive 1 unit</button>
            <button type="button" data-delete="${item.id}" role="menuitem">Delete item</button>
          </div>
        </div>
      </td>
    </tr>
  `).join("") : emptyState("No inventory items match the current filters.", 9);
}

function renderRestock() {
  els.restockBudget.textContent = formatMoney(state.restock?.estimatedBudget || 0);
  const candidates = state.restock?.candidates || [];
  els.restockCards.innerHTML = candidates.length
    ? candidates.map((item) => `
        <article class="rounded-xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="text-lg font-black text-gray-900 dark:text-white">${escapeHtml(item.name)}</h3>
                ${statusBadge(item.status)}
              </div>
              <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">Item.Num ${escapeHtml(item.sku)} - ${escapeHtml(item.category)} - ${escapeHtml(item.supplier)}</p>
            </div>
            <button class="btn-primary" type="button" data-order="${item.itemId}" data-qty="${item.suggestedOrderQuantity}">Create Order</button>
          </div>
          <div class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div class="rounded-lg bg-white p-3 dark:bg-gray-800"><span class="block text-xs font-black uppercase text-gray-500 dark:text-gray-400">Current Qty</span><strong class="mt-1 block text-xl">${item.quantity}</strong></div>
            <div class="rounded-lg bg-white p-3 dark:bg-gray-800"><span class="block text-xs font-black uppercase text-gray-500 dark:text-gray-400">Reorder Level</span><strong class="mt-1 block text-xl">${item.reorderPoint}</strong></div>
            <div class="rounded-lg bg-white p-3 dark:bg-gray-800"><span class="block text-xs font-black uppercase text-gray-500 dark:text-gray-400">Suggested Qty</span><strong class="mt-1 block text-xl">${item.suggestedOrderQuantity}</strong></div>
            <div class="rounded-lg bg-white p-3 dark:bg-gray-800"><span class="block text-xs font-black uppercase text-gray-500 dark:text-gray-400">Est. Cost</span><strong class="mt-1 block text-xl">${formatMoney(item.estimatedCost)}</strong></div>
          </div>
          <p class="mt-4 text-sm text-gray-500 dark:text-gray-400">Last restock: <strong>${formatDate(item.lastRestockDate)}</strong> - ${item.lastRestockQuantity || 0} units - ${item.leadTimeDays} day lead</p>
        </article>
      `).join("")
    : `<p class="empty-state">No restock candidates right now.</p>`;

  els.orderList.innerHTML = state.orders.length
    ? state.orders.map((order) => `
        <article class="rounded-xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h3 class="font-black text-gray-900 dark:text-white">${escapeHtml(order.itemName)}</h3>
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">Item.Num ${escapeHtml(order.sku)} - ${order.requestedQuantity} units - ${formatMoney(order.estimatedCost)}</p>
            </div>
            ${orderStatusBadge(order.status)}
          </div>
          <p class="mt-3 text-sm text-gray-500 dark:text-gray-400">Created ${formatDateTime(order.createdAt)} by ${escapeHtml(order.requestedBy)}</p>
          ${order.receivedAt ? `<p class="mt-1 text-sm text-gray-500 dark:text-gray-400">Received ${formatDateTime(order.receivedAt)}</p>` : ""}
          ${order.status !== "RECEIVED" ? `<button class="btn-primary mt-4" type="button" data-receive-order="${order.id}">Mark Received</button>` : ""}
        </article>
      `).join("")
    : `<p class="empty-state">No restock orders have been created yet.</p>`;
}

function renderManagers() {
  if (!state.managers.length) {
    els.managerListCount.textContent = "0";
    els.managerList.innerHTML = `<p class="empty-state">No managers available.</p>`;
    els.managerDetail.innerHTML = `<p class="empty-state">Manager details will appear here.</p>`;
    return;
  }

  if (!state.selectedManagerId || !state.managers.some((manager) => manager.id === state.selectedManagerId)) {
    state.selectedManagerId = state.managers[0].id;
  }

  els.managerListCount.textContent = state.managers.length;
  els.managerList.innerHTML = state.managers.map((manager) => {
    const assignedItems = managerInventory(manager);
    const openIssues = assignedItems.filter((item) => item.status !== "HEALTHY").length;
    const isActive = manager.id === state.selectedManagerId;
    return `
      <button class="manager-card ${isActive ? "is-active" : ""}" type="button" data-manager-id="${manager.id}">
        <span class="min-w-0">
          <strong>${escapeHtml(manager.fullName)}</strong>
          <small>${escapeHtml(manager.title)}</small>
        </span>
        <span class="grid justify-items-end gap-1">
          <small>${escapeHtml(manager.department)}</small>
          <b>${openIssues}</b>
        </span>
      </button>
    `;
  }).join("");

  renderManagerDetail(state.managers.find((manager) => manager.id === state.selectedManagerId));
}

function managerInventory(manager) {
  if (!manager) return [];
  if (manager.department === "Operations") {
    return [...state.inventory].sort((a, b) => normalizeStatus(a.status).localeCompare(normalizeStatus(b.status)) || a.name.localeCompare(b.name));
  }
  return state.inventory
    .filter((item) => item.category === manager.department)
    .sort((a, b) => normalizeStatus(a.status).localeCompare(normalizeStatus(b.status)) || a.name.localeCompare(b.name));
}

function renderManagerDetail(manager) {
  if (!manager) {
    els.managerDetail.innerHTML = `<p class="empty-state">Select a manager to view details.</p>`;
    return;
  }

  const assignedItems = managerInventory(manager);
  const openIssues = assignedItems.filter((item) => item.status !== "HEALTHY").length;
  const inventoryValue = assignedItems.reduce((sum, item) => sum + Number(item.inventoryValue || 0), 0);

  els.managerDetail.innerHTML = `
    <div class="manager-detail-content">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <span class="soft-badge bg-cyan-100 text-cyan-800 dark:bg-cyan-900/40 dark:text-cyan-200">${escapeHtml(manager.status)}</span>
          <h3 class="mt-3 text-3xl font-black tracking-normal text-gray-900 dark:text-white">${escapeHtml(manager.fullName)}</h3>
          <p class="mt-1 text-gray-500 dark:text-gray-400">${escapeHtml(manager.title)}</p>
        </div>
        <div class="rounded-xl bg-gray-50 p-4 text-center dark:bg-gray-900/50">
          <strong class="block text-3xl font-black text-gray-900 dark:text-white">${openIssues}</strong>
          <span class="text-xs font-black uppercase text-gray-500 dark:text-gray-400">Open Issues</span>
        </div>
      </div>

      <div class="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        ${detailTile("Department", manager.department)}
        ${detailTile("Shift", manager.shiftName)}
        ${detailTile("Phone", manager.phone)}
        ${detailTile("Email", manager.email)}
        ${detailTile("Assigned Items", assignedItems.length)}
        ${detailTile("Inventory Value", formatMoney(inventoryValue))}
      </div>

      <section class="mt-5 rounded-xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
        <span class="text-xs font-black uppercase text-gray-500 dark:text-gray-400">Responsibilities</span>
        <p class="mt-2 text-sm leading-6 text-gray-700 dark:text-gray-300">${escapeHtml(manager.responsibilities || "No responsibilities recorded.")}</p>
      </section>

      <section class="manager-assigned-section mt-5">
        <div class="manager-assigned-header mb-3 flex justify-between gap-3">
          <span class="text-xs font-black uppercase text-gray-500 dark:text-gray-400">Assigned Inventory</span>
          <button class="btn-secondary shrink-0" type="button" data-manager-action="inventory">Open Inventory</button>
        </div>
        <div class="manager-assigned-scroll">
          ${assignedItems.length ? assignedItems.map((item) => `
            <article class="manager-assigned-item flex items-center justify-between gap-3 rounded-xl border border-gray-200 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-900/40">
              <span class="min-w-0">
                <strong class="block truncate text-gray-900 dark:text-white">${escapeHtml(item.name)}</strong>
                <small class="block truncate text-gray-500 dark:text-gray-400">Item.Num ${escapeHtml(item.sku)} - ${escapeHtml(item.location)}</small>
              </span>
              <span class="grid shrink-0 justify-items-end gap-2">
                <b>${item.quantity}</b>
                ${statusBadge(item.status)}
              </span>
            </article>
          `).join("") : `<p class="empty-state">No inventory items assigned to this department.</p>`}
        </div>
      </section>
    </div>
  `;
}

function detailTile(label, value) {
  return `
    <div class="rounded-xl border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
      <span class="text-xs font-black uppercase text-gray-500 dark:text-gray-400">${escapeHtml(label)}</span>
      <strong class="mt-2 block break-words text-gray-900 dark:text-white">${escapeHtml(value)}</strong>
    </div>
  `;
}

function matchesActivityDateFilter(createdAt) {
  if (!state.activityDateFilter) return true;
  const created = new Date(createdAt);
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  if (state.activityDateFilter === "today") {
    return created >= startOfToday;
  }
  if (state.activityDateFilter === "week") {
    const startOfWeek = new Date(startOfToday);
    startOfWeek.setDate(startOfToday.getDate() - startOfToday.getDay());
    return created >= startOfWeek;
  }
  if (state.activityDateFilter === "month") {
    return created >= new Date(now.getFullYear(), now.getMonth(), 1);
  }
  return true;
}

function renderActivity() {
  const filtered = state.activity
    .filter((entry) => !state.activityTypeFilter || entry.type === state.activityTypeFilter)
    .filter((entry) => matchesActivityDateFilter(entry.createdAt));
  els.activityRows.innerHTML = filtered.length ? filtered.map((entry) => `
    <tr>
      <td><span class="soft-badge bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200">${formatActivityType(entry.type)}</span></td>
      <td>
        <strong class="text-gray-900 dark:text-white">${escapeHtml(entry.message)}</strong>
        ${entry.itemSku ? `<span class="mt-1 block text-xs text-gray-500 dark:text-gray-400">Item.Num ${escapeHtml(entry.itemSku)}</span>` : ""}
      </td>
      <td>${escapeHtml(entry.actorEmail)}</td>
      <td>${formatDateTime(entry.createdAt)}</td>
    </tr>
  `).join("") : emptyState("No activities match the selected filters.", 4);
}

function renderSettings() {
  if (els.settingsTheme) {
    els.settingsTheme.textContent = currentTheme() === "dark" ? "Dark" : "Light";
  }
}

function setView(view) {
  state.view = view;
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.view === view);
  });
  document.querySelectorAll(".view").forEach((panel) => {
    panel.classList.toggle("is-active", panel.id === `${view}View`);
  });
  if (els.metricGrid) {
    els.metricGrid.hidden = view !== "dashboard";
  }
  setSidebarOpen(false);
  if (view === "dashboard") {
    renderDashboard();
  } else {
    destroyCharts();
  }
}

async function adjustStock(itemId, quantityChange, reason) {
  await api(`/api/inventory/${itemId}/stock`, {
    method: "PATCH",
    body: JSON.stringify({ quantityChange, reason }),
  });
  showToast("Stock updated");
  await loadAll();
}

async function createOrder(itemId, quantity) {
  await api("/api/restock/orders", {
    method: "POST",
    body: JSON.stringify({ itemId: Number(itemId), requestedQuantity: Number(quantity) }),
  });
  showToast("Restock order created");
  await loadAll();
}

async function receiveOrder(orderId) {
  await api(`/api/restock/orders/${orderId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status: "RECEIVED" }),
  });
  showToast("Restock order received");
  await loadAll();
}

async function deleteItem(itemId) {
  const item = state.inventory.find((entry) => entry.id === Number(itemId));
  if (!window.confirm(`Delete ${item?.name || "this item"}?`)) return;
  await api(`/api/inventory/${itemId}`, { method: "DELETE" });
  showToast("Inventory item deleted");
  await loadAll();
}

function itemPayloadFromForm() {
  const formData = new FormData(els.itemForm);
  const payload = Object.fromEntries(formData.entries());
  delete payload.managerId;
  delete payload.customCategory;
  if (payload.category === OTHER_CATEGORY_VALUE) {
    const customCategory = els.customCategory?.value.trim() || "";
    if (!customCategory) {
      throw new Error("Custom category is required");
    }
    payload.category = customCategory;
  }
  ["quantity", "reorderPoint", "leadTimeDays", "lastRestockQuantity"].forEach((key) => {
    payload[key] = payload[key] === "" || payload[key] == null ? null : Number(payload[key]);
  });
  ["unitCost", "retailPrice"].forEach((key) => {
    payload[key] = Number(payload[key]);
  });
  if (!Number.isFinite(payload.unitCost) || payload.unitCost <= 0) {
    throw new Error("Invalid Unit Cost");
  }
  if (!Number.isFinite(payload.retailPrice) || payload.retailPrice <= 0) {
    throw new Error("Invalid Retail Price");
  }
  if (payload.retailPrice < payload.unitCost) {
    throw new Error("Retail Price cannot be less than Unit Cost");
  }
  if (state.itemDialogMode !== "edit") {
    delete payload.sku;
  }
  if (!payload.lastRestockDate) {
    payload.lastRestockDate = null;
  }
  if (payload.lastRestockQuantity === null) {
    payload.lastRestockQuantity = 0;
  }
  return payload;
}

async function saveItem(event) {
  event.preventDefault();
  if (state.itemDialogMode === "view") {
    closeItemDialog();
    return;
  }
  const payload = itemPayloadFromForm();
  const isEdit = state.itemDialogMode === "edit" && state.editingItemId;
  await api(isEdit ? `/api/inventory/${state.editingItemId}` : "/api/inventory", {
    method: isEdit ? "PUT" : "POST",
    body: JSON.stringify(payload),
  });
  closeItemDialog();
  showToast(isEdit ? "Inventory item updated" : "Inventory item created");
  await loadAll();
}

function openItemDialog(mode = "create", item = null) {
  state.itemDialogMode = mode;
  state.editingItemId = item?.id || null;
  els.itemForm.reset();
  populateManagerOptions();
  populateItemCategoryOptions(item?.category || "");

  if (item) {
    const fields = els.itemForm.elements;
    fields.sku.value = item.sku || "";
    fields.name.value = item.name || "";
    fields.supplier.value = item.supplier || "";
    fields.quantity.value = item.quantity ?? 0;
    fields.reorderPoint.value = item.reorderPoint ?? 10;
    fields.unitCost.value = item.unitCost ?? 0;
    fields.retailPrice.value = item.retailPrice ?? 0;
    fields.leadTimeDays.value = item.leadTimeDays ?? 0;
    fields.location.value = item.location || "";
    fields.lastRestockDate.value = item.lastRestockDate || "";
    fields.lastRestockQuantity.value = item.lastRestockQuantity || "";
    fields.notes.value = item.notes || "";
  } else {
    els.itemForm.elements.sku.value = nextItemNumPreview();
    els.itemForm.elements.quantity.value = 0;
    els.itemForm.elements.reorderPoint.value = 10;
    els.itemForm.elements.unitCost.value = "1.00";
    els.itemForm.elements.retailPrice.value = "2.00";
    els.itemForm.elements.leadTimeDays.value = 5;
    els.itemForm.elements.location.value = "Stockroom";
  }

  const readonly = mode === "view";
  Array.from(els.itemForm.elements).forEach((field) => {
    if (field.id === "cancelItem" || field.id === "saveItemButton" || field.id === "closeItemDialog") return;
    field.disabled = readonly;
  });
  if (els.itemNumInput) {
    els.itemNumInput.readOnly = true;
    els.itemNumInput.disabled = false;
  }
  els.itemDialogTitle.textContent = mode === "edit" ? "Edit Inventory Item" : mode === "view" ? "Inventory Item Details" : "Add Inventory Item";
  els.saveItemButton.textContent = mode === "view" ? "Close" : mode === "edit" ? "Save Changes" : "Save Item";
  els.itemDialog.showModal();
}

function closeItemDialog() {
  Array.from(els.itemForm.elements).forEach((field) => {
    field.disabled = false;
  });
  els.itemDialog.close();
  state.itemDialogMode = "create";
  state.editingItemId = null;
}

function populateManagerOptions() {
  if (!els.itemManager) return;
  const current = els.itemManager.value;
  els.itemManager.innerHTML = `<option value="">Department based</option>${state.managers
    .map((manager) => `<option value="${manager.id}">${escapeHtml(manager.fullName)} - ${escapeHtml(manager.department)}</option>`)
    .join("")}`;
  els.itemManager.value = current;
}

function nextItemNumPreview() {
  const latest = state.inventory
    .map((item) => String(item.sku || "").trim().toUpperCase())
    .filter((value) => /^ITEM-\d+$/.test(value))
    .map((value) => Number(value.slice("ITEM-".length)))
    .filter(Number.isFinite)
    .reduce((max, value) => Math.max(max, value), 0);
  return `ITEM-${String(latest + 1).padStart(4, "0")}`;
}

function populateItemCategoryOptions(selectedCategory = "") {
  if (!els.itemCategory) return;
  const available = categories();
  const selected = selectedCategory || "";
  const usesCustom = selected && !available.some((category) => category.toLowerCase() === selected.toLowerCase());
  els.itemCategory.innerHTML = [
    `<option value="">Select category</option>`,
    ...available.map((category) => `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`),
    `<option value="${OTHER_CATEGORY_VALUE}">Other</option>`,
  ].join("");
  els.itemCategory.value = usesCustom ? OTHER_CATEGORY_VALUE : selected;
  if (els.customCategory) {
    els.customCategory.value = usesCustom ? selected : "";
  }
  updateCustomCategoryVisibility();
}

function updateCustomCategoryVisibility() {
  const showCustom = els.itemCategory?.value === OTHER_CATEGORY_VALUE;
  els.customCategoryWrap?.classList.toggle("hidden", !showCustom);
  if (els.customCategory) {
    els.customCategory.required = showCustom;
  }
}

async function addActivity(event) {
  event.preventDefault();
  const message = els.activityNote.value.trim();
  if (!message) return;
  await api("/api/activity", {
    method: "POST",
    body: JSON.stringify({ type: "NOTE", message }),
  });
  els.activityNote.value = "";
  showToast("Activity note added");
  await loadAll();
}

function logout() {
  redirectToLogin();
}

document.querySelectorAll(".nav-item").forEach((button) => {
  button.addEventListener("click", () => setView(button.dataset.view));
});

document.querySelectorAll("[data-jump]").forEach((button) => {
  button.addEventListener("click", () => {
    const jump = button.dataset.jump;
    if (jump === "inventory-low") {
      state.statusFilter = "LOW";
      els.statusFilter.value = "LOW";
      setView("inventory");
      renderInventory();
    }
  });
});

els.sidebarToggle?.addEventListener("click", () => {
  setSidebarOpen(!document.body.classList.contains("sidebar-open"));
});
els.sidebarBackdrop?.addEventListener("click", () => setSidebarOpen(false));
els.logoutButton?.addEventListener("click", logout);
els.profileLogoutButton?.addEventListener("click", logout);
els.profileMenuButton?.addEventListener("click", () => {
  els.profileMenu.classList.toggle("is-open");
  els.profileMenuButton.setAttribute("aria-expanded", String(els.profileMenu.classList.contains("is-open")));
});
document.addEventListener("click", (event) => {
  if (!event.target.closest("#profileMenu") && !event.target.closest("#profileMenuButton")) {
    els.profileMenu?.classList.remove("is-open");
  }
});

els.themeToggle?.addEventListener("click", () => {
  applyTheme(currentTheme() === "dark" ? "light" : "dark");
});
els.settingsThemeToggle?.addEventListener("click", () => {
  applyTheme(currentTheme() === "dark" ? "light" : "dark");
});
els.refreshButton?.addEventListener("click", loadAll);
els.openAddItem?.addEventListener("click", () => openItemDialog("create"));
els.inventorySearch?.addEventListener("input", (event) => {
  state.search = event.target.value;
  renderInventory();
});
els.statusFilter?.addEventListener("change", (event) => {
  state.statusFilter = event.target.value;
  renderInventory();
});
els.categoryFilter?.addEventListener("change", (event) => {
  state.categoryFilter = event.target.value;
  renderInventory();
});

els.activityTypeButtons.forEach((button) => {
  button.addEventListener("click", () => {
    state.activityTypeFilter = button.dataset.activityType;
    els.activityTypeButtons.forEach((entry) => entry.classList.toggle("is-active", entry === button));
    renderActivity();
  });
});
els.activityDateButtons.forEach((button) => {
  button.addEventListener("click", () => {
    state.activityDateFilter = button.dataset.activityDate;
    els.activityDateButtons.forEach((entry) => entry.classList.toggle("is-active", entry === button));
    renderActivity();
  });
});
els.closeItemDialog?.addEventListener("click", closeItemDialog);
els.cancelItem?.addEventListener("click", closeItemDialog);
els.itemCategory?.addEventListener("change", updateCustomCategoryVisibility);
els.itemForm?.addEventListener("submit", (event) => {
  saveItem(event).catch((error) => showToast(error.message));
});
els.activityForm?.addEventListener("submit", (event) => {
  addActivity(event).catch((error) => showToast(error.message));
});

function closeActionMenus() {
  document.querySelectorAll("[data-action-panel]").forEach((panel) => panel.classList.remove("is-open"));
  document.querySelectorAll("[data-action-menu]").forEach((button) => button.setAttribute("aria-expanded", "false"));
}

document.addEventListener("click", (event) => {
  const actionMenuButton = event.target.closest("[data-action-menu]");
  if (actionMenuButton) {
    const panel = document.querySelector(`[data-action-panel="${actionMenuButton.dataset.actionMenu}"]`);
    const willOpen = !panel?.classList.contains("is-open");
    closeActionMenus();
    if (panel && willOpen) {
      panel.classList.add("is-open");
      actionMenuButton.setAttribute("aria-expanded", "true");
    }
    return;
  }

  if (!event.target.closest(".action-menu")) {
    closeActionMenus();
  }

  const viewButton = event.target.closest("[data-view-item]");
  const editButton = event.target.closest("[data-edit]");
  const deleteButton = event.target.closest("[data-delete]");
  const receiveButton = event.target.closest("[data-receive]");
  const orderButton = event.target.closest("[data-order]");
  const receiveOrderButton = event.target.closest("[data-receive-order]");
  const managerButton = event.target.closest("[data-manager-id]");
  const managerAction = event.target.closest("[data-manager-action]");

  if (viewButton) {
    closeActionMenus();
    const item = state.inventory.find((entry) => entry.id === Number(viewButton.dataset.viewItem));
    if (item) openItemDialog("view", item);
  }
  if (editButton) {
    closeActionMenus();
    const item = state.inventory.find((entry) => entry.id === Number(editButton.dataset.edit));
    if (item) openItemDialog("edit", item);
  }
  if (deleteButton) {
    closeActionMenus();
    deleteItem(deleteButton.dataset.delete).catch((error) => showToast(error.message));
  }
  if (receiveButton) {
    closeActionMenus();
    adjustStock(receiveButton.dataset.receive, 1, "Quick receive from dashboard").catch((error) => showToast(error.message));
  }
  if (orderButton) {
    createOrder(orderButton.dataset.order, orderButton.dataset.qty).catch((error) => showToast(error.message));
  }
  if (receiveOrderButton) {
    receiveOrder(receiveOrderButton.dataset.receiveOrder).catch((error) => showToast(error.message));
  }
  if (managerButton) {
    state.selectedManagerId = Number(managerButton.dataset.managerId);
    renderManagers();
  }
  if (managerAction) {
    const manager = state.managers.find((entry) => entry.id === state.selectedManagerId);
    if (manager) {
      state.search = manager.department === "Operations" ? "" : manager.department;
      state.statusFilter = "";
      state.categoryFilter = manager.department === "Operations" ? "" : manager.department;
      els.inventorySearch.value = state.search;
      els.statusFilter.value = "";
      els.categoryFilter.value = state.categoryFilter;
      setView("inventory");
      renderInventory();
    }
  }
});

window.addEventListener("resize", () => {
  if (state.view === "dashboard") {
    window.clearTimeout(renderCharts.resizeTimer);
    renderCharts.resizeTimer = window.setTimeout(renderCharts, 180);
  }
});

if (!state.token) {
  applyTheme(localStorage.getItem(THEME_KEY) || document.documentElement.dataset.theme);
  redirectToLogin();
} else {
  applyTheme(localStorage.getItem(THEME_KEY) || document.documentElement.dataset.theme);
  render();
  loadAll();
}
