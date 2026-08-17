package com.startupgraph.graph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.ObjectProvider;

import com.startupgraph.dto.Dto;

@EnabledIfEnvironmentVariable(named = "COGNODB_URI", matches = ".+")
class GraphRepositoryIntegrationTest {

    private static GraphRepository repository;

    @BeforeAll
    static void connect() {
        String uri = System.getenv("COGNODB_URI");
        String user = System.getenv().getOrDefault("COGNODB_USER", "cognodb");
        String password = System.getenv("COGNODB_PASSWORD");
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        ObjectProvider<Driver> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(driver);
        repository = new GraphRepository(provider);
    }

    @Test
    void healthIsOk() {
        Dto.Health health = repository.health();
        assertTrue(health.nodes() > 0, "seed data should be present");
        assertTrue(health.rels() > 0);
    }

    @Test
    void companiesAndPeopleAreSearchable() {
        List<Dto.CompanySummary> companies = repository.searchCompanies("stripe", "");
        assertFalse(companies.isEmpty());
        List<Dto.PersonSummary> people = repository.searchPeople("collison");
        assertFalse(people.isEmpty());
    }

    @Test
    void multiHopQueryFindsBigTechFounders() {
        assertFalse(repository.bigTechFounders().isEmpty());
    }

    @Test
    void shortestPathFindsConnections() {
        Dto.PathResult path = repository.shortestPath("Patrick Collison", "SpaceX").orElseThrow();
        assertNotNull(path.nodes());
        assertFalse(path.edges().isEmpty());
    }

    @Test
    void coInvestmentNetworkFindsPairs() {
        assertFalse(repository.coInvestmentNetwork().isEmpty());
    }
}
