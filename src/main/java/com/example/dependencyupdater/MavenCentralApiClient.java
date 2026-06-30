package com.example.dependencyupdater;

import com.example.dependencyupdater.settings.DependencyUpdaterSettingsState;
import com.example.dependencyupdater.settings.Repository;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.util.io.HttpRequests;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MavenCentralApiClient {

    public CompletableFuture<List<String>> fetchAvailableVersions(String groupId, String artifactId) {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        List<Repository> repositories = state.repositories;

        if (repositories == null || repositories.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String groupPath = groupId.replace(".", "/");
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();

        for (Repository repo : repositories) {
            String baseUrl = repo.getUrl();
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            if (repo.isHtmlListing()) {
                String releaseUrl = String.format("%slibs-release/%s/%s/", baseUrl, groupPath, artifactId);
                String snapshotUrl = String.format("%slibs-snapshot/%s/%s/", baseUrl, groupPath, artifactId);
                futures.add(createFetchFuture(repo, releaseUrl, true));
                futures.add(createFetchFuture(repo, snapshotUrl, true));
            } else {
                String xmlUrl = String.format("%s%s/%s/maven-metadata.xml", baseUrl, groupPath, artifactId);
                futures.add(createFetchFuture(repo, xmlUrl, false));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Set<String> allVersions = new HashSet<>();
                    for (CompletableFuture<List<String>> future : futures) {
                        allVersions.addAll(future.join());
                    }
                    return new ArrayList<>(allVersions);
                });
    }

    private CompletableFuture<List<String>> createFetchFuture(Repository repo, String url, boolean isHtml) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return HttpRequests.request(url)
                        .tuner(connection -> {
                            if (repo.getAuthType() == Repository.AuthType.NONE) return;
                            
                            CredentialAttributes attributes = new CredentialAttributes(
                                    CredentialAttributesKt.generateServiceName("DependencyUpdater", repo.getId())
                            );
                            Credentials credentials = PasswordSafe.getInstance().get(attributes);
                            String secret = credentials != null ? credentials.getPasswordAsString() : null;
                            if (secret == null || secret.isEmpty()) return;

                            if (repo.getAuthType() == Repository.AuthType.BASIC) {
                                String username = repo.getUsername() != null ? repo.getUsername() : "";
                                String authString = username + ":" + secret;
                                String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
                                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                            } else if (repo.getAuthType() == Repository.AuthType.BEARER) {
                                connection.setRequestProperty("Authorization", "Bearer " + secret);
                            }
                        })
                        .readString();
            } catch (Exception e) {
                return null;
            }
        }).thenApply(body -> {
            if (body != null) {
                if (isHtml) {
                    return parseHtmlVersions(body);
                } else {
                    return parseVersions(body);
                }
            }
            return new ArrayList<String>();
        }).exceptionally(ex -> new ArrayList<>());
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

    List<String> parseHtmlVersions(String htmlBody) {
        List<String> versions = new ArrayList<>();
        if (htmlBody == null || htmlBody.isEmpty()) return versions;
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("href=\"([0-9][^\"]+)/\"");
            java.util.regex.Matcher matcher = pattern.matcher(htmlBody);
            while (matcher.find()) {
                versions.add(matcher.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versions;
    }
}
