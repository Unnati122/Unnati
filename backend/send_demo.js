const emailService = require('./services/emailService');

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
                Hello DEMO MANAGER,<br><br>
                You have been assigned as the Project Manager for <strong>DEMO PROJECT EX-1</strong>.
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
                    <strong style="font-size: 16px; color: #000000; font-family: monospace;">demo-manager</strong>
                  </td>
                </tr>
                <tr>
                  <td>
                    <span style="font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #666666;">Password</span><br>
                    <strong style="font-size: 16px; color: #000000; font-family: monospace;">TempPass123!</strong>
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

async function run() {
  await emailService.sendEmail({
    to: '2300033156cse4@gmail.com',
    subject: 'DEMO V4: Ultra Minimal & Professional',
    body: 'Demo plain text fallback',
    html: managerHtmlTemplate
  });
  console.log('Demo sent!');
  process.exit(0);
}
run();
