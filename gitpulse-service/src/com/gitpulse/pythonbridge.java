package com.gitpulse;

import com.gitpulse.Algorithm.RepositoryAnalyzer;
import com.gitpulse.Algorithm.RepositoryReport;
import com.gitpulse.model.Commit;
import com.gitpulse.model.Repository;
import com.gitpulse.model.WeeklySummary;
import com.gitpulse.service.DataService;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PythonBridge {

    private static final DataService dataService = new DataService();
    private static final RepositoryAnalyzer analyzer = new RepositoryAnalyzer();

    public static void main(String[] args) throws Exception {
        // Mode 1: One-shot CLI mode -> java com.gitpulse.PythonBridge <owner> <repo> [--ai]
        if (args.length >= 2) {
            boolean includeAi = args.length >= 3 && "--ai".equalsIgnoreCase(args[2]);
            System.out.println(analyzeToJson(args[0], args[1], includeAi));
            return;
        }

        // Mode 2: Local REST Microservice mode -> http://localhost:8765/analyze?owner=X&repo=Y&ai=true
        int port = 8765;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        server.createContext("/analyze", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String owner = params.get("owner");
            String repoName = params.get("repo");
            boolean includeAi = "true".equalsIgnoreCase(params.getOrDefault("ai", "false"));

            if (owner == null || repoName == null || owner.isBlank() || repoName.isBlank()) {
                byte[] err = "{\"error\":\"Missing owner or repo query parameter\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, err.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(err); }
                return;
            }

            try {
                String json = analyzeToJson(owner, repoName, includeAi);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            } catch (Exception e) {
                String errJson = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                byte[] bytes = errJson.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            }
        });

        server.setExecutor(null);
        server.start();
        System.err.println("GitPulse PythonBridge listening on http://127.0.0.1:" + port + "/analyze");
    }

    public static String analyzeToJson(String owner, String repoName, boolean includeAi) {
        Repository repo = dataService.loadRepository(owner, repoName);
        RepositoryReport report = analyzer.analyze(repo);

        String repoSummary = includeAi ? dataService.getRepositorySummary(repo) : "";
        List<WeeklySummary> weekly = includeAi ? dataService.getWeeklySummaries(repo) : List.of();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"source\": \"git\",\n");
        sb.append("  \"owner\": \"").append(escapeJson(owner)).append("\",\n");
        sb.append("  \"repo\": \"").append(escapeJson(repoName)).append("\",\n");
        sb.append("  \"projectHealth\": \"").append(escapeJson(String.valueOf(report.getProjectHealthLabel()))).append("\",\n");
        sb.append("  \"activityTrend\": \"").append(escapeJson(String.valueOf(report.getActivityTrend()))).append("\",\n");
        sb.append("  \"dominantContributor\": \"").append(escapeJson(String.valueOf(report.getDominantContributor()))).append("\",\n");
        sb.append("  \"busDriverRisk\": ").append(report.isBusDriver()).append(",\n");
        sb.append("  \"plainEnglishSummary\": \"").append(escapeJson(report.getPlainEnglishSummary())).append("\",\n");
        sb.append("  \"aiRepoSummary\": \"").append(escapeJson(repoSummary)).append("\",\n");

        // Serialize commits for Python meaningful-commit filtering
        sb.append("  \"commits\": [\n");
        List<Commit> commits = repo.getCommits();
        if (commits != null) {
            for (int i = 0; i < commits.size(); i++) {
                Commit c = commits.get(i);
                sb.append("    {")
                  .append("\"author\":\"").append(escapeJson(c.getAuthorName())).append("\",")
                  .append("\"message\":\"").append(escapeJson(c.getMessage())).append("\",")
                  .append("\"date\":\"").append(escapeJson(String.valueOf(c.getDate()))).append("\"")
                  .append("}");
                if (i < commits.size() - 1) sb.append(",");
                sb.append("\n");
            }
        }
        sb.append("  ],\n");

        // Serialize AI weekly summaries
        sb.append("  \"weeklySummaries\": [\n");
        for (int i = 0; i < weekly.size(); i++) {
            WeeklySummary ws = weekly.get(i);
            sb.append("    {")
              .append("\"week\":\"").append(escapeJson(String.valueOf(ws.getWeekStart()))).append("\",")
              .append("\"totalCommits\":").append(ws.getTotalCommits()).append(",")
              .append("\"topAuthor\":\"").append(escapeJson(ws.getTopAuthor())).append("\",")
              .append("\"summary\":\"").append(escapeJson(ws.getSummaryText())).append("\"")
              .append("}");
            if (i < weekly.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(
                    URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}