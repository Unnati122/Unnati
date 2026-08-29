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
    let matched = false;

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
        matched = true;
        break;
      }
    }

    if (!matched && sentence.length > 5) {
      // Flagged as pending review
      extractedUpdates.push({
        activity: sentence,
        date: new Date().toISOString().split('T')[0],
        discipline: 'Unassigned',
        status: 'In Progress',
        matchedTaskId: null,
        confidenceScore: 0.35,
        reasoning: 'No keyword triggers found in mock database. Flagged for review.'
      });
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
  
  const addDays = (date, days) => {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  };

  const tasks = [
    {
      "id": "TSK-101",
      "description": "Right of Way (ROW) Clearing and Preparations",
      "discipline": "Civil",
      "plannedStart": formatDate(start),
      "plannedEnd": formatDate(addDays(start, Math.floor(diffDays * 0.15))),
      "durationDays": Math.floor(diffDays * 0.15),
      "dependencies": "None",
      "owner": owner
    },
    {
      "id": "TSK-102",
      "description": "Trenching and Foundation Excavation",
      "discipline": "Civil",
      "plannedStart": formatDate(addDays(start, Math.floor(diffDays * 0.15))),
      "plannedEnd": formatDate(addDays(start, Math.floor(diffDays * 0.35))),
      "durationDays": Math.floor(diffDays * 0.20),
      "dependencies": "TSK-101",
      "owner": owner
    },
    {
      "id": "TSK-201",
      "description": "Material Handling and Stringing",
      "discipline": "Piping",
      "plannedStart": formatDate(addDays(start, Math.floor(diffDays * 0.30))),
      "plannedEnd": formatDate(addDays(start, Math.floor(diffDays * 0.55))),
      "durationDays": Math.floor(diffDays * 0.25),
      "dependencies": "TSK-102",
      "owner": "Logistics Team"
    },
    {
      "id": "TSK-301",
      "description": "Mainline Fitting and Welding",
      "discipline": "Mechanical",
      "plannedStart": formatDate(addDays(start, Math.floor(diffDays * 0.50))),
      "plannedEnd": formatDate(addDays(start, Math.floor(diffDays * 0.75))),
      "durationDays": Math.floor(diffDays * 0.25),
      "dependencies": "TSK-201",
      "owner": "Fabrication Crew"
    },
    {
      "id": "TSK-401",
      "description": "Quality Inspections and Non-Destructive Testing (NDT)",
      "discipline": "Quality/HSE",
      "plannedStart": formatDate(addDays(start, Math.floor(diffDays * 0.70))),
      "plannedEnd": formatDate(addDays(start, Math.floor(diffDays * 0.85))),
      "durationDays": Math.floor(diffDays * 0.15),
      "dependencies": "TSK-301",
      "owner": "Q/C Inspectors"
    },
    {
      "id": "TSK-501",
      "description": "Pre-commissioning Hydrotesting and Joint Coating",
      "discipline": "Commissioning",
      "plannedStart": formatDate(addDays(start, Math.floor(diffDays * 0.82))),
      "plannedEnd": formatDate(end),
      "durationDays": Math.max(5, diffDays - Math.floor(diffDays * 0.82)),
      "dependencies": "TSK-401",
      "owner": "Commissioning Lead"
    }
  ];

  const milestones = (details.milestones && details.milestones.length > 0)
    ? details.milestones.map((m, idx) => ({ "name": m, "date": formatDate(addDays(start, Math.floor(diffDays * (idx + 1) / (details.milestones.length + 1)))) }))
    : [
        { "name": "Civil Foundations Completed", "date": formatDate(addDays(start, Math.floor(diffDays * 0.35))) },
        { "name": "Mechanical Erection Completed", "date": formatDate(addDays(start, Math.floor(diffDays * 0.75))) },
        { "name": "Pre-Commissioning Sign-off", "date": formatDate(addDays(start, Math.floor(diffDays * 0.90))) },
        { "name": "Project Completion Certificate", "date": formatDate(end) }
      ];

  const weekByWeekPlan = [];
  const totalWeeks = Math.ceil(diffDays / 7);
  for (let w = 1; w <= totalWeeks; w++) {
    let theme = "Construction & Execution Phase";
    let acts = [];
    if (w <= Math.ceil(totalWeeks * 0.2)) {
      theme = "Phase 1: Mobilization and Civil Groundworks";
      acts = ["Right of Way clearance and site levelling", "Mobilization of civil equipment and excavation crew"];
    } else if (w <= Math.ceil(totalWeeks * 0.55)) {
      theme = "Phase 2: Pipe Laying & Mechanical Alignment";
      acts = ["Hauling pipes to site nodes", "Positioning, stringing, and initial welding passes"];
    } else if (w <= Math.ceil(totalWeeks * 0.85)) {
      theme = "Phase 3: Radiography (NDT) & Tie-ins";
      acts = ["Joint radiography testing and NDT inspections", "Joint coating application and lowering-in"];
    } else {
      theme = "Phase 4: Commissioning & Handover";
      acts = ["Pipeline hydrostatic testing and pressure checks", "Final safety audit and signing off clearance certificates"];
    }
    weekByWeekPlan.push({
      "week": `Week ${w}`,
      "theme": theme,
      "activities": acts
    });
  }

  return {
    "overview": details.description || `This plan outlines the structured implementation methodology for the ${pName} project under the directive of Oil India Limited.`,
    "objectives": details.objectives || "Ensure the seamless completion of designated works within the baseline timeline while strictly adhering to safety regulations.",
    "scope": details.scope || "The complete execution of site works including civil preparation, mechanical installation, quality inspections, and final commissioning.",
    "milestones": milestones,
    "weekByWeekPlan": weekByWeekPlan,
    "tasks": tasks,
    "deliverables": [
      "Completed and certified installations",
      "Radiography & NDT quality logbooks",
      "Hydrotesting compliance records",
      "As-built safety clearances & approvals"
    ],
    "reportingStructure": `Structured reporting to occur at a ${details.reportingFrequency || 'Daily'} frequency. Supervisor is required to submit digital reports through the UNNATI portal.`,
    "risks": [
      { "risk": "Monsoon disruptions/weather delay", "mitigation": "Incorporate contingency weather days into schedule margins" },
      { "risk": "Material logistics supply bottlenecks", "mitigation": "Pre-stage critical equipment and valves at onsite warehouse yards" }
    ],
    "completionCriteria": "Successful completion of hydrostatic pressure testing with zero leakage, followed by formal handover signature from the quality engineer."
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

