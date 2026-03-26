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
 * Represents an Apigee Flow configuration.
 * A flow can be a PreFlow, PostFlow, or a conditional flow.
 */
public class Flow {

    public enum FlowType {
        PRE_FLOW,
        CONDITIONAL,
        POST_FLOW,
        FAULT_RULE,
        DEFAULT_FAULT_RULE
    }

    private String name;
    private String description;
    private String condition;
    private FlowType flowType = FlowType.CONDITIONAL;
    
    private List<Step> requestSteps = new ArrayList<>();
    private List<Step> responseSteps = new ArrayList<>();

    // Parsed condition components
    private String pathPattern;
    private String httpMethod;

    public Flow() {}

    public Flow(String name) {
        this.name = name;
    }

    public Flow(String name, FlowType flowType) {
        this.name = name;
        this.flowType = flowType;
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

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public FlowType getFlowType() {
        return flowType;
    }

    public void setFlowType(FlowType flowType) {
        this.flowType = flowType;
    }

    public List<Step> getRequestSteps() {
        return requestSteps;
    }

    public void setRequestSteps(List<Step> requestSteps) {
        this.requestSteps = requestSteps;
    }

    public void addRequestStep(Step step) {
        this.requestSteps.add(step);
    }

    public List<Step> getResponseSteps() {
        return responseSteps;
    }

    public void setResponseSteps(List<Step> responseSteps) {
        this.responseSteps = responseSteps;
    }

    public void addResponseStep(Step step) {
        this.responseSteps.add(step);
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * Returns all steps in this flow (request + response).
     */
    public List<Step> getAllSteps() {
        List<Step> allSteps = new ArrayList<>();
        allSteps.addAll(requestSteps);
        allSteps.addAll(responseSteps);
        return allSteps;
    }

    /**
     * Checks if this flow has a condition that can be parsed for path/method.
     */
    public boolean hasParseableCondition() {
        return condition != null && !condition.trim().isEmpty();
    }

    /**
     * Checks if this flow appears to be a path-based conditional flow.
     */
    public boolean isPathBasedFlow() {
        return hasParseableCondition() && 
               (condition.contains("proxy.pathsuffix") || 
                condition.contains("request.path") ||
                condition.contains("MatchesPath"));
    }

    @Override
    public String toString() {
        return "Flow{" +
                "name='" + name + '\'' +
                ", flowType=" + flowType +
                ", condition='" + condition + '\'' +
                ", pathPattern='" + pathPattern + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                '}';
    }
}
