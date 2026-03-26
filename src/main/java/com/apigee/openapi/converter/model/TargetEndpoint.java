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

/**
 * Represents an Apigee TargetEndpoint configuration.
 * Contains HTTP target connection settings and flows.
 */
public class TargetEndpoint {

    private String name;
    private String description;
    private String url;
    private boolean sslEnabled;
    private String loadBalancerConfigName;
    
    private Flow preFlow;
    private Flow postFlow;

    public TargetEndpoint() {}

    public TargetEndpoint(String name) {
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getLoadBalancerConfigName() {
        return loadBalancerConfigName;
    }

    public void setLoadBalancerConfigName(String loadBalancerConfigName) {
        this.loadBalancerConfigName = loadBalancerConfigName;
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

    /**
     * Extracts the server URL scheme (http or https).
     */
    public String getScheme() {
        if (url != null && url.startsWith("https://")) {
            return "https";
        }
        return "http";
    }

    /**
     * Extracts the host from the URL.
     */
    public String getHost() {
        if (url == null) {
            return null;
        }
        String withoutScheme = url.replaceFirst("^https?://", "");
        int pathStart = withoutScheme.indexOf('/');
        return pathStart > 0 ? withoutScheme.substring(0, pathStart) : withoutScheme;
    }

    @Override
    public String toString() {
        return "TargetEndpoint{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
