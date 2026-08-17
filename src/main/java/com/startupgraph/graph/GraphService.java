package com.startupgraph.graph;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.startupgraph.dto.Dto;

@Service
public class GraphService {

    private final GraphRepository repository;

    public GraphService(GraphRepository repository) {
        this.repository = repository;
    }

    public Dto.Health health() {
        if (!repository.isConfigured()) {
            return new Dto.Health("not_configured",
                    "CognoDB is not configured. Set COGNODB_URI, COGNODB_USER and COGNODB_PASSWORD.", null, null);
        }
        try {
            return repository.health();
        } catch (DbUnavailableException e) {
            return new Dto.Health("degraded", e.getMessage(), null, null);
        }
    }

    public Dto.Stats stats() {
        return repository.stats();
    }

    public List<Dto.CompanySummary> searchCompanies(String q, String industry) {
        return repository.searchCompanies(q, industry);
    }

    public Dto.CompanyDetail companyDetail(String name) {
        return repository.companyDetail(name)
                .orElseThrow(() -> new NotFoundException("Company not found: " + name));
    }

    public List<Dto.PersonSummary> searchPeople(String q) {
        return repository.searchPeople(q);
    }

    public Dto.PersonDetail personDetail(String name) {
        return repository.personDetail(name)
                .orElseThrow(() -> new NotFoundException("Person not found: " + name));
    }

    public List<Dto.InvestorSummary> searchInvestors(String q) {
        return repository.searchInvestors(q);
    }

    public Dto.InvestorDetail investorDetail(String name) {
        return repository.investorDetail(name)
                .orElseThrow(() -> new NotFoundException("Investor not found: " + name));
    }

    public List<String> universities() {
        return repository.universities();
    }

    public List<Dto.Match> autocomplete(String q) {
        return repository.autocomplete(q == null ? "" : q.trim());
    }

    public Dto.PathResult shortestPath(String from, String to) {
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("Both 'from' and 'to' are required.");
        }
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("'from' and 'to' must be different entities.");
        }
        return repository.shortestPath(from.trim(), to.trim())
                .orElseThrow(() -> new NotFoundException(
                        "No connection found between \"" + from + "\" and \"" + to + "\" within 6 hops."));
    }

    public List<Map<String, Object>> bigTechFounders() {
        return repository.bigTechFounders();
    }

    public List<Map<String, Object>> alumniReach(String university) {
        if (university == null || university.isBlank()) {
            throw new IllegalArgumentException("A university is required.");
        }
        return repository.alumniReach(university);
    }

    public List<Map<String, Object>> commonInvestors(String companyA, String companyB) {
        if (companyA == null || companyB == null || companyA.isBlank() || companyB.isBlank()) {
            throw new IllegalArgumentException("Both companies are required.");
        }
        if (companyA.equalsIgnoreCase(companyB)) {
            throw new IllegalArgumentException("Pick two different companies.");
        }
        return repository.commonInvestors(companyA, companyB);
    }

    public List<Map<String, Object>> coInvestmentNetwork() {
        return repository.coInvestmentNetwork();
    }

    public List<Map<String, Object>> networkReach(String person) {
        if (person == null || person.isBlank()) {
            throw new IllegalArgumentException("A person is required.");
        }
        return repository.networkReach(person);
    }
}
