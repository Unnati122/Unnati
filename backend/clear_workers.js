const db = require('./services/db');

async function clean() {
  try {
    const res = await db.query("DELETE FROM users WHERE role = 'field'");
    console.log(`Deleted ${res.rowCount} field workers.`);
  } catch (err) {
    console.error(err);
  } finally {
    process.exit(0);
  }
}

clean();
