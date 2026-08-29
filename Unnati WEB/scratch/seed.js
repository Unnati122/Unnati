const fs = require('fs');
const path = require('path');

const BASELINE_PATH = path.join(__dirname, '..', 'data', 'baselineSchedule.json');
const PROGRESS_PATH = path.join(__dirname, '..', 'data', 'progressDatabase.json');

const SEED_BASELINE = [
  { "id": "ACT-101", "description": "Right of Way (ROW) Clearing and Grading - Ch 0.0 to Ch 5.0", "discipline": "Civil", "plannedStart": "2026-09-01", "plannedEnd": "2026-09-10", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.1.1" },
  { "id": "ACT-102", "description": "Trench Excavation - Ch 0.0 to Ch 2.5", "discipline": "Civil", "plannedStart": "2026-09-11", "plannedEnd": "2026-09-20", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.1.2" },
  { "id": "ACT-103", "description": "Trench Excavation - Ch 2.5 to Ch 5.0", "discipline": "Civil", "plannedStart": "2026-09-21", "plannedEnd": "2026-09-30", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.1.3" },
  { "id": "ACT-201", "description": "Stringing of 18 inch Pipeline - Segment 1 (Ch 0.0 to Ch 2.5)", "discipline": "Piping", "plannedStart": "2026-09-15", "plannedEnd": "2026-09-25", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.2.1" },
  { "id": "ACT-202", "description": "Stringing of 18 inch Pipeline - Segment 2 (Ch 2.5 to Ch 5.0)", "discipline": "Piping", "plannedStart": "2026-09-26", "plannedEnd": "2026-10-05", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.2.2" },
  { "id": "ACT-301", "description": "Mainline Welding and Fit-up - Segment 1 (Ch 0.0 to Ch 2.5)", "discipline": "Mechanical/Piping", "plannedStart": "2026-09-20", "plannedEnd": "2026-10-05", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.3.1" },
  { "id": "ACT-302", "description": "Mainline Welding and Fit-up - Segment 2 (Ch 2.5 to Ch 5.0)", "discipline": "Mechanical/Piping", "plannedStart": "2026-10-01", "plannedEnd": "2026-10-15", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.3.2" },
  { "id": "ACT-401", "description": "Non-Destructive Testing (NDT) & Radiography - Segment 1", "discipline": "Quality/HSE", "plannedStart": "2026-09-25", "plannedEnd": "2026-10-10", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.4.1" },
  { "id": "ACT-501", "description": "Field Joint Coating and Inspection - Segment 1", "discipline": "Mechanical/Piping", "plannedStart": "2026-10-01", "plannedEnd": "2026-10-15", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.5.1" },
  { "id": "ACT-601", "description": "Pipeline Lowering-in & Backfilling - Segment 1", "discipline": "Civil", "plannedStart": "2026-10-05", "plannedEnd": "2026-10-20", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.6.1" },
  { "id": "ACT-701", "description": "Hydrotesting & Commissioning of Segment 1", "discipline": "Commissioning", "plannedStart": "2026-10-25", "plannedEnd": "2026-11-05", "actualStart": null, "actualEnd": null, "progress": 0, "status": "Not Started", "wbs": "1.7.1" }
];

const SEED_PROGRESS = [
  {
    "id": "log-1",
    "timestamp": "2026-08-24T18:30:00.000Z",
    "source": "Text Report Upload",
    "rawText": "Completed ROW grading from chainage 0.0 to 3.5. Excavation on first block started on Monday.",
    "extracted": { "activity": "ROW grading from chainage 0.0 to 3.5", "date": "2026-08-24", "discipline": "Civil", "status": "In Progress" },
    "matchedTaskId": "ACT-101",
    "matchedTaskDescription": "Right of Way (ROW) Clearing and Grading - Ch 0.0 to Ch 5.0",
    "confidenceScore": 0.85,
    "status": "Linked",
    "auditTrail": "Auto-linked to [ACT-101] with 85% confidence on 2026-08-24T18:30:15Z."
  },
  {
    "id": "log-2",
    "timestamp": "2026-08-24T19:15:00.000Z",
    "source": "Chat Interface",
    "rawText": "erected spool 4 and started pipe welding on mainline segment 1",
    "extracted": { "activity": "pipe welding segment 1", "date": "2026-08-24", "discipline": "Mechanical/Piping", "status": "In Progress" },
    "matchedTaskId": "ACT-301",
    "matchedTaskDescription": "Mainline Welding and Fit-up - Segment 1 (Ch 0.0 to Ch 2.5)",
    "confidenceScore": 0.92,
    "status": "Linked",
    "auditTrail": "Auto-matched by UNNATI on 2026-08-24T19:15:05Z."
  },
  {
    "id": "log-3",
    "timestamp": "2026-08-24T20:00:00.000Z",
    "source": "Text Report Upload",
    "rawText": "HSE inspection completed for welding crew.",
    "extracted": { "activity": "HSE inspection completed", "date": "2026-08-24", "discipline": "Quality/HSE", "status": "Completed" },
    "matchedTaskId": null,
    "matchedTaskDescription": null,
    "confidenceScore": 0.45,
    "status": "Linked",
    "auditTrail": "Auto-match confidence below threshold (45%). Flagged for Planner Review."
  },
  {
    "id": "tsk-8492",
    "timestamp": new Date(Date.now() - 60000).toISOString(),
    "source": "Voice Chat",
    "rawText": "Electrical work completed in Unit 4.",
    "extracted": { "activity": "Electrical work completed", "date": "2026-08-25", "discipline": "Electrical", "status": "Completed" },
    "matchedTaskId": "ACT-302",
    "matchedTaskDescription": "Mainline Welding and Fit-up - Segment 2 (Ch 2.5 to Ch 5.0)",
    "confidenceScore": 0.76,
    "status": "Pending Review",
    "auditTrail": "Auto-match confidence below threshold (76%). Flagged for Planner Review."
  },
  {
    "id": "tsk-8493",
    "timestamp": new Date(Date.now() - 600000).toISOString(),
    "source": "Report Upload",
    "rawText": "Pipes delivered to sector 7 staging area.",
    "extracted": { "activity": "Pipes delivered to staging area", "date": "2026-08-25", "discipline": "Mechanical", "status": "Completed" },
    "matchedTaskId": "ACT-202",
    "matchedTaskDescription": "Stringing of 18 inch Pipeline - Segment 2 (Ch 2.5 to Ch 5.0)",
    "confidenceScore": 0.68,
    "status": "Pending Review",
    "auditTrail": "Auto-match confidence below threshold (68%). Flagged for Planner Review."
  }
];

fs.writeFileSync(BASELINE_PATH, JSON.stringify(SEED_BASELINE, null, 2));
fs.writeFileSync(PROGRESS_PATH, JSON.stringify(SEED_PROGRESS, null, 2));
console.log("Demo database seeded successfully.");

