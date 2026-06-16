package com.example.dependencyupdater;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
public class MavenCentralApiClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    public CompletableFuture<List<String>> fetchAvailableVersions(String groupId, String artifactId) {
        String groupPath = groupId.replace(".", "/");
        String url = String.format("https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml", 
                                   groupPath, artifactId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseVersions);
    }
    List<String> parseVersions(String xmlBody) {
        List<String> versions = new ArrayList<>();
        if (xmlBody == null || xmlBody.isEmpty()) return versions;
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<version>(.*?)</version>");
            java.util.regex.Matcher matcher = pattern.matcher(xmlBody);
            while (matcher.find()) {
                versions.add(matcher.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versions;
    }
}
