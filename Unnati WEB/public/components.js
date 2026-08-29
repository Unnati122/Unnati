/**
 * components.js
 * ─────────────────────────────────────────────
 * Shared layout components for Unnati OIL.
 * Change header or sidebar here → updates ALL pages.
 *
 * Usage in any HTML page:
 *   <script src="components.js"></script>
 *   <script>renderSharedComponents('overview');</script>
 *
 * Valid activePage values:
 *   'overview' | 'field-updates' | 'schedule' | 'agent'
 *   'review' | 'ingest' | 'analytics' | 'memory' | 'audit' | 'settings'
 * ─────────────────────────────────────────────
 */

const PROJECTS = [
  { id: 'PROJ-1', name: 'Pipeline Segment 1' },
  { id: 'PROJ-2', name: 'Digboi Expansion' },
  { id: 'PROJ-3', name: 'Numaligarh Bio-Refinery' }
];

function renderSharedComponents(activePage = '') {
  const userRole = localStorage.getItem('userRole') || 'admin';
  const selectedProjectId = localStorage.getItem('selectedProjectId') || 'PROJ-1';
  const loggedInUser = localStorage.getItem('username') || 'oil-admin';

  // Determine footer metadata
  let profileName = "Oil India Admin";
  let profileRole = "Planning Lead";
  
  if (userRole === 'manager') {
    if (loggedInUser === 'arjun-manager') {
      profileName = "Arjun Dev";
      profileRole = "Pipeline Manager";
    } else if (loggedInUser === 'priya-manager') {
      profileName = "Priya Sharma";
      profileRole = "Refinery Manager";
    } else if (loggedInUser === 'amit-manager') {
      profileName = "Amit Gohain";
      profileRole = "Bio-Refinery Manager";
    } else {
      profileName = "Project Manager";
      profileRole = "Site Manager";
    }
  }

  // Filter navigation items by role
  let itemsToShow = [];
  if (userRole === 'manager') {
    itemsToShow = [
      { key: 'overview',      icon: 'fa-border-all',        label: 'Overview',        href: 'index.html' },
      { key: 'field-updates', icon: 'fa-clock',             label: 'Field Updates',   href: 'chat.html' },
      { key: 'review',        icon: 'fa-list-check',        label: 'Review Queue',    href: 'review.html', badge: true },
      { key: 'schedule',      icon: 'fa-calendar-days',     label: 'Schedule',        href: 'schedule.html' },
      { key: 'agent',         icon: 'fa-brain',             label: 'AI Assistant',    href: 'agent.html' },
      { key: 'plan',          icon: 'fa-pen-to-square',     label: 'Project Planner', href: 'plan.html' },
      { key: 'analytics',     icon: 'fa-chart-bar',         label: 'Site Analytics',  href: 'analytics.html' }
    ];
  } else {
    itemsToShow = [
      { key: 'overview',      icon: 'fa-border-all',        label: 'Overview',        href: 'index.html' },
      { key: 'field-updates', icon: 'fa-clock',             label: 'Field Updates',   href: 'chat.html' },
      { key: 'schedule',      icon: 'fa-calendar-days',     label: 'Schedule',        href: 'schedule.html' },
      { key: 'agent',         icon: 'fa-brain',             label: 'AI Agent',        href: 'agent.html' },
      { key: 'review',        icon: 'fa-list-check',        label: 'Review Queue',    href: 'review.html', badge: true },
      { key: 'ingest',        icon: 'fa-file-import',       label: 'Data Ingestion',  href: 'ingest.html' },
      { key: 'analytics',     icon: 'fa-chart-bar',         label: 'Analytics',       href: 'analytics.html' },
      { key: 'memory',        icon: 'fa-microchip',         label: 'Project Memory',  href: 'memory.html' },
      { key: 'audit',         icon: 'fa-clock-rotate-left', label: 'Audit Trail',     href: 'audit.html' },
      { key: 'settings',      icon: 'fa-gear',              label: 'Settings',        href: 'settings.html' }
    ];
  }

  // ── 1. GOV BANNER ───────────────────────────────────────────────
  const govBanner = `
    <div class="gov-top-banner">
      <div class="container-fluid d-flex justify-content-between align-items-center">
        <div class="gov-banner-left">
          <img class="gov-emblem" src="emblem.svg" alt="National Emblem of India">
          <span class="text-dark font-weight-semibold">भारत सरकार</span>
          <span class="gov-text-divider">|</span>
          <span class="text-muted">Government of India</span>
          <span class="gov-text-divider">|</span>
          <span class="text-muted font-size-11">Ministry of Petroleum &amp; Natural Gas</span>
        </div>
        <div class="gov-banner-right d-none d-md-flex">
          <a href="#" class="font-size-11 text-decoration-none">Screen Reader Access</a>
          <a href="#" class="font-size-11 text-decoration-none">A-</a>
          <a href="#" class="font-size-11 text-decoration-none font-weight-bold">A</a>
          <a href="#" class="font-size-11 text-decoration-none">A+</a>
          <a href="#" class="font-size-11 text-decoration-none font-weight-semibold">English</a>
          <a href="#" class="font-size-11 text-decoration-none">हिन्दी</a>
        </div>
      </div>
    </div>`;

  // ── 2. MAIN HEADER ───────────────────────────────────────────────
  // Project Display / Selector
  let projectHeaderControl = '';
  const assignedProjects = JSON.parse(localStorage.getItem('assignedProjects') || '[]');
  const allowedProjects = PROJECTS.filter(p => userRole === 'admin' || assignedProjects.includes(p.id));

  if (userRole === 'admin') {
    const options = [
      `<option value="ALL"${selectedProjectId === 'ALL' ? ' selected' : ''}>All Projects</option>`,
      ...PROJECTS.map(p => `<option value="${p.id}"${p.id === selectedProjectId ? ' selected' : ''}>${p.name}</option>`)
    ].join('');
    projectHeaderControl = `
      <div class="project-selector-container me-3" style="display:inline-flex; align-items:center; gap:8px;">
        <label style="font-size:10.5px; font-weight:700; color:#64748B; text-transform:uppercase; margin:0; letter-spacing:0.5px;">Project View:</label>
        <select class="form-select form-select-sm" id="globalProjectSelector" style="font-size:12.5px; font-weight:600; color:#1E293B; border-radius:6px; border:1px solid #CBD5E1; padding:4px 30px 4px 10px; background:#FFF; cursor:pointer;" onchange="handleProjectChange(this.value)">
          ${options}
        </select>
      </div>`;
  } else if (allowedProjects.length > 1) {
    const options = allowedProjects.map(p => `<option value="${p.id}"${p.id === selectedProjectId ? ' selected' : ''}>${p.name}</option>`).join('');
    projectHeaderControl = `
      <div class="project-selector-container me-3" style="display:inline-flex; align-items:center; gap:8px;">
        <label style="font-size:10.5px; font-weight:700; color:#64748B; text-transform:uppercase; margin:0; letter-spacing:0.5px;">Project View:</label>
        <select class="form-select form-select-sm" id="globalProjectSelector" style="font-size:12.5px; font-weight:600; color:#1E293B; border-radius:6px; border:1px solid #CBD5E1; padding:4px 30px 4px 10px; background:#FFF; cursor:pointer;" onchange="handleProjectChange(this.value)">
          ${options}
        </select>
      </div>`;
  } else {
    const activeProj = allowedProjects[0] || PROJECTS[0];
    projectHeaderControl = `
      <div class="project-display-container me-3" style="display:inline-flex; align-items:center;">
        <span style="font-size:11.5px; font-weight:700; background:#FFF7ED; color:var(--gov-primary); border:1px solid #FFEDD5; padding:4px 12px; border-radius:20px; letter-spacing:0.3px;">
          <i class="fa-solid fa-folder-open me-1"></i> Project: ${activeProj.name}
        </span>
      </div>`;
  }

  const header = `
    <header class="main-header navbar navbar-expand-lg">
      <div class="container-fluid d-flex justify-content-between align-items-center">
        <a class="navbar-brand brand-logo-container text-decoration-none" href="index.html">
          <img class="brand-logo-img" src="oil-india.svg" alt="Oil India Limited Logo">
          <div class="brand-text">
            <h1 class="brand-title">UNNATI</h1>
            <span class="brand-subtitle">Intelligent Schedule-Linking Layer &amp; Progress Capture</span>
          </div>
        </a>
        <div class="header-right ms-auto d-flex align-items-center">
          ${projectHeaderControl}
          <img class="digital-india-logo d-none d-lg-block me-3" src="digital-india.png" alt="Digital India Logo">
          <button class="btn btn-outline-warning btn-sm me-3" onclick="resetDatabase()">
            <i class="fa-solid fa-rotate-right me-1"></i> Reset Demo
          </button>
          <div class="status-indicator online">
            <span class="dot"></span> Active
          </div>
        </div>
      </div>
    </header>`;

  // ── 3. SIDEBAR NAV ITEMS ─────────────────────────────────────────
  const collapsedPages = ['agent', 'plan'];
  const isCollapsed = collapsedPages.includes(activePage);
  const sidebarClass = `sidebar${isCollapsed ? ' collapsed' : ''}`;

  const navLinks = itemsToShow.map(item => {
    const isActive = item.key === activePage;
    const onclickAttr = item.onclick ? ` onclick="${item.onclick}"` : '';
    const badge = item.badge
      ? `<span class="badge bg-danger ms-auto px-2 py-1 rounded-pill" id="pendingReviewBadge">0</span>`
      : '';
    return `
        <a class="nav-link${isActive ? ' active' : ''}" href="${item.href}"${onclickAttr}>
          <i class="fa-solid ${item.icon} nav-icon"></i>
          <span>${item.label}</span>
          ${badge}
        </a>`;
  }).join('');

  const sidebar = `
    <aside class="${sidebarClass}">
      <nav class="nav flex-column sidebar-nav">
        ${navLinks}
      </nav>
      <div class="sidebar-footer">
        <div class="user-profile d-flex justify-content-between align-items-center w-100" style="width:100%;">
          <div class="d-flex align-items-center">
            <i class="fa-solid fa-user-tie profile-avatar me-2" style="margin-right:8px;"></i>
            <div class="profile-info">
              <span class="profile-name">${profileName}</span>
              <span class="profile-role" style="font-size:11px;">${profileRole}</span>
            </div>
          </div>
          <a href="login.html" class="logout-btn" title="Log Out"
             style="color:var(--gov-primary);transition:color 0.15s;font-size:15px;margin-left:auto;cursor:pointer;">
            <i class="fa-solid fa-right-from-bracket"></i>
          </a>
        </div>
      </div>
    </aside>`;

  // ── 4. INJECT INTO PAGE ──────────────────────────────────────────
  const layoutEl = document.getElementById('app-layout');
  if (!layoutEl) {
    console.error('[components.js] Could not find #app-layout element.');
    return;
  }

  layoutEl.insertAdjacentHTML('beforebegin', govBanner + header);
  layoutEl.insertAdjacentHTML('afterbegin', sidebar);
}

window.handleProjectChange = function(projId) {
  localStorage.setItem('selectedProjectId', projId);
  window.location.reload();
};
