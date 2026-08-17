"use strict";

const LABEL_COLORS = {
    Person: "#6366f1",
    Company: "#0ea5e9",
    Investor: "#f59e0b",
    University: "#10b981",
    Industry: "#f43f5e"
};

const LABEL_BADGE = {
    Person: "person",
    Company: "company",
    Investor: "investor",
    University: "university",
    Industry: "industry"
};

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

function esc(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function fmtMoney(amountM) {
    if (amountM == null) return "";
    if (amountM >= 1000) return `$${(amountM / 1000).toFixed(amountM % 1000 === 0 ? 0 : 1)}B`;
    if (amountM >= 1) return `$${amountM % 1 === 0 ? amountM : amountM.toFixed(1)}M`;
    return `$${Math.round(amountM * 100)}K`;
}

function fmtYear(value) {
    return value == null ? "" : String(value);
}

function badge(label) {
    const cls = LABEL_BADGE[label] || "node";
    return `<span class="badge ${cls}">${esc(label)}</span>`;
}

function debounce(fn, ms) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), ms);
    };
}

async function api(path) {
    let res;
    try {
        res = await fetch(path);
    } catch {
        throw new Error("Cannot reach the server.");
    }
    let body = null;
    try {
        body = await res.json();
    } catch {
        /* no body */
    }
    if (!res.ok) {
        const message = body && body.message ? body.message : `Request failed (${res.status})`;
        if (res.status === 503) {
            showBanner("warn", `${message} — data will appear once the database is reachable.`);
        }
        throw new Error(message);
    }
    return body;
}

function showBanner(kind, message) {
    const banner = $("#banner");
    banner.className = `banner ${kind}`;
    banner.innerHTML = `${esc(message)} <button id="banner-retry">Retry</button>`;
    $("#banner-retry").addEventListener("click", () => {
        window.location.hash = window.location.hash || "#/";
        window.location.reload();
    });
}

function hideBanner() {
    $("#banner").className = "banner hidden";
}

function skeletonLines(count) {
    return `<div class="skeleton">${Array.from({ length: count }, () => `<div class="skel-line"></div>`).join("")}</div>`;
}

function skeletonCards(count) {
    return Array.from({ length: count }, () => `<div class="skeleton skel-card"></div>`).join("");
}

function emptyState(icon, title, text) {
    return `<div class="state">
        <div class="state-icon">${icon}</div>
        <h3>${esc(title)}</h3>
        <p>${esc(text)}</p>
    </div>`;
}

function errorBlock(message) {
    return `<div class="error-state"><strong>Something went wrong.</strong> ${esc(message)}</div>`;
}

async function withState(container, loadFn) {
    container.innerHTML = skeletonLines(4);
    try {
        const html = await loadFn();
        container.innerHTML = html;
    } catch (e) {
        container.innerHTML = errorBlock(e.message);
    }
}

const router = {
    routes: {},

    add(path, handler) {
        this.routes[path] = handler;
    },

    start() {
        window.addEventListener("hashchange", () => this.dispatch());
        this.dispatch();
    },

    dispatch() {
        const hash = window.location.hash.replace(/^#/, "") || "/";
        const [pathPart, queryPart] = hash.split("?");
        const params = new URLSearchParams(queryPart || "");
        const segments = pathPart.split("/").filter(Boolean);
        const app = $("#app");
        app.scrollTop = 0;
        window.scrollTo(0, 0);

        let handler = this.routes[pathPart];
        if (!handler) {
            const detail = this.routes[`/${segments[0]}/:name`];
            if (detail) {
                handler = detail;
                segments[1] = decodeURIComponent(segments[1] || "");
            }
        }
        const nav = $$(".nav a");
        nav.forEach(a => a.classList.remove("active"));
        const activeNav = nav.find(a => a.getAttribute("href").split("?")[0] === pathPart);
        if (activeNav) activeNav.classList.add("active");

        if (handler) {
            handler(app, segments, params);
        } else {
            app.innerHTML = emptyState("🧭", "Page not found", "This page does not exist.");
        }
    }
};

router.add("/", (app) => renderHome(app));
router.add("/explore", (app, seg, params) => renderExplore(app, params));
router.add("/company/:name", (app, seg) => renderCompany(app, seg[1]));
router.add("/person/:name", (app, seg) => renderPerson(app, seg[1]));
router.add("/investor/:name", (app, seg) => renderInvestor(app, seg[1]));
router.add("/path", (app, seg, params) => renderPath(app, seg, params));
router.add("/insights", (app) => renderInsights(app));

/* ---------------- home ---------------- */

async function renderHome(app) {
    app.innerHTML = `
        <div class="page">
            <section class="hero">
                <h1>Every startup story is a web of people, money and ideas.</h1>
                <p>VentureGraph explores the startup ecosystem as a graph: founders, companies, investors, universities and industries — and the relationships between them. Follow a connection from one company to the next.</p>
                <div class="hero-actions">
                    <a class="chip" href="#/explore">Explore the graph</a>
                    <a class="chip ghost" href="#/path">Find a connection path</a>
                    <a class="chip ghost" href="#/insights">Run graph queries</a>
                </div>
            </section>

            <h2 class="section-title">The ecosystem at a glance</h2>
            <div id="home-stats" class="grid cols-4">${skeletonCards(4)}</div>

            <h2 class="section-title">Freshly founded</h2>
            <div id="home-newest" class="card row-list">${skeletonLines(5)}</div>

            <h2 class="section-title">Why a graph?</h2>
            <div class="grid cols-3">
                <div class="card">
                    <h3>🕸️ Connections are the data</h3>
                    <p style="color:var(--text-soft);font-size:14px;margin-top:6px;">“Who invested in the company of a founder who worked at Google?” is a hop through the graph — not a chain of joins.</p>
                </div>
                <div class="card">
                    <h3>🔗 Paths, not tables</h3>
                    <p style="color:var(--text-soft);font-size:14px;margin-top:6px;">The Path Finder answers “how am I connected to this company?” with a real traversal between any two entities.</p>
                </div>
                <div class="card">
                    <h3>⚡ Variable-depth queries</h3>
                    <p style="color:var(--text-soft);font-size:14px;margin-top:6px;">“Everything in my 3-hop network” is one pattern. In SQL that is recursive and painful.</p>
                </div>
            </div>
        </div>`;

    withState($("#home-stats"), async () => {
        const stats = await api("/api/stats");
        const topIndustry = stats.topIndustries[0];
        const topInvestor = stats.topInvestors[0];
        const companies = stats.nodesByLabel.find(r => r.name === "Company");
        const people = stats.nodesByLabel.find(r => r.name === "Person");
        const investors = stats.nodesByLabel.find(r => r.name === "Investor");
        const rels = stats.relsByType.reduce((sum, r) => sum + r.value, 0);
        return `
            <div class="card stat-card"><span class="stat-value">${companies?.value ?? 0}</span><span class="stat-label">Companies</span></div>
            <div class="card stat-card"><span class="stat-value">${people?.value ?? 0}</span><span class="stat-label">People</span></div>
            <div class="card stat-card"><span class="stat-value">${investors?.value ?? 0}</span><span class="stat-label">Investors</span></div>
            <div class="card stat-card"><span class="stat-value">${rels}</span><span class="stat-label">Relationships</span></div>
            ${topIndustry ? `<div class="card stat-card" style="grid-column:1/-1"><span class="stat-label">Top industry</span><span class="stat-value" style="font-size:20px;">${esc(topIndustry.name)}</span>
                <div class="stat-bar"><div style="width:100%"></div></div></div>` : ""}
            ${topInvestor ? `<div class="card stat-card" style="grid-column:1/-1"><span class="stat-label">Most active investor</span><span class="stat-value" style="font-size:20px;">${esc(topInvestor.name)}</span>
                <div class="stat-bar"><div style="width:${Math.min(100, Math.round(topInvestor.value / 5 * 100))}%"></div></div></div>` : ""}`;
    });

    withState($("#home-newest"), async () => {
        const stats = await api("/api/stats");
        if (!stats.newestCompanies.length) {
            return emptyState("🌱", "No companies yet", "Seed the database to get started.");
        }
        return stats.newestCompanies.map(c => `
            <a class="row-item" href="#/company/${encodeURIComponent(c.name)}">
                <span class="row-main">
                    <span class="badge company">${esc(c.foundedYear ?? "—")}</span>
                    <span class="row-title">${esc(c.name)}</span>
                    <span class="row-sub">${esc(c.headquarters ?? "")}</span>
                </span>
                <span class="badge stage">${esc(c.stage ?? "")}</span>
            </a>`).join("");
    });
}

/* ---------------- explore ---------------- */

const EXPLORE_TABS = [
    { key: "companies", label: "Companies", endpoint: "/api/companies", card: companyCard },
    { key: "people", label: "People", endpoint: "/api/people", card: personCard },
    { key: "investors", label: "Investors", endpoint: "/api/investors", card: investorCard }
];

function companyCard(c) {
    return `<a class="card clickable" href="#/company/${encodeURIComponent(c.name)}">
        <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start;">
            <h3 style="font-size:17px;">${esc(c.name)}</h3>
            ${badge("Company")}
        </div>
        <p style="color:var(--text-soft);font-size:13.5px;margin-top:6px;min-height:40px;">${esc(c.description || "No description.")}</p>
        <div style="display:flex;gap:10px;margin-top:12px;font-size:13px;color:var(--text-soft);flex-wrap:wrap;">
            <span>🏢 ${esc(c.headquarters || "—")}</span>
            <span>🗓 ${esc(c.foundedYear ?? "—")}</span>
            <span class="badge stage">${esc(c.stage || "")}</span>
        </div>
    </a>`;
}

function personCard(p) {
    return `<a class="card clickable" href="#/person/${encodeURIComponent(p.name)}">
        <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start;">
            <h3 style="font-size:17px;">${esc(p.name)}</h3>
            ${badge("Person")}
        </div>
        <p style="color:var(--text-soft);font-size:13.5px;margin-top:6px;min-height:40px;">${esc(p.title || "No title.")}</p>
        <div style="margin-top:12px;font-size:13px;color:var(--text-soft);">📍 ${esc(p.location || "—")}</div>
    </a>`;
}

function investorCard(i) {
    return `<a class="card clickable" href="#/investor/${encodeURIComponent(i.name)}">
        <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start;">
            <h3 style="font-size:17px;">${esc(i.name)}</h3>
            ${badge("Investor")}
        </div>
        <p style="color:var(--text-soft);font-size:13.5px;margin-top:6px;min-height:40px;">${esc(i.description || "No description.")}</p>
        <div style="display:flex;gap:10px;margin-top:12px;font-size:13px;color:var(--text-soft);">
            <span>🏷 ${esc(i.kind || "—")}</span>
            <span>📍 ${esc(i.location || "—")}</span>
        </div>
    </a>`;
}

function renderExplore(app, params) {
    const tabKey = params.get("tab") || "companies";
    const query = params.get("q") || "";
    const tab = EXPLORE_TABS.find(t => t.key === tabKey) || EXPLORE_TABS[0];

    app.innerHTML = `
        <div class="page">
            <div class="page-head">
                <h1>Explore</h1>
                <p>Browse companies, founders and investors in the ecosystem. Every card is a node in the graph.</p>
            </div>
            <div class="tabs">
                ${EXPLORE_TABS.map(t => `<button class="tab ${t.key === tab.key ? "active" : ""}" data-tab="${t.key}">${t.label}</button>`).join("")}
            </div>
            <form id="explore-search" class="searchbar">
                <input class="input" id="explore-q" placeholder="Search ${tab.label.toLowerCase()}…" value="${esc(query)}">
                <button class="btn" type="submit">Search</button>
            </form>
            <div id="explore-results" class="grid cols-3">${skeletonCards(6)}</div>
        </div>`;

    $$(".tab", app).forEach(btn => btn.addEventListener("click", () => {
        window.location.hash = `#/explore?tab=${btn.dataset.tab}`;
    }));

    $("#explore-search", app).addEventListener("submit", (e) => {
        e.preventDefault();
        const q = $("#explore-q", app).value.trim();
        const params = new URLSearchParams({ tab: tab.key });
        if (q) params.set("q", q);
        window.location.hash = `#/explore?${params.toString()}`;
    });

    withState($("#explore-results", app), async () => {
        const params = new URLSearchParams();
        if (query) params.set("q", query);
        const rows = await api(`${tab.endpoint}?${params.toString()}`);
        if (!rows.length) {
            return emptyState("🔍", "Nothing found", query
                ? `No ${tab.label.toLowerCase()} match “${query}”.`
                : `No ${tab.label.toLowerCase()} in the database yet.`);
        }
        return rows.map(tab.card).join("");
    });
}

/* ---------------- detail pages ---------------- */

function participationRow(p) {
    return `<div class="kv-item">
        <span class="k">${esc(p.name || "")}</span>
        <span class="v">${esc(p.round || "")} · ${fmtYear(p.year)} · ${fmtMoney(p.amountM)}</span>
    </div>`;
}

async function renderCompany(app, name) {
    app.innerHTML = `<div class="page"><div id="company-body">${skeletonLines(6)}</div></div>`;
    try {
        const c = await api(`/api/companies/${encodeURIComponent(name)}`);
        app.innerHTML = `
            <div class="page">
                <a href="#/explore?tab=companies" style="font-size:13.5px;">← Back to companies</a>
                <div class="detail-head">
                    <div>
                        <h1 class="detail-title">${esc(c.info.name)}</h1>
                        <p class="detail-sub">${esc(c.info.description || "No description.")}</p>
                    </div>
                    <span class="badge company lg">${esc(c.info.stage || "Company")}</span>
                </div>
                <div class="detail-grid">
                    <div class="card">
                        <h2 class="section-title">Overview</h2>
                        <dl class="kv">
                            <dt>Founded</dt><dd>${fmtYear(c.info.foundedYear)}</dd>
                            <dt>Headquarters</dt><dd>${esc(c.info.headquarters || "—")}</dd>
                            <dt>Industries</dt><dd>${c.industries.map(i => `<a href="#/explore?tab=companies&q=${encodeURIComponent(i)}">${esc(i)}</a>`).join(", ") || "—"}</dd>
                            <dt>Partners</dt><dd>${c.partners.map(p => `<a href="#/company/${encodeURIComponent(p)}">${esc(p)}</a>`).join(", ") || "—"}</dd>
                        </dl>
                    </div>
                    <div class="card">
                        <h2 class="section-title">Founders</h2>
                        ${c.founders.length ? `<div class="kv-list">${c.founders.map(f => `<div class="kv-item"><span class="k"><a href="#/person/${encodeURIComponent(f.person)}">${esc(f.person)}</a></span><span class="v">${esc(f.role || "")} · ${fmtYear(f.since)}</span></div>`).join("")}</div>` : emptyState("👤", "No founders", "No founder relationships recorded.")}
                    </div>
                    <div class="card">
                        <h2 class="section-title">Backers</h2>
                        ${c.investors.length ? `<div class="kv-list">${c.investors.map(i => `<div class="kv-item"><span class="k"><a href="${i.kind === "Investor" ? `#/investor/${encodeURIComponent(i.name)}` : `#/person/${encodeURIComponent(i.name)}`}">${esc(i.name)}</a></span><span class="v">${esc(i.round)} · ${fmtYear(i.year)} · ${fmtMoney(i.amountM)}</span></div>`).join("")}</div>` : emptyState("💰", "No backers", "No investments recorded for this company.")}
                    </div>
                    <div class="card">
                        <h2 class="section-title">Alumni</h2>
                        ${c.alumni.length ? `<div class="kv-list">${c.alumni.map(a => `<div class="kv-item"><span class="k"><a href="#/person/${encodeURIComponent(a.person)}">${esc(a.person)}</a></span><span class="v">${esc(a.role)} · ${fmtYear(a.start)}${a.end ? "–" + a.end : "+"}</span></div>`).join("")}</div>` : emptyState("🎓", "No alumni", "No one has a recorded work history here.")}
                    </div>
                </div>
            </div>`;
    } catch (e) {
        app.innerHTML = `<div class="page">${errorBlock(e.message)}</div>`;
    }
}

async function renderPerson(app, name) {
    app.innerHTML = `<div class="page"><div id="person-body">${skeletonLines(6)}</div></div>`;
    try {
        const p = await api(`/api/people/${encodeURIComponent(name)}`);
        app.innerHTML = `
            <div class="page">
                <a href="#/explore?tab=people" style="font-size:13.5px;">← Back to people</a>
                <div class="detail-head">
                    <div>
                        <h1 class="detail-title">${esc(p.info.name)}</h1>
                        <p class="detail-sub">${esc(p.info.title || "")}${p.info.location ? ` · ${esc(p.info.location)}` : ""}</p>
                    </div>
                    ${badge("Person")}
                </div>
                <p style="color:var(--text-soft);max-width:680px;margin-top:10px;">${esc(p.info.bio || "No bio.")}</p>
                <div class="detail-grid">
                    <div class="card">
                        <h2 class="section-title">Founded</h2>
                        ${p.founded.length ? `<div class="kv-list">${p.founded.map(f => `<div class="kv-item"><span class="k"><a href="#/company/${encodeURIComponent(f.company)}">${esc(f.company)}</a></span><span class="v">${esc(f.role || "")} · ${fmtYear(f.since)}</span></div>`).join("")}</div>` : emptyState("🚀", "No companies", "No founding history recorded.")}
                    </div>
                    <div class="card">
                        <h2 class="section-title">Work history</h2>
                        ${p.workedAt.length ? `<div class="kv-list">${p.workedAt.map(w => `<div class="kv-item"><span class="k"><a href="#/company/${encodeURIComponent(w.company)}">${esc(w.company)}</a></span><span class="v">${esc(w.role)} · ${fmtYear(w.start)}–${fmtYear(w.end || "now")}</span></div>`).join("")}</div>` : emptyState("💼", "No work history", "No employers recorded.")}
                    </div>
                    <div class="card">
                        <h2 class="section-title">Angel investments</h2>
                        ${p.investedIn.length ? `<div class="kv-list">${p.investedIn.map(participationRow).join("")}</div>` : emptyState("💰", "No investments", "No angel investments recorded.")}
                    </div>
                    <div class="card">
                        <h2 class="section-title">Education</h2>
                        ${p.education.length ? `<div class="kv-list">${p.education.map(e => `<div class="kv-item"><span class="k">${esc(e.degree || "Degree")}</span><span class="v"><a href="#/explore?tab=companies&q=${encodeURIComponent(e.university)}">${esc(e.university)}</a> · ${fmtYear(e.year)}</span></div>`).join("")}</div>` : emptyState("🎓", "No education", "No education recorded.")}
                    </div>
                </div>
                ${p.knows.length ? `<h2 class="section-title" style="margin-top:22px;">Connections</h2>
                    <div class="card" style="display:flex;flex-wrap:wrap;gap:8px;">${p.knows.map(k => `<a class="badge person lg" href="#/person/${encodeURIComponent(k)}">${esc(k)}</a>`).join("")}</div>` : ""}
            </div>`;
    } catch (e) {
        app.innerHTML = `<div class="page">${errorBlock(e.message)}</div>`;
    }
}

async function renderInvestor(app, name) {
    app.innerHTML = `<div class="page"><div id="investor-body">${skeletonLines(6)}</div></div>`;
    try {
        const i = await api(`/api/investors/${encodeURIComponent(name)}`);
        app.innerHTML = `
            <div class="page">
                <a href="#/explore?tab=investors" style="font-size:13.5px;">← Back to investors</a>
                <div class="detail-head">
                    <div>
                        <h1 class="detail-title">${esc(i.info.name)}</h1>
                        <p class="detail-sub">${esc(i.info.kind || "")}${i.info.location ? ` · ${esc(i.info.location)}` : ""}</p>
                    </div>
                    ${badge("Investor")}
                </div>
                <p style="color:var(--text-soft);max-width:680px;margin-top:10px;">${esc(i.info.description || "No description.")}</p>
                <h2 class="section-title" style="margin-top:26px;">Portfolio (${i.portfolio.length})</h2>
                <div class="table-wrap">
                    <table>
                        <thead><tr><th>Company</th><th>Round</th><th>Year</th><th>Amount</th></tr></thead>
                        <tbody>
                            ${i.portfolio.map(p => `<tr>
                                <td><a href="#/company/${encodeURIComponent(p.name)}">${esc(p.name)}</a></td>
                                <td>${esc(p.round || "—")}</td>
                                <td>${fmtYear(p.year)}</td>
                                <td>${fmtMoney(p.amountM)}</td>
                            </tr>`).join("") || `<tr><td colspan="4" style="text-align:center;color:var(--text-soft);">No portfolio companies recorded.</td></tr>`}
                        </tbody>
                    </table>
                </div>
            </div>`;
    } catch (e) {
        app.innerHTML = `<div class="page">${errorBlock(e.message)}</div>`;
    }
}

/* ---------------- autocomplete ---------------- */

function autocomplete(input, onPick) {
    let menu;
    let selected = null;

    const close = () => {
        if (menu) {
            menu.remove();
            menu = null;
        }
    };

    const pick = (item) => {
        selected = item;
        input.value = item.name;
        close();
        if (onPick) onPick(item);
    };

    const search = debounce(async (q) => {
        close();
        if (!q.trim()) return;
        let rows;
        try {
            rows = await api(`/api/autocomplete?q=${encodeURIComponent(q)}`);
        } catch {
            return;
        }
        if (!rows.length) return;
        menu = document.createElement("div");
        menu.className = "ac-menu";
        rows.forEach(r => {
            const item = document.createElement("div");
            item.className = "ac-item";
            item.innerHTML = `<span class="ac-item-name">${esc(r.name)}</span>${badge(r.label)}`;
            item.addEventListener("click", () => pick(r));
            menu.appendChild(item);
        });
        input.parentElement.appendChild(menu);
    }, 220);

    input.addEventListener("input", () => {
        selected = null;
        search(input.value);
    });
    input.addEventListener("focus", () => {
        if (input.value && !menu) search(input.value);
    });
    input.addEventListener("blur", () => setTimeout(close, 160));
    input.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && selected) {
            e.preventDefault();
            pick(selected);
        }
        if (e.key === "Escape") close();
    });

    return {
        getSelected: () => selected,
        close
    };
}

/* ---------------- path finder ---------------- */

function graphRenderer(container, nodes, edges) {
    const width = container.clientWidth || 900;
    const height = 460;
    const svgNS = "http://www.w3.org/2000/svg";

    const positions = new Map();
    nodes.forEach((n, i) => {
        positions.set(n.id, {
            x: width / 2 + (Math.random() - 0.5) * width * 0.5,
            y: height / 2 + (Math.random() - 0.5) * height * 0.5,
            vx: 0,
            vy: 0
        });
    });

    const adjacency = new Map();
    edges.forEach(e => {
        if (!adjacency.has(e.from)) adjacency.set(e.from, []);
        if (!adjacency.has(e.to)) adjacency.set(e.to, []);
        adjacency.get(e.from).push(e.to);
        adjacency.get(e.to).push(e.from);
    });

    for (let iter = 0; iter < 320; iter++) {
        nodes.forEach(n => {
            const p = positions.get(n.id);
            p.vx = 0;
            p.vy = 0;
        });
        for (let i = 0; i < nodes.length; i++) {
            for (let j = i + 1; j < nodes.length; j++) {
                const a = positions.get(nodes[i].id);
                const b = positions.get(nodes[j].id);
                const dx = a.x - b.x;
                const dy = a.y - b.y;
                const dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1);
                const force = 900 / (dist * dist);
                const fx = (dx / dist) * force;
                const fy = (dy / dist) * force;
                a.vx += fx; a.vy += fy;
                b.vx -= fx; b.vy -= fy;
            }
        }
        edges.forEach(e => {
            const a = positions.get(e.from);
            const b = positions.get(e.to);
            if (!a || !b) return;
            const dx = b.x - a.x;
            const dy = b.y - a.y;
            const dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1);
            const force = (dist - 170) * 0.02;
            const fx = (dx / dist) * force;
            const fy = (dy / dist) * force;
            a.vx += fx; a.vy += fy;
            b.vx -= fx; b.vy -= fy;
        });
        nodes.forEach(n => {
            const p = positions.get(n.id);
            p.vx += (width / 2 - p.x) * 0.005;
            p.vy += (height / 2 - p.y) * 0.005;
            p.x += p.vx * 0.06;
            p.y += p.vy * 0.06;
            p.x = Math.max(30, Math.min(width - 30, p.x));
            p.y = Math.max(30, Math.min(height - 30, p.y));
        });
    }

    const svg = document.createElementNS(svgNS, "svg");
    svg.setAttribute("viewBox", `0 0 ${width} ${height}`);
    svg.classList.add("graph-svg");

    const edgeMap = new Map();
    edges.forEach(e => {
        const key = [e.from, e.to].sort().join("|");
        if (!edgeMap.has(key)) edgeMap.set(key, []);
        edgeMap.get(key).push(e);
    });

    edgeMap.forEach((edgeList, key) => {
        const [from, to] = key.split("|");
        const a = positions.get(from);
        const b = positions.get(to);
        if (!a || !b) return;
        const line = document.createElementNS(svgNS, "line");
        line.setAttribute("x1", a.x);
        line.setAttribute("y1", a.y);
        line.setAttribute("x2", b.x);
        line.setAttribute("y2", b.y);
        line.classList.add("edge-line");
        svg.appendChild(line);

        const midX = (a.x + b.x) / 2;
        const midY = (a.y + b.y) / 2 - 8;
        const label = document.createElementNS(svgNS, "text");
        label.setAttribute("x", midX);
        label.setAttribute("y", midY);
        label.setAttribute("text-anchor", "middle");
        label.classList.add("edge-label");
        label.textContent = edgeList.map(e => e.type).join(" / ");
        svg.appendChild(label);
    });

    nodes.forEach(n => {
        const p = positions.get(n.id);
        const color = LABEL_COLORS[n.label] || "#8b91a7";
        const circle = document.createElementNS(svgNS, "circle");
        circle.setAttribute("cx", p.x);
        circle.setAttribute("cy", p.y);
        circle.setAttribute("r", 16);
        circle.setAttribute("fill", color);
        circle.classList.add("node-circle");
        circle.addEventListener("click", () => {
            const target = n.label === "Company" ? `#/company/${encodeURIComponent(n.name)}`
                : n.label === "Person" ? `#/person/${encodeURIComponent(n.name)}`
                : n.label === "Investor" ? `#/investor/${encodeURIComponent(n.name)}` : null;
            if (target) window.location.hash = target;
        });
        svg.appendChild(circle);

        const text = document.createElementNS(svgNS, "text");
        text.setAttribute("x", p.x);
        text.setAttribute("y", p.y + 4);
        text.setAttribute("text-anchor", "middle");
        text.classList.add("node-label");
        text.textContent = n.name.length > 22 ? n.name.slice(0, 21) + "…" : n.name;
        svg.appendChild(text);
    });

    container.innerHTML = "";
    container.appendChild(svg);
}

async function renderPath(app, segments, params) {
    app.innerHTML = `
        <div class="page">
            <div class="page-head">
                <h1>Path Finder</h1>
                <p>Pick any two entities — a founder, a company, an investor — and VentureGraph finds the shortest connection path between them through the graph.</p>
            </div>
            <div class="card" style="margin-bottom:20px;">
                <div class="insight-controls">
                    <div class="ac" style="flex:1;max-width:340px;"><input class="input" id="path-from" placeholder="From… e.g. Patrick Collison"></div>
                    <span style="color:var(--text-faint);">↔</span>
                    <div class="ac" style="flex:1;max-width:340px;"><input class="input" id="path-to" placeholder="To… e.g. SpaceX"></div>
                    <button class="btn" id="path-go">Find path</button>
                </div>
                <p style="font-size:12.5px;color:var(--text-faint);">Tip: try “Patrick Collison” → “SpaceX”, or “Elad Gil” → “Airbnb”. Paths are searched up to 6 hops.</p>
            </div>
            <div id="path-result"></div>
        </div>`;

    const fromInput = $("#path-from", app);
    const toInput = $("#path-to", app);
    const acFrom = autocomplete(fromInput);
    const acTo = autocomplete(toInput);

    const go = async () => {
        const from = fromInput.value.trim();
        const to = toInput.value.trim();
        const result = $("#path-result", app);
        if (from && to) {
            const next = new URLSearchParams({ from, to });
            if (window.location.hash !== `#/path?${next.toString()}`) {
                history.replaceState(null, "", `#/path?${next.toString()}`);
            }
        }
        result.innerHTML = skeletonLines(4);
        try {
            const path = await api(`/api/paths?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
            const nodes = path.nodes.map(n => ({ id: n.name, name: n.name, label: n.label }));
            const edges = path.edges.map(e => ({ from: e.from, to: e.to, type: e.type }));
            const steps = path.edges.map((e, i) => `
                <div class="row-item">
                    <span class="row-main">
                        <span class="badge stage">Hop ${i + 1}</span>
                        <span class="row-title">${esc(e.from)}</span>
                        <span style="color:var(--text-faint);">—${esc(e.type)}→</span>
                        <span class="row-title">${esc(e.to)}</span>
                    </span>
                </div>`).join("");
            result.innerHTML = `
                <div class="graph-wrap">
                    <div class="graph-toolbar">
                        <strong>${esc(from)} ↔ ${esc(to)} · ${path.edges.length} hop${path.edges.length === 1 ? "" : "s"}</strong>
                        <div class="graph-legend">
                            ${Object.entries(LABEL_COLORS).map(([label, color]) => `<span><i style="background:${color}"></i>${label}</span>`).join("")}
                        </div>
                    </div>
                    <div id="path-graph"></div>
                </div>
                ${steps ? `<h2 class="section-title" style="margin-top:22px;">The path, step by step</h2><div class="card row-list">${steps}</div>` : ""}`;
            graphRenderer($("#path-graph", result), nodes, edges);
        } catch (e) {
            result.innerHTML = errorBlock(e.message);
        }
    };

    $("#path-go", app).addEventListener("click", go);
    [fromInput, toInput].forEach(input => input.addEventListener("keydown", (e) => {
        if (e.key === "Enter") go();
    }));

    const fromParam = params.get("from");
    const toParam = params.get("to");
    if (fromParam && toParam) {
        fromInput.value = fromParam;
        toInput.value = toParam;
        go();
    }
}

/* ---------------- insights ---------------- */

function insightShell(title, why, controls, bodyId, body) {
    return `<section class="card insight">
        <div class="insight-head">
            <div>
                <h3>${esc(title)}</h3>
                <p class="insight-why">${why}</p>
            </div>
            ${badge("cypher")}
        </div>
        ${controls || ""}
        <div id="${bodyId}">${body}</div>
    </section>`;
}

function renderInsights(app) {
    app.innerHTML = `
        <div class="page">
            <div class="page-head">
                <h1>Insights</h1>
                <p>Queries that show why this data lives in a graph: multi-hop traversals and relationship patterns that a relational database would find awkward.</p>
            </div>
            <div id="insights-body">${skeletonCards(4)}</div>
        </div>`;

    const body = $("#insights-body", app);
    body.innerHTML = [
        insightShell("Founders with a big-tech pedigree",
            "A 3-hop traversal: every founder who worked at a big company before starting their own, plus everyone who backed them. In SQL this needs several self-joins over employment and investment tables.",
            "",
            "insight-bigtech", skeletonLines(4)),

        insightShell("Alumni → companies → investors",
            "Pick a university and traverse GRADUATED_FROM → FOUNDED → INVESTED_IN: three hops that find which investors are betting on that university's founders.",
            `<div class="insight-controls"><select class="input" id="insight-univ"><option value="">Loading universities…</option></select><button class="btn secondary" id="insight-univ-go">Run</button></div>`,
            "insight-alumni", emptyState("🎓", "Pick a university", "Choose a university above to see who invests in its founders.")),

        insightShell("Common investors between two companies",
            "The set-intersection query: which investors back both companies? A relational version needs an intersection of two joined subqueries.",
            `<div class="insight-controls"><div class="ac"><input class="input" id="insight-ca" placeholder="Company A…"></div><div class="ac"><input class="input" id="insight-cb" placeholder="Company B…"></div><button class="btn secondary" id="insight-common-go">Compare</button></div>`,
            "insight-common", emptyState("🏦", "Compare two companies", "Pick two companies to find shared investors.")),

        insightShell("Co-investment network",
            "Pairs of investors who have backed the same companies, ranked. This is a common-neighbour pattern — a self-join nightmare in SQL.",
            "",
            "insight-coinvest", skeletonLines(4)),

        insightShell("Your 3-hop ecosystem reach",
            "Pick a person and find every company within three steps of them — through founding, employment and investment relationships. A variable-length traversal that relational databases cannot express without recursive CTEs.",
            `<div class="insight-controls"><div class="ac"><input class="input" id="insight-person" placeholder="Person… e.g. Elad Gil"></div><button class="btn secondary" id="insight-reach-go">Explore</button></div>`,
            "insight-reach", emptyState("🕸️", "Pick a person", "Choose a person to map their multi-hop network."))
    ].join("");

    const table = (rows, cols, emptyMsg) => {
        if (!rows.length) return emptyState("📭", "No results", emptyMsg);
        return `<div class="table-wrap"><table><thead><tr>${cols.map(c => `<th>${esc(c.label)}</th>`).join("")}</tr></thead>
            <tbody>${rows.map(r => `<tr>${cols.map(c => `<td>${c.render ? c.render(r) : esc(r[c.key] ?? "—")}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
    };

    withState($("#insight-bigtech", body), async () => {
        const rows = await api("/api/insights/big-tech-founders");
        return table(rows, [
            { label: "Founder", key: "founder", render: r => `<a href="#/person/${encodeURIComponent(r.founder)}">${esc(r.founder)}</a>` },
            { label: "Title", key: "title" },
            { label: "Worked at", key: "almaMater", render: r => `<a href="#/company/${encodeURIComponent(r.almaMater)}">${esc(r.almaMater)}</a>` },
            { label: "Founded", key: "startup", render: r => `<a href="#/company/${encodeURIComponent(r.startup)}">${esc(r.startup)}</a>` },
            { label: "Backers", key: "backers", render: r => (r.backers || []).map(b => esc(b)).join(", ") }
        ], "No founders with recorded big-tech work history yet.");
    });

    withState($("#insight-coinvest", body), async () => {
        const rows = await api("/api/insights/co-investment-network");
        return table(rows, [
            { label: "Investor A", key: "investorA", render: r => `<a href="#/investor/${encodeURIComponent(r.investorA)}">${esc(r.investorA)}</a>` },
            { label: "Investor B", key: "investorB", render: r => `<a href="#/investor/${encodeURIComponent(r.investorB)}">${esc(r.investorB)}</a>` },
            { label: "Shared companies", key: "sharedCompanies" },
            { label: "Examples", key: "examples", render: r => (r.examples || []).map(esc).join(", ") }
        ], "No co-investment pairs found yet.");
    });

    api("/api/universities").then(list => {
        const select = $("#insight-univ", body);
        select.innerHTML = `<option value="">Select a university…</option>` + list.map(u => `<option value="${esc(u)}">${esc(u)}</option>`).join("");
    }).catch(() => {
        $("#insight-univ", body).innerHTML = `<option value="">Universities unavailable</option>`;
    });

    $("#insight-univ-go", body).addEventListener("click", async () => {
        const university = $("#insight-univ", body).value;
        const target = $("#insight-alumni", body);
        if (!university) return;
        target.innerHTML = skeletonLines(4);
        try {
            const rows = await api(`/api/insights/alumni-reach?university=${encodeURIComponent(university)}`);
            target.innerHTML = table(rows, [
                { label: "Investor", key: "investor", render: r => `<a href="#/investor/${encodeURIComponent(r.investor)}">${esc(r.investor)}</a>` },
                { label: "Companies", key: "companies" },
                { label: "Portfolio", key: "portfolio", render: r => (r.portfolio || []).map(esc).join(", ") }
            ], `No investments found from alumni of ${esc(university)}.`);
        } catch (e) {
            target.innerHTML = errorBlock(e.message);
        }
    });

    const ca = autocomplete($("#insight-ca", body));
    const cb = autocomplete($("#insight-cb", body));
    $("#insight-common-go", body).addEventListener("click", async () => {
        const companyA = $("#insight-ca", body).value.trim();
        const companyB = $("#insight-cb", body).value.trim();
        const target = $("#insight-common", body);
        if (!companyA || !companyB) return;
        target.innerHTML = skeletonLines(4);
        try {
            const rows = await api(`/api/insights/common-investors?companyA=${encodeURIComponent(companyA)}&companyB=${encodeURIComponent(companyB)}`);
            target.innerHTML = table(rows, [
                { label: "Investor", key: "investor", render: r => {
                    const kind = r.kind || "";
                    const isFirm = ["VC", "Accelerator", "Hedge Fund", "Strategic", "Corporate"].includes(kind);
                    return isFirm
                        ? `<a href="#/investor/${encodeURIComponent(r.investor)}">${esc(r.investor)}</a>`
                        : `<a href="#/person/${encodeURIComponent(r.investor)}">${esc(r.investor)}</a>`;
                } },
                { label: "Kind", key: "kind", render: r => badge(r.kind || "angel") }
            ], `No shared investors between “${esc(companyA)}” and “${esc(companyB)}”.`);
        } catch (e) {
            target.innerHTML = errorBlock(e.message);
        }
    });

    const personAc = autocomplete($("#insight-person", body));
    $("#insight-reach-go", body).addEventListener("click", async () => {
        const person = $("#insight-person", body).value.trim();
        const target = $("#insight-reach", body);
        if (!person) return;
        target.innerHTML = skeletonLines(4);
        try {
            const rows = await api(`/api/insights/network-reach?person=${encodeURIComponent(person)}`);
            target.innerHTML = table(rows, [
                { label: "Company", key: "name", render: r => `<a href="#/company/${encodeURIComponent(r.name)}">${esc(r.name)}</a>` },
                { label: "Stage", key: "stage" },
                { label: "Headquarters", key: "headquarters" },
                { label: "Founded", key: "foundedYear" },
                { label: "Paths", key: "paths" }
            ], `No companies found within 3 hops of “${esc(person)}”.`);
        } catch (e) {
            target.innerHTML = errorBlock(e.message);
        }
    });
}

/* ---------------- init ---------------- */

async function checkHealth() {
    try {
        const health = await api("/api/health");
        if (health.status === "ok") {
            hideBanner();
        } else {
            showBanner("warn", health.message);
        }
    } catch {
        showBanner("error", "Cannot reach the application server.");
    }
}

checkHealth();
router.start();
