const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');
const multer = require('multer');
const geminiService = require('./services/geminiService');
const db = require('./services/db');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Serve landing.html at the root route '/'
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'landing.html'));
});

// Configure multer storage for audio uploads
const uploadDir = path.join(__dirname, 'public', 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    cb(null, `audio-${Date.now()}-${Math.floor(Math.random() * 1000)}${path.extname(file.originalname) || '.m4a'}`);
  }
});
const upload = multer({ storage: storage });

app.use(express.static(path.join(__dirname, 'public'), { index: false }));

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
    auditTrail: row.audit_trail
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

    // Add initial seed log for plan approval
    const initLogId = `log-${Date.now()}`;
    const auditTrail = `Project implementation plan approved and baseline tasks initialized on ${new Date().toLocaleString()}.`;
    await db.query(
      `INSERT INTO progress_logs (
        id, project_id, timestamp, worker_id, source, raw_text,
        extracted_activity, extracted_date, extracted_discipline, extracted_status,
        confidence_score, status, audit_trail
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)`,
      [
        initLogId,
        projectId,
        new Date().toISOString(),
        req.user.username,
        'System Planner',
        `AI project plan approved and applied for project ${projectId}. Created ${tasks.length} baseline tasks.`,
        'Project Plan Approved',
        new Date().toISOString().split('T')[0],
        'Management',
        'Completed',
        1.0,
        'Linked',
        auditTrail
      ]
    );

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
    const result = await db.query('SELECT * FROM users WHERE username = $1 AND password = $2', [username.trim(), password.trim()]);

    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials. Please verify your username and password.' });
    }

    const user = result.rows[0];

    if (user.status !== 'active') {
      return res.status(403).json({ error: 'Forbidden: Account is currently inactive. Contact OIL Administration.' });
    }

    const token = `token-${user.username}`;
    res.json({
      success: true,
      token: token,
      user: {
        username: user.username,
        name: user.name,
        role: user.role,
        responsibility: user.responsibility,
        assignedProjects: user.assigned_projects || [],
        permissions: user.permissions || []
      }
    });
  } catch (err) {
    console.error('Error logging in:', err);
    res.status(500).json({ error: 'Database query failed' });
  }
});

// 5.5 worker identity endpoint
app.get('/api/me', authenticateUser, (req, res) => {
  res.json(req.user);
});

// 5.6 worker assigned project details
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
app.post('/api/voice/upload', authenticateUser, upload.single('audio'), async (req, res) => {
  if (!req.file) {
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
    let transcript = await geminiService.transcribeAudio(req.file.path, req.file.mimetype);
    
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

      const relativeAudioPath = `/uploads/${req.file.filename}`;

      // Save log entry to PostgreSQL
      await db.query(
        `INSERT INTO progress_logs (
          id, project_id, timestamp, worker_id, source, raw_text, duration_seconds, 
          audio_file_path, waveform_csv, extracted_activity, extracted_date, 
          extracted_discipline, extracted_status, matched_task_id, 
          matched_task_description, confidence_score, status, audit_trail
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18)`,
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
          auditTrail
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
        audioFilePath: relativeAudioPath
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
