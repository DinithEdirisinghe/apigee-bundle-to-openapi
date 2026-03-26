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
 * Represents an Apigee RouteRule configuration.
 * Route rules determine how requests are routed to target endpoints.
 */
public class RouteRule {

    private String name;
    private String condition;
    private String targetEndpoint;

    public RouteRule() {}

    public RouteRule(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    /**
     * Checks if this is a "no route" rule (e.g., for OPTIONS preflight).
     */
    public boolean isNoRoute() {
        return targetEndpoint == null || targetEndpoint.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "RouteRule{" +
                "name='" + name + '\'' +
                ", targetEndpoint='" + targetEndpoint + '\'' +
                ", condition='" + condition + '\'' +
                '}';
    }
}
