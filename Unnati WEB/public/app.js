// UNNATI Frontend Application Logic
const API_URL = 'http://localhost:3005'; // Point to standalone backend

// Helper functions for API requests
async function fetchAPI(endpoint, options = {}) {
  const token = localStorage.getItem('token');
  if (!token && !window.location.pathname.endsWith('login.html')) {
    window.location.href = 'login.html';
    return null;
  }

  const pId = localStorage.getItem('selectedProjectId') || 'PROJ-1';
  let separator = endpoint.includes('?') ? '&' : '?';
  let url = `${endpoint}`;
  if (!endpoint.includes('projectId=') && !endpoint.startsWith('/api/reset') && !endpoint.startsWith('/api/projects')) {
    url = `${url}${separator}projectId=${pId}`;
  }

  if (options.method === 'POST' && options.body) {
    try {
      const parsedBody = JSON.parse(options.body);
      if (!parsedBody.projectId) {
        parsedBody.projectId = pId;
        options.body = JSON.stringify(parsedBody);
      }
    } catch (e) {}
  }

  try {
    const { headers: optHeaders, ...restOptions } = options;
    const response = await fetch(`${API_URL}${url}`, {
      ...restOptions,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...(optHeaders || {})
      }
    });
    
    if (response.status === 401) {
      const errData = await response.json();
      alert(`Access Denied: ${errData.error || 'Session expired. Please log in again.'}`);
      localStorage.clear();
      window.location.href = 'login.html';
      return null;
    }
    
    if (!response.ok) {
      let errMsg = `Request failed (${response.status})`;
      try {
        const errData = await response.json();
        errMsg = errData.error || errMsg;
      } catch(e) {}
      alert(errMsg);
      return null;
    }
    return await response.json();
  } catch (error) {
    console.error(`API Error on ${endpoint}:`, error);
    alert('Could not connect to the server. Please check your connection.');
    return null;
  }
}

// 1. DASHBOARD LOGIC
async function loadDashboard() {
  const baseline = await fetchAPI('/api/baseline');
  const logs = await fetchAPI('/api/logs');

  if (!baseline || !logs) return;

  // Calculate Metrics
  const totalTasks = baseline.length;
  const pendingReviews = logs.filter(l => l.status === 'Pending Review').length;
  
  // Progress calculations
  const totalProgress = baseline.reduce((acc, task) => acc + (task.progress || 0), 0);
  const avgProgress = totalTasks > 0 ? (totalProgress / totalTasks).toFixed(0) : 0;

  // Average confidence calculations (linked items)
  const linkedLogs = logs.filter(l => l.status === 'Linked');
  const avgConfidence = linkedLogs.length > 0
    ? (linkedLogs.reduce((acc, log) => acc + (log.confidenceScore || 0), 0) / linkedLogs.length * 100).toFixed(0)
    : 100;

  // Update DOM metrics
  document.getElementById('totalTasksCount').innerText = totalTasks;
  document.getElementById('overallProgress').innerText = `${avgProgress}%`;
  document.getElementById('pendingReviewsCount').innerText = pendingReviews;
  document.getElementById('avgConfidence').innerText = `${avgConfidence}%`;

  // Sync badges
  const badge = document.getElementById('pendingReviewBadge');
  if (badge) {
    badge.innerText = pendingReviews;
    badge.style.display = pendingReviews > 0 ? 'inline-block' : 'none';
  }
  const dot = document.getElementById('aiMatchingDot');
  if (dot) {
    dot.style.display = 'none';
  }

  // Render Table
  renderScheduleTable(baseline);

  // Render Ingestion Audit logs
  renderTimeline(logs);
}

function renderScheduleTable(tasks) {
  const tbody = document.querySelector('#scheduleTable tbody');
  if (!tbody) return;

  if (tasks.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" class="text-center py-4">No baseline tasks loaded.</td></tr>';
    return;
  }

  tbody.innerHTML = tasks.map(task => {
    // Helper to format dates YYYY-MM-DD -> DD-MMM-YY
    const formatD = (dStr) => {
      if (!dStr || dStr === '-') return '-';
      const parts = dStr.split('-');
      if (parts.length === 3) {
        const dateObj = new Date(parts[0], parts[1] - 1, parts[2]);
        const day = String(dateObj.getDate()).padStart(2, '0');
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const month = months[dateObj.getMonth()];
        const year = String(dateObj.getFullYear()).substring(2);
        return `${day}-${month}-${year}`;
      }
      return dStr;
    };

    const plannedStartFormatted = formatD(task.plannedStart);
    const plannedEndFormatted = formatD(task.plannedEnd);
    const actualStartFormatted = formatD(task.actualStart);
    const actualEndFormatted = formatD(task.actualEnd);

    // Dynamic icon selection based on leaf type and status
    let iconHTML = '';
    if (task.isParent) {
      iconHTML = `<i class="fa-solid fa-angle-down" style="color: #64748B; margin-right: 6px; width: 12px; font-size: 10px;"></i>`;
    } else {
      if (task.status === 'Closed') {
        iconHTML = `<i class="fa-regular fa-circle" style="color: #94A3B8; margin-right: 6px; width: 12px; font-size: 10px;"></i>`;
      } else if (task.status === 'Active') {
        iconHTML = `<i class="fa-regular fa-circle-play" style="color: #0A2240; margin-right: 6px; width: 12px; font-size: 11px;"></i>`;
      } else {
        iconHTML = `<i class="fa-regular fa-clock" style="color: #94A3B8; margin-right: 6px; width: 12px; font-size: 10px;"></i>`;
      }
    }

    // Status Badge Styling matching the design screenshot
    let badgeStyle = '';
    const statusUpper = (task.status || '').toUpperCase();
    if (statusUpper === 'DELAYED') {
      badgeStyle = 'background: #FEE2E2; color: #991B1B; font-weight: 700; font-size: 9.5px; padding: 4px 8px; border-radius: 4px; display: inline-block; width: 75px; text-align: center; text-transform: uppercase;';
    } else if (statusUpper === 'ON TRACK') {
      badgeStyle = 'background: #DBEAFE; color: #1E3A8A; font-weight: 700; font-size: 9.5px; padding: 4px 8px; border-radius: 4px; display: inline-block; width: 75px; text-align: center; text-transform: uppercase;';
    } else if (statusUpper === 'CLOSED') {
      badgeStyle = 'background: #F3F4F6; color: #374151; font-weight: 700; font-size: 9.5px; padding: 4px 8px; border-radius: 4px; display: inline-block; width: 75px; text-align: center; text-transform: uppercase;';
    } else if (statusUpper === 'ACTIVE') {
      badgeStyle = 'background: #0A2240; color: #FFFFFF; font-weight: 700; font-size: 9.5px; padding: 4px 8px; border-radius: 4px; display: inline-block; width: 75px; text-align: center; text-transform: uppercase;';
    } else { // PLANNED
      badgeStyle = 'background: #F9FAFB; color: #6B7280; border: 1px solid #E5E7EB; font-weight: 700; font-size: 9.5px; padding: 3px 8px; border-radius: 4px; display: inline-block; width: 75px; text-align: center; text-transform: uppercase;';
    }

    // Variance formatting
    let varStyle = 'color: #64748B; font-weight: 500;';
    if (task.variance && task.variance.startsWith('-') && task.variance !== '-') {
      varStyle = 'color: #EF4444; font-weight: 700;';
    }

    // Confidence formatting
    const confVal = task.aiConf || '-';
    let confStyle = 'color: #64748B; font-weight: 500;';
    if (confVal !== '-' && confVal !== '') {
      confStyle = 'color: #0A2240; font-weight: 700;';
    }

    const levelOffset = (task.level || 0) * 16;
    const isRowDelayed = statusUpper === 'DELAYED';
    const rowBgStyle = isRowDelayed ? 'background: #FFF5F5;' : '';

    return `
      <tr style="${rowBgStyle}">
        <td style="padding: 10px 8px; padding-left: ${12 + levelOffset}px; font-weight: 500; color: #1E293B; width: 21%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">
          ${iconHTML}${task.description}
        </td>
        <td style="padding: 10px 8px; color: #64748B; font-weight: 500; width: 11.5%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${task.id}</td>
        <td style="padding: 10px 8px; color: #334155; width: 9%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${plannedStartFormatted}</td>
        <td style="padding: 10px 8px; color: #334155; width: 9%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${plannedEndFormatted}</td>
        <td style="padding: 10px 8px; color: #334155; text-align: center; width: 9%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${actualStartFormatted || '-'}</td>
        <td style="padding: 10px 8px; color: #334155; text-align: center; width: 9%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${actualEndFormatted || '-'}</td>
        <td style="padding: 10px 8px; text-align: right; font-weight: 700; color: #1E293B; width: 7%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${task.progress}%</td>
        <td style="padding: 10px 8px; text-align: right; ${varStyle} width: 7%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${task.variance || '-'}</td>
        <td style="padding: 10px 8px; text-align: right; ${confStyle} width: 6.5%; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${confVal}</td>
        <td style="padding: 10px 16px; text-align: right; width: 11%; white-space: nowrap;">
          <span style="${badgeStyle}">${task.status}</span>
        </td>
      </tr>
    `;
  }).join('');
}

function renderTimeline(logs) {
  const container = document.getElementById('timelineContainer');
  if (!container) return;

  if (logs.length === 0) {
    container.innerHTML = '<div class="text-center py-4 text-muted">No activities ingested yet.</div>';
    return;
  }

  // Render recent 8 logs
  const recentLogs = logs.slice(0, 8);
  container.innerHTML = recentLogs.map(log => {
    const isLinked = log.status === 'Linked';
    const logTime = new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const logDate = new Date(log.timestamp).toLocaleDateString([], { month: 'short', day: 'numeric' });
    const activityTitle = log.extracted && log.extracted.activity ? log.extracted.activity : log.rawText;
    const photoHtml = log.photoFilePath ? `
      <div style="margin-top: 8px;">
        <img src="${API_URL}${log.photoFilePath}" style="max-width: 260px; max-height: 140px; border-radius: 8px; border: 1px solid #E2E8F0;" alt="Site photo" />
      </div>
    ` : '';
    const audioHtml = log.audioFilePath ? `
      <div style="margin-top: 8px;">
        <audio controls src="${API_URL}${log.audioFilePath}" style="width: 100%; max-width: 300px; height: 28px;"></audio>
      </div>
    ` : '';
    
    return `
      <div class="timeline-item ${isLinked ? 'linked' : 'pending'}">
        <span class="timeline-time">${logDate} at ${logTime} &bull; ${log.source}</span>
        <div class="timeline-title text-dark">${activityTitle}</div>
        <div class="timeline-desc font-size-12 mt-1">
          <strong>Discipline:</strong> ${log.extracted.discipline || 'General'} | <strong>Status:</strong> ${log.extracted.status || 'Pending Review'}<br>
          <span class="text-secondary font-size-11">${log.auditTrail}</span>
          ${photoHtml}
          ${audioHtml}
        </div>
      </div>
    `;
  }).join('');
}

window.clearAllData = async function() {
  window.showConfirmModal("Are you sure you want to completely erase all project data and logs from the database? This action is irreversible.", async () => {
    try {
      const response = await fetch('/api/reset', { method: 'POST' });
      const result = await response.json();
      if (result.success) {
        window.showAlertModal("All database records have been erased successfully. Reloading dashboard.");
        setTimeout(() => {
          window.location.href = 'index.html';
        }, 1500);
      } else {
        window.showAlertModal("Error erasing data: " + result.error);
      }
    } catch (error) {
      console.error("Error calling reset API:", error);
      window.showAlertModal("Network error while trying to reset database.");
    }
  }, "Erase Database");
};

// Helper to keep page badges updated globally
async function updatePendingReviewBadge() {
  const logs = await fetchAPI('/api/logs');
  if (!logs) return;
  const pendingReviews = logs.filter(l => l.status === 'Pending Review').length;
  const badge = document.getElementById('pendingReviewBadge');
  if (badge) {
    badge.innerText = pendingReviews;
    badge.style.display = pendingReviews > 0 ? 'inline-block' : 'none';
  }
  const dot = document.getElementById('aiMatchingDot');
  if (dot) {
    dot.style.display = 'none';
  }
}


// 2. DATA INGESTION PORTAL LOGIC
async function handleIngest(event) {
  event.preventDefault();
  const textInput = document.getElementById('rawReportText').value;
  const statusBadge = document.getElementById('extractionStatus');
  const placeholder = document.getElementById('placeholderMsg');
  const resultsList = document.getElementById('resultsList');
  const submitBtn = document.getElementById('submitBtn');

  if (!textInput.trim()) return;

  statusBadge.className = 'badge bg-warning animate-pulse';
  statusBadge.innerText = 'Analyzing with Gemini LLM...';
  submitBtn.disabled = true;

  const result = await fetchAPI('/api/ingest-text', {
    method: 'POST',
    body: JSON.stringify({ rawText: textInput, source: 'Text Report Upload' })
  });

  submitBtn.disabled = false;

  if (!result || !result.success) {
    statusBadge.className = 'badge bg-danger';
    statusBadge.innerText = 'Analysis Failed';
    return;
  }

  statusBadge.className = 'badge bg-success';
  statusBadge.innerText = 'Successfully Synced';

  // Clear placeholders
  placeholder.classList.add('d-none');
  resultsList.classList.remove('d-none');

  // Render logs
  resultsList.innerHTML = `
    <h6 class="mb-3 text-uppercase font-size-12 tracking-wider text-muted font-weight-bold">Extracted Updates & Mapping</h6>
  ` + result.processedLogs.map(log => {
    const isMatched = log.status === 'Linked';
    const confidencePct = (log.confidenceScore * 100).toFixed(0);
    const cardClass = isMatched ? 'matched' : 'review';
    
    return `
      <div class="extraction-card ${cardClass} p-3 mb-3">
        <div class="d-flex justify-content-between align-items-start mb-2">
          <span class="badge ${isMatched ? 'bg-success' : 'bg-danger'}">${log.status}</span>
          <span class="font-size-12 confidence-indicator ${isMatched ? 'text-success' : 'text-danger'}">
            <i class="fa-solid ${isMatched ? 'fa-circle-check' : 'fa-circle-exclamation'}"></i> ${confidencePct}% Match Confidence
          </span>
        </div>
        <div class="mb-2">
          <strong>Raw Extracted:</strong> <span class="text-dark font-size-13">${log.extracted.activity}</span>
        </div>
        <div class="row font-size-12 text-muted mb-2">
          <div class="col-6"><strong>Discipline:</strong> ${log.extracted.discipline}</div>
          <div class="col-6"><strong>Progress Status:</strong> ${log.extracted.status}</div>
        </div>
        <div class="border-top pt-2 mt-2 font-size-12 text-secondary">
          <i class="fa-solid fa-code-merge me-1"></i> 
          <strong>Mapped Task:</strong> ${isMatched ? `[${log.matchedTaskId}] ${log.matchedTaskDescription}` : 'Pending Planner Selection'}
          <p class="mb-0 mt-1 text-muted font-italic">${log.auditTrail}</p>
        </div>
      </div>
    `;
  }).join('');

  updatePendingReviewBadge();
}


// 3. CONVERSATIONAL UNNATI LOGIC
let activeChatHistory = [];

async function loadChatPanel() {
  const baseline = await fetchAPI('/api/baseline');
  const listContainer = document.getElementById('activeTasksList');
  if (!baseline || !listContainer) return;

  listContainer.innerHTML = baseline.map(task => {
    let progressColor = 'bg-secondary';
    if (task.progress > 0) progressColor = 'bg-primary';
    if (task.progress === 100) progressColor = 'bg-success';

    return `
      <div class="list-group-item list-group-item-action px-3 py-2.5" style="border: 1px solid #E2E8F0 !important; border-radius: 8px !important; margin-bottom: 12px !important; background: #FFFFFF; box-shadow: 0 1px 3px rgba(0,0,0,0.02); transition: transform 0.15s ease, box-shadow 0.15s ease;" onmouseover="this.style.transform='translateY(-1px)'; this.style.boxShadow='0 4px 8px rgba(0,0,0,0.04)';" onmouseout="this.style.transform='none'; this.style.boxShadow='0 1px 3px rgba(0,0,0,0.02)';">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <span class="badge ${progressColor}" style="font-size: 9.5px; padding: 3px 6px; font-weight: 700;">${task.status} (${task.progress}%)</span>
          <span class="text-muted" style="font-size: 10px; font-weight: 600; letter-spacing: 0.3px;">${task.id}</span>
        </div>
        <div class="font-weight-semibold text-dark mb-2" style="font-size: 12px; line-height: 1.4; font-family: 'Outfit', sans-serif;">${task.description}</div>
        <div style="font-size: 10.5px; color: #64748B; border-top: 1px solid #F1F5F9; padding-top: 6px; display: flex; justify-content: space-between;">
          <span>WBS: ${task.wbs}</span>
          <span style="font-weight: 500; color: #475569;">${task.discipline}</span>
        </div>
      </div>
    `;
  }).join('');
}

async function handleSendChat(event) {
  event.preventDefault();
  const inputEl = document.getElementById('chatInput');
  const chatLogs = document.getElementById('chatLogs');
  const messageText = inputEl.value.trim();

  if (!messageText) return;

  // Append user message (supports both old chat-message and new msg-row layouts)
  const userMsgEl = document.createElement('div');
  const userTimeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const isNewLayout = !!document.querySelector('.messages-feed');
  if (isNewLayout) {
    userMsgEl.className = 'msg-row user-row';
    userMsgEl.innerHTML = `
      <div class="msg-av user-av"><i class="fa-solid fa-user"></i></div>
      <div class="msg-content">
        <div class="msg-bubble">${messageText}</div>
        <div class="msg-meta">${userTimeStr}</div>
      </div>`;
  } else {
    userMsgEl.className = 'chat-message user';
    userMsgEl.innerHTML = `<div class="chat-bubble">${messageText}</div><span class="chat-time">${userTimeStr}</span>`;
  }
  chatLogs.appendChild(userMsgEl);
  chatLogs.scrollTop = chatLogs.scrollHeight;

  // Clear input
  inputEl.value = '';

  // Append loading bubble
  const loadingMsgEl = document.createElement('div');
  if (isNewLayout) {
    loadingMsgEl.className = 'msg-row';
    loadingMsgEl.innerHTML = `
      <div class="msg-av agent-av"><i class="fa-solid fa-robot"></i></div>
      <div class="msg-content"><div class="msg-bubble" style="color:#94A3B8;"><i class="fa-solid fa-ellipsis fa-fade"></i>&nbsp; Processing update&hellip;</div></div>`;
  } else {
    loadingMsgEl.className = 'chat-message agent';
    loadingMsgEl.innerHTML = `<div class="chat-bubble text-muted"><i class="fa-solid fa-ellipsis animate-pulse"></i> Processing update...</div>`;
  }
  chatLogs.appendChild(loadingMsgEl);
  chatLogs.scrollTop = chatLogs.scrollHeight;

  // Send request
  const result = await fetchAPI('/api/chat', {
    method: 'POST',
    body: JSON.stringify({
      message: messageText,
      chatHistory: activeChatHistory
    })
  });

  // Remove loading bubble
  chatLogs.removeChild(loadingMsgEl);

  if (result && result.reply) {
    // Generate matches HTML if there are matches
    let matchesHTML = '';
    if (result.matches && result.matches.length > 0) {
      const match = result.matches[0];
      const confPct = Math.round((match.confidenceScore || 0) * 100);
      
      if (match.confidenceScore >= 0.75) {
        matchesHTML = `
          <div class="match-card">
            <div class="match-label"><span class="match-label-dot"></span> Matched Schedule Activity</div>
            <div class="match-grid">
              <div class="match-field">
                <label>Activity</label>
                <div class="fv">${match.matchedTaskDescription || match.extracted.activity}</div>
              </div>
              <div class="match-field">
                <label>Discipline</label>
                <div class="fv">${match.extracted.discipline}</div>
              </div>
            </div>
            <div class="match-grid" style="margin-top:14px;">
              <div class="match-field">
                <label>Status</label>
                <div class="stag"><span class="dot"></span>${match.extracted.status}</div>
              </div>
              <div class="match-field">
                <label>Confidence</label>
                <div class="conf-wrap">
                  <div class="conf-track"><div class="conf-fill" style="width:${confPct}%;"></div></div>
                  <span class="conf-pct">${confPct}%</span>
                </div>
              </div>
            </div>
            <div class="match-divider"></div>
            <div class="match-actions">
              <button onclick="confirmChatMatch('${match.id}', this)" class="btn-confirm">
                <i class="fa-solid fa-check"></i> Confirm Update
              </button>
              <button onclick="editChatMatch('${match.id}', this)" class="btn-ghost">
                <i class="fa-solid fa-pencil" style="font-size:11px;"></i> Edit
              </button>
              <a href="schedule.html" class="btn-ghost">
                <i class="fa-solid fa-eye" style="font-size:11px;"></i> View Schedule
              </a>
            </div>
          </div>`;
      } else {
        const optionsHTML = result.matches.map((item) => {
          const itemPct = Math.round((item.confidenceScore || 0) * 100);
          const matchColor = itemPct >= 75 ? '#059669' : '#D97706';
          return `
            <div style="background:#FFF;border:1px solid #E8EEF4;border-radius:8px;padding:12px 16px;margin-bottom:8px;display:flex;justify-content:space-between;align-items:center;cursor:pointer;transition:all .15s;" onclick="selectChatMatchOption('${item.id}','${item.matchedTaskId}',this)">
              <div>
                <div style="font-size:13px;font-weight:700;color:#1E293B;line-height:1.35;">${item.matchedTaskDescription || item.extracted.activity}</div>
                <div style="font-size:11px;color:#64748B;margin-top:4px;">Activity ID: ${item.matchedTaskId} &bull; ${item.extracted.discipline}</div>
              </div>
              <div style="display:flex;align-items:center;gap:12px;">
                <div style="text-align:right;">
                  <span style="font-size:10.5px;color:#64748B;display:block;">Match</span>
                  <span style="font-size:13px;font-weight:700;color:${matchColor};">${itemPct}%</span>
                </div>
                <input type="radio" name="matchSelect-${match.id}" style="width:16px;height:16px;accent-color:#0A2240;">
              </div>
            </div>`;
        }).join('');
        matchesHTML = `
          <div class="match-card">
            <div class="match-label"><i class="fa-solid fa-list-ul" style="color:#D97706;"></i>&nbsp; Possible Schedule Matches &mdash; Select One</div>
            <div style="margin-bottom:14px;">${optionsHTML}</div>
            <div style="display:flex;justify-content:flex-end;">
              <button onclick="confirmSelectedMatchOption('${match.id}',this)" class="btn-confirm">Confirm Selected</button>
            </div>
          </div>`;
      }
    }

    const agentMsgEl = document.createElement('div');
    const agentTimeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    if (isNewLayout) {
      agentMsgEl.className = 'msg-row';
      agentMsgEl.innerHTML = `
        <div class="msg-av agent-av"><i class="fa-solid fa-robot"></i></div>
        <div class="msg-content" style="max-width:72%;">
          <div class="msg-bubble" style="max-width:600px;">${result.reply}${matchesHTML}</div>
          <div class="msg-meta">Agent &bull; ${agentTimeStr}</div>
        </div>`;
    } else {
      agentMsgEl.className = 'chat-message agent';
      agentMsgEl.innerHTML = `<div class="chat-bubble">${result.reply}${matchesHTML}</div><span class="chat-time">Agent &bull; ${agentTimeStr}</span>`;
    }
    chatLogs.appendChild(agentMsgEl);
    chatLogs.scrollTop = chatLogs.scrollHeight;

    // Update history
    activeChatHistory.push({ role: 'user', content: messageText });
    activeChatHistory.push({ role: 'model', content: result.reply });
  }

  // Refresh panels
  loadChatPanel();
  updatePendingReviewBadge();
  if (typeof loadFieldUpdates === 'function') {
    loadFieldUpdates();
  }
  if (typeof loadRecentLogs === 'function') {
    loadRecentLogs();
  }
}


// 4. PLANNER REVIEW LOGIC
async function loadReviewQueue() {
  const logs = await fetchAPI('/api/logs');
  const baseline = await fetchAPI('/api/baseline');
  const queueContainer = document.getElementById('reviewQueue');
  const queueCount = document.getElementById('queueCount');

  if (!logs || !baseline || !queueContainer) return;

  const pendingLogs = logs.filter(l => l.status === 'Pending Review');
  queueCount.innerText = `${pendingLogs.length} Item${pendingLogs.length !== 1 ? 's' : ''}`;

  const badge = document.getElementById('pendingReviewBadge');
  if (badge) {
    badge.innerText = pendingLogs.length;
    badge.style.display = pendingLogs.length > 0 ? 'inline-block' : 'none';
  }
  const dot = document.getElementById('aiMatchingDot');
  if (dot) {
    dot.style.display = 'none';
  }

  if (pendingLogs.length === 0) {
    queueContainer.innerHTML = `
      <div class="col-12 text-center py-5 text-muted">
        <i class="fa-solid fa-circle-check text-success display-4 mb-3 d-block"></i>
        All caught up! No progress logs require manual verification.
      </div>
    `;
    return;
  }

  queueContainer.innerHTML = pendingLogs.map(log => {
    const confidencePct = (log.confidenceScore * 100).toFixed(0);
    const selectOptions = baseline.map(task => {
      const isSelected = log.matchedTaskId === task.id;
      return `<option value="${task.id}" ${isSelected ? 'selected' : ''}>[${task.id}] ${task.description} (${task.discipline})</option>`;
    }).join('');

    return `
      <div class="list-group-item" style="padding: 32px 24px; border-bottom: 1px solid #E2E8F0 !important; background-color: #FFFFFF;">
        <div class="review-item-row text-dark">
          
          <!-- Column 1: Raw Report and Source (Takes 45% width on large screens) -->
          <div style="min-width: 0;">
            <div class="d-flex align-items-center" style="margin-bottom: 8px; gap: 12px !important;">
              <span class="badge bg-danger-subtle text-danger px-2 py-0.5 rounded" style="font-size: 9.5px; font-weight: 600;">Verification Required</span>
              <span class="confidence-indicator text-danger" style="font-size: 11.5px; font-weight: 600;">
                <i class="fa-solid fa-triangle-exclamation"></i> Match: ${confidencePct}%
              </span>
            </div>
            <div class="font-weight-medium text-dark" style="font-size: 14px; line-height: 1.5; border-left: 3px solid var(--gov-primary); padding-left: 12px; font-style: italic; margin-bottom: 8px;">
              "${log.rawText}"
            </div>
            <span class="text-muted d-block" style="font-size: 10.5px; padding-left: 15px;">Source: ${log.source} &bull; ${new Date(log.timestamp).toLocaleString()}</span>
          </div>
 
          <!-- Column 2: Extracted Details in Badges (Takes 25% width on large screens) -->
          <div style="min-width: 0;">
            <div class="d-flex flex-column" style="align-items: flex-start !important; justify-content: flex-start !important;">
              <div class="text-muted text-uppercase" style="font-size: 9.5px; letter-spacing: 0.5px; font-weight: 600; margin-bottom: 6px;">Extracted Activity</div>
              <div class="font-weight-semibold text-dark" style="font-size: 13px; line-height: 1.4; margin-bottom: 8px;">${log.extracted.activity}</div>
              <div style="display: flex; flex-direction: row; justify-content: flex-start; align-items: center; gap: 8px; margin-top: 4px; width: 100%;">
                ${log.extracted.discipline && log.extracted.discipline.trim() ? `<span class="badge" style="font-size: 9.5px; padding: 4px 8px; margin: 0 !important; color: #475569 !important; background-color: #F1F5F9 !important; border: 1px solid #CBD5E1 !important;">${log.extracted.discipline}</span>` : ''}<span class="badge bg-success-subtle text-success" style="font-size: 9.5px; padding: 4px 8px; margin: 0 !important;">${log.extracted.status}</span>
              </div>
            </div>
          </div>

          <!-- Column 3: Linking Actions (Takes 30% width on large screens) -->
          <div style="min-width: 0;">
            <div class="text-dark font-weight-semibold" style="font-size: 12.5px; margin-bottom: 8px;">Link to baseline activity</div>
            <div class="d-flex w-100" style="gap: 12px;">
              <select class="form-select form-select-sm flex-grow-1" id="select-${log.id}" style="font-size: 12px; height: 36px; border-radius: 6px; border: 1px solid #CBD5E1; min-width: 0; width: 1%;">
                <option value="">-- Link baseline activity --</option>
                ${selectOptions}
              </select>
              <button class="btn btn-primary btn-sm px-3 flex-shrink-0" onclick="submitManualLink('${log.id}')" style="height: 36px; border-radius: 6px;">
                <i class="fa-solid fa-link me-1"></i> Link
              </button>
            </div>
          </div>

        </div>
      </div>
    `;
  }).join('');
}

async function submitManualLink(logId) {
  const selectEl = document.getElementById(`select-${logId}`);
  const taskId = selectEl.value;

  if (!taskId) {
    alert('Please select a valid baseline activity to link.');
    return;
  }

  const result = await fetchAPI('/api/link-manual', {
    method: 'POST',
    body: JSON.stringify({ logId, taskId })
  });

  if (result && result.success) {
    // Reload Review Queue
    loadReviewQueue();
  }
}


// 5. GLOBAL ACTIONS
async function resetDatabase() {
  if (confirm('Are you sure you want to reset the databases to baseline seed data? This will clear active logs.')) {
    const result = await fetchAPI('/api/reset', { method: 'POST' });
    if (result && result.success) {
      alert('Demo database successfully reset.');
      // Refresh current page if dashboard, review, etc.
      if (typeof loadDashboard === 'function' && document.getElementById('scheduleTable')) {
        loadDashboard();
      } else if (typeof loadReviewQueue === 'function' && document.getElementById('reviewQueue')) {
        loadReviewQueue();
      } else {
        window.location.reload();
      }
    }
  }
}

// Setup global project selector on load
document.addEventListener('DOMContentLoaded', async () => {
  if (!window.location.pathname.endsWith('login.html')) {
    await populateGlobalProjects();
    
    // Auto-refresh interval (every 4 seconds) for live updates
    if (typeof loadDashboard === 'function' && document.getElementById('scheduleTable')) {
      setInterval(loadDashboard, 4000);
    }
    if (typeof loadReviewQueue === 'function' && document.getElementById('reviewQueue')) {
      setInterval(loadReviewQueue, 4000);
    }
  }
});

async function populateGlobalProjects() {
  try {
    const result = await fetchAPI('/api/projects');
    if (result && Array.isArray(result)) {
      const selectedProjectId = localStorage.getItem('selectedProjectId') || 'PROJ-1';
      const userRole = localStorage.getItem('userRole');
      const loggedInUser = localStorage.getItem('username') || 'oil-admin';

      // Update sidebar project name for generic managers
      if (userRole === 'manager' && !['arjun-manager', 'priya-manager', 'amit-manager'].includes(loggedInUser)) {
        const assignedProjects = JSON.parse(localStorage.getItem('assignedProjects') || '[]');
        const allowedProjects = result.filter(p => userRole === 'admin' || assignedProjects.includes(p.id));
        const activeProj = allowedProjects.find(p => p.id === selectedProjectId) || allowedProjects[0] || result[0] || { name: 'No Active Project' };
        
        const profileNameEl = document.querySelector('.profile-name');
        if (profileNameEl) {
          profileNameEl.innerText = activeProj.name !== 'No Active Project' ? activeProj.name : "Project Overview";
        }
      }

      const select = document.getElementById('globalProjectSelector');
      if (select) {
        let optionsHtml = '';
        if (userRole === 'admin') {
          optionsHtml = `<option value="ALL"${selectedProjectId === 'ALL' ? ' selected' : ''}>All Projects</option>`;
        }
        
        optionsHtml += result.map(p => 
          `<option value="${p.id}"${p.id === selectedProjectId ? ' selected' : ''}>${p.name}</option>`
        ).join('');

        select.innerHTML = optionsHtml;
      }
    }
  } catch (e) {
    console.error('Failed to load global projects:', e);
  }
}
