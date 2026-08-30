const { GoogleGenAI } = require('@google/generative-ai');
const dotenv = require('dotenv');
const fs = require('fs');

dotenv.config();

const apiKey = process.env.GEMINI_API_KEY;
let aiModel = null;

if (apiKey) {
  try {
    // Note: We use import/require style for @google/generative-ai
    const { GoogleGenerativeAI } = require('@google/generative-ai');
    const genAI = new GoogleGenerativeAI(apiKey);
    // Use gemini-2.5-flash or gemini-1.5-flash
    aiModel = genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });
    console.log('Gemini AI Service initialized successfully.');
  } catch (error) {
    console.error('Error initializing Gemini AI. Falling back to local Mock engine:', error);
  }
} else {
  console.log('No GEMINI_API_KEY found in environment variables. Operating in Local Mock Engine mode.');
}

/**
 * Perform extraction and fuzzy matching using Gemini LLM
 * Falls back to local heuristics if API key is not configured.
 */
async function extractAndMatch(rawText, baselineTasks) {
  if (aiModel) {
    try {
      const prompt = `
You are the "UNNATI" AI backend for Oil India Limited's infrastructure project management tracking.
Your task is to analyze raw daily progress reports (DPRs) or supervisor updates, extract specific work progress updates, and fuzzy-match them against the project's baseline activities.

Here is the baseline schedule (L5/L6 activities):
${JSON.stringify(baselineTasks, null, 2)}

---
Raw progress input to analyze:
"${rawText}"
---

Instructions:
1. Extract all discrete progress updates mentioned.
2. For each extracted update:
   - Identify the activity description.
   - Identify the date (default to current date if not mentioned).
   - Identify the discipline (Civil, Piping, Mechanical, HSE, Commissioning, etc.).
   - Determine status ("In Progress", "Completed", or "Not Started").
   - Fuzzy match this update to the most relevant baseline task. Choose the best matching task from the baseline schedule list provided.
   - Provide a "confidenceScore" between 0.0 and 1.0 based on how closely the raw activity description matches the baseline task description and discipline. If it is a completely unrelated or new activity, return null for "matchedTaskId" and set confidenceScore < 0.5.
   - Provide a brief "reasoning" for the match or failure to match.

Return the response ONLY as a valid JSON object matching this schema (do not include markdown wrappers like \`\`\`json):
{
  "extractedUpdates": [
    {
      "activity": "string - extracted text activity",
      "date": "string - YYYY-MM-DD",
      "discipline": "string - Civil/Piping/Mechanical/HSE/etc.",
      "status": "string - In Progress / Completed",
      "matchedTaskId": "string - ACT-XXX or null",
      "confidenceScore": number,
      "reasoning": "string"
    }
  ]
}
`;

      const result = await aiModel.generateContent(prompt);
      const responseText = result.response.text().trim();
      
      // Clean potential JSON markdown blocks if present
      const cleanJson = responseText.replace(/^```json/i, '').replace(/```$/, '').trim();
      return JSON.parse(cleanJson);
    } catch (e) {
      console.error('Gemini API execution failed. Falling back to local mock.', e);
      return runLocalMockExtraction(rawText, baselineTasks);
    }
  } else {
    return runLocalMockExtraction(rawText, baselineTasks);
  }
}

/**
 * Handle Conversational UNNATI requests
 */
async function generateChatResponse(chatHistory, userMessage, baselineTasks) {
  if (aiModel) {
    try {
      // Build a history summary
      const prompt = `
You are the "UNNATI" chat assistant. A site supervisor is reporting daily work progress directly to you via voice/text.
Your goal is to be helpful, understand what progress they are reporting, extract it, and confirm it back to them.

Baseline schedule:
${JSON.stringify(baselineTasks.map(t => ({ id: t.id, description: t.description })), null, 2)}

Chat History:
${JSON.stringify(chatHistory, null, 2)}

User's new message:
"${userMessage}"

Respond in a conversational, professional tone. If they are reporting progress:
1. Validate what task you think they completed or updated.
2. Ask for details if anything is missing (e.g. date, segment, percentage).
3. If they mention an update that clearly matches a baseline task, confirm that you have linked it and will update the master schedule.
Keep the response under 3 sentences.
`;

      const result = await aiModel.generateContent(prompt);
      return result.response.text().trim();
    } catch (e) {
      console.error('Gemini chat response failed. Falling back to mock.', e);
      return generateMockChatResponse(userMessage, baselineTasks);
    }
  } else {
    return generateMockChatResponse(userMessage, baselineTasks);
  }
}

/**
 * Local Fallback Extraction Heuristics (Regex & Jaro-Winkler/Token similarity logic)
 */
function runLocalMockExtraction(rawText, baselineTasks) {
  const extractedUpdates = [];
  const text = rawText.toLowerCase();
  
  // Simple check routines for demo keywords
  const keywords = [
    { key: 'grading', id: 'ACT-101', disc: 'Civil' },
    { key: 'row clearing', id: 'ACT-101', disc: 'Civil' },
    { key: 'excavation', id: 'ACT-102', disc: 'Civil', altId: 'ACT-103' },
    { key: 'trenching', id: 'ACT-102', disc: 'Civil', altId: 'ACT-103' },
    { key: 'stringing', id: 'ACT-201', disc: 'Piping', altId: 'ACT-202' },
    { key: 'welding', id: 'ACT-301', disc: 'Mechanical/Piping', altId: 'ACT-302' },
    { key: 'fit-up', id: 'ACT-301', disc: 'Mechanical/Piping', altId: 'ACT-302' },
    { key: 'ndt', id: 'ACT-401', disc: 'Quality/HSE' },
    { key: 'radiography', id: 'ACT-401', disc: 'Quality/HSE' },
    { key: 'coating', id: 'ACT-501', disc: 'Mechanical/Piping' },
    { key: 'lowering', id: 'ACT-601', disc: 'Civil' },
    { key: 'backfilling', id: 'ACT-601', disc: 'Civil' },
    { key: 'hydrotesting', id: 'ACT-701', disc: 'Commissioning' },
    { key: 'commissioning', id: 'ACT-701', disc: 'Commissioning' }
  ];

  // Try split lines to simulate multiple records
  const sentences = rawText.split(/[.!\n;]/).map(s => s.trim()).filter(Boolean);

  for (const sentence of sentences) {
    const sLower = sentence.toLowerCase();

    for (const kw of keywords) {
      if (sLower.includes(kw.key)) {
        // Simple mock mapping
        let targetId = kw.id;
        
        // If there's an alternative (e.g. segment 2) and they mention segment 2 or Ch 2.5-5.0
        if (kw.altId && (sLower.includes('segment 2') || sLower.includes('2.5') || sLower.includes('ch 3') || sLower.includes('ch 4') || sLower.includes('ch 5'))) {
          targetId = kw.altId;
        }

        const taskObj = baselineTasks.find(t => t.id === targetId);
        
        extractedUpdates.push({
          activity: sentence,
          date: new Date().toISOString().split('T')[0],
          discipline: kw.disc,
          status: sLower.includes('complete') || sLower.includes('finish') || sLower.includes('done') ? 'Completed' : 'In Progress',
          matchedTaskId: targetId,
          confidenceScore: 0.88,
          reasoning: `Matched via mock heuristic keyword: "${kw.key}"`
        });
        break;
      }
    }
  }

  // Ensure we return at least something
  if (extractedUpdates.length === 0) {
    extractedUpdates.push({
      activity: rawText,
      date: new Date().toISOString().split('T')[0],
      discipline: 'General',
      status: 'In Progress',
      matchedTaskId: null,
      confidenceScore: 0.30,
      reasoning: 'Fallback default extraction.'
    });
  }

  return { extractedUpdates };
}

function generateMockChatResponse(userMessage, baselineTasks) {
  const lowerMsg = userMessage.toLowerCase();
  
  if (lowerMsg.includes('hello') || lowerMsg.includes('hi')) {
    return 'Hello! I am your UNNATI assistant. What pipeline progress are you reporting today?';
  }
  
  // Keyword matches
  if (lowerMsg.includes('welding') || lowerMsg.includes('weld')) {
    return 'I detected progress on welding. I\'m linking this to "ACT-301 Mainline Welding & Fit-up - Segment 1". Is this segment 1 (Ch 0.0 to 2.5) or segment 2?';
  }
  if (lowerMsg.includes('excavation') || lowerMsg.includes('trench')) {
    return 'Understood, trench excavation progress has been logged. I will fuzzy-match this to the excavation schedule and update the status.';
  }
  if (lowerMsg.includes('finish') || lowerMsg.includes('complete')) {
    return 'Great job! I\'ve logged that activity as completed. I will flag the schedule for verification by the planning team.';
  }
  
  return 'Thank you. I have captured your update: "' + userMessage + '". I will link it to the baseline schedule and notify the project planners.';
}

async function generateProjectPlan(details) {
  if (aiModel) {
    try {
      const prompt = `
You are the "UNNATI" AI planner for Oil India Limited.
A project manager has requested to generate a detailed project implementation plan.

Here are the project inputs:
Project Name: ${details.projectName}
Description: ${details.description}
Objectives: ${details.objectives}
Scope: ${details.scope}
Start Date: ${details.startDate}
End Date: ${details.endDate}
Reporting Preference: ${details.reportingFrequency}
Milestones: ${JSON.stringify(details.milestones)}
Constraints: ${details.guidelines}
Responsible Owner: ${details.owner}

Instructions:
Generate a detailed implementation plan.
Return the response ONLY as a valid JSON object matching this schema (do not include markdown wrappers like \`\`\`json):
{
  "overview": "Detailed overview text...",
  "objectives": "Detailed objectives text...",
  "scope": "Detailed scope description...",
  "milestones": [
    { "name": "Milestone Name", "date": "YYYY-MM-DD" }
  ],
  "weekByWeekPlan": [
    { "week": "Week 1", "theme": "Preparation...", "activities": ["Activity 1", "Activity 2"] }
  ],
  "detailedDailySchedule": [
    { "dayRange": "Day 1-5", "activity": "Activity description...", "discipline": "Civil/IT/etc." }
  ],
  "tasks": [
    { "id": "TASK-001", "description": "Task description...", "discipline": "Civil/Mechanical/Piping/etc.", "plannedStart": "YYYY-MM-DD", "plannedEnd": "YYYY-MM-DD", "durationDays": 10, "dependencies": "None", "owner": "Owner..." }
  ],
  "deliverables": ["Deliverable 1", "Deliverable 2"],
  "reportingStructure": "Reporting structure details...",
  "risks": [
    { "risk": "Description of risk", "mitigation": "Mitigation strategy" }
  ],
  "completionCriteria": "Criteria details..."
}
`;
      const result = await aiModel.generateContent(prompt);
      const cleanJson = result.response.text().trim().replace(/^```json/i, '').replace(/```$/, '').trim();
      return JSON.parse(cleanJson);
    } catch (e) {
      console.error("Gemini project plan generation failed, falling back to mock", e);
      return generateMockProjectPlan(details);
    }
  } else {
    return generateMockProjectPlan(details);
  }
}

function generateMockProjectPlan(details) {
  const start = new Date(details.startDate || '2026-09-01');
  const end = new Date(details.endDate || '2026-11-30');
  const diffTime = Math.abs(end - start);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) || 90;
  
  const formatDate = (date) => date.toISOString().split('T')[0];
  const pName = details.projectName || 'Infrastructure Project';
  const owner = details.owner || 'Planning Team';
  const desc = ((details.description || '') + ' ' + (details.projectName || '') + ' ' + (details.scope || '')).toLowerCase();
  
  const addDays = (date, days) => {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  };

  const itActivities = [
    "Define Project Objectives & Core Scope",
    "Conduct User Persona Studies & Workflows",
    "Finalize Wireframes & UI Design Concept",
    "UI/UX Design Review & Client Sign-off",
    "Project Repo Setup & CI/CD Pipeline Config",
    "Design PostgreSQL Database Schema",
    "Setup Express Server & Initial API Routes",
    "User Authentication REST API Implementation",
    "Authentication Integration with SQLite/Web",
    "Dashboard Landing Screen Layout & CSS",
    "User Profiles & Settings Frontend Dev",
    "Media Upload Endpoint & Multer Storage",
    "Camera Capture & Preview Integration",
    "Offline Storage Engine & Local Cache Setup",
    "Automatic Sync Queue & Worker Engine",
    "Supervisor Approval Web Interface Layout",
    "Web Dashboard Analytics Metrics Charts",
    "Backend Route Unit Tests Verification",
    "Frontend UI Component Regression Checks",
    "Manual App Sandbox Testing on Devices",
    "Production Cloud Environment Deployment",
    "Beta Build Compilation & APK Release",
    "Pre-Launch Safety & Penetration Checks",
    "Database Performance & Index Optimization",
    "API SSL Handshake & HTTPS Configuration",
    "Complete System Integration Review",
    "User Guide Documentation & Readme Final",
    "App Store/Play Store Listing Submission",
    "System Handover & Administrator Training",
    "Project Completion Sign-off & Retrospective"
  ];

  const civilActivities = [
    "Site Mobilization & Excavator Setup",
    "Topographical Survey & Route Staking",
    "Right-of-Way Clearing & Levelling",
    "Civil Equipment Safety Pre-Checks",
    "Excavator Position & Staging Area Setup",
    "Trenching Segment 1 Excavation (0-200m)",
    "Trenching Segment 2 Excavation (200-400m)",
    "Soil Stability & Slope Clearance Inspection",
    "Pipe Hauling & Joint Distribution",
    "Pipe Stringing Along Trench Profile",
    "Line Pipe Internal Cleaning & Inspection",
    "Fit-up Clamping & Tack Welding Segment 1",
    "Mainline Root-Pass Welding Segment 1",
    "Hot-Pass Welding & Slag Removal Segment 1",
    "Filler & Cap Welding Completion Segment 1",
    "Non-Destructive Radiography (NDT) Checks",
    "NDT Review & Weld Defect Repairs",
    "Joint Coating Sandblasting & Pre-heat",
    "Polyurethane Joint Coating Application",
    "Holiday Spark Leak Detection Checking",
    "Trench Bottom Padding Sand Laying",
    "Lowering-in Pipe Segment with Sidebooms",
    "Line Tie-in Welding & Joint Inspection",
    "Trench Backfilling & Soil Compaction",
    "ROW Restoration & Surface Levelling",
    "Hydrotesting Manifold Setup & Water Filling",
    "Hydrostatic Strength Testing Pressurization",
    "Pressure Hold Leakage Check Verification",
    "Pipeline Dewatering & Nitrogen Purging",
    "Cathodic Protection Commissioning & Handover"
  ];

  const genericActivities = [
    "Kickoff Meeting & Stakeholder Onboarding",
    "Detailed Work Breakdown Structure Draft",
    "Resource Allocation & Mobilization",
    "Baseline Schedule Setup & Sign-off",
    "Procurement List Approval & Purchase Orders",
    "Material Dispatch & Transit Tracking",
    "Site Delivery & Inventory Verification",
    "Technical Design Review & Specifications",
    "Infrastructure Staging & Base Foundations",
    "Core Installation Phase 1 Execution",
    "Core Installation Phase 2 Execution",
    "Core Installation Phase 3 Execution",
    "Mid-Project Review & Timeline Alignment",
    "Equipment Integration & Connections Setup",
    "System Configuration & Calibration",
    "Internal Quality Checks & Punchlist",
    "Defect Rectification & Clean-up",
    "Safety Assurance Audits & Clearances",
    "Standard Operating Procedures (SOP) Draft",
    "Integration Verification & Sandbox Run",
    "User Acceptance Testing (UAT) Onboarding",
    "UAT Execution & Client Feedback Capture",
    "Post-UAT Fixes & Layout Polishing",
    "System Performance Optimization Checks",
    "Security Review & Credentials Handoff",
    "User Training Sessions & Workshop Run",
    "As-built Drawings Documentation Compile",
    "Official Inspection & Regulatory Clearance",
    "Final Handover Certificate Signing",
    "Project Review Meeting & Closeout"
  ];

  // Pick template array
  let sourceActivities = genericActivities;
  let discipline = "Management";
  if (desc.includes('app') || desc.includes('mobile') || desc.includes('software') || desc.includes('website') || desc.includes('it') || desc.includes('system') || desc.includes('develop') || desc.includes('code')) {
    sourceActivities = itActivities;
    discipline = "IT/Development";
  } else if (desc.includes('road') || desc.includes('bridge') || desc.includes('building') || desc.includes('construction') || desc.includes('civil') || desc.includes('earth') || desc.includes('pipe') || desc.includes('line')) {
    sourceActivities = civilActivities;
    discipline = "Civil";
  }

  // Generate strictly DAILY tasks (1 task per calendar day, duration = 1 day each)
  const numTasks = Math.min(diffDays, 30);
  const tasks = [];
  for (let i = 0; i < numTasks; i++) {
    const taskDate = addDays(start, i);
    const dateStr = formatDate(taskDate);
    const activityDesc = sourceActivities[i] || `Project Execution Task Phase ${i+1}`;
    
    // Assign smart discipline based on index
    let taskDisc = discipline;
    if (discipline === "IT/Development") {
      if (i < 4) taskDisc = "IT/Design";
      else if (i === 5) taskDisc = "IT/Database";
      else if (i > 24) taskDisc = "Management";
      else if (i > 20) taskDisc = "Quality/HSE";
    } else if (discipline === "Civil") {
      if (i > 27) taskDisc = "Commissioning";
      else if (i > 14 && i < 18) taskDisc = "Quality/HSE";
      else if (i >= 11 && i <= 14) taskDisc = "Mechanical/Piping";
    }

    tasks.push({
      id: `TSK-10${i+1 > 9 ? i+1 : '0' + (i+1)}`,
      description: activityDesc,
      discipline: taskDisc,
      plannedStart: dateStr,
      plannedEnd: dateStr,
      durationDays: 1,
      dependencies: i === 0 ? "None" : `TSK-10${i > 9 ? i : '0' + i}`,
      owner: owner
    });
  }

  const weekByWeekPlan = [];
  const totalWeeks = Math.ceil(diffDays / 7) || 12;

  for (let w = 1; w <= totalWeeks; w++) {
    const currentTaskIndex = Math.min(tasks.length - 1, Math.floor((w - 1) / (totalWeeks / tasks.length)));
    const activeTask = tasks[currentTaskIndex];
    weekByWeekPlan.push({
      week: `Week ${w}`,
      theme: activeTask ? `${activeTask.description.substring(0, 30)}...` : 'Execution Focus',
      activities: [
        activeTask ? `Active execution of ${activeTask.description}` : 'General site progress tracking',
        `Perform quality check matching discipline ${activeTask ? activeTask.discipline : 'General'}`
      ]
    });
  }

  const milestones = [
    { name: "Project Kickoff", date: formatDate(start) },
    { name: "Mid-Term Review & Intermediate Clearance", date: formatDate(addDays(start, Math.floor(diffDays / 2))) },
    { name: "Final Handover & Commissioning Approval", date: formatDate(end) }
  ];

  const detailedDailySchedule = tasks.map(t => {
    const startOffset = Math.ceil((new Date(t.plannedStart) - start) / (1000 * 60 * 60 * 24)) + 1;
    const endOffset = Math.ceil((new Date(t.plannedEnd) - start) / (1000 * 60 * 60 * 24)) + 1;
    const dayRange = startOffset === endOffset ? `Day ${startOffset}` : `Day ${startOffset}-${endOffset}`;
    return {
      dayRange: dayRange,
      activity: t.description,
      discipline: t.discipline
    };
  });

  return {
    "overview": details.description || `This plan outlines the structured implementation methodology for the ${pName} project under the directive of Oil India Limited.`,
    "objectives": details.objectives || "Ensure the seamless completion of designated works within the baseline timeline while strictly adhering to safety regulations.",
    "scope": details.scope || "The complete execution of site works including preparation, mechanical installation, quality inspections, and final commissioning.",
    "milestones": milestones,
    "weekByWeekPlan": weekByWeekPlan,
    "detailedDailySchedule": detailedDailySchedule,
    "tasks": tasks,
    "deliverables": [
      "Completed and certified installations",
      "Quality compliance record books",
      "Safety clearances & approvals"
    ],
    "reportingStructure": `Structured reporting to occur at a ${details.reportingFrequency || 'Daily'} frequency. Responsible owner is ${owner}.`,
    "risks": [
      { "risk": "Logistics bottlenecks / resource constraints", "mitigation": "Establish secondary vendor chains and monitor timelines closely" },
      { "risk": "Quality non-conformance issues", "mitigation": "Conduct strict internal daily checkups under designated owner" }
    ],
    "completionCriteria": "Successful completion of all specified milestone tasks followed by formal handover signature from the engineering reviewer."
  };
}

async function transcribeAudio(filePath, mimeType) {
  if (aiModel) {
    try {
      const audioData = {
        inlineData: {
          data: Buffer.from(fs.readFileSync(filePath)).toString("base64"),
          mimeType: mimeType || "audio/mp4"
        }
      };

      const prompt = "Transcribe the spoken text in this audio file verbatim. Do not translate. Return only the transcription text and nothing else.";
      const result = await aiModel.generateContent([prompt, audioData]);
      return result.response.text().trim();
    } catch (e) {
      console.error('Gemini audio transcription failed:', e);
      return null;
    }
  }
  return null;
}

module.exports = {
  extractAndMatch,
  generateChatResponse,
  generateProjectPlan,
  transcribeAudio
};

