package com.startupgraph.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.startupgraph.dto.Dto;
import com.startupgraph.graph.GraphService;

@RestController
@RequestMapping("/api")
public class GraphController {

    private final GraphService service;

    public GraphController(GraphService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public Dto.Stats stats() {
        return service.stats();
    }

    @GetMapping("/companies")
    public List<Dto.CompanySummary> companies(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String industry) {
        return service.searchCompanies(q, industry);
    }

    @GetMapping("/companies/{name}")
    public Dto.CompanyDetail company(@PathVariable String name) {
        return service.companyDetail(name);
    }

    @GetMapping("/people")
    public List<Dto.PersonSummary> people(@RequestParam(defaultValue = "") String q) {
        return service.searchPeople(q);
    }

    @GetMapping("/people/{name}")
    public Dto.PersonDetail person(@PathVariable String name) {
        return service.personDetail(name);
    }

    @GetMapping("/investors")
    public List<Dto.InvestorSummary> investors(@RequestParam(defaultValue = "") String q) {
        return service.searchInvestors(q);
    }

    @GetMapping("/investors/{name}")
    public Dto.InvestorDetail investor(@PathVariable String name) {
        return service.investorDetail(name);
    }

    @GetMapping("/universities")
    public List<String> universities() {
        return service.universities();
    }

    @GetMapping("/autocomplete")
    public List<Dto.Match> autocomplete(@RequestParam String q) {
        return service.autocomplete(q);
    }

    @GetMapping("/paths")
    public Dto.PathResult path(@RequestParam String from, @RequestParam String to) {
        return service.shortestPath(from, to);
    }

    @GetMapping("/insights/big-tech-founders")
    public List<Map<String, Object>> bigTechFounders() {
        return service.bigTechFounders();
    }

    @GetMapping("/insights/alumni-reach")
    public List<Map<String, Object>> alumniReach(@RequestParam String university) {
        return service.alumniReach(university);
    }

    @GetMapping("/insights/common-investors")
    public List<Map<String, Object>> commonInvestors(
            @RequestParam String companyA, @RequestParam String companyB) {
        return service.commonInvestors(companyA, companyB);
    }

    @GetMapping("/insights/co-investment-network")
    public List<Map<String, Object>> coInvestmentNetwork() {
        return service.coInvestmentNetwork();
    }

    @GetMapping("/insights/network-reach")
    public List<Map<String, Object>> networkReach(@RequestParam String person) {
        return service.networkReach(person);
    }
}
