package com.startupgraph.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.startupgraph.dto.Dto;

class GraphServiceTest {

    private GraphRepository repository;
    private GraphService service;

    @BeforeEach
    void setUp() {
        repository = mock(GraphRepository.class);
        service = new GraphService(repository);
    }

    @Test
    void healthReportsNotConfiguredWhenDriverMissing() {
        when(repository.isConfigured()).thenReturn(false);
        Dto.Health health = service.health();
        assertEquals("not_configured", health.status());
    }

    @Test
    void healthReportsOkWhenConnected() {
        when(repository.isConfigured()).thenReturn(true);
        when(repository.health()).thenReturn(new Dto.Health("ok", "Connected", 100L, 200L));
        Dto.Health health = service.health();
        assertEquals("ok", health.status());
        assertEquals(100L, health.nodes());
    }

    @Test
    void healthReportsDegradedWhenDatabaseUnreachable() {
        when(repository.isConfigured()).thenReturn(true);
        when(repository.health()).thenThrow(new DbUnavailableException("boom"));
        Dto.Health health = service.health();
        assertEquals("degraded", health.status());
    }

    @Test
    void shortestPathRejectsSameEntity() {
        assertThrows(IllegalArgumentException.class, () -> service.shortestPath("Stripe", "stripe"));
    }

    @Test
    void shortestPathRejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> service.shortestPath(" ", "Stripe"));
    }

    @Test
    void commonInvestorsRejectsSameCompany() {
        assertThrows(IllegalArgumentException.class, () -> service.commonInvestors("Stripe", "STRIPE"));
    }
}
