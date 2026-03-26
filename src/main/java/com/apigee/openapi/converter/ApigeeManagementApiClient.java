/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.openapi.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Client for the Apigee Management API.
 * 
 * <p>Provides methods to interact with Apigee X/hybrid Management API for downloading
 * proxy bundles, listing proxies, and retrieving revision information.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ApigeeApiConfig config = ApigeeApiConfig.builder()
 *     .organization("my-org")
 *     .serviceAccountKeyPath("/path/to/sa.json")
 *     .build();
 * 
 * ApigeeManagementApiClient client = new ApigeeManagementApiClient(config);
 * 
 * // List all proxies
 * List<String> proxies = client.listProxies();
 * 
 * // Get latest revision
 * String latestRevision = client.getLatestRevision("my-proxy");
 * 
 * // Download bundle as InputStream
 * InputStream bundleStream = client.downloadBundle("my-proxy", latestRevision);
 * }</pre>
 */
public class ApigeeManagementApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApigeeManagementApiClient.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final ApigeeApiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new Apigee Management API client.
     *
     * @param config The API configuration containing organization and credentials
     */
    public ApigeeManagementApiClient(ApigeeApiConfig config) {
        this.config = Objects.requireNonNull(config, "config is required");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lists all API proxies in the organization.
     *
     * @return List of proxy names
     * @throws ApigeeApiException if the API call fails
     */
    public List<String> listProxies() throws ApigeeApiException {
        String url = String.format("%s/organizations/%s/apis",
                config.getBaseUrl(), config.getOrganization());
        
        log.debug("Listing proxies from: {}", url);
        
        try {
            HttpRequest request = buildRequest(url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            checkResponse(response, "list proxies");
            
            JsonNode root = objectMapper.readTree(response.body());
            List<String> proxies = new ArrayList<>();
            
            if (root.has("proxies")) {
                for (JsonNode proxy : root.get("proxies")) {
                    if (proxy.has("name")) {
                        proxies.add(proxy.get("name").asText());
                    }
                }
            }
            
            log.info("Found {} proxies in organization {}", proxies.size(), config.getOrganization());
            return proxies;
            
        } catch (ApigeeApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApigeeApiException("Failed to list proxies: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all revisions for a specific proxy.
     *
     * @param proxyName Name of the proxy
     * @return List of revision numbers as strings, sorted in ascending order
     * @throws ApigeeApiException if the API call fails
     */
    public List<String> listRevisions(String proxyName) throws ApigeeApiException {
        String url = String.format("%s/organizations/%s/apis/%s/revisions",
                config.getBaseUrl(), config.getOrganization(), proxyName);
        
        log.debug("Listing revisions for proxy {} from: {}", proxyName, url);
        
        try {
            HttpRequest request = buildRequest(url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            checkResponse(response, "list revisions for " + proxyName);
            
            JsonNode root = objectMapper.readTree(response.body());
            List<String> revisions = new ArrayList<>();
            
            if (root.isArray()) {
                for (JsonNode rev : root) {
                    revisions.add(rev.asText());
                }
            }
            
            // Sort numerically
            revisions.sort((a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            });
            
            log.debug("Found {} revisions for proxy {}", revisions.size(), proxyName);
            return revisions;
            
        } catch (ApigeeApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApigeeApiException("Failed to list revisions for " + proxyName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Gets the latest (highest) revision number for a proxy.
     *
     * @param proxyName Name of the proxy
     * @return The latest revision number as a string
     * @throws ApigeeApiException if the API call fails or proxy has no revisions
     */
    public String getLatestRevision(String proxyName) throws ApigeeApiException {
        List<String> revisions = listRevisions(proxyName);
        if (revisions.isEmpty()) {
            throw new ApigeeApiException("Proxy '" + proxyName + "' has no revisions");
        }
        return revisions.get(revisions.size() - 1);
    }

    /**
     * Downloads a proxy bundle as an InputStream.
     *
     * @param proxyName Name of the proxy
     * @param revision  Revision number to download
     * @return InputStream containing the ZIP bundle
     * @throws ApigeeApiException if the download fails
     */
    public InputStream downloadBundle(String proxyName, String revision) throws ApigeeApiException {
        String url = String.format("%s/organizations/%s/apis/%s/revisions/%s?format=bundle",
                config.getBaseUrl(), config.getOrganization(), proxyName, revision);
        
        log.info("Downloading bundle for {}, revision {} from Apigee", proxyName, revision);
        
        try {
            HttpRequest request = buildRequest(url);
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() != 200) {
                String errorBody = new String(response.body());
                throw new ApigeeApiException(String.format(
                        "Failed to download bundle for %s revision %s: HTTP %d - %s",
                        proxyName, revision, response.statusCode(), errorBody));
            }
            
            byte[] bundleBytes = response.body();
            log.info("Downloaded bundle: {} bytes", bundleBytes.length);
            
            return new ByteArrayInputStream(bundleBytes);
            
        } catch (ApigeeApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApigeeApiException("Failed to download bundle for " + proxyName + 
                    " revision " + revision + ": " + e.getMessage(), e);
        }
    }

    /**
     * Downloads the latest revision of a proxy bundle as an InputStream.
     *
     * @param proxyName Name of the proxy
     * @return InputStream containing the ZIP bundle
     * @throws ApigeeApiException if the download fails
     */
    public InputStream downloadLatestBundle(String proxyName) throws ApigeeApiException {
        String latestRevision = getLatestRevision(proxyName);
        return downloadBundle(proxyName, latestRevision);
    }

    /**
     * Gets details about a specific proxy.
     *
     * @param proxyName Name of the proxy
     * @return JSON string containing proxy details
     * @throws ApigeeApiException if the API call fails
     */
    public String getProxyDetails(String proxyName) throws ApigeeApiException {
        String url = String.format("%s/organizations/%s/apis/%s",
                config.getBaseUrl(), config.getOrganization(), proxyName);
        
        log.debug("Getting details for proxy {} from: {}", proxyName, url);
        
        try {
            HttpRequest request = buildRequest(url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            checkResponse(response, "get details for " + proxyName);
            
            return response.body();
            
        } catch (ApigeeApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApigeeApiException("Failed to get details for " + proxyName + ": " + e.getMessage(), e);
        }
    }

    private HttpRequest buildRequest(String url) throws IOException {
        String accessToken = config.getAccessToken();
        
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json, application/zip")
                .timeout(DEFAULT_TIMEOUT)
                .GET()
                .build();
    }

    private void checkResponse(HttpResponse<String> response, String operation) throws ApigeeApiException {
        if (response.statusCode() != 200) {
            throw new ApigeeApiException(String.format(
                    "Failed to %s: HTTP %d - %s",
                    operation, response.statusCode(), response.body()));
        }
    }

    /**
     * Exception thrown when an Apigee API operation fails.
     */
    public static class ApigeeApiException extends Exception {
        public ApigeeApiException(String message) {
            super(message);
        }

        public ApigeeApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
