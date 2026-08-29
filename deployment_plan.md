# Unnati Cloud Deployment Plan

This guide outlines exactly what parameters, credentials, and configuration files you need to deploy the **Unnati** system in real-time using **Supabase** (Database), **Render** (Express Backend), and **Vercel** (Manager Web Panel).

---

## Phase 1: Database Setup (Supabase)

Supabase runs fully managed PostgreSQL databases that are 100% compatible with our database schemas and seeders.

1. Go to [Supabase](https://supabase.com) and create a free project.
2. Once the project is provisioned, go to **Project Settings** -> **Database**.
3. Under **Connection Pooler**, copy your database connection details. Make sure you use the **Session** or **Transaction** mode connection parameters.
4. Note down the following values to configure in Render later:
   * **Host** (`PGHOST`)
   * **Port** (`PGPORT` - usually `6543`)
   * **User** (`PGUSER` - usually `postgres.xxxxx`)
   * **Password** (`PGPASSWORD` - the password you set during Supabase project creation)
   * **Database** (`PGDATABASE` - usually `postgres`)

---

## Phase 2: Backend Setup (Render)

Render will host the Node.js Express server (`Unnati WEB`) and handle incoming API calls from both Vercel and the Android mobile application.

1. Create a free account at [Render](https://render.com).
2. Connect your GitHub repository containing the **Unnati** code.
3. Create a new **Web Service** on Render and point it to the `Unnati WEB` folder.
4. Choose the following runtime parameters:
   * **Runtime**: `Node`
   * **Build Command**: `npm install`
   * **Start Command**: `node server.js`
5. Go to the **Environment** tab of your Render Web Service and add the following Environment Variables:

| Variable Name | Description / Example Value |
| :--- | :--- |
| `PORT` | `3000` |
| `PGHOST` | *Your Supabase Host* (e.g. `aws-0-us-east-1.pooler.supabase.com`) |
| `PGPORT` | `6543` |
| `PGUSER` | *Your Supabase User* (e.g. `postgres.xxxxxx`) |
| `PGPASSWORD` | *Your Supabase Database Password* |
| `PGDATABASE` | `postgres` |
| `GEMINI_API_KEY` | *Your Google AI Studio Gemini API Key* (optional, for real-time AI transcription & linking) |

6. Deploy the web service. Render will automatically launch the service and log:
   * `Initializing PostgreSQL database...`
   * `PostgreSQL database initialization completed.`
   * `UNNATI backend listening on port 3000`
7. Copy your deployed Render service URL (e.g. `https://unnati-backend.onrender.com`).

---

## Phase 3: Frontend Setup (Vercel)

Vercel will host the HTML/CSS/JS frontend files. To prevent Cross-Origin Resource Sharing (CORS) issues and ensure all `/api/*` endpoints are dynamically routed to your Render backend, we use Vercel's rewrite proxy.

1. Create a `vercel.json` file in the root of your `Unnati WEB` directory:

```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://your-backend-app.onrender.com/api/:path*"
    }
  ]
}
```
*(Replace `https://your-backend-app.onrender.com` with the actual Render service URL you copied in Phase 2)*.

2. Deploy the `Unnati WEB` directory to [Vercel](https://vercel.com) by connecting Vercel to your GitHub repository or running `vercel` in the CLI.

---

## Phase 4: Mobile App Compilation (Android Studio)

Before compiling the mobile application to install on your mobile device, update the server URL to point to your live backend.

1. Open [`RetrofitClient.kt`](file:///c:/Users/swara/OneDrive/Desktop/Unnati/Unnati%20APP/app/src/main/java/com/example/data/network/RetrofitClient.kt) inside Android Studio.
2. Locate the `baseUrl` variable near the top:
   ```kotlin
   var baseUrl: String = "https://your-backend-app.onrender.com/"
   ```
   Replace this URL with your live Render Web Service URL.
3. Build the release APK inside Android Studio (`Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`).
4. Transfer the generated APK to your Android phone and install it.
