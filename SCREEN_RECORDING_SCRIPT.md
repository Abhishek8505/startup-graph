# 🎬 Screen Recording Walkthrough Script

**Duration:** 60-90 seconds
**Tool:** Loom (free) or OBS Studio (free)
**Resolution:** 1920x1080 or 1280x720

---

## 🎯 What to Show (in order)

### Scene 1: Home Page (10-15 sec)
**Show:** http://localhost:8080 (or your Render URL)

**Script to say:**
> "This is VentureGraph — a graph-based startup ecosystem explorer built with Spring Boot and Neo4j, connected to CognoDB.
>
> The home page shows our graph stats: 38 companies, 38 people, 12 investors, and 194 relationships — all loaded from a single Cypher seed file."

**Action:** Hover over stats cards to highlight them

---

### Scene 2: Company Detail (10-15 sec)
**Show:** Click on any company (e.g., Stripe or SpaceX)

**Script to say:**
> "Let me explore a company — Stripe. We can see founders, investors, partner companies, and industries — all connected via graph relationships.
>
> Notice the backers section — these are real investors from our dataset."

**Action:** Scroll through the detail page, highlight Founders and Backers sections

---

### Scene 3: Path Finder (20-25 sec) ⭐ KEY DEMO
**Show:** Path Finder page

**Script to say:**
> "Now let's use the Path Finder — this is where graph databases really shine.
>
> I'll find the connection between Patrick Collison, CEO of Stripe, and SpaceX.
>
> [Run query] The result shows a 6-hop path through MIT, Elad Gil, Reid Hoffman, and Sam Altman — connecting two seemingly unrelated entities through shared relationships.
>
> This kind of multi-hop query would require complex self-joins in a relational database, but in Neo4j it's a single shortestPath traversal."

**Action:**
1. Type "Patrick Collison" in first box
2. Type "SpaceX" in second box
3. Click "Find Path"
4. Wait for SVG graph to render
5. Hover over nodes in the graph

---

### Scene 4: Insights (15-20 sec)
**Show:** Insights page

**Script to say:**
> "The Insights page showcases 5 different Cypher queries — multi-hop traversal, alumni network reach, co-investment analysis, shortest path, and variable-length pattern matching.
>
> Let me run the Co-Investment Network query — it finds investor pairs who have invested in the same companies."

**Action:**
1. Click on "Co-Investment Network" card
2. Wait for results to load
3. Scroll through the results showing investor pairs

---

### Scene 5: Closing (5-10 sec)
**Show:** Back to Home page

**Script to say:**
> "VentureGraph demonstrates the power of graph databases for relationship-heavy data. The entire application is built with Spring Boot, Neo4j Java Driver, and CognoDB — with a vanilla HTML/CSS/JS frontend.
>
> All queries are parameterized, the app gracefully handles database unavailability, and there are 17 passing tests including live integration tests against CognoDB."

---

## 📝 Quick Reference Card

| Timestamp | What | Duration |
|-----------|------|----------|
| 0:00 | Home page stats | 10-15 sec |
| 0:15 | Company detail (Stripe) | 10-15 sec |
| 0:30 | Path Finder demo | 20-25 sec |
| 0:55 | Insights query | 15-20 sec |
| 1:10 | Closing summary | 5-10 sec |

---

## 🎤 Key Phrases to Include

- "Built with Spring Boot and Neo4j driver"
- "Connected to CognoDB cloud instance"
- "130 nodes, 194 relationships"
- "Parameterized Cypher queries"
- "Graph traversal vs relational joins"
- "ShortestPath algorithm"
- "Graceful degradation when DB unavailable"

---

## ⚠️ Recording Tips

1. **Close unnecessary tabs/apps** — clean desktop
2. **Use incognito mode** if showing hosted URL (no bookmarks bar)
3. **Speak clearly** — pause briefly between scenes
4. **Mouse cursor** — use a visible cursor highlight tool (optional)
5. **Test recording first** — 10-sec clip to check audio/video

---

## 🔗 Upload Options

| Platform | Link Format | Notes |
|----------|-------------|-------|
| **Loom** | `https://loom.com/share/xxxxx` | Free, easy, instant link |
| **YouTube** | Unlisted video | Free, longer processing |
| **Google Drive** | Shareable link | Free with Gmail |

**Recommended:** Loom — fastest, gives you a shareable link immediately.
