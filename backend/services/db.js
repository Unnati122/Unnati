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
  database: process.env.PGDATABASE || 'unnati',
  ssl: { rejectUnauthorized: false }
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
      email VARCHAR(255),
      role VARCHAR(50) NOT NULL,
      name VARCHAR(100) NOT NULL,
      status VARCHAR(20) DEFAULT 'active',
      responsibility TEXT,
      assigned_projects TEXT[] DEFAULT '{}',
      permissions TEXT[] DEFAULT '{}',
      reset_otp VARCHAR(10),
      reset_otp_expiry TIMESTAMP,
      phone VARCHAR(20),
      digital_id VARCHAR(100)
    );
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS projects (
      id VARCHAR(50) PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      code VARCHAR(50) NOT NULL,
      location VARCHAR(255),
      client VARCHAR(255),
      active_workers_count INTEGER DEFAULT 0,
      status VARCHAR(20) DEFAULT 'active',
      latitude DOUBLE PRECISION,
      longitude DOUBLE PRECISION,
      radius INTEGER DEFAULT 100
    );
  `);

  // Migration: Add status to existing projects table if it doesn't exist
  try {
    await query(`ALTER TABLE projects ADD COLUMN status VARCHAR(20) DEFAULT 'active';`);
  } catch(e) {
    // Column already exists, ignore
  }

  // Migration: Add reset_otp to existing users table if it doesn't exist
  try {
    await query(`ALTER TABLE users ADD COLUMN reset_otp VARCHAR(10);`);
    await query(`ALTER TABLE users ADD COLUMN reset_otp_expiry TIMESTAMP;`);
  } catch(e) {}

  // Migration: Add phone and digital_id to existing users
  try {
    await query(`ALTER TABLE users ADD COLUMN phone VARCHAR(20);`);
    await query(`ALTER TABLE users ADD COLUMN digital_id VARCHAR(100);`);
  } catch(e) {}

  // Migration: Add geofencing to existing projects
  try {
    await query(`ALTER TABLE projects ADD COLUMN latitude DOUBLE PRECISION;`);
    await query(`ALTER TABLE projects ADD COLUMN longitude DOUBLE PRECISION;`);
    await query(`ALTER TABLE projects ADD COLUMN radius INTEGER DEFAULT 100;`);
  } catch(e) {}

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
      audit_trail TEXT,
      photo_file_path TEXT
    );
  `);

  // Migration: Add photo_file_path to existing progress_logs
  try {
    await query(`ALTER TABLE progress_logs ADD COLUMN photo_file_path TEXT;`);
  } catch(e) {}

  // Enable Row Level Security (RLS) on all tables to secure them from the public Data API
  await query(`
    ALTER TABLE users ENABLE ROW LEVEL SECURITY;
    ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
    ALTER TABLE schedule_activities ENABLE ROW LEVEL SECURITY;
    ALTER TABLE progress_logs ENABLE ROW LEVEL SECURITY;
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS memory_sources (
      id VARCHAR(50) PRIMARY KEY,
      project_id VARCHAR(50) NOT NULL,
      filename VARCHAR(255) NOT NULL,
      file_type VARCHAR(50) NOT NULL,
      uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
      raw_content TEXT
    );
  `);
  
  await query(`ALTER TABLE memory_sources ENABLE ROW LEVEL SECURITY;`);

  // 2. Seed default admin user (always ensure admin exists)
  try {
    await query(
      `INSERT INTO users (username, password, email, role, name, status, responsibility)
       VALUES ('paramjitbaral@gmail.com', 'Swaraj@0405', 'paramjitbaral@gmail.com', 'admin', 'Paramjit Baral', 'active', 'Platform Admin')
       ON CONFLICT (username) DO NOTHING`
    );
    console.log('Admin user seeded/verified: paramjitbaral@gmail.com');
  } catch (err) {
    console.error('Error seeding admin user:', err);
  }

  console.log('PostgreSQL database initialization completed.');
}

module.exports = {
  pool,
  query,
  initDatabase
};
