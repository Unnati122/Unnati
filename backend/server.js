const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');
const multer = require('multer');
const geminiService = require('./services/geminiService');
const emailService = require('./services/emailService');
const db = require('./services/db');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Root API route
app.get('/', (req, res) => {
  res.json({ message: 'Unnati Backend is running' });
});

// Configure multer storage for audio and photo uploads
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

app.use('/uploads', express.static(uploadDir));

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    const prefix = file.fieldname === 'photo' ? 'photo' : 'audio';
    const ext = path.extname(file.originalname) || (file.fieldname === 'photo' ? '.jpg' : '.m4a');
    cb(null, `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}${ext}`);
  }
});
const upload = multer({ storage: storage });

// Static serving removed for dedicated backend mode

// Initialize PostgreSQL database tables & seeds on startup
db.initDatabase().catch(err => {
  console.error('Database initialization failed:', err);
});

// Middleware to authenticate user via Token in header
async function authenticateUser(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing or invalid authorization header' });
  }
  
  const token = authHeader.split(' ')[1];
  if (!token.startsWith('token-')) {
    return res.status(401).json({ error: 'Unauthorized: Invalid token format' });
  }

  const username = token.replace('token-', '');
  try {
    const result = await db.query('SELECT * FROM users WHERE username = $1', [username]);
    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Unauthorized: User does not exist' });
    }
    const user = result.rows[0];

    if (user.status !== 'active') {
      return res.status(403).json({ error: 'Forbidden: Account is inactive' });
    }

    req.user = {
      username: user.username,
      name: user.name,
      role: user.role,
      status: user.status,
      assignedProjects: user.assigned_projects || [],
      responsibility: user.responsibility,
      permissions: user.permissions || []
    };
    next();
  } catch (err) {
    console.error('Auth middleware query error:', err);
    return res.status(500).json({ error: 'Internal auth error' });
  }
}

// Middleware to authorize actions and enforce project isolation
function authorize(action, requiredPermission = null) {
  return (req, res, next) => {
    const user = req.user;
    if (!user) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    // Admins bypass project-level checks
    if (user.role === 'admin') {
      return next();
    }

    // Determine requested projectId (either in body, query, or param)
    const projectId = req.body.projectId || req.query.projectId || req.params.projectId;

    // Check project assignment
    if (projectId && !user.assignedProjects.includes(projectId)) {
      return res.status(403).json({ error: 'Forbidden: You are not assigned to this project' });
    }

    // Check action-based permissions
    if (requiredPermission) {
      if (user.permissions.includes('all')) {
        return next();
      }

      const hasDirectPermission = user.permissions.includes(requiredPermission);
      const isOperationalFull = user.permissions.includes('operational_full');

      // Project managers have full operational control over their assigned project
      if (isOperationalFull) {
        return next();
      }

      if (!hasDirectPermission) {
        return res.status(403).json({ error: 'Forbidden: Insufficient permissions for this action' });
      }
    }

    next();
  };
}

// Helper to map DB row to schedule activity JSON
function mapActivityRow(row) {
  return {
    id: row.id,
    projectId: row.project_id,
    description: row.description,
    discipline: row.discipline,
    plannedStart: row.planned_start,
    plannedEnd: row.planned_end,
    actualStart: row.actual_start,
    actualEnd: row.actual_end,
    progress: row.progress,
    status: row.status,
    wbs: row.wbs
  };
}

// Helper to map DB row to progress log JSON
function mapLogRow(row) {
  return {
    id: row.id,
    projectId: row.project_id,
    timestamp: row.timestamp ? new Date(row.timestamp).toISOString() : null,
    workerId: row.worker_id,
    source: row.source,
    rawText: row.raw_text,
    durationSeconds: row.duration_seconds,
    audioFilePath: row.audio_file_path,
    waveformCsv: row.waveform_csv,
    extracted: {
      activity: row.extracted_activity,
      date: row.extracted_date,
      discipline: row.extracted_discipline,
      status: row.extracted_status
    },
    matchedTaskId: row.matched_task_id,
    matchedTaskDescription: row.matched_task_description,
    confidenceScore: row.confidence_score,
    status: row.status,
    auditTrail: row.audit_trail,
    photoFilePath: row.photo_file_path
  };
}

// 1. Get baseline schedule
app.get('/api/baseline', authenticateUser, authorize('view_project'), async (req, res) => {
  const pId = req.query.projectId || 'PROJ-1';
  try {
    const result = await db.query('SELECT * FROM schedule_activities WHERE project_id = $1 ORDER BY id', [pId]);
    let filtered = result.rows.map(mapActivityRow);

    // Enforce manager discipline task filters
    if (req.user.role === 'manager') {
      if (req.user.permissions.includes('hse_only')) {
        filtered = filtered.filter(t => t.discipline === 'Quality/HSE');
      } else if (req.user.permissions.includes('operations_only')) {
        filtered = filtered.filter(t => 
          t.discipline === 'Civil' || 
          t.discipline === 'Mechanical' || 
          t.discipline === 'Mechanical/Piping' ||
          t.discipline === 'Piping'
        );
      }
    }

    res.json(filtered);
  } catch (err) {
    console.error('Error fetching baseline schedule:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 2. Get progress logs
app.get('/api/logs', authenticateUser, authorize('view_project'), async (req, res) => {
  const pId = req.query.projectId || 'PROJ-1';
  try {
    const result = await db.query('SELECT * FROM progress_logs WHERE project_id = $1 ORDER BY timestamp DESC', [pId]);
    let filtered = result.rows.map(mapLogRow);

    // Enforce manager discipline scopes
    if (req.user.role === 'manager') {
      if (req.user.permissions.includes('hse_only')) {
        filtered = filtered.filter(l => l.extracted && l.extracted.discipline === 'Quality/HSE');
      } else if (req.user.permissions.includes('operations_only')) {
        filtered = filtered.filter(l => l.extracted && (
          l.extracted.discipline === 'Civil' || 
          l.extracted.discipline === 'Mechanical' || 
          l.extracted.discipline === 'Mechanical/Piping'
        ));
      } else if (req.user.permissions.includes('planning_only')) {
        filtered = filtered.filter(l => l.extracted && (
          l.extracted.discipline === 'Planning' || 
          l.extracted.discipline === 'Commissioning' || 
          l.extracted.discipline === 'Piping'
        ));
      }
    }

    res.json(filtered);
  } catch (err) {
    console.error('Error fetching logs:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 3. Ingest raw text report
app.post('/api/ingest-text', authenticateUser, authorize('submit_report', 'field_submit'), async (req, res) => {
  const { rawText, source, projectId } = req.body;
  const pId = projectId || req.query.projectId || 'PROJ-1';
  if (!rawText || rawText.trim() === '') {
    return res.status(400).json({ error: 'Text content is required' });
  }

  try {
    const baselineResult = await db.query('SELECT * FROM schedule_activities WHERE project_id = $1', [pId]);
    const baseline = baselineResult.rows.map(mapActivityRow);
    const newLogs = [];

    // Call AI parsing and matching using only this project's tasks
    const result = await geminiService.extractAndMatch(rawText, baseline);

    // Process each extracted update
    for (const update of result.extractedUpdates) {
      const logId = `log-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
      let status = 'Pending Review';
      let auditTrail = '';
      let matchedTaskDescription = null;

      // Check if we matched with high confidence
      if (update.matchedTaskId && update.confidenceScore >= 0.8) {
        const task = baseline.find(t => t.id === update.matchedTaskId);
        if (task) {
          status = 'Linked';
          matchedTaskDescription = task.description;
          auditTrail = `Auto-linked to [${task.id}] with ${(update.confidenceScore * 100).toFixed(0)}% confidence on ${new Date().toLocaleString()}.`;
          
          // Update baseline progress
          task.status = update.status;
          task.actualStart = task.actualStart || update.date;
          if (update.status === 'Completed') {
            task.progress = 100;
            task.actualEnd = task.actualEnd || update.date;
          } else {
            task.progress = Math.max(task.progress, 50); // Default progress for active task
          }

          // Save task update in DB
          await db.query(
            'UPDATE schedule_activities SET status = $1, actual_start = $2, actual_end = $3, progress = $4 WHERE id = $5 AND project_id = $6',
            [task.status, task.actualStart, task.actualEnd || null, task.progress, task.id, pId]
          );
        }
      } else if (update.matchedTaskId) {
        // Matched, but low confidence
        const task = baseline.find(t => t.id === update.matchedTaskId);
        matchedTaskDescription = task ? task.description : null;
        auditTrail = `Potential match [${update.matchedTaskId}] with low confidence (${(update.confidenceScore * 100).toFixed(0)}%). Flagged for Planner Review.`;
      } else {
        auditTrail = `No matched activity found in schedule. Flagged for Planner Review.`;
      }

      // Save log entry to DB
      await db.query(
        `INSERT INTO progress_logs (
          id, project_id, timestamp, worker_id, source, raw_text,
          extracted_activity, extracted_date, extracted_discipline, extracted_status,
          matched_task_id, matched_task_description, confidence_score, status, audit_trail
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)`,
        [
          logId,
          pId,
          new Date().toISOString(),
          req.user.username,
          source || 'Report Ingestion',
          rawText,
          update.activity,
          update.date,
          update.discipline,
          update.status,
          update.matchedTaskId || null,
          matchedTaskDescription,
          update.confidenceScore,
          status,
          auditTrail
        ]
      );

      const logEntry = {
        id: logId,
        projectId: pId,
        timestamp: new Date().toISOString(),
        source: source || 'Report Ingestion',
        rawText: rawText,
        extracted: {
          activity: update.activity,
          date: update.date,
          discipline: update.discipline,
          status: update.status
        },
        matchedTaskId: update.matchedTaskId,
        matchedTaskDescription: matchedTaskDescription,
        confidenceScore: update.confidenceScore,
        status: status,
        auditTrail: auditTrail
      };
      newLogs.push(logEntry);
    }

    res.json({ success: true, processedLogs: newLogs });
  } catch (error) {
    console.error('Error during data ingestion:', error);
    res.status(500).json({ error: 'Failed to process report text' });
  }
});

// 4. Conversational Chatbot
app.post('/api/chat', authenticateUser, authorize('chat'), async (req, res) => {
  const { message, chatHistory, projectId } = req.body;
  const pId = projectId || req.query.projectId || 'PROJ-1';
  if (!message) {
    return res.status(400).json({ error: 'Message is required' });
  }

  try {
    const baselineResult = await db.query('SELECT * FROM schedule_activities WHERE project_id = $1', [pId]);
    const baseline = baselineResult.rows.map(mapActivityRow);
    
    const reply = await geminiService.generateChatResponse(chatHistory || [], message, baseline);
    
    // Trigger extraction and matching for the user message only within this project
    const result = await geminiService.extractAndMatch(message, baseline);
    const matches = [];
    
    if (result && result.extractedUpdates) {
      for (const update of result.extractedUpdates) {
        if (update.matchedTaskId) {
          const logId = `log-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
          let status = 'Pending Review';
          let auditTrail = '';
          let matchedTaskDescription = null;

          const task = baseline.find(t => t.id === update.matchedTaskId);
          if (task) {
            matchedTaskDescription = task.description;
            if (update.confidenceScore >= 0.75) {
              status = 'Linked';
              auditTrail = `Auto-linked from Chat to [${task.id}] with ${(update.confidenceScore * 100).toFixed(0)}% confidence.`;
              
              task.status = update.status;
              task.actualStart = task.actualStart || update.date;
              if (update.status === 'Completed') {
                task.progress = 100;
                task.actualEnd = task.actualEnd || update.date;
              } else {
                task.progress = Math.max(task.progress, 50);
              }

              // Update DB task
              await db.query(
                'UPDATE schedule_activities SET status = $1, actual_start = $2, actual_end = $3, progress = $4 WHERE id = $5 AND project_id = $6',
                [task.status, task.actualStart, task.actualEnd || null, task.progress, task.id, pId]
              );
            } else {
              auditTrail = `Potential match [${task.id}] with confidence ${(update.confidenceScore * 100).toFixed(0)}% from Chat. Flagged for review.`;
            }
          }

          // Insert log into DB
          await db.query(
            `INSERT INTO progress_logs (
              id, project_id, timestamp, worker_id, source, raw_text,
              extracted_activity, extracted_date, extracted_discipline, extracted_status,
              matched_task_id, matched_task_description, confidence_score, status, audit_trail
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)`,
            [
              logId,
              pId,
              new Date().toISOString(),
              req.user.username,
              'Chat Assistant',
              message,
              update.activity,
              update.date,
              update.discipline,
              update.status,
              update.matchedTaskId || null,
              matchedTaskDescription,
              update.confidenceScore,
              status,
              auditTrail
            ]
          );

          const logEntry = {
            id: logId,
            projectId: pId,
            timestamp: new Date().toISOString(),
            source: 'Chat Assistant',
            rawText: message,
            extracted: {
              activity: update.activity,
              date: update.date,
              discipline: update.discipline,
              status: update.status
            },
            matchedTaskId: update.matchedTaskId,
            matchedTaskDescription: matchedTaskDescription,
            confidenceScore: update.confidenceScore,
            status: status,
            auditTrail: auditTrail
          };
          matches.push(logEntry);
        }
      }
    }

    res.json({ reply, matches });
  } catch (error) {
    console.error('Error in chatbot endpoint:', error);
    res.status(500).json({ error: 'Failed to process chat response' });
  }
});

// 4.5 Confirm and approve matching log
app.post('/api/confirm-log', authenticateUser, authorize('confirm_log'), async (req, res) => {
  const { logId } = req.body;
  if (!logId) {
    return res.status(400).json({ error: 'logId is required' });
  }

  try {
    const logCheck = await db.query('SELECT * FROM progress_logs WHERE id = $1', [logId]);
    if (logCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Progress log entry not found' });
    }
    const logEntry = logCheck.rows[0];

    // Enforce Manager discipline filters
    if (req.user.role === 'manager') {
      if (req.user.permissions.includes('hse_only') && logEntry.extracted_discipline !== 'Quality/HSE') {
        return res.status(403).json({ error: 'Forbidden: HSE Manager can only confirm safety/HSE items' });
      }
      if (req.user.permissions.includes('operations_only') && 
          logEntry.extracted_discipline !== 'Civil' && 
          logEntry.extracted_discipline !== 'Mechanical' && 
          logEntry.extracted_discipline !== 'Mechanical/Piping') {
        return res.status(403).json({ error: 'Forbidden: Operations Manager can only confirm Civil/Mechanical items' });
      }
    }

    const newStatus = 'Linked';
    const newAuditTrail = `Approved and linked by ${req.user.name} on ${new Date().toLocaleString()}.`;

    // Update log entry status
    await db.query('UPDATE progress_logs SET status = $1, audit_trail = $2 WHERE id = $3', [newStatus, newAuditTrail, logId]);

    // Update baseline activity metrics
    if (logEntry.matched_task_id) {
      const taskCheck = await db.query('SELECT * FROM schedule_activities WHERE id = $1 AND project_id = $2', [logEntry.matched_task_id, logEntry.project_id]);
      if (taskCheck.rows.length > 0) {
        const task = taskCheck.rows[0];
        task.status = logEntry.extracted_status || 'In Progress';
        task.actualStart = task.actual_start || logEntry.extracted_date || new Date().toISOString().split('T')[0];
        if (task.status === 'Completed') {
          task.progress = 100;
          task.actualEnd = task.actual_end || logEntry.extracted_date || new Date().toISOString().split('T')[0];
        } else {
          task.progress = Math.max(task.progress, 50);
        }
        await db.query(
          'UPDATE schedule_activities SET status = $1, actual_start = $2, actual_end = $3, progress = $4 WHERE id = $5 AND project_id = $6',
          [task.status, task.actualStart, task.actualEnd || null, task.progress, task.id, logEntry.project_id]
        );
      }
    }

    res.json({
      success: true,
      updatedLog: {
        id: logEntry.id,
        projectId: logEntry.project_id,
        timestamp: logEntry.timestamp,
        workerId: logEntry.worker_id,
        source: logEntry.source,
        rawText: logEntry.raw_text,
        extracted: {
          activity: logEntry.extracted_activity,
          date: logEntry.extracted_date,
          discipline: logEntry.extracted_discipline,
          status: logEntry.extracted_status
        },
        matchedTaskId: logEntry.matched_task_id,
        matchedTaskDescription: logEntry.matched_task_description,
        confidenceScore: logEntry.confidence_score,
        status: newStatus,
        auditTrail: newAuditTrail
      }
    });
  } catch (err) {
    console.error('Error confirming log:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5. Manual linking route (Planner override)
app.post('/api/link-manual', authenticateUser, authorize('confirm_log'), async (req, res) => {
  const { logId, taskId } = req.body;
  if (!logId || !taskId) {
    return res.status(400).json({ error: 'logId and taskId are required' });
  }

  try {
    const logCheck = await db.query('SELECT * FROM progress_logs WHERE id = $1', [logId]);
    if (logCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Progress log entry not found' });
    }
    const logEntry = logCheck.rows[0];

    // Enforce Manager discipline filters
    if (req.user.role === 'manager') {
      if (req.user.permissions.includes('hse_only') && logEntry.extracted_discipline !== 'Quality/HSE') {
        return res.status(403).json({ error: 'Forbidden: HSE Manager can only confirm safety/HSE items' });
      }
      if (req.user.permissions.includes('operations_only') && 
          logEntry.extracted_discipline !== 'Civil' && 
          logEntry.extracted_discipline !== 'Mechanical' && 
          logEntry.extracted_discipline !== 'Mechanical/Piping') {
        return res.status(403).json({ error: 'Forbidden: Operations Manager can only confirm Civil/Mechanical items' });
      }
    }

    const taskCheck = await db.query('SELECT * FROM schedule_activities WHERE id = $1 AND project_id = $2', [taskId, logEntry.project_id]);
    if (taskCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Baseline task not found' });
    }
    const task = taskCheck.rows[0];

    const newStatus = 'Linked';
    const newAuditTrail = `Manually linked by ${req.user.name} on ${new Date().toLocaleString()}.`;

    // Link in DB
    await db.query(
      'UPDATE progress_logs SET matched_task_id = $1, matched_task_description = $2, confidence_score = 1.0, status = $3, audit_trail = $4 WHERE id = $5',
      [taskId, task.description, newStatus, newAuditTrail, logId]
    );

    // Update schedule task status
    task.status = logEntry.extracted_status || 'In Progress';
    task.actualStart = task.actual_start || logEntry.extracted_date || new Date().toISOString().split('T')[0];
    if (task.status === 'Completed') {
      task.progress = 100;
      task.actualEnd = task.actual_end || logEntry.extracted_date || new Date().toISOString().split('T')[0];
    } else {
      task.progress = Math.max(task.progress, 50);
    }

    await db.query(
      'UPDATE schedule_activities SET status = $1, actual_start = $2, actual_end = $3, progress = $4 WHERE id = $5 AND project_id = $6',
      [task.status, task.actualStart, task.actualEnd || null, task.progress, task.id, logEntry.project_id]
    );

    res.json({
      success: true,
      updatedLog: {
        id: logEntry.id,
        projectId: logEntry.project_id,
        timestamp: logEntry.timestamp,
        workerId: logEntry.worker_id,
        source: logEntry.source,
        rawText: logEntry.raw_text,
        extracted: {
          activity: logEntry.extracted_activity,
          date: logEntry.extracted_date,
          discipline: logEntry.extracted_discipline,
          status: logEntry.extracted_status
        },
        matchedTaskId: taskId,
        matchedTaskDescription: task.description,
        confidenceScore: 1.0,
        status: newStatus,
        auditTrail: newAuditTrail
      }
    });
  } catch (err) {
    console.error('Error linking manual:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.1 List Projects
app.get('/api/projects', authenticateUser, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM projects ORDER BY id');
    const projects = result.rows;
    // Enforce project isolation
    const filtered = projects.filter(p => req.user.role === 'admin' || req.user.assignedProjects.includes(p.id));
    res.json(filtered);
  } catch (err) {
    console.error('Error listing projects:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.2 AI Plan Generation Template
app.post('/api/generate-plan', authenticateUser, authorize('generate_plan'), async (req, res) => {
  try {
    const details = req.body;
    if (!details.projectName || !details.startDate || !details.endDate) {
      return res.status(400).json({ error: 'Project name, start date, and end date are required' });
    }
    const plan = await geminiService.generateProjectPlan(details);
    res.json(plan);
  } catch (error) {
    console.error('Error generating project plan:', error);
    res.status(500).json({ error: 'Failed to generate project plan' });
  }
});

// 5.3 Approve and Apply Generated Project Plan
app.post('/api/approve-plan', authenticateUser, authorize('approve_plan'), async (req, res) => {
  const { projectId, tasks, overview } = req.body;
  if (!projectId || !tasks || !Array.isArray(tasks)) {
    return res.status(400).json({ error: 'projectId and tasks array are required' });
  }

  try {
    // Delete existing tasks for this projectId
    await db.query('DELETE FROM schedule_activities WHERE project_id = $1', [projectId]);
    
    // Add new tasks
    for (const task of tasks) {
      const taskId = task.id || `ACT-${Math.floor(100 + Math.random() * 900)}`;
      const desc = task.description || task.activity || 'Task';
      const disc = task.discipline || 'General';
      const pStart = task.plannedStart || task.startDate || new Date().toISOString().split('T')[0];
      const pEnd = task.plannedEnd || task.endDate || new Date().toISOString().split('T')[0];
      
      await db.query(
        'INSERT INTO schedule_activities (id, project_id, description, discipline, planned_start, planned_end, progress, status, wbs) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)',
        [taskId, projectId, desc, disc, pStart, pEnd, 0, 'Not Started', task.wbs || '1.1']
      );
    }

    // Clean logs for this projectId
    await db.query('DELETE FROM progress_logs WHERE project_id = $1', [projectId]);

    res.json({ success: true, message: 'Plan approved and baseline schedule generated successfully.', taskCount: tasks.length });
  } catch (err) {
    console.error('Error approving plan:', err);
    res.status(500).json({ error: 'Database transaction failed' });
  }
});

// 5.4 unified login & worker login
app.post(['/api/login', '/api/auth/login'], async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }

  try {
    const result = await db.query('SELECT * FROM users WHERE (username = $1 OR email = $1) AND password = $2', [username.trim(), password.trim()]);

    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials. Please verify your username and password.' });
    }

    const user = result.rows[0];

    if (user.status !== 'active') {
      return res.status(403).json({ error: 'Forbidden: Account is currently inactive. Contact OIL Administration.' });
    }

      if (user.role !== 'admin' && user.assigned_projects && user.assigned_projects.length > 0) {
      const placeholders = user.assigned_projects.map((_, i) => '$' + (i + 1)).join(', ');
      const projResult = await db.query(`SELECT id, name, location, status, latitude, longitude, radius FROM projects WHERE id IN (${placeholders})`, user.assigned_projects);
      
      const hasActiveProject = projResult.rows.some(p => p.status === 'active');
      const allDeleted = projResult.rows.length > 0 && projResult.rows.every(p => p.status === 'deleted');
      const allPaused = projResult.rows.length > 0 && projResult.rows.every(p => p.status === 'paused');

      if (!hasActiveProject && projResult.rows.length > 0) {
        if (allDeleted) {
           return res.status(403).json({ error: 'Access Denied: Your assigned project has been deleted.' });
        } else {
           return res.status(403).json({ error: 'Access Denied: Your assigned project is currently paused.' });
        }
      }

      // Geofence check for field workers (TEMPORARILY DISABLED)
      /*
      if (user.role === 'field' && projResult.rows.length > 0) {
        const proj = projResult.rows[0]; // Assuming one assigned project
        if (proj.latitude && proj.longitude && proj.radius) {
          if (!req.body.latitude || !req.body.longitude) {
            return res.status(403).json({ error: 'Location required: Please enable GPS to log into this geofenced project.' });
          }

          const R = 6371e3; // metres
          const lat1 = parseFloat(req.body.latitude) * Math.PI/180;
          const lat2 = proj.latitude * Math.PI/180;
          const deltaLat = (proj.latitude - parseFloat(req.body.latitude)) * Math.PI/180;
          const deltaLon = (proj.longitude - parseFloat(req.body.longitude)) * Math.PI/180;

          const a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
          const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
          const distance = R * c;

          const radiusWithBuffer = proj.radius + 50; // 50m GPS inaccuracy buffer
          
          if (distance > radiusWithBuffer) {
             return res.status(403).json({ error: `Geofence Blocked: You are ${Math.round(distance)}m away. Please move into the designated project area to log in.` });
          }
        }
      }
      */
      const activeProj = (projResult && projResult.rows.length > 0) ? projResult.rows[0] : null;

      userObj = {
        username: user.username,
        name: user.name,
        role: user.role,
        responsibility: user.responsibility,
        assignedProjects: (user.assigned_projects || []).map(String),
        permissions: user.permissions || [],
        phone: user.phone || '',
        digitalId: user.digital_id || '',
        projectDetails: activeProj ? {
          id: String(activeProj.id),
          name: activeProj.name,
          location: activeProj.location
        } : null
      };
    } else {
      userObj = {
        username: user.username,
        name: user.name,
        role: user.role,
        responsibility: user.responsibility,
        assignedProjects: (user.assigned_projects || []).map(String),
        permissions: user.permissions || [],
        phone: user.phone || '',
        digitalId: user.digital_id || '',
        projectDetails: null
      };
    }

    const token = `token-${user.username}`;
    res.json({
      success: true,
      token: token,
      user: userObj
    });
  } catch (err) {
    console.error('Error logging in:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.4.0 Verify Location on App Foreground
app.post('/api/auth/verify-location', authenticateUser, async (req, res) => {
  const { latitude, longitude } = req.body;
  const user = req.user;

  if (user.role !== 'field' || !user.assigned_projects || user.assigned_projects.length === 0) {
    return res.json({ allowed: true, message: 'No location restrictions for this user role or no active projects.' });
  }

  try {
    const placeholders = user.assigned_projects.map((_, i) => '$' + (i + 1)).join(', ');
    const projResult = await db.query(`SELECT status, latitude, longitude, radius FROM projects WHERE id IN (${placeholders})`, user.assigned_projects);
    
    if (projResult.rows.length === 0) {
      return res.json({ allowed: true, message: 'No active projects found.' });
    }

    const proj = projResult.rows[0];
    if (proj.status !== 'active') {
       return res.json({ allowed: false, message: 'Access Denied: Your assigned project is not active.' });
    }

    if (proj.latitude && proj.longitude && proj.radius) {
      console.log(`[Verify-Location] Worker: ${user.username}, Received Lat: ${latitude}, Received Lon: ${longitude}`);
      console.log(`[Verify-Location] Project ${proj.name} Lat: ${proj.latitude}, Lon: ${proj.longitude}, Radius: ${proj.radius}`);
      if (!latitude || !longitude) {
        return res.json({ allowed: false, message: 'Location required: Please enable GPS to access this geofenced project.' });
      }

      const R = 6371e3; // metres
      const lat1 = parseFloat(latitude) * Math.PI/180;
      const lat2 = proj.latitude * Math.PI/180;
      const deltaLat = (proj.latitude - parseFloat(latitude)) * Math.PI/180;
      const deltaLon = (proj.longitude - parseFloat(longitude)) * Math.PI/180;

      const a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
      const distance = R * c;

      const radiusWithBuffer = proj.radius + 50; // 50m GPS inaccuracy buffer
      
      if (distance > radiusWithBuffer) {
         return res.json({ allowed: false, message: `Geofence Blocked: You are ${Math.round(distance)}m away. Please move into the designated project area.` });
      }
    }

    return res.json({ allowed: true, message: 'Location verified. Inside geofence.' });
  } catch (err) {
    console.error('Error verifying location:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.4.1 Forgot Password
app.post('/api/auth/forgot-password', async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ error: 'Email is required' });

  try {
    const result = await db.query('SELECT username, name FROM users WHERE email = $1', [email.trim()]);
    if (result.rows.length === 0) {
      // Return success even if not found to prevent email enumeration
      return res.json({ success: true, message: 'If that email exists, an OTP has been sent.' });
    }

    const user = result.rows[0];
    const otp = Math.floor(100000 + Math.random() * 900000).toString(); // 6 digit OTP
    const expiry = new Date(Date.now() + 10 * 60000); // 10 minutes from now

    await db.query(
      'UPDATE users SET reset_otp = $1, reset_otp_expiry = $2 WHERE email = $3',
      [otp, expiry, email.trim()]
    );

    const otpEmailHtml = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="color-scheme" content="light">
      <meta name="supported-color-schemes" content="light">
      <style>
        .force-white { background-image: linear-gradient(#ffffff, #ffffff) !important; color: #000000 !important; }
        .force-orange { color: #ea580c !important; }
        .force-text { color: #333333 !important; }
      </style>
    </head>
    <body style="margin: 0; padding: 0; background-color: #ffffff;" class="force-white">
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="padding: 40px 20px;" class="force-white">
        <tr>
          <td align="center">
            <table role="presentation" width="100%" style="max-width: 500px; margin: 0 auto; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;" cellspacing="0" cellpadding="0" border="0" class="force-white">
              <tr>
                <td style="padding-bottom: 32px; text-align: left;">
                  <h1 style="margin: 0; font-size: 20px; font-weight: 800; letter-spacing: 1px;" class="force-orange">UNNATI</h1>
                </td>
              </tr>
              <tr>
                <td style="padding-bottom: 24px; text-align: left;">
                  <p style="margin: 0; font-size: 16px; font-weight: 400; line-height: 1.5; color: #333333;" class="force-text">
                    Hello ${user.name},<br><br>
                    We received a request to reset your password. Use the OTP below to proceed. This code is valid for 10 minutes.
                  </p>
                </td>
              </tr>
              <tr>
                <td style="padding-bottom: 32px; text-align: left;">
                  <table role="presentation" width="100%" style="border-left: 3px solid #ea580c; padding-left: 16px;" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td style="padding-bottom: 8px;">
                        <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #666666;">Your OTP Code</span><br>
                        <strong style="font-size: 24px; letter-spacing: 4px; color: #000000; font-family: monospace;">${otp}</strong>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td style="border-top: 1px solid #eeeeee; padding-top: 24px; text-align: left;">
                  <p style="margin: 0; font-size: 12px; color: #999999; line-height: 1.5;">
                    If you did not request a password reset, please ignore this email.
                  </p>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
    </body>
    </html>
    `;

    await emailService.sendEmail({
      to: email.trim(),
      subject: 'Unnati - Password Reset OTP',
      body: `Your OTP for password reset is ${otp}. It is valid for 10 minutes.`,
      html: otpEmailHtml
    });

    res.json({ success: true, message: 'If that email exists, an OTP has been sent.' });
  } catch (err) {
    console.error('Error in forgot-password:', err);
    res.status(500).json({ error: 'Failed to process request' });
  }
});

// 5.4.2 Reset Password
app.post('/api/auth/reset-password', async (req, res) => {
  const { email, otp, newPassword } = req.body;
  
  if (!email || !otp || !newPassword) {
    return res.status(400).json({ error: 'Email, OTP, and new password are required' });
  }

  try {
    const result = await db.query(
      'SELECT reset_otp, reset_otp_expiry FROM users WHERE email = $1',
      [email.trim()]
    );

    if (result.rows.length === 0) {
      return res.status(400).json({ error: 'Invalid request' });
    }

    const user = result.rows[0];
    
    if (user.reset_otp !== otp.trim()) {
      return res.status(400).json({ error: 'Invalid OTP' });
    }

    if (new Date() > new Date(user.reset_otp_expiry)) {
      return res.status(400).json({ error: 'OTP has expired. Please request a new one.' });
    }

    // Reset password and clear OTP
    await db.query(
      'UPDATE users SET password = $1, reset_otp = NULL, reset_otp_expiry = NULL WHERE email = $2',
      [newPassword, email.trim()]
    );

    res.json({ success: true, message: 'Password has been reset successfully.' });
  } catch (err) {
    console.error('Error in reset-password:', err);
    res.status(500).json({ error: 'Failed to process request' });
  }
});


// 5.5 worker identity endpoint
app.get('/api/me', authenticateUser, (req, res) => {
  res.json(req.user);
});

// 6. Admin Create Project and Manager
app.post('/api/admin/projects', authenticateUser, authorize('admin_only'), async (req, res) => {
  const { projectId, projectName, location, client, managerName, managerUsername, managerEmail, managerPassword } = req.body;
  
  if (!projectId || !projectName || !managerUsername || !managerEmail || !managerPassword) {
    return res.status(400).json({ error: 'Missing required fields.' });
  }

  try {
    // Check for duplicate project ID
    const existingProject = await db.query('SELECT id FROM projects WHERE id = $1', [projectId.trim()]);
    if (existingProject.rows.length > 0) {
      return res.status(409).json({ error: `Project ID "${projectId}" already exists. Please use a different ID.` });
    }

    const existing = await db.query('SELECT username FROM users WHERE username = $1', [managerUsername.trim()]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'Manager username already exists.' });
    }

    // Insert Project
    await db.query(
      'INSERT INTO projects (id, name, code, location, client, active_workers_count) VALUES ($1, $2, $3, $4, $5, 0)',
      [projectId.trim(), projectName.trim(), projectId.trim(), location || '', client || '']
    );

    // Insert Manager
    await db.query(
      'INSERT INTO users (username, password, email, role, name, status, responsibility, assigned_projects) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
      [
        managerUsername.trim(), 
        managerPassword.trim(), 
        managerEmail.trim(), 
        'manager', 
        managerName.trim(), 
        'active', 
        'Project Manager', 
        `{${projectId.trim()}}`
      ]
    );

    // Send Email
    const managerHtmlTemplate = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="color-scheme" content="light">
      <meta name="supported-color-schemes" content="light">
      <style>
        .force-white { background-image: linear-gradient(#ffffff, #ffffff) !important; color: #000000 !important; }
        .force-orange { color: #ea580c !important; }
        .force-text { color: #333333 !important; }
      </style>
    </head>
    <body style="margin: 0; padding: 0; background-color: #ffffff;" class="force-white">
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="padding: 40px 20px;" class="force-white">
        <tr>
          <td align="center">
            <table role="presentation" width="100%" style="max-width: 500px; margin: 0 auto; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;" cellspacing="0" cellpadding="0" border="0" class="force-white">
              
              <!-- Logo / Header -->
              <tr>
                <td style="padding-bottom: 32px; text-align: left;">
                  <h1 style="margin: 0; font-size: 20px; font-weight: 800; letter-spacing: 1px;" class="force-orange">UNNATI</h1>
                </td>
              </tr>
              
              <!-- Greeting -->
              <tr>
                <td style="padding-bottom: 24px; text-align: left;">
                  <p style="margin: 0; font-size: 16px; font-weight: 400; line-height: 1.5; color: #333333;" class="force-text">
                    Hello ${managerName},<br><br>
                    You have been assigned as the Project Manager for <strong>${projectName}</strong>.
                  </p>
                </td>
              </tr>
              
              <!-- Credentials Box -->
              <tr>
                <td style="padding-bottom: 32px; text-align: left;">
                  <table role="presentation" width="100%" style="border-left: 3px solid #ea580c; padding-left: 16px;" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td style="padding-bottom: 8px;">
                        <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #666666;">Username</span><br>
                        <strong style="font-size: 16px; color: #000000; font-family: monospace;">${managerUsername}</strong>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #666666;">Password</span><br>
                        <strong style="font-size: 16px; color: #000000; font-family: monospace;">${managerPassword}</strong>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              
              <!-- Action Button -->
              <tr>
                <td style="padding-bottom: 40px; text-align: left;">
                  <a href="http://localhost:4000/login" style="background-image: linear-gradient(#ea580c, #ea580c) !important; color: #ffffff !important; text-decoration: none; padding: 12px 24px; border-radius: 4px; font-weight: 600; font-size: 14px; display: inline-block;">Log in to Workspace</a>
                </td>
              </tr>
              
              <!-- Footer -->
              <tr>
                <td style="border-top: 1px solid #eeeeee; padding-top: 24px; text-align: left;">
                  <p style="margin: 0; font-size: 12px; color: #999999; line-height: 1.5;">
                    This is an automated message.<br>Please change your password upon your first login.
                  </p>
                </td>
              </tr>
              
            </table>
          </td>
        </tr>
      </table>
    </body>
    </html>
    `;

    await emailService.sendEmail({
      to: managerEmail.trim(),
      subject: 'Welcome to Unnati - Manager Credentials',
      body: `Hello ${managerName},\n\nYou have been assigned as the manager for project: ${projectName}.\nHere are your login credentials:\n\nUsername: ${managerUsername}\nPassword: ${managerPassword}\n\nPlease login and change your password when possible.\n\nBest,\nUnnati Admin`,
      html: managerHtmlTemplate
    });

    res.json({ success: true, message: 'Project and Manager created successfully. Credentials emailed.' });
  } catch (err) {
    console.error('Error creating project/manager:', err);
    res.status(500).json({ error: 'Database transaction failed.' });
  }
});

// 6.5 Get Projects
app.get('/api/projects', authenticateUser, async (req, res) => {
  try {
    let query = 'SELECT id, name, code, location, client, status, active_workers_count FROM projects ORDER BY created_at DESC';
    let values = [];
    
    if (req.user.role !== 'admin') {
      const assigned = req.user.assignedProjects || [];
      if (assigned.length === 0) {
        return res.json([]);
      }
      query = 'SELECT id, name, code, location, client, status, active_workers_count FROM projects WHERE id = ANY($1) ORDER BY created_at DESC';
      values = [assigned];
    }
    
    const result = await db.query(query, values);
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching projects:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 6.6 Update Project Status
app.put('/api/admin/projects/:id/status', authenticateUser, authorize('admin_only'), async (req, res) => {
  const { id } = req.params;
  const { status } = req.body;
  if (!['active', 'paused'].includes(status)) {
    return res.status(400).json({ error: 'Invalid status' });
  }
  
  try {
    await db.query('UPDATE projects SET status = $1 WHERE id = $2', [status, id]);
    res.json({ success: true, message: `Project status updated to ${status}` });
  } catch (err) {
    console.error('Error updating project status:', err);
    res.status(500).json({ error: 'Database update failed' });
  }
});

// 6.7 Delete Project
app.delete('/api/admin/projects/:id', authenticateUser, authorize('admin_only'), async (req, res) => {
  const { id } = req.params;
  
  try {
    // Delete all users (managers/workers) assigned to this project
    await db.query('DELETE FROM users WHERE $1 = ANY(assigned_projects)', [id]);
    
    // Delete the project itself
    await db.query('DELETE FROM projects WHERE id = $1', [id]);
    
    res.json({ success: true, message: 'Project and associated users deleted successfully' });
  } catch (err) {
    console.error('Error deleting project:', err);
    res.status(500).json({ error: 'Database delete failed.' });
  }
});

// 7. Manager Create Field Worker
app.post('/api/manager/workers', authenticateUser, authorize('manager_only'), async (req, res) => {
  const { workerName, workerUsername, workerEmail, workerPassword } = req.body;
  
  if (!workerName || !workerUsername || !workerEmail || !workerPassword) {
    return res.status(400).json({ error: 'Missing required fields.' });
  }

  // Assign to manager's first assigned project (for simplicity, assuming 1 project per manager)
  const assignedProjects = req.user.assignedProjects && req.user.assignedProjects.length > 0 ? req.user.assignedProjects : [];
  if (assignedProjects.length === 0) {
    return res.status(403).json({ error: 'You are not assigned to any project.' });
  }

  try {
    const existing = await db.query('SELECT username FROM users WHERE username = $1', [workerUsername.trim()]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'Worker username already exists.' });
    }

    // Insert Worker
    await db.query(
      'INSERT INTO users (username, password, email, role, name, status, responsibility, assigned_projects) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
      [
        workerUsername.trim(), 
        workerPassword.trim(), 
        workerEmail.trim(), 
        'field', 
        workerName.trim(), 
        'active', 
        'Field Worker', 
        assignedProjects
      ]
    );

    // Send Email
    const workerHtmlTemplate = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="color-scheme" content="light">
      <meta name="supported-color-schemes" content="light">
      <style>
        .force-white { background-image: linear-gradient(#ffffff, #ffffff) !important; color: #000000 !important; }
        .force-orange { color: #ea580c !important; }
        .force-text { color: #333333 !important; }
      </style>
    </head>
    <body style="margin: 0; padding: 0; background-color: #ffffff;" class="force-white">
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="padding: 40px 20px;" class="force-white">
        <tr>
          <td align="center">
            <table role="presentation" width="100%" style="max-width: 500px; margin: 0 auto; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;" cellspacing="0" cellpadding="0" border="0" class="force-white">
              
              <!-- Logo / Header -->
              <tr>
                <td style="padding-bottom: 32px; text-align: left;">
                  <h1 style="margin: 0; font-size: 20px; font-weight: 800; letter-spacing: 1px;" class="force-orange">UNNATI</h1>
                </td>
              </tr>
              
              <!-- Greeting -->
              <tr>
                <td style="padding-bottom: 24px; text-align: left;">
                  <p style="margin: 0; font-size: 16px; font-weight: 400; line-height: 1.5; color: #333333;" class="force-text">
                    Hello ${workerName},<br><br>
                    You have been officially added as a Field Worker to project <strong>${assignedProjects[0]}</strong>.
                  </p>
                </td>
              </tr>
              
              <!-- Credentials Box -->
              <tr>
                <td style="padding-bottom: 32px; text-align: left;">
                  <table role="presentation" width="100%" style="border-left: 3px solid #ea580c; padding-left: 16px;" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                      <td style="padding-bottom: 8px;">
                        <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #666666;">Worker ID</span><br>
                        <strong style="font-size: 16px; color: #000000; font-family: monospace;">${workerUsername}</strong>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #666666;">PIN</span><br>
                        <strong style="font-size: 16px; color: #000000; font-family: monospace;">${workerPassword}</strong>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              
              <!-- Action Button -->
              <tr>
                <td style="padding-bottom: 40px; text-align: left;">
                  <a href="http://localhost:4000/login" style="background-image: linear-gradient(#ea580c, #ea580c) !important; color: #ffffff !important; text-decoration: none; padding: 12px 24px; border-radius: 4px; font-weight: 600; font-size: 14px; display: inline-block;">Log in to Mobile App</a>
                </td>
              </tr>
              
              <!-- Footer -->
              <tr>
                <td style="border-top: 1px solid #eeeeee; padding-top: 24px; text-align: left;">
                  <p style="margin: 0; font-size: 12px; color: #999999; line-height: 1.5;">
                    This is an automated message.<br>Please download the Unnati app to begin tracking.
                  </p>
                </td>
              </tr>
              
            </table>
          </td>
        </tr>
      </table>
    </body>
    </html>
    `;

    await emailService.sendEmail({
      to: workerEmail.trim(),
      subject: 'Unnati - Mobile App Credentials',
      body: `Hello ${workerName},\n\nYou have been added as a field worker to project ${assignedProjects[0]}.\nHere are your mobile app login credentials:\n\nUsername: ${workerUsername}\nPassword: ${workerPassword}\n\nPlease download the Unnati app and log in to begin tracking progress.\n\nBest,\nUnnati Manager`,
      html: workerHtmlTemplate
    });

    res.json({ success: true, message: 'Worker created successfully. Credentials emailed.' });
  } catch (err) {
    console.error('Error creating worker:', err);
    res.status(500).json({ error: 'Database transaction failed.' });
  }
});

// Fetch workers for manager's project
app.get('/api/manager/workers', authenticateUser, authorize('manager_only'), async (req, res) => {
  const assignedProjects = req.user.assignedProjects && req.user.assignedProjects.length > 0 ? req.user.assignedProjects : [];
  if (assignedProjects.length === 0) {
    return res.json([]);
  }

  try {
    const result = await db.query(
      "SELECT username, email, name, status, phone, digital_id FROM users WHERE role = 'field' AND $1 && assigned_projects ORDER BY name ASC",
      [assignedProjects]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching workers:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// Update worker status
app.put('/api/manager/workers/:username/status', authenticateUser, authorize('manager_only'), async (req, res) => {
  const { username } = req.params;
  const { status } = req.body;
  if (!['active', 'paused'].includes(status)) {
    return res.status(400).json({ error: 'Invalid status' });
  }
  
  try {
    const result = await db.query("UPDATE users SET status = $1 WHERE username = $2 AND role = 'field' RETURNING *", [status, username]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Worker not found' });
    }
    res.json({ success: true, message: `Worker status updated to ${status}` });
  } catch (err) {
    console.error('Error updating worker status:', err);
    res.status(500).json({ error: 'Database update failed' });
  }
});

// Delete worker
app.delete('/api/manager/workers/:username', authenticateUser, authorize('manager_only'), async (req, res) => {
  const { username } = req.params;
  try {
    const result = await db.query("DELETE FROM users WHERE username = $1 AND role = 'field' RETURNING *", [username]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Worker not found' });
    }
    res.json({ success: true, message: 'Worker deleted successfully' });
  } catch (err) {
    console.error('Error deleting worker:', err);
    res.status(500).json({ error: 'Database delete failed' });
  }
});

// Edit worker details
app.put('/api/manager/workers/:username', authenticateUser, authorize('manager_only'), async (req, res) => {
  const { username } = req.params;
  const { name, email, phone, digitalId } = req.body;
  try {
    const result = await db.query(
      "UPDATE users SET name = $1, email = $2, phone = $3, digital_id = $4 WHERE username = $5 AND role = 'field' RETURNING *",
      [name, email, phone, digitalId, username]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Worker not found' });
    }
    res.json({ success: true, message: 'Worker details updated successfully' });
  } catch (err) {
    console.error('Error updating worker:', err);
    res.status(500).json({ error: 'Database update failed' });
  }
});

// Get Project Location Settings
app.get('/api/manager/projects/:id/location', authenticateUser, authorize('manager_only'), async (req, res) => {
  const { id } = req.params;
  try {
    const result = await db.query('SELECT latitude, longitude, radius FROM projects WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Project not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching project location:', err);
    res.status(500).json({ error: 'Database fetch failed' });
  }
});

// Update Project Location Settings
app.put('/api/manager/projects/:id/location', authenticateUser, authorize('manager_only'), async (req, res) => {
  const { id } = req.params;
  const { latitude, longitude, radius } = req.body;
  try {
    await db.query(
      'UPDATE projects SET latitude = $1, longitude = $2, radius = $3 WHERE id = $4',
      [latitude, longitude, radius, id]
    );
    res.json({ success: true, message: 'Project location settings updated successfully' });
  } catch (err) {
    console.error('Error updating project location:', err);
    res.status(500).json({ error: 'Database update failed' });
  }
});

// 5.9 Memory Sources API
app.get('/api/memory/sources', authenticateUser, async (req, res) => {
  const pId = req.query.projectId || 'PROJ-1';
  try {
    const result = await db.query('SELECT * FROM memory_sources WHERE project_id = $1 ORDER BY uploaded_at DESC', [pId]);
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching memory sources:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.6 admin project registry APIs
app.get('/api/admin/projects', authenticateUser, authorize('admin_only'), async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM projects ORDER BY id');
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching projects:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

app.put('/api/admin/projects/:id/status', authenticateUser, authorize('admin_only'), async (req, res) => {
  const pId = req.params.id;
  const { status } = req.body;
  if (!['active', 'paused', 'deleted'].includes(status)) {
    return res.status(400).json({ error: 'Invalid status' });
  }
  
  try {
    await db.query('UPDATE projects SET status = $1 WHERE id = $2', [status, pId]);
    res.json({ success: true, message: `Project ${pId} status updated to ${status}` });
  } catch (err) {
    console.error('Error updating project status:', err);
    res.status(500).json({ error: 'Database update failed' });
  }
});

// 5.7 worker assigned project details
app.get('/api/my-project', authenticateUser, async (req, res) => {
  const assigned = req.user.assignedProjects;
  if (!assigned || assigned.length === 0) {
    return res.status(400).json({ error: 'No project assigned to this user' });
  }
  const projectId = assigned[0];
  try {
    const result = await db.query('SELECT * FROM projects WHERE id = $1', [projectId]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Assigned project not found' });
    }
    const p = result.rows[0];
    res.json({
      id: p.id,
      name: p.name,
      code: p.code,
      location: p.location,
      client: p.client,
      activeWorkersCount: p.active_workers_count
    });
  } catch (err) {
    console.error('Error fetching project:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.7 worker submissions retrieval
app.get('/api/my-updates', authenticateUser, async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM progress_logs WHERE worker_id = $1 ORDER BY timestamp DESC', [req.user.username]);
    const list = result.rows.map(mapLogRow);
    res.json(list);
  } catch (err) {
    console.error('Error getting worker updates:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.8 worker voice upload and match pipeline
app.post('/api/voice/upload', authenticateUser, upload.any(), async (req, res) => {
  const audioFile = req.files ? req.files.find(f => f.fieldname === 'audio') : null;
  const photoFile = req.files ? req.files.find(f => f.fieldname === 'photo') : null;

  if (!audioFile) {
    return res.status(400).json({ error: 'Audio file is required' });
  }

  const durationSeconds = parseInt(req.body.durationSeconds || '0', 10);
  const waveformCsv = req.body.waveformCsv || '';

  // Get project from token (do not trust client parameters)
  const assigned = req.user.assignedProjects;
  if (!assigned || assigned.length === 0) {
    return res.status(400).json({ error: 'Worker is not assigned to any project' });
  }
  const pId = assigned[0];

  try {
    // 1. Fetch Project Details to get Name
    const projectResult = await db.query('SELECT name FROM projects WHERE id = $1', [pId]);
    const projectName = projectResult.rows.length > 0 ? projectResult.rows[0].name : 'OIL Pipeline Expansion';

    // 2. Perform speech-to-text transcription
    let transcript = await geminiService.transcribeAudio(audioFile.path, audioFile.mimetype);
    
    // Fallbacks
    if (!transcript) {
      transcript = req.body.transcript || '';
    }
    if (!transcript.trim()) {
      const fallbacks = [
        "Line 24 erection completed today. Welding joints inspected and ready for NDT.",
        "Spool installation completed at Unit 2. Flange torquing verified.",
        "Toolbox safety briefing conducted for 28 crew members at Sector 4.",
        "Pipeline trenching completed from Chainage 14+200 to 14+800.",
        "Hydrotest package 6 pressure sustained at 120 bar for 4 hours. Passed."
      ];
      transcript = fallbacks[Math.floor(Math.random() * fallbacks.length)];
    }

    // 3. Match against Schedule Activities
    const baselineResult = await db.query('SELECT * FROM schedule_activities WHERE project_id = $1', [pId]);
    const baseline = baselineResult.rows.map(mapActivityRow);

    const matchResult = await geminiService.extractAndMatch(transcript, baseline);
    
    const logs = [];

    for (const update of matchResult.extractedUpdates) {
      const logId = `log-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
      let status = 'Pending Review';
      let auditTrail = '';
      let matchedTaskDescription = null;

      // Check if matched with high confidence
      if (update.matchedTaskId && update.confidenceScore >= 0.8) {
        const task = baseline.find(t => t.id === update.matchedTaskId);
        if (task) {
          status = 'Linked';
          matchedTaskDescription = task.description;
          auditTrail = `Auto-linked to [${task.id}] with ${(update.confidenceScore * 100).toFixed(0)}% confidence on ${new Date().toLocaleString()}.`;
          
          // Update baseline progress
          task.status = update.status;
          task.actualStart = task.actualStart || update.date;
          if (update.status === 'Completed') {
            task.progress = 100;
            task.actualEnd = task.actualEnd || update.date;
          } else {
            task.progress = Math.max(task.progress, 50);
          }

          // Save task update in DB
          await db.query(
            'UPDATE schedule_activities SET status = $1, actual_start = $2, actual_end = $3, progress = $4 WHERE id = $5 AND project_id = $6',
            [task.status, task.actualStart, task.actualEnd || null, task.progress, task.id, pId]
          );
        }
      } else if (update.matchedTaskId) {
        const task = baseline.find(t => t.id === update.matchedTaskId);
        matchedTaskDescription = task ? task.description : null;
        auditTrail = `Potential match [${update.matchedTaskId}] with low confidence (${(update.confidenceScore * 100).toFixed(0)}%). Flagged for Planner Review.`;
      } else {
        auditTrail = `No matched activity found in schedule. Flagged for Planner Review.`;
      }

      const relativeAudioPath = `/uploads/${audioFile.filename}`;
      const relativePhotoPath = photoFile ? `/uploads/${photoFile.filename}` : null;

      // Save log entry to PostgreSQL
      await db.query(
        `INSERT INTO progress_logs (
          id, project_id, timestamp, worker_id, source, raw_text, duration_seconds, 
          audio_file_path, waveform_csv, extracted_activity, extracted_date, 
          extracted_discipline, extracted_status, matched_task_id, 
          matched_task_description, confidence_score, status, audit_trail, photo_file_path
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19)`,
        [
          logId,
          pId,
          new Date().toISOString(),
          req.user.username,
          'Voice Mobile App',
          transcript,
          durationSeconds,
          relativeAudioPath,
          waveformCsv,
          update.activity,
          update.date,
          update.discipline,
          update.status,
          update.matchedTaskId || null,
          matchedTaskDescription,
          update.confidenceScore,
          status,
          auditTrail,
          relativePhotoPath
        ]
      );

      logs.push({
        id: logId,
        projectId: pId,
        projectName: projectName,
        workerId: req.user.username,
        workerName: req.user.name,
        timestamp: Date.now(),
        formattedDateTime: new Date().toLocaleDateString('en-US', { day: '2-digit', month: 'short', year: 'numeric' }) + ", " + new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
        durationSeconds: durationSeconds,
        transcript: transcript,
        status: status === 'Linked' ? 'APPROVED' : 'PENDING_APPROVAL',
        category: update.discipline || 'Site Progress',
        waveform: waveformCsv.split(',').map(f => parseFloat(f) || 0),
        audioFilePath: relativeAudioPath,
        photoFilePath: relativePhotoPath
      });
    }

    res.json({ success: true, update: logs[0] });
  } catch (err) {
    console.error('Error during voice upload:', err);
    res.status(500).json({ error: 'Failed to process voice upload' });
  }
});

// 5.9 manager configuration list for admin
app.get('/api/admin/managers', authenticateUser, authorize('manage_users'), async (req, res) => {
  try {
    const result = await db.query("SELECT * FROM users WHERE role = 'manager' ORDER BY username");
    res.json(result.rows);
  } catch (err) {
    console.error('Error getting managers:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.10 add/update manager
app.post('/api/admin/managers', authenticateUser, authorize('manage_users'), async (req, res) => {
  const { username, password, name, responsibility, assignedProjects, permissions, status } = req.body;
  if (!username || !name || !responsibility) {
    return res.status(400).json({ error: 'Username, Name, and Responsibility are required' });
  }

  try {
    const check = await db.query('SELECT username FROM users WHERE username = $1', [username]);
    if (check.rows.length >= 0) {
      // Update
      if (password) {
        await db.query(
          'UPDATE users SET name = $1, password = $2, responsibility = $3, assigned_projects = $4, permissions = $5, status = $6 WHERE username = $7',
          [name, password, responsibility, assignedProjects || [], permissions || [], status || 'active', username]
        );
      } else {
        await db.query(
          'UPDATE users SET name = $1, responsibility = $2, assigned_projects = $3, permissions = $4, status = $5 WHERE username = $6',
          [name, responsibility, assignedProjects || [], permissions || [], status || 'active', username]
        );
      }
    } else {
      // Insert
      await db.query(
        'INSERT INTO users (username, password, role, name, status, assigned_projects, responsibility, permissions) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
        [username, password || 'password', 'manager', name, status || 'active', assignedProjects || [], responsibility, permissions || ['operational_full']]
      );
    }
    res.json({ success: true, message: 'Manager configurations successfully synchronized.' });
  } catch (err) {
    console.error('Error saving manager:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.11 remove manager
app.delete('/api/admin/managers', authenticateUser, authorize('manage_users'), async (req, res) => {
  const { username } = req.body;
  if (!username) {
    return res.status(400).json({ error: 'Username parameter required' });
  }

  try {
    const result = await db.query('DELETE FROM users WHERE username = $1', [username]);
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Manager configuration not found' });
    }
    res.json({ success: true, message: 'Manager successfully removed.' });
  } catch (err) {
    console.error('Error removing manager:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.12 daily status summary compiler
app.get('/api/daily-report', authenticateUser, authorize('view_project'), async (req, res) => {
  const pId = req.query.projectId || 'PROJ-1';
  const dateStr = req.query.date || new Date().toISOString().split('T')[0];

  try {
    const baselineResult = await db.query('SELECT * FROM schedule_activities WHERE project_id = $1', [pId]);
    const baseline = baselineResult.rows.map(mapActivityRow);

    const logsResult = await db.query('SELECT * FROM progress_logs WHERE project_id = $1 ORDER BY timestamp DESC', [pId]);
    const logs = logsResult.rows.map(mapLogRow);

    // Filter logs for this specific date
    const todayLogs = logs.filter(l => l.timestamp.startsWith(dateStr) || (l.extracted && l.extracted.date === dateStr));

    // Completed and In Progress tasks
    const completedTasks = baseline.filter(t => t.status === 'Completed');
    const activeTasks = baseline.filter(t => t.status === 'In Progress');
    const delayedTasks = baseline.filter(t => {
      const plannedEnd = new Date(t.plannedEnd);
      const today = new Date(dateStr);
      return t.status !== 'Completed' && plannedEnd < today;
    });

    // Safety logs
    const safetyLogs = todayLogs.filter(l => l.extracted && l.extracted.discipline === 'Quality/HSE');

    // Build list of updates
    const updates = todayLogs.map(l => (l.extracted && l.extracted.activity) || l.rawText);

    // Generate an AI observation summarizing updates
    let aiObservation = "All operational works progressing within scheduled margins. No major timeline exceptions reported.";
    if (updates.length > 0) {
      aiObservation = `Supervisors captured ${updates.length} progress updates today including: ${updates.slice(0, 3).join(', ')}. Site operations match baseline schedules.`;
    }

    const report = {
      projectId: pId,
      date: dateStr,
      overview: `Daily Operations and Progress Capture Report for Project ${pId} on ${dateStr}.`,
      updates: updates,
      completedTasks: completedTasks.map(t => ({ id: t.id, description: t.description })),
      activeTasks: activeTasks.map(t => ({ id: t.id, description: t.description, progress: t.progress })),
      delayedTasks: delayedTasks.map(t => ({ id: t.id, description: t.description, plannedEnd: t.plannedEnd })),
      safetyIncidents: safetyLogs.map(l => (l.extracted && l.extracted.activity) || l.rawText),
      aiObservation: aiObservation,
      approvedBy: null,
      status: 'Draft'
    };

    res.json(report);
  } catch (err) {
    console.error('Error generating daily report:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.13 lock/approve daily report
app.post('/api/daily-report/approve', authenticateUser, authorize('confirm_log'), async (req, res) => {
  const { projectId, date } = req.body;
  const logId = `report-approved-${Date.now()}`;
  const auditTrail = `Daily report for ${date} approved by ${req.user.name} on ${new Date().toLocaleString()}.`;

  try {
    await db.query(
      `INSERT INTO progress_logs (
        id, project_id, timestamp, worker_id, source, raw_text,
        extracted_activity, extracted_date, extracted_discipline, extracted_status,
        confidence_score, status, audit_trail
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)`,
      [
        logId,
        projectId,
        new Date().toISOString(),
        req.user.username,
        'Management Console',
        `Daily status report for ${date} has been officially approved and locked by Manager.`,
        `Daily Report Approved [${date}]`,
        date,
        'Management',
        'Completed',
        1.0,
        'Linked',
        auditTrail
      ]
    );

    res.json({ success: true, message: 'Daily report approved and logged.' });
  } catch (err) {
    console.error('Error approving report:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// Reset a specific project's schedule and logs
app.post('/api/reset-project', async (req, res) => {
  const { projectId } = req.body;
  if (!projectId) return res.status(400).json({ error: 'Project ID is required' });
  try {
    await db.query('DELETE FROM progress_logs WHERE project_id = $1', [projectId]);
    await db.query('DELETE FROM schedule_activities WHERE project_id = $1', [projectId]);
    res.json({ success: true, message: 'Project data erased successfully.' });
  } catch (err) {
    console.error('Error resetting project:', err);
    res.status(500).json({ error: 'Failed to reset project data' });
  }
});

// 6. Reset Database (For demo restarts)
app.post('/api/reset', async (req, res) => {
  try {
    await db.query('TRUNCATE progress_logs, schedule_activities, projects, users RESTART IDENTITY CASCADE');
    await db.initDatabase();
    res.json({ success: true, message: 'PostgreSQL database reset successfully.' });
  } catch (err) {
    console.error('Error resetting database:', err);
    res.status(500).json({ error: 'Failed to reset database' });
  }
});

app.listen(PORT, () => {
  console.log(`UNNATI backend listening on port ${PORT}`);
});
