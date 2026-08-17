package com.startupgraph.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.startupgraph.dto.Dto;
import com.startupgraph.graph.GraphService;

@RestController
public class HealthController {

    private final GraphService service;

    public HealthController(GraphService service) {
        this.service = service;
    }

    @GetMapping("/api/health")
    public Dto.Health health() {
        return service.health();
    }
}
