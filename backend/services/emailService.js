const nodemailer = require('nodemailer');
require('dotenv').config();

/**
 * Email Service
 * Uses nodemailer if SMTP credentials are provided, otherwise falls back to console logging.
 */

async function sendEmail({ to, subject, body, html }) {
  const { SMTP_USER, SMTP_PASS, SMTP_HOST, SMTP_PORT } = process.env;

  if (SMTP_USER && SMTP_PASS) {
    try {
      const transporter = nodemailer.createTransport({
        host: SMTP_HOST || 'smtp.gmail.com',
        port: SMTP_PORT || 587,
        secure: false, // true for 465, false for other ports
        auth: {
          user: SMTP_USER,
          pass: SMTP_PASS,
        },
      });

      const info = await transporter.sendMail({
        from: `"Unnati Admin" <${process.env.EMAIL_FROM || 'paramjitbaral@gmail.com'}>`,
        to: to,
        subject: subject,
        text: body,
        html: html || body,
      });

      console.log(`📧 REAL EMAIL DISPATCHED to ${to} (Message ID: ${info.messageId})`);
      return true;
    } catch (err) {
      console.error('Failed to send real email. Check SMTP credentials:', err);
      // Fallback to mock if real fails (optional, but good for debugging)
      logMockEmail(to, subject, body);
      return false;
    }
  } else {
    // Fallback to mock
    logMockEmail(to, subject, body);
    return true;
  }
}

function logMockEmail(to, subject, body) {
  console.log('\n======================================================');
  console.log('📧 MOCK EMAIL DISPATCHED (Add SMTP details to .env for real emails)');
  console.log('======================================================');
  console.log(`To:      ${to}`);
  console.log(`Subject: ${subject}`);
  console.log('------------------------------------------------------');
  console.log(body);
  console.log('======================================================\n');
}

module.exports = {
  sendEmail
};
