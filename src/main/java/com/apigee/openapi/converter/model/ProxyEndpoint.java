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
import java.util.List;

/**
 * Represents an Apigee ProxyEndpoint configuration.
 * Contains flow definitions, HTTP proxy connection settings, and route rules.
 */
public class ProxyEndpoint {

    private String name;
    private String description;
    private String basePath;
    private List<String> virtualHosts = new ArrayList<>();
    
    private Flow preFlow;
    private Flow postFlow;
    private List<Flow> flows = new ArrayList<>();
    private List<Flow> faultRules = new ArrayList<>();
    private Flow defaultFaultRule;
    
    private List<RouteRule> routeRules = new ArrayList<>();

    public ProxyEndpoint() {}

    public ProxyEndpoint(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<String> getVirtualHosts() {
        return virtualHosts;
    }

    public void setVirtualHosts(List<String> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }

    public void addVirtualHost(String virtualHost) {
        this.virtualHosts.add(virtualHost);
    }

    public Flow getPreFlow() {
        return preFlow;
    }

    public void setPreFlow(Flow preFlow) {
        this.preFlow = preFlow;
    }

    public Flow getPostFlow() {
        return postFlow;
    }

    public void setPostFlow(Flow postFlow) {
        this.postFlow = postFlow;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public void addFlow(Flow flow) {
        this.flows.add(flow);
    }

    public List<Flow> getFaultRules() {
        return faultRules;
    }

    public void setFaultRules(List<Flow> faultRules) {
        this.faultRules = faultRules;
    }

    public void addFaultRule(Flow faultRule) {
        this.faultRules.add(faultRule);
    }

    public Flow getDefaultFaultRule() {
        return defaultFaultRule;
    }

    public void setDefaultFaultRule(Flow defaultFaultRule) {
        this.defaultFaultRule = defaultFaultRule;
    }

    public List<RouteRule> getRouteRules() {
        return routeRules;
    }

    public void setRouteRules(List<RouteRule> routeRules) {
        this.routeRules = routeRules;
    }

    public void addRouteRule(RouteRule routeRule) {
        this.routeRules.add(routeRule);
    }

    /**
     * Returns all flows including conditional flows.
     */
    public List<Flow> getAllFlows() {
        List<Flow> allFlows = new ArrayList<>();
        if (preFlow != null) {
            allFlows.add(preFlow);
        }
        allFlows.addAll(flows);
        if (postFlow != null) {
            allFlows.add(postFlow);
        }
        return allFlows;
    }

    /**
     * Returns only the conditional flows (not PreFlow/PostFlow).
     */
    public List<Flow> getConditionalFlows() {
        return new ArrayList<>(flows);
    }

    @Override
    public String toString() {
        return "ProxyEndpoint{" +
                "name='" + name + '\'' +
                ", basePath='" + basePath + '\'' +
                ", flows=" + flows.size() +
                '}';
    }
}
