package com.example.dependencyupdater;

import com.example.dependencyupdater.settings.DependencyUpdaterSettingsState;
import com.example.dependencyupdater.settings.Repository;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MavenCentralApiClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CompletableFuture<List<String>> fetchAvailableVersions(String groupId, String artifactId) {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        List<Repository> repositories = state.repositories;

        if (repositories == null || repositories.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String groupPath = groupId.replace(".", "/");

        List<CompletableFuture<List<String>>> futures = repositories.stream().map(repo -> {
            String baseUrl = repo.getUrl();
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            String url = String.format("%s%s/%s/maven-metadata.xml", baseUrl, groupPath, artifactId);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET();

            addAuthHeader(requestBuilder, repo);

            return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return parseVersions(response.body());
                        }
                        return new ArrayList<String>();
                    })
                    .exceptionally(ex -> new ArrayList<>());
        }).collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Set<String> allVersions = new HashSet<>();
                    for (CompletableFuture<List<String>> future : futures) {
                        allVersions.addAll(future.join());
                    }
                    return new ArrayList<>(allVersions);
                });
    }

    private void addAuthHeader(HttpRequest.Builder requestBuilder, Repository repo) {
        if (repo.getAuthType() == Repository.AuthType.NONE) {
            return;
        }

        CredentialAttributes attributes = new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("DependencyUpdater", repo.getId())
        );
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        String secret = credentials != null ? credentials.getPasswordAsString() : null;

        if (secret == null || secret.isEmpty()) {
            return;
        }

        if (repo.getAuthType() == Repository.AuthType.BASIC) {
            String username = repo.getUsername() != null ? repo.getUsername() : "";
            String authString = username + ":" + secret;
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
            requestBuilder.header("Authorization", "Basic " + encodedAuth);
        } else if (repo.getAuthType() == Repository.AuthType.BEARER) {
            requestBuilder.header("Authorization", "Bearer " + secret);
        }
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
