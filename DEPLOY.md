# 🚀 Deploy to Render (Free Tier)

## Prerequisites
- GitHub account with `startup-graph` repo
- CognoDB instance running (your URI + password)
- Render account (free)

---

## Step 1: Create Render Account

1. Go to [render.com](https://render.com)
2. Click **"Get Started for Free"**
3. Sign up with **GitHub** (recommended — one-click)

---

## Step 2: Create New Web Service

1. Dashboard → **"New +"** → **"Web Service"**
2. **"Build and deploy from a Git repo"** → **"Next"**
3. Connect your GitHub repository:
   - Search for `startup-graph`
   - Click **"Connect"**

---

## Step 3: Configure Service

| Field | Value |
|-------|-------|
| **Name** | `venturegraph` |
| **Region** | `Oregon` (or closest to you) |
| **Branch** | `main` |
| **Runtime** | `Java` |
| **Build Command** | `./mvnw clean package -DskipTests` |
| **Start Command** | `java -jar target/startup-graph-1.0.0.jar` |
| **Plan** | `Free` |

---

## Step 4: Add Environment Variables

Scroll to **"Environment Variables"** section and add:

| Key | Value |
|-----|-------|
| `COGNODB_URI` | `bolt+s://db-7f2f0b9f.databases.cognodb.com` |
| `COGNODB_USER` | `cognodb` |
| `COGNODB_PASSWORD` | `50f9a46c6a5acc5d4674e5ffbbde052a` |
| `COGNODB_SEED` | `true` |
| `JAVA_VERSION` | `17` |

⚠️ **Important:** `COGNODB_SEED=true` will auto-seed data on first deploy.

---

## Step 5: Deploy

1. Click **"Create Web Service"**
2. Wait for build (~3-5 minutes first time)
3. Render will show build logs — watch for:
   ```
   BUILD SUCCESS
   Started StartupGraphApplication in X seconds
   Seed complete. XXX entities and relationships processed.
   ```

---

## Step 6: Get Your Demo Link

After successful deploy:
- Render gives you a URL like: `https://venturegraph-xxxx.onrender.com`
- **Copy this URL** — this is your **hosted demo link** for the assignment!

---

## Step 7: Verify

Open your Render URL and check:
- ✅ Home page loads with stats
- ✅ Explore page shows companies/people
- ✅ Path Finder works (try: `Patrick Collison` → `SpaceX`)
- ✅ Insights page shows query results

---

## Troubleshooting

### Build fails with "Java version not found"
→ Add env var: `JAVA_VERSION` = `17`

### App starts but 503 / "not_configured"
→ Check `COGNODB_URI` env var is correct (no trailing space)

### Seed doesn't run
→ Make sure `COGNODB_SEED=true` is set

### App is slow on first load
→ Render free tier spins down after inactivity — first request takes 30-60 seconds (normal)

---

## Updating After First Deploy

Just `git push` to main — Render auto-deploys on every push!

```bash
cd startup-graph
git add .
git commit -m "Update: ..."
git push origin main
```
