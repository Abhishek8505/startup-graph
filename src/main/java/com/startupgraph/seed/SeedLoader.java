package com.startupgraph.seed;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupgraph.config.DbProperties;

@Component
public class SeedLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedLoader.class);

    private static final String MERGE_INDUSTRIES =
            "UNWIND $rows AS row MERGE (n:Industry {name: row})";

    private static final String MERGE_UNIVERSITIES =
            "UNWIND $rows AS row MERGE (n:University {name: row})";

    private static final String MERGE_COMPANIES =
            "UNWIND $rows AS row MERGE (c:Company {name: row.name}) SET c.foundedYear = row.foundedYear, "
            + "c.stage = row.stage, c.headquarters = row.headquarters, c.description = row.description";

    private static final String MERGE_PEOPLE =
            "UNWIND $rows AS row MERGE (p:Person {name: row.name}) SET p.title = row.title, "
            + "p.location = row.location, p.bio = row.bio";

    private static final String MERGE_INVESTORS =
            "UNWIND $rows AS row MERGE (i {name: row.name}) SET i:Investor, i.kind = row.kind, "
            + "i.location = row.location, i.description = row.description";

    private static final String MERGE_REL =
            "UNWIND $rows AS row "
            + "MATCH (a {name: row.from}) "
            + "MATCH (b {name: row.to}) "
            + "MERGE (a)-[r:TYPE]->(b) SET r += row.props";

    private static final Map<String, String> REL_TYPES = Map.ofEntries(
            Map.entry("founded", "FOUNDED"),
            Map.entry("workedAt", "WORKED_AT"),
            Map.entry("graduatedFrom", "GRADUATED_FROM"),
            Map.entry("investedIn", "INVESTED_IN"),
            Map.entry("operatesIn", "OPERATES_IN"),
            Map.entry("partnersWith", "PARTNERS_WITH"),
            Map.entry("knows", "KNOWS"));

    private final ObjectProvider<Driver> driverProvider;
    private final DbProperties props;
    private final ObjectMapper mapper;

    public SeedLoader(ObjectProvider<Driver> driverProvider, DbProperties props, ObjectMapper mapper) {
        this.driverProvider = driverProvider;
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!props.seed()) {
            return;
        }
        Driver driver = driverProvider.getIfAvailable();
        if (driver == null) {
            log.warn("COGNODB_SEED=true but CognoDB is not configured; skipping seed.");
            return;
        }
        try (InputStream in = new ClassPathResource("seed/startup-graph.json").getInputStream()) {
            Map<String, Object> data = mapper.readValue(in, new TypeReference<>() {
            });
            try (Session session = driver.session()) {
                long count = session.executeWrite(tx -> {
                    long total = 0;
                    total += run(tx, MERGE_INDUSTRIES, data, "industries");
                    total += run(tx, MERGE_UNIVERSITIES, data, "universities");
                    total += run(tx, MERGE_COMPANIES, data, "companies");
                    total += run(tx, MERGE_PEOPLE, data, "people");
                    total += run(tx, MERGE_INVESTORS, data, "investors");
                    for (String key : REL_TYPES.keySet()) {
                        total += runRel(tx, key, data);
                    }
                    return total;
                });
                log.info("Seed complete. {} entities and relationships processed.", count);
            }
        } catch (Neo4jException e) {
            log.error("Seeding failed: {}", e.getMessage());
        }
    }

    private long run(org.neo4j.driver.TransactionContext tx, String cypher, Map<String, Object> data, String key) {
        List<Map<String, Object>> rows = castRows(data.get(key));
        if (rows.isEmpty()) {
            return 0;
        }
        tx.run(cypher, Map.of("rows", rows)).consume();
        return rows.size();
    }

    private long runRel(org.neo4j.driver.TransactionContext tx, String key, Map<String, Object> data) {
        List<Map<String, Object>> raw = castRows(data.get(key));
        if (raw.isEmpty()) {
            return 0;
        }
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Map<String, Object> row : raw) {
            Map<String, Object> props = new LinkedHashMap<>(row);
            props.remove("from");
            props.remove("to");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("from", row.get("from"));
            out.put("to", row.get("to"));
            out.put("props", props);
            rows.add(out);
        }
        tx.run(MERGE_REL.replace("TYPE", REL_TYPES.get(key)), Map.of("rows", rows)).consume();
        return rows.size();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castRows(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return (List<Map<String, Object>>) (List<?>) list;
    }
}
