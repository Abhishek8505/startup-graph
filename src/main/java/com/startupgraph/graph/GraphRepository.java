package com.startupgraph.graph;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import com.startupgraph.dto.Dto;

@Repository
public class GraphRepository {

    private static final String STATS_NODES =
            "MATCH (n) UNWIND labels(n) AS label "
            + "RETURN label AS name, count(*) AS value ORDER BY value DESC";

    private static final String STATS_RELS =
            "MATCH ()-[r]->() RETURN type(r) AS name, count(*) AS value ORDER BY value DESC";

    private static final String STATS_INDUSTRIES =
            "MATCH (c:Company)-[:OPERATES_IN]->(i:Industry) "
            + "RETURN i.name AS name, count(c) AS value ORDER BY value DESC LIMIT 6";

    private static final String STATS_INVESTORS =
            "MATCH (i:Investor)-[:INVESTED_IN]->(c:Company) "
            + "RETURN i.name AS name, count(c) AS value ORDER BY value DESC LIMIT 6";

    private static final String STATS_NEWEST =
            "MATCH (c:Company) RETURN c.name AS name, c.foundedYear AS foundedYear, c.stage AS stage, "
            + "c.headquarters AS headquarters ORDER BY c.foundedYear DESC LIMIT 5";

    private static final String SEARCH_COMPANIES =
            "MATCH (c:Company) OPTIONAL MATCH (c)-[:OPERATES_IN]->(ind:Industry) "
            + "WITH c, collect(DISTINCT ind.name) AS industries "
            + "WHERE ($q = '' OR toLower(c.name) CONTAINS toLower($q) "
            + "OR toLower(coalesce(c.description, '')) CONTAINS toLower($q)) "
            + "AND ($industry = '' OR $industry IN industries) "
            + "RETURN c.name AS name, c.foundedYear AS foundedYear, c.stage AS stage, "
            + "c.headquarters AS headquarters, c.description AS description, industries "
            + "ORDER BY c.name LIMIT 50";

    private static final String COMPANY_DETAIL =
            "MATCH (c:Company {name: $name}) "
            + "OPTIONAL MATCH (c)<-[frel:FOUNDED]-(f:Person) "
            + "OPTIONAL MATCH (c)<-[irel:INVESTED_IN]-(inv) "
            + "OPTIONAL MATCH (c)-[:OPERATES_IN]->(ind:Industry) "
            + "OPTIONAL MATCH (c)-[:PARTNERS_WITH]->(part:Company) "
            + "OPTIONAL MATCH (alum:Person)-[w:WORKED_AT]->(c) "
            + "RETURN c, "
            + "collect(DISTINCT {person: f.name, role: frel.role, since: frel.since}) AS founders, "
            + "collect(DISTINCT {investor: inv.name, round: irel.round, year: irel.year, amountM: irel.amountM, kind: CASE WHEN inv:Person THEN 'Person' ELSE 'Investor' END}) AS investors, "
            + "collect(DISTINCT ind.name) AS industries, "
            + "collect(DISTINCT part.name) AS partners, "
            + "collect(DISTINCT {person: alum.name, role: w.role, start: w.start, end: w.end}) AS alumni";

    private static final String SEARCH_PEOPLE =
            "MATCH (p:Person) "
            + "WHERE $q = '' OR toLower(p.name) CONTAINS toLower($q) "
            + "OR toLower(coalesce(p.title, '')) CONTAINS toLower($q) "
            + "OR toLower(coalesce(p.location, '')) CONTAINS toLower($q) "
            + "RETURN p.name AS name, p.title AS title, p.location AS location, p.bio AS bio "
            + "ORDER BY p.name LIMIT 50";

    private static final String PERSON_DETAIL =
            "MATCH (p:Person {name: $name}) "
            + "OPTIONAL MATCH (p)-[f:FOUNDED]->(c:Company) "
            + "OPTIONAL MATCH (p)-[w:WORKED_AT]->(wc:Company) "
            + "OPTIONAL MATCH (p)-[i:INVESTED_IN]->(ic:Company) "
            + "OPTIONAL MATCH (p)-[g:GRADUATED_FROM]->(u:University) "
            + "OPTIONAL MATCH (p)-[:KNOWS]->(k:Person) "
            + "RETURN p, "
            + "collect(DISTINCT {company: c.name, role: f.role, since: f.since}) AS founded, "
            + "collect(DISTINCT {company: wc.name, role: w.role, start: w.start, end: w.end}) AS workedAt, "
            + "collect(DISTINCT {company: ic.name, round: i.round, year: i.year, amountM: i.amountM}) AS investedIn, "
            + "collect(DISTINCT {university: u.name, degree: g.degree, year: g.year}) AS education, "
            + "collect(DISTINCT k.name) AS knows";

    private static final String SEARCH_INVESTORS =
            "MATCH (i:Investor) "
            + "WHERE $q = '' OR toLower(i.name) CONTAINS toLower($q) "
            + "OR toLower(coalesce(i.kind, '')) CONTAINS toLower($q) "
            + "RETURN i.name AS name, i.kind AS kind, i.location AS location, i.description AS description "
            + "ORDER BY i.name LIMIT 50";

    private static final String INVESTOR_DETAIL =
            "MATCH (i:Investor {name: $name}) "
            + "OPTIONAL MATCH (i)-[inv:INVESTED_IN]->(c:Company) "
            + "RETURN i, collect(DISTINCT {company: c.name, round: inv.round, year: inv.year, amountM: inv.amountM}) AS portfolio";

    private static final String UNIVERSITIES =
            "MATCH (u:University) RETURN u.name AS name ORDER BY u.name";

    private static final String AUTOCOMPLETE =
            "MATCH (n) WHERE (n:Person OR n:Company OR n:Investor) AND toLower(n.name) CONTAINS toLower($q) "
            + "RETURN n.name AS name, labels(n)[0] AS label "
            + "ORDER BY CASE WHEN toLower(n.name) STARTS WITH toLower($q) THEN 0 ELSE 1 END, n.name "
            + "LIMIT 12";

    private static final String SHORTEST_PATH =
            "MATCH (a {name: $from}), (b {name: $to}) "
            + "MATCH p = shortestPath((a)-[*1..6]-(b)) "
            + "RETURN [n IN nodes(p) | {name: n.name, label: labels(n)[0]}] AS nodes, "
            + "[r IN relationships(p) | {type: type(r), from: startNode(r).name, to: endNode(r).name}] AS edges "
            + "LIMIT 1";

    private static final String BIG_TECH_FOUNDERS =
            "MATCH (p:Person)-[:WORKED_AT]->(big:Company) "
            + "WITH p, big MATCH (p)-[:FOUNDED]->(startup:Company) "
            + "MATCH (inv)-[:INVESTED_IN]->(startup) "
            + "RETURN p.name AS founder, p.title AS title, big.name AS almaMater, startup.name AS startup, "
            + "collect(DISTINCT inv.name) AS backers "
            + "ORDER BY startup LIMIT 50";

    private static final String ALUMNI_REACH =
            "MATCH (u:University {name: $university})<-[:GRADUATED_FROM]-(p:Person)-[:FOUNDED]->(c:Company)<-[:INVESTED_IN]-(inv) "
            + "RETURN inv.name AS investor, count(DISTINCT c) AS companies, collect(DISTINCT c.name) AS portfolio "
            + "ORDER BY companies DESC LIMIT 20";

    private static final String COMMON_INVESTORS =
            "MATCH (a:Company {name: $companyA})<-[:INVESTED_IN]-(i)-[:INVESTED_IN]->(b:Company {name: $companyB}) "
            + "RETURN i.name AS investor, i.kind AS kind ORDER BY investor";

    private static final String CO_INVESTMENT_NETWORK =
            "MATCH (i1:Investor)-[:INVESTED_IN]->(c:Company)<-[:INVESTED_IN]-(i2:Investor) "
            + "WHERE i1.name < i2.name "
            + "RETURN i1.name AS investorA, i2.name AS investorB, count(DISTINCT c) AS sharedCompanies, "
            + "collect(DISTINCT c.name)[0..4] AS examples "
            + "ORDER BY sharedCompanies DESC LIMIT 25";

    private static final String NETWORK_REACH =
            "MATCH (me:Person {name: $person}) "
            + "MATCH (me)-[:FOUNDED|WORKED_AT|INVESTED_IN*1..3]-(c:Company) "
            + "WITH c, count(*) AS paths "
            + "RETURN c.name AS name, c.stage AS stage, c.headquarters AS headquarters, "
            + "c.foundedYear AS foundedYear, paths "
            + "ORDER BY paths DESC, name LIMIT 40";

    private final ObjectProvider<Driver> driverProvider;

    public GraphRepository(ObjectProvider<Driver> driverProvider) {
        this.driverProvider = driverProvider;
    }

    public boolean isConfigured() {
        return driverProvider.getIfAvailable() != null;
    }

    public Dto.Health health() {
        Driver driver = requireDriver();
        try (Session session = driver.session()) {
            long nodes = session.run("MATCH (n) RETURN count(n) AS c").single().get("c").asLong();
            long rels = session.run("MATCH ()-[r]->() RETURN count(r) AS c").single().get("c").asLong();
            return new Dto.Health("ok", "Connected", nodes, rels);
        } catch (Neo4jException e) {
            throw new DbUnavailableException("Database unreachable: " + e.getMessage(), e);
        }
    }

    public Dto.Stats stats() {
        Map<String, Object> params = Map.of();
        return new Dto.Stats(
                countRows(STATS_NODES, params),
                countRows(STATS_RELS, params),
                countRows(STATS_INDUSTRIES, params),
                countRows(STATS_INVESTORS, params),
                rows(STATS_NEWEST, params));
    }

    public List<Dto.CompanySummary> searchCompanies(String q, String industry) {
        return read(tx -> {
            var result = tx.run(SEARCH_COMPANIES, Map.of("q", q == null ? "" : q, "industry", industry == null ? "" : industry));
            return result.stream().map(record -> {
                var row = ValueUtil.recordToMap(record);
                return new Dto.CompanySummary(
                        str(row, "name"), intOrNull(row, "foundedYear"), str(row, "stage"),
                        str(row, "headquarters"), str(row, "description"));
            }).toList();
        });
    }

    public Optional<Dto.CompanyDetail> companyDetail(String name) {
        return read(tx -> {
            var result = tx.run(COMPANY_DETAIL, Map.of("name", name));
            if (!result.hasNext()) {
                return Optional.empty();
            }
            var row = ValueUtil.recordToMap(result.single());
            Map<String, Object> company = map(row.get("c"));
            if (company == null) {
                return Optional.empty();
            }
            Dto.CompanySummary info = new Dto.CompanySummary(
                    str(company, "name"), intOrNull(company, "foundedYear"), str(company, "stage"),
                    str(company, "headquarters"), str(company, "description"));
            List<Dto.Found> founders = mapList(row.get("founders")).stream()
                    .filter(m -> m.get("person") != null)
                    .map(m -> new Dto.Found(str(m, "person"), str(m, "role"), intOrNull(m, "since")))
                    .toList();
            List<Dto.Participation> investors = mapList(row.get("investors")).stream()
                    .filter(m -> m.get("investor") != null)
                    .map(m -> new Dto.Participation(str(m, "investor"), str(m, "kind"), str(m, "round"), intOrNull(m, "year"), dblOrNull(m, "amountM")))
                    .toList();

            List<String> industries = stringList(row.get("industries"));
            List<String> partners = stringList(row.get("partners"));
            List<Dto.Work> alumni = mapList(row.get("alumni")).stream()
                    .filter(m -> m.get("person") != null)
                    .map(m -> new Dto.Work(str(m, "person"), str(m, "role"), intOrNull(m, "start"), intOrNull(m, "end")))
                    .toList();
            return Optional.of(new Dto.CompanyDetail(info, founders, investors, industries, partners, alumni));
        });
    }

    public List<Dto.PersonSummary> searchPeople(String q) {
        return read(tx -> tx.run(SEARCH_PEOPLE, Map.of("q", q == null ? "" : q)).stream()
                .map(record -> {
                    var row = ValueUtil.recordToMap(record);
                    return new Dto.PersonSummary(
                            str(row, "name"), str(row, "title"), str(row, "location"), str(row, "bio"));
                }).toList());
    }

    public Optional<Dto.PersonDetail> personDetail(String name) {
        return read(tx -> {
            var result = tx.run(PERSON_DETAIL, Map.of("name", name));
            if (!result.hasNext()) {
                return Optional.empty();
            }
            var row = ValueUtil.recordToMap(result.single());
            Map<String, Object> person = map(row.get("p"));
            if (person == null) {
                return Optional.empty();
            }
            Dto.PersonSummary info = new Dto.PersonSummary(
                    str(person, "name"), str(person, "title"), str(person, "location"), str(person, "bio"));
            List<Dto.Founded> founded = mapList(row.get("founded")).stream()
                    .filter(m -> m.get("company") != null)
                    .map(m -> new Dto.Founded(str(m, "company"), str(m, "role"), intOrNull(m, "since")))
                    .toList();
            List<Dto.Work> workedAt = mapList(row.get("workedAt")).stream()
                    .filter(m -> m.get("company") != null)
                    .map(m -> new Dto.Work(str(m, "company"), str(m, "role"), intOrNull(m, "start"), intOrNull(m, "end")))
                    .toList();
            List<Dto.Participation> investedIn = mapList(row.get("investedIn")).stream()
                    .filter(m -> m.get("company") != null)
                    .map(m -> new Dto.Participation(str(m, "company"), "Person", str(m, "round"), intOrNull(m, "year"), dblOrNull(m, "amountM")))
                    .toList();

            List<Dto.Education> education = mapList(row.get("education")).stream()
                    .filter(m -> m.get("university") != null)
                    .map(m -> new Dto.Education(str(m, "university"), str(m, "degree"), intOrNull(m, "year")))
                    .toList();
            List<String> knows = stringList(row.get("knows"));
            return Optional.of(new Dto.PersonDetail(info, founded, workedAt, investedIn, education, knows));
        });
    }

    public List<Dto.InvestorSummary> searchInvestors(String q) {
        return read(tx -> tx.run(SEARCH_INVESTORS, Map.of("q", q == null ? "" : q)).stream()
                .map(record -> {
                    var row = ValueUtil.recordToMap(record);
                    return new Dto.InvestorSummary(
                            str(row, "name"), str(row, "kind"), str(row, "location"), str(row, "description"));
                }).toList());
    }

    public Optional<Dto.InvestorDetail> investorDetail(String name) {
        return read(tx -> {
            var result = tx.run(INVESTOR_DETAIL, Map.of("name", name));
            if (!result.hasNext()) {
                return Optional.empty();
            }
            var row = ValueUtil.recordToMap(result.single());
            Map<String, Object> investor = map(row.get("i"));
            if (investor == null) {
                return Optional.empty();
            }
            Dto.InvestorSummary info = new Dto.InvestorSummary(
                    str(investor, "name"), str(investor, "kind"), str(investor, "location"), str(investor, "description"));
            List<Dto.Participation> portfolio = mapList(row.get("portfolio")).stream()
                    .filter(m -> m.get("company") != null)
                    .map(m -> new Dto.Participation(str(m, "company"), "Investor", str(m, "round"), intOrNull(m, "year"), dblOrNull(m, "amountM")))
                    .toList();

            return Optional.of(new Dto.InvestorDetail(info, portfolio));
        });
    }

    public List<String> universities() {
        return read(tx -> tx.run(UNIVERSITIES, Map.of()).stream()
                .map(record -> record.get("name").asString()).toList());
    }

    public List<Dto.Match> autocomplete(String q) {
        return read(tx -> tx.run(AUTOCOMPLETE, Map.of("q", q)).stream()
                .map(record -> new Dto.Match(record.get("name").asString(), record.get("label").asString()))
                .toList());
    }

    public Optional<Dto.PathResult> shortestPath(String from, String to) {
        return read(tx -> {
            var result = tx.run(SHORTEST_PATH, Map.of("from", from, "to", to));
            if (!result.hasNext()) {
                return Optional.empty();
            }
            var row = ValueUtil.recordToMap(result.single());
            List<Dto.GraphNode> nodes = mapList(row.get("nodes")).stream()
                    .map(m -> new Dto.GraphNode(str(m, "name"), str(m, "name"), str(m, "label")))
                    .toList();
            List<Dto.GraphEdge> edges = mapList(row.get("edges")).stream()
                    .map(m -> new Dto.GraphEdge(str(m, "from"), str(m, "to"), str(m, "type")))
                    .toList();
            return Optional.of(new Dto.PathResult(nodes, edges));
        });
    }

    public List<Map<String, Object>> bigTechFounders() {
        return rows(BIG_TECH_FOUNDERS, Map.of());
    }

    public List<Map<String, Object>> alumniReach(String university) {
        return rows(ALUMNI_REACH, Map.of("university", university));
    }

    public List<Map<String, Object>> commonInvestors(String companyA, String companyB) {
        return rows(COMMON_INVESTORS, Map.of("companyA", companyA, "companyB", companyB));
    }

    public List<Map<String, Object>> coInvestmentNetwork() {
        return rows(CO_INVESTMENT_NETWORK, Map.of());
    }

    public List<Map<String, Object>> networkReach(String person) {
        return rows(NETWORK_REACH, Map.of("person", person));
    }

    private List<Dto.CountRow> countRows(String cypher, Map<String, Object> params) {
        return read(tx -> tx.run(cypher, params).stream().map(record -> {
            var row = ValueUtil.recordToMap(record);
            return new Dto.CountRow(str(row, "name"), longVal(row.get("value")));
        }).toList());
    }

    private List<Map<String, Object>> rows(String cypher, Map<String, Object> params) {
        return read(tx -> tx.run(cypher, params).stream()
                .map(ValueUtil::recordToMap).toList());
    }

    private <T> T read(org.neo4j.driver.TransactionCallback<T> work) {
        Driver driver = requireDriver();
        try (Session session = driver.session()) {
            return session.executeRead(work);
        } catch (Neo4jException e) {
            throw new DbUnavailableException("Database unreachable: " + e.getMessage(), e);
        }
    }

    private Driver requireDriver() {
        Driver driver = driverProvider.getIfAvailable();
        if (driver == null) {
            throw new DbUnavailableException(
                    "CognoDB is not configured. Set COGNODB_URI, COGNODB_USER and COGNODB_PASSWORD.");
        }
        return driver;
    }

    private static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private static Integer intOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.intValue() : null;
    }

    private static Double dblOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private static long longVal(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return (List<Map<String, Object>>) (List<?>) list;
    }
}
