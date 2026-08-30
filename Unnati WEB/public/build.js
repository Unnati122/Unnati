const fs = require('fs');

// 1. Generate new-project.html
let html1 = fs.readFileSync('new-project.html', 'utf8');

const newContent1 = `
        <!-- Page Header -->
        <div class="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h2 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 700; color: #1E293B; margin: 0 0 4px 0;">Create New Project</h2>
            <p style="font-size: 12.5px; color: #64748B; margin: 0;">Provision a new project and assign a Project Manager.</p>
          </div>
        </div>

        <div class="row">
          <div class="col-lg-8 mx-auto">
            <div class="card settings-card">
              <div class="settings-card-header d-flex justify-content-between align-items-center">
                <h5 class="mb-0 text-dark font-weight-bold" style="font-size: 14.5px; font-family: 'Outfit', sans-serif;">
                  <i class="fa-solid fa-plus text-primary me-2"></i>New Project Details
                </h5>
              </div>
              <div class="card-body p-4">
                <form id="newProjectForm" onsubmit="createProject(event)">
                  <h6 class="text-primary font-weight-bold mb-3">Project Information</h6>
                  <div class="row mb-3">
                    <div class="col-md-6">
                      <label class="form-label">Project ID</label>
                      <input type="text" class="form-control" id="pId" placeholder="e.g. PROJ-4" required>
                    </div>
                    <div class="col-md-6">
                      <label class="form-label">Project Name</label>
                      <input type="text" class="form-control" id="pName" placeholder="e.g. Digboi Expansion" required>
                    </div>
                  </div>
                  
                  <hr class="my-4">
                  <h6 class="text-primary font-weight-bold mb-3">Manager Credentials</h6>
                  <p style="font-size:12px; color:#64748B; margin-bottom:15px;">Credentials will be emailed directly to the manager. They can log in immediately.</p>
                  
                  <div class="row mb-3">
                    <div class="col-md-6">
                      <label class="form-label">Manager Name</label>
                      <input type="text" class="form-control" id="mName" placeholder="e.g. Rahul Bose" required>
                    </div>
                    <div class="col-md-6">
                      <label class="form-label">Manager Email</label>
                      <input type="email" class="form-control" id="mEmail" placeholder="e.g. rahul@oilindia.in" required>
                    </div>
                  </div>

                  <div class="row mb-4">
                    <div class="col-md-6">
                      <label class="form-label">Username</label>
                      <input type="text" class="form-control" id="mUsername" placeholder="e.g. rahul-pm" required>
                    </div>
                    <div class="col-md-6">
                      <label class="form-label">Temporary Password</label>
                      <input type="text" class="form-control" id="mPassword" placeholder="e.g. tempPass123!" required>
                    </div>
                  </div>

                  <button type="submit" class="btn btn-primary w-100 font-weight-bold" style="background:var(--gov-primary); border:none;">
                    <i class="fa-solid fa-paper-plane me-2"></i>Create Project & Email Credentials
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
`;

html1 = html1.replace(/<!-- Page Header -->[\s\S]*?(?=<\/div>\s*<\/main>)/, newContent1);
html1 = html1.replace(/let activePage = 'settings';/, "let activePage = 'new-project';");

const scripts1 = `
  <script>
    async function createProject(e) {
      e.preventDefault();
      const payload = {
        projectId: document.getElementById('pId').value,
        projectName: document.getElementById('pName').value,
        managerName: document.getElementById('mName').value,
        managerEmail: document.getElementById('mEmail').value,
        managerUsername: document.getElementById('mUsername').value,
        managerPassword: document.getElementById('mPassword').value,
      };

      const btn = e.target.querySelector('button[type="submit"]');
      btn.disabled = true;
      btn.innerText = 'Creating...';

      const result = await fetchAPI('/api/admin/projects', {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      if(result && result.success) {
        alert(result.message);
        window.location.reload();
      } else {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-paper-plane me-2"></i>Create Project & Email Credentials';
      }
    }
  </script>
`;
html1 = html1.replace(/<script>[\s\S]*?async function fetchManagers[\s\S]*?<\/script>/, scripts1);
fs.writeFileSync('new-project.html', html1);


// 2. Generate add-workers.html
let html2 = fs.readFileSync('add-workers.html', 'utf8');

const newContent2 = `
        <!-- Page Header -->
        <div class="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h2 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 700; color: #1E293B; margin: 0 0 4px 0;">Add Field Workers</h2>
            <p style="font-size: 12.5px; color: #64748B; margin: 0;">Provision mobile app access for field staff.</p>
          </div>
        </div>

        <div class="row">
          <div class="col-lg-8 mx-auto">
            <div class="card settings-card">
              <div class="settings-card-header d-flex justify-content-between align-items-center">
                <h5 class="mb-0 text-dark font-weight-bold" style="font-size: 14.5px; font-family: 'Outfit', sans-serif;">
                  <i class="fa-solid fa-user-plus text-primary me-2"></i>New Worker Credentials
                </h5>
              </div>
              <div class="card-body p-4">
                <form id="addWorkerForm" onsubmit="createWorker(event)">
                  <p style="font-size:12px; color:#64748B; margin-bottom:15px;">Credentials will be emailed directly to the worker. They can log in immediately.</p>
                  
                  <div class="row mb-3">
                    <div class="col-md-6">
                      <label class="form-label">Worker Name</label>
                      <input type="text" class="form-control" id="wName" placeholder="e.g. Amit Singh" required>
                    </div>
                    <div class="col-md-6">
                      <label class="form-label">Worker Email</label>
                      <input type="email" class="form-control" id="wEmail" placeholder="e.g. amit@oilindia.in" required>
                    </div>
                  </div>

                  <div class="row mb-4">
                    <div class="col-md-6">
                      <label class="form-label">Username</label>
                      <input type="text" class="form-control" id="wUsername" placeholder="e.g. amit-field" required>
                    </div>
                    <div class="col-md-6">
                      <label class="form-label">Temporary Password</label>
                      <input type="text" class="form-control" id="wPassword" placeholder="e.g. tempPass123!" required>
                    </div>
                  </div>

                  <button type="submit" class="btn btn-primary w-100 font-weight-bold" style="background:var(--gov-primary); border:none;">
                    <i class="fa-solid fa-paper-plane me-2"></i>Create Worker & Email Credentials
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
`;

html2 = html2.replace(/<!-- Page Header -->[\s\S]*?(?=<\/div>\s*<\/main>)/, newContent2);
html2 = html2.replace(/let activePage = 'settings';/, "let activePage = 'add-workers';");

const scripts2 = `
  <script>
    async function createWorker(e) {
      e.preventDefault();
      const payload = {
        workerName: document.getElementById('wName').value,
        workerEmail: document.getElementById('wEmail').value,
        workerUsername: document.getElementById('wUsername').value,
        workerPassword: document.getElementById('wPassword').value,
      };

      const btn = e.target.querySelector('button[type="submit"]');
      btn.disabled = true;
      btn.innerText = 'Creating...';

      const result = await fetchAPI('/api/manager/workers', {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      if(result && result.success) {
        alert(result.message);
        window.location.reload();
      } else {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-paper-plane me-2"></i>Create Worker & Email Credentials';
      }
    }
  </script>
`;
html2 = html2.replace(/<script>[\s\S]*?async function fetchManagers[\s\S]*?<\/script>/, scripts2);
fs.writeFileSync('add-workers.html', html2);
console.log('Pages generated successfully!');
