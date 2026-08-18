# 🕸️ VentureGraph

**A startup-ecosystem explorer backed by a graph database (CognoDB).**

VentureGraph answers questions that are really about *connections*: *“Which investors backed the company of a founder who used to work at Google?”*, *“How is this founder connected to that company?”*, *“Which two investors keep co-investing in the same startups?”*. Those questions are hops through a graph — and they are exactly the kind of query that makes a graph database earn its place.

The stack: **Java 17 · Spring Boot 3 · Neo4j Java Driver 5 (Bolt 5.x)** talking to **CognoDB Cloud**, with a hand-rolled, dependency-free frontend served from the same JAR.

---

## Why a graph database?

The domain is a classic graph problem: founders, companies, investors, universities and industries, connected by *founded*, *worked at*, *invested in*, *graduated from*, *operates in* and *partners with* relationships.

Three queries in this app show why a relational schema would fight you:

1. **Multi-hop traversal** — *“Founders with a big-tech pedigree, and everyone who backed them.”*

   ```
   (Person)-[:WORKED_AT]->(Company)<-[:FOUNDED]-(Person)-[:FOUNDED]->(Company)<-[:INVESTED_IN]-(Investor)
   ```

   In SQL this is employment and investment tables joined through a company table *twice*, then deduplicated. In Cypher it is one pattern, and the traversal cost is proportional to the actual connections, not to table size.

2. **Shortest path** — *“Find the connection path between Patrick Collison and SpaceX.”*

   A relational database cannot express this at all without recursive CTEs plus a hand-written path-walking loop. Cypher has `shortestPath` built in, and the graph database executes it natively.

3. **Variable-length traversal** — *“Every company within 3 hops of this person.”*

   The depth of the query is a parameter of the pattern (`*1..3`), not a fixed join chain. Adding a fourth hop is changing one number, not writing another self-join.

The data model is also *evolving-friendly*: adding a new relationship type (e.g. `ADVISES`) is a schema change that requires no migration — a new edge type simply appears in queries that use it.

---

## Data model

```
┌──────────┐  WORKED_AT     ┌──────────┐
│  Person  │───────────────▶│          │
└────┬─────┘                │ Company  │
     │  FOUNDED             │          │
     ├────────────────────▶ │          │
     │                      └────┬─────┘
     │  GRADUATED_FROM          │  OPERATES_IN
     ▼                          ▼
┌──────────┐              ┌──────────┐
│University│              │ Industry │
└──────────┘              └──────────┘

┌──────────┐  INVESTED_IN  ┌──────────┐
│ Investor │──────────────▶│ Company  │
│ (also    │               └────┬─────┘
│  Person) │                    │  PARTNERS_WITH
└──────────┘                    ▼
                          ┌──────────┐
                          │ Company  │
                          └──────────┘

(Person)-[:KNOWS]->(Person)
```

**Nodes (labels):** `Person`, `Company`, `Investor`, `University`, `Industry`

**Relationships (types + key properties):**

| Type | From | To | Properties |
|---|---|---|---|
| `FOUNDED` | Person | Company | `role`, `since` |
| `WORKED_AT` | Person | Company | `role`, `start`, `end` |
| `INVESTED_IN` | Investor / Person | Company | `round`, `year`, `amountM` |
| `GRADUATED_FROM` | Person | University | `degree`, `year` |
| `OPERATES_IN` | Company | Industry | — |
| `PARTNERS_WITH` | Company | Company | — |
| `KNOWS` | Person | Person | — |

A single node can carry more than one label: `Microsoft` is both a `Company` and an `Investor` (corporate). Angels are modelled as `Person` nodes with their own `INVESTED_IN` edges, so they show up as people *and* backers.

The seed dataset (38 companies, 38 people, 12 investor firms, 22 industries, 21 universities, ~130 nodes and 194 relationships) uses real, recognisable companies and founders — rounds and amounts are illustrative. Data is loaded idempotently with `MERGE`, so the script is safe to re-run.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.5 |
| Database driver | `neo4j-java-driver` 5.28 (Bolt 5.x) |
| Database | CognoDB Cloud (free c0 tier) |
| Frontend | Vanilla HTML/CSS/JS single-page app (no build step, no framework) |
| Build | Maven (Maven Wrapper included) |
| Tests | JUnit 5 + Mockito |

The frontend is intentionally dependency-free: it is served as static resources from the same Spring Boot JAR, which keeps deployment to a single artifact.

---

## Architecture

```
Browser (vanilla SPA, hash-router)
        │  fetch /api/...
        ▼
┌───────────────────────────────────────────────┐
│ Spring Boot                                   │
│  Controllers  →  GraphService  →  Repository  │
│                      │              │         │
│                 validation        Cypher      │
│                 error mapping   (parameterised)│
└───────────────────────────────────────────────┘
        │  Bolt (bolt+s://) + auth
        ▼
CognoDB Cloud (openCypher graph database)
```

- **Repository** owns every Cypher statement. Values are always passed as named parameters (`$name`, `$q`, …) — there is no string-concatenated Cypher. The only text built into statements are relationship type literals and a server-side hop cap (`6`), both fixed constants in code.
- **Service** validates input and maps database failures to friendly errors.
- **Controllers** expose a small JSON API; a global exception handler turns `DbUnavailableException` into a `503` with a human-readable message.
- **Graceful degradation:** if `COGNODB_URI` is not set, the app still boots, the UI shows a clear banner, and every data endpoint returns a clean `503` — the app never crashes when the database is down.
- **Seed:** `SeedLoader` is a Spring `ApplicationRunner` triggered by `COGNODB_SEED=true`. It reads `src/main/resources/seed/startup-graph.json` and loads everything in one write transaction using `UNWIND … MERGE … SET` (idempotent). The same data is also shipped as raw Cypher in `scripts/seed.cypher`.

---

## API surface

| Endpoint | Purpose |
|---|---|
| `GET /api/health` | Connectivity + node/relationship counts |
| `GET /api/stats` | Home-page overview (counts, top industries, newest companies) |
| `GET /api/companies?q=&industry=` | Search companies |
| `GET /api/companies/{name}` | Company detail (founders, backers, industries, partners, alumni) |
| `GET /api/people?q=` / `GET /api/people/{name}` | Search / person detail |
| `GET /api/investors?q=` / `GET /api/investors/{name}` | Search / investor detail |
| `GET /api/universities` | List for the alumni-reach picker |
| `GET /api/autocomplete?q=` | Unified entity search (path finder) |
| `GET /api/paths?from=&to=` | `shortestPath` between any two entities (≤ 6 hops) |
| `GET /api/insights/big-tech-founders` | 3-hop: founder → big company → founder → startup → backers |
| `GET /api/insights/alumni-reach?university=` | 3-hop: university → founder → startup → investor |
| `GET /api/insights/common-investors?companyA=&companyB=` | Shared-investor intersection |
| `GET /api/insights/co-investment-network` | Investor pairs with shared portfolio companies |
| `GET /api/insights/network-reach?person=` | Variable-length: every company within 3 hops |

---

## The queries, explained

**1. Multi-hop — “Founders with a big-tech pedigree and their backers”** *(3 hops)*

```cypher
MATCH (p:Person)-[:WORKED_AT]->(big:Company)
WITH p, big
MATCH (p)-[:FOUNDED]->(startup:Company)
MATCH (inv)-[:INVESTED_IN]->(startup)
RETURN p.name AS founder, big.name AS almaMater, startup.name AS startup,
       collect(DISTINCT inv.name) AS backers
ORDER BY startup
```

Walk from a person to a company they worked at, then back to the person, then to a company they founded, then to everyone who invested in it. Four patterns, one query — the relational equivalent needs joins across `employment`, `company` and `investment` tables with deduplication.

**2. Shortest path — “Patrick Collison ↔ SpaceX”**

```cypher
MATCH (a {name: $from}), (b {name: $to})
MATCH p = shortestPath((a)-[*1..6]-(b))
RETURN [n IN nodes(p) | {name: n.name, label: labels(n)[0]}] AS nodes,
       [r IN relationships(p) | {type: type(r), from: startNode(r).name, to: endNode(r).name}] AS edges
```

`shortestPath` is native to the engine. The UI renders the returned path as an interactive SVG graph.

**3. Variable-length — “Every company within 3 hops of a person”**

```cypher
MATCH (me:Person {name: $person})
MATCH (me)-[:FOUNDED|WORKED_AT|INVESTED_IN*1..3]-(c:Company)
WITH c, count(*) AS paths
RETURN c.name AS name, c.stage AS stage, c.headquarters AS headquarters,
       c.foundedYear AS foundedYear, paths
ORDER BY paths DESC, name LIMIT 40
```

The `*1..3` makes depth a parameter of the pattern. Adding a hop is a one-character change.

**4. Intersection — “Common investors of two companies”**

```cypher
MATCH (a:Company {name: $companyA})<-[:INVESTED_IN]-(i)-[:INVESTED_IN]->(b:Company {name: $companyB})
RETURN i.name AS investor, i.kind AS kind
```

A relational version needs two subqueries intersected on investor id. Here it is a single pattern with one shared node.

**5. Common-neighbour — “Co-investment network”**

```cypher
MATCH (i1:Investor)-[:INVESTED_IN]->(c:Company)<-[:INVESTED_IN]-(i2:Investor)
WHERE i1.name < i2.name
RETURN i1.name AS investorA, i2.name AS investorB, count(DISTINCT c) AS sharedCompanies,
       collect(DISTINCT c.name)[0..4] AS examples
ORDER BY sharedCompanies DESC LIMIT 25
```

“People who invested in the same things as other people” is a self-join over an edge table with pair-generation — awkward in SQL, natural in a graph.

---

## Setup

### 1. Create the CognoDB instance

1. Sign up at **https://console.cognodb.com/signup** (free tier, no credit card).
2. Create a free **c0** instance and pick a region. It provisions in under a minute.
3. Copy the connection URI — it looks like `bolt+s://<instance-id>.databases.cognodb.cloud` — and the generated password for user `cognodb`. **The password is shown only once — save it now.**

### 2. Configure the app

```bash
cp .env.example .env
```

Edit `.env`:

```bash
COGNODB_URI=bolt+s://YOUR-INSTANCE-ID.databases.cognodb.cloud
COGNODB_USER=cognodb
COGNODB_PASSWORD=your-generated-password
COGNODB_SEED=true
```

`.env` is git-ignored; connection details are never committed. On deployment platforms, set the same variables in the platform's environment dashboard.

### 3. Build and run

Requires **Java 17+** (Maven comes via the wrapper):

```bash
./mvnw package          # Windows: mvnw.cmd package
COGNODB_SEED=true java -jar target/startup-graph-1.0.0.jar
```

With `COGNODB_SEED=true`, the app seeds the database on startup (idempotent — safe to re-run). Then open **http://localhost:8080**.

Set `COGNODB_SEED=false` (or unset it) afterwards so the seed does not run on every boot.

### Alternative: load seed data with raw Cypher

```bash
# with cypher-shell pointed at your instance, or from the CognoDB console query editor:
:source scripts/seed.cypher
```

### Run the tests

```bash
./mvnw test
```

The unit/integration suites run without a database. `GraphRepositoryIT` additionally runs against your live instance when `COGNODB_URI`/`COGNODB_PASSWORD` are set — a handy post-seed smoke test:

```bash
COGNODB_URI=... COGNODB_PASSWORD=... ./mvnw test
```

---

## UI tour

| Page | What it does |
|---|---|
| **Home** | Overview stats pulled from the graph, newest companies, “why a graph?” |
| **Explore** | Search companies / people / investors; every result is a node |
| **Detail pages** | Company (founders, backers, industries, partners, alumni), person (founded, work history, angel investments, education, connections), investor (portfolio) |
| **Path Finder** | Pick any two entities, get the shortest path rendered as an interactive SVG graph, plus a step-by-step list |
| **Insights** | The five showcase queries above, each with a plain-English explanation of why it is hard in SQL |

Every page has proper loading (skeletons), empty and error states, and a global banner when the database is unreachable.

### Screenshots

| Screen | Image |
|---|---|
| Home | `docs/screenshots/home.png` |
| Path Finder | `docs/screenshots/path-finder.png` |
| Insights | `docs/screenshots/insights.png` |
| Company detail | `docs/screenshots/company.png` |

> Screenshots captured from the live app against a seeded CognoDB instance.

---

## Deployment (Render — Free Tier)

The app is deployable on [Render](https://render.com) as a single web service:

1. Push this repo to GitHub
2. On Render → **New Web Service** → connect your repo
3. Set env vars:
   - `COGNODB_URI` = your CognoDB bolt+s:// URL
   - `COGNODB_USER` = `cognodb`
   - `COGNODB_PASSWORD` = your password
   - `COGNODB_SEED` = `true` (first deploy only)
   - `JAVA_VERSION` = `17`
4. Build command: `./mvnw clean package -DskipTests`
5. Start command: `java -jar target/startup-graph-1.0.0.jar`
6. Health check path: `/api/health`

Full step-by-step guide: see **[DEPLOY.md](DEPLOY.md)**

> ⚠️ Keep your CognoDB instance running. Free tier Render spins down after inactivity — first request takes 30-60 seconds.

---

## Screen Recording

See **[SCREEN_RECORDING_SCRIPT.md](SCREEN_RECORDING_SCRIPT.md)** for a 60-90 second walkthrough script covering all key features.

---

## Project layout

```
startup-graph/
├── pom.xml
├── mvnw / mvnw.cmd                  # Maven Wrapper
├── .env.example                     # documented env vars (never commit .env)
├── scripts/
│   ├── seed.cypher                  # raw Cypher seed (generated, idempotent)
│   └── generate_seed_cypher.py      # regenerates seed.cypher from the JSON
├── src/main/java/com/startupgraph/
│   ├── StartupGraphApplication.java
│   ├── config/                      # DbProperties, Neo4jConfig (driver bean)
│   ├── graph/                       # GraphRepository (Cypher), GraphService, ValueUtil, exceptions
│   ├── web/                         # REST controllers, global exception handler
│   ├── dto/                         # API response records
│   └── seed/                        # SeedLoader (parameterised UNWIND/MERGE)
├── src/main/resources/
│   ├── application.yml              # env-driven config
│   ├── seed/startup-graph.json      # the dataset
│   └── static/                      # index.html, css/app.css, js/app.js
└── src/test/java/                   # unit tests + optional live-DB integration test
```

---

## Notes for reviewers

- **No string-concatenated Cypher.** All user-supplied values flow through named parameters. Relationship types and the hop cap are fixed constants in code.
- **Secrets** live only in environment variables; `.env` is ignored.
- **Graceful failure** is a first-class concern: the app boots without a database, reports it honestly in the UI, and recovers as soon as the database is reachable (Retry button).
- The dataset is realistic but deliberately small (a few hundred nodes) — comfortably inside the free c0 tier (1 GB disk).
