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
package com.apigee.openapi.converter.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a parsed Apigee API proxy bundle.
 * Contains all components extracted from the bundle including proxy endpoints,
 * target endpoints, and policies.
 */
public class ApigeeBundle {

    private String name;
    private String basePath;
    private String description;
    private Integer revision;
    private long createdAt;
    
    private final List<ProxyEndpoint> proxyEndpoints = new ArrayList<>();
    private final List<TargetEndpoint> targetEndpoints = new ArrayList<>();
    private final Map<String, Policy> policies = new HashMap<>();
    private final List<String> resources = new ArrayList<>();
    
    public ApigeeBundle() {}

    public ApigeeBundle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<ProxyEndpoint> getProxyEndpoints() {
        return proxyEndpoints;
    }

    public void addProxyEndpoint(ProxyEndpoint endpoint) {
        this.proxyEndpoints.add(endpoint);
    }

    public List<TargetEndpoint> getTargetEndpoints() {
        return targetEndpoints;
    }

    public void addTargetEndpoint(TargetEndpoint endpoint) {
        this.targetEndpoints.add(endpoint);
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public void addPolicy(Policy policy) {
        this.policies.put(policy.getName(), policy);
    }

    public Optional<Policy> getPolicy(String name) {
        return Optional.ofNullable(policies.get(name));
    }

    public List<String> getResources() {
        return resources;
    }

    public void addResource(String resource) {
        this.resources.add(resource);
    }

    /**
     * Gets the primary proxy endpoint (usually "default").
     */
    public Optional<ProxyEndpoint> getPrimaryProxyEndpoint() {
        if (proxyEndpoints.isEmpty()) {
            return Optional.empty();
        }
        return proxyEndpoints.stream()
                .filter(pe -> "default".equals(pe.getName()))
                .findFirst()
                .or(() -> Optional.of(proxyEndpoints.get(0)));
    }

    /**
     * Gets the primary target endpoint (usually "default").
     */
    public Optional<TargetEndpoint> getPrimaryTargetEndpoint() {
        if (targetEndpoints.isEmpty()) {
            return Optional.empty();
        }
        return targetEndpoints.stream()
                .filter(te -> "default".equals(te.getName()))
                .findFirst()
                .or(() -> Optional.of(targetEndpoints.get(0)));
    }

    /**
     * Gets the backend URL from the primary target endpoint.
     */
    public Optional<String> getBackendUrl() {
        return getPrimaryTargetEndpoint().map(TargetEndpoint::getUrl);
    }

    @Override
    public String toString() {
        return "ApigeeBundle{" +
                "name='" + name + '\'' +
                ", basePath='" + basePath + '\'' +
                ", proxyEndpoints=" + proxyEndpoints.size() +
                ", targetEndpoints=" + targetEndpoints.size() +
                ", policies=" + policies.size() +
                '}';
    }
}
