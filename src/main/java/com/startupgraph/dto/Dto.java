package com.startupgraph.dto;

import java.util.List;
import java.util.Map;

public final class Dto {

    private Dto() {
    }

    public record Health(String status, String message, Long nodes, Long rels) {
    }

    public record CountRow(String name, long value) {
    }

    public record Stats(
            List<CountRow> nodesByLabel,
            List<CountRow> relsByType,
            List<CountRow> topIndustries,
            List<CountRow> topInvestors,
            List<Map<String, Object>> newestCompanies) {
    }

    public record CompanySummary(String name, Integer foundedYear, String stage, String headquarters, String description) {
    }

    public record PersonSummary(String name, String title, String location, String bio) {
    }

    public record InvestorSummary(String name, String kind, String location, String description) {
    }

    public record Participation(String name, String kind, String round, Integer year, Double amountM) {
    }

    public record Found(String person, String role, Integer since) {
    }

    public record Founded(String company, String role, Integer since) {
    }

    public record Work(String company, String role, Integer start, Integer end) {
    }

    public record Education(String university, String degree, Integer year) {
    }

    public record CompanyDetail(
            CompanySummary info,
            List<Found> founders,
            List<Participation> investors,
            List<String> industries,
            List<String> partners,
            List<Work> alumni) {
    }

    public record PersonDetail(
            PersonSummary info,
            List<Founded> founded,
            List<Work> workedAt,
            List<Participation> investedIn,
            List<Education> education,
            List<String> knows) {
    }

    public record InvestorDetail(InvestorSummary info, List<Participation> portfolio) {
    }

    public record Match(String name, String label) {
    }

    public record GraphNode(String id, String name, String label) {
    }

    public record GraphEdge(String from, String to, String type) {
    }

    public record PathResult(List<GraphNode> nodes, List<GraphEdge> edges) {
    }
}
