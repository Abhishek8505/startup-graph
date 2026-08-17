package com.startupgraph.seed;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class SeedDataIntegrityTest {

    private static Map<String, Object> data;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = new ClassPathResource("seed/startup-graph.json").getInputStream()) {
            data = new ObjectMapper().readValue(in, new TypeReference<>() {
            });
        }
    }

    private Set<String> names(String key) {
        Set<String> set = new HashSet<>();
        for (Object item : (List<?>) data.get(key)) {
            String name = item instanceof Map<?, ?> m ? (String) m.get("name") : (String) item;
            assertTrue(set.add(name), "duplicate name in " + key + ": " + name);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(String key) {
        return (List<Map<String, Object>>) (List<?>) data.get(key);
    }

    @Test
    void entityNamesAreUnique() {
        names("companies");
        names("people");
        names("investors");
        names("universities");
        names("industries");
    }

    @Test
    void relationshipEndpointsExist() {
        Set<String> companies = names("companies");
        Set<String> people = names("people");
        Set<String> investors = names("investors");
        Set<String> universities = names("universities");
        Set<String> industries = names("industries");

        assertEndpoints(rows("founded"), people, companies, "FOUNDED");
        assertEndpoints(rows("workedAt"), people, companies, "WORKED_AT");
        assertEndpoints(rows("graduatedFrom"), people, universities, "GRADUATED_FROM");
        assertEndpoints(rows("operatesIn"), companies, industries, "OPERATES_IN");
        assertEndpoints(rows("partnersWith"), companies, companies, "PARTNERS_WITH");
        assertEndpoints(rows("knows"), people, people, "KNOWS");

        Set<String> everyone = new HashSet<>(people);
        everyone.addAll(investors);
        assertEndpoints(rows("investedIn"), everyone, companies, "INVESTED_IN");
    }

    private void assertEndpoints(List<Map<String, Object>> rels, Set<String> allowedFrom, Set<String> allowedTo, String type) {
        for (Map<String, Object> row : rels) {
            String from = (String) row.get("from");
            String to = (String) row.get("to");
            assertTrue(allowedFrom.contains(from), type + " from unknown node: " + from);
            assertTrue(allowedTo.contains(to), type + " to unknown node: " + to);
        }
    }

    @Test
    void investorsCoverFirmsAndAngels() {
        Set<String> investorNames = names("investors");
        Set<String> investedFrom = new HashSet<>();
        rows("investedIn").forEach(row -> investedFrom.add((String) row.get("from")));
        for (String name : investedFrom) {
            boolean isFirm = investorNames.contains(name);
            boolean isPerson = names("people").contains(name);
            assertTrue(isFirm || isPerson, "INVESTED_IN from entity that is neither investor nor person: " + name);
        }
    }
}
