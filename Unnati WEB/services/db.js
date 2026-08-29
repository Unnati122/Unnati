const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

dotenv.config();

const pool = new Pool({
  host: process.env.PGHOST || 'localhost',
  port: parseInt(process.env.PGPORT || '5432', 10),
  user: process.env.PGUSER || 'postgres',
  password: process.env.PGPASSWORD || 'postgres',
  database: process.env.PGDATABASE || 'unnati'
});

// Helper to run query
async function query(text, params) {
  return pool.query(text, params);
}

// Initialize tables and migrate data if empty
async function initDatabase() {
  console.log('Initializing PostgreSQL database...');
  
  // 1. Create tables if they do not exist
  await query(`
    CREATE TABLE IF NOT EXISTS users (
      username VARCHAR(100) PRIMARY KEY,
      password VARCHAR(100) NOT NULL,
      role VARCHAR(50) NOT NULL,
      name VARCHAR(100) NOT NULL,
      status VARCHAR(20) DEFAULT 'active',
      responsibility TEXT,
      assigned_projects TEXT[] DEFAULT '{}',
      permissions TEXT[] DEFAULT '{}'
    );
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS projects (
      id VARCHAR(50) PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      code VARCHAR(50) NOT NULL,
      location VARCHAR(255),
      client VARCHAR(255),
      active_workers_count INTEGER DEFAULT 0
    );
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS schedule_activities (
      id VARCHAR(50) NOT NULL,
      project_id VARCHAR(50) NOT NULL,
      description TEXT NOT NULL,
      discipline VARCHAR(100),
      planned_start VARCHAR(20),
      planned_end VARCHAR(20),
      actual_start VARCHAR(20),
      actual_end VARCHAR(20),
      progress INTEGER DEFAULT 0,
      status VARCHAR(50) DEFAULT 'Not Started',
      wbs VARCHAR(50),
      PRIMARY KEY (id, project_id)
    );
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS progress_logs (
      id VARCHAR(100) PRIMARY KEY,
      project_id VARCHAR(50) NOT NULL,
      timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
      worker_id VARCHAR(100),
      source VARCHAR(100) NOT NULL,
      raw_text TEXT NOT NULL,
      duration_seconds INTEGER DEFAULT 0,
      audio_file_path TEXT,
      waveform_csv TEXT,
      extracted_activity TEXT,
      extracted_date VARCHAR(20),
      extracted_discipline VARCHAR(100),
      extracted_status VARCHAR(50),
      matched_task_id VARCHAR(50),
      matched_task_description TEXT,
      confidence_score REAL,
      status VARCHAR(50) NOT NULL,
      audit_trail TEXT
    );
  `);

  // 2. Seed data if tables are empty
  const USERS_PATH = path.join(__dirname, '..', 'data', 'users.json');
  const BASELINE_PATH = path.join(__dirname, '..', 'data', 'baselineSchedule.json');
  const PROGRESS_PATH = path.join(__dirname, '..', 'data', 'progressDatabase.json');

  // Seed Users
  const userCheck = await query('SELECT COUNT(*) FROM users');
  if (parseInt(userCheck.rows[0].count, 10) === 0 && fs.existsSync(USERS_PATH)) {
    console.log('Seeding users into PostgreSQL...');
    try {
      const usersData = JSON.parse(fs.readFileSync(USERS_PATH, 'utf8'));
      for (const u of usersData) {
        await query(
          'INSERT INTO users (username, password, role, name, status, responsibility, assigned_projects, permissions) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
          [u.username, u.password, u.role, u.name, u.status || 'active', u.responsibility || '', u.assignedProjects || [], u.permissions || []]
        );
      }
      
      // Also add the mobile worker profiles if not already there
      const mobileWorkers = [
        { username: 'WK-10245', password: '4892', role: 'field', name: 'Rajesh Sharma', responsibility: 'Site Supervisor', assignedProjects: ['PROJ-1'], permissions: ['field_submit'] },
        { username: 'WK-10882', password: 'password', role: 'field', name: 'Amit Patel', responsibility: 'Quality Inspector', assignedProjects: ['PROJ-1'], permissions: ['field_submit'] },
        { username: 'WK-11409', password: 'password', role: 'field', name: 'Suresh Yadav', responsibility: 'Safety Marshal', assignedProjects: ['PROJ-1'], permissions: ['field_submit'] }
      ];
      for (const w of mobileWorkers) {
        const check = await query('SELECT username FROM users WHERE username = $1', [w.username]);
        if (check.rows.length === 0) {
          await query(
            'INSERT INTO users (username, password, role, name, status, responsibility, assigned_projects, permissions) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
            [w.username, w.password, w.role, w.name, 'active', w.responsibility, w.assignedProjects, w.permissions]
          );
        }
      }
    } catch (err) {
      console.error('Error seeding users:', err);
    }
  }

  // Seed Projects
  const projectCheck = await query('SELECT COUNT(*) FROM projects');
  if (parseInt(projectCheck.rows[0].count, 10) === 0) {
    console.log('Seeding projects into PostgreSQL...');
    const defaultProjects = [
      { id: 'PROJ-1', name: 'Pipeline Construction Segment 1', code: 'OPE-24', location: 'Barmer-Salaya Corridor, Rajasthan', client: 'Oil India Ltd / Bharat Petro Infra', activeWorkersCount: 164 },
      { id: 'PROJ-2', name: 'Digboi Infrastructure Expansion', code: 'REF-EXP', location: 'Digboi Refinery, Assam', client: 'Oil India Ltd', activeWorkersCount: 148 },
      { id: 'PROJ-3', name: 'Numaligarh Bio-Refinery Development', code: 'NRL-BIO', location: 'Numaligarh, Assam', client: 'Numaligarh Refinery Ltd', activeWorkersCount: 92 }
    ];
    for (const p of defaultProjects) {
      await query(
        'INSERT INTO projects (id, name, code, location, client, active_workers_count) VALUES ($1, $2, $3, $4, $5, $6)',
        [p.id, p.name, p.code, p.location, p.client, p.activeWorkersCount]
      );
    }
  }

  // Seed Schedule Activities
  const activityCheck = await query('SELECT COUNT(*) FROM schedule_activities');
  if (parseInt(activityCheck.rows[0].count, 10) === 0 && fs.existsSync(BASELINE_PATH)) {
    console.log('Seeding schedule activities into PostgreSQL...');
    try {
      const scheduleData = JSON.parse(fs.readFileSync(BASELINE_PATH, 'utf8'));
      for (const task of scheduleData) {
        await query(
          'INSERT INTO schedule_activities (id, project_id, description, discipline, planned_start, planned_end, actual_start, actual_end, progress, status, wbs) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)',
          [task.id, task.projectId, task.description, task.discipline, task.plannedStart, task.plannedEnd, task.actualStart, task.actualEnd, task.progress || 0, task.status || 'Not Started', task.wbs]
        );
      }
    } catch (err) {
      console.error('Error seeding schedule activities:', err);
    }
  }

  // Seed Progress Logs
  const logsCheck = await query('SELECT COUNT(*) FROM progress_logs');
  if (parseInt(logsCheck.rows[0].count, 10) === 0 && fs.existsSync(PROGRESS_PATH)) {
    console.log('Seeding progress logs into PostgreSQL...');
    try {
      const logsData = JSON.parse(fs.readFileSync(PROGRESS_PATH, 'utf8'));
      for (const log of logsData) {
        await query(
          `INSERT INTO progress_logs (
            id, project_id, timestamp, worker_id, source, raw_text, duration_seconds, 
            audio_file_path, waveform_csv, extracted_activity, extracted_date, 
            extracted_discipline, extracted_status, matched_task_id, 
            matched_task_description, confidence_score, status, audit_trail
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18)`,
          [
            log.id,
            log.projectId,
            log.timestamp,
            log.workerId || null,
            log.source || 'Unknown',
            log.rawText,
            log.durationSeconds || 0,
            log.audioFilePath || null,
            log.waveformCsv || (log.waveform ? log.waveform.join(',') : null),
            log.extracted ? log.extracted.activity : null,
            log.extracted ? log.extracted.date : null,
            log.extracted ? log.extracted.discipline : null,
            log.extracted ? log.extracted.status : null,
            log.matchedTaskId || null,
            log.matchedTaskDescription || null,
            log.confidenceScore !== undefined ? log.confidenceScore : null,
            log.status,
            log.auditTrail
          ]
        );
      }
    } catch (err) {
      console.error('Error seeding progress logs:', err);
    }
  }

  // Remove existing demo logs if any to ensure clean state
  try {
    await query("DELETE FROM progress_logs WHERE id IN ('log-1', 'log-2', 'log-3', 'log-r1', 'log-b1', 'tsk-8492', 'log-1787731682223-949', 'log-1787731682222-766')");
  } catch (err) {
    console.error('Error clearing demo logs:', err);
  }

  console.log('PostgreSQL database initialization completed.');
}

module.exports = {
  pool,
  query,
  initDatabase
};
