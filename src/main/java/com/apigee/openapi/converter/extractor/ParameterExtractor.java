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
package com.apigee.openapi.converter.extractor;

import com.apigee.openapi.converter.model.ApigeeBundle;
import com.apigee.openapi.converter.model.Flow;
import com.apigee.openapi.converter.model.Policy;
import com.apigee.openapi.converter.model.Policy.ParameterInfo;
import com.apigee.openapi.converter.model.ProxyEndpoint;
import com.apigee.openapi.converter.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts parameter information from Apigee policies and flow conditions.
 * Supports extraction from ExtractVariables, AssignMessage, and path patterns.
 */
public class ParameterExtractor {

    private static final Logger log = LoggerFactory.getLogger(ParameterExtractor.class);

    // Pattern to extract path parameters from paths like /pets/{petId}
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /**
     * Result of parameter extraction for a specific operation.
     */
    public static class ExtractedParameters {
        private final List<ParameterInfo> pathParameters = new ArrayList<>();
        private final List<ParameterInfo> queryParameters = new ArrayList<>();
        private final List<ParameterInfo> headerParameters = new ArrayList<>();
        private final List<ParameterInfo> bodyParameters = new ArrayList<>();

        public List<ParameterInfo> getPathParameters() {
            return pathParameters;
        }

        public List<ParameterInfo> getQueryParameters() {
            return queryParameters;
        }

        public List<ParameterInfo> getHeaderParameters() {
            return headerParameters;
        }

        public List<ParameterInfo> getBodyParameters() {
            return bodyParameters;
        }

        public void addParameter(ParameterInfo param) {
            switch (param.getLocation()) {
                case PATH:
                    pathParameters.add(param);
                    break;
                case QUERY:
                    queryParameters.add(param);
                    break;
                case HEADER:
                    headerParameters.add(param);
                    break;
                case BODY:
                case FORM:
                    bodyParameters.add(param);
                    break;
            }
        }

        public boolean isEmpty() {
            return pathParameters.isEmpty() && queryParameters.isEmpty() && 
                   headerParameters.isEmpty() && bodyParameters.isEmpty();
        }
    }

    /**
     * Extracts all parameters for an operation from the bundle and flow.
     */
    public ExtractedParameters extractParameters(ApigeeBundle bundle, Flow flow, String pathPattern) {
        ExtractedParameters result = new ExtractedParameters();

        // Extract path parameters from the path pattern
        extractPathParametersFromPattern(pathPattern, result);

        // Extract parameters from policies referenced in the flow
        extractParametersFromFlow(bundle, flow, result);

        // Remove duplicates
        deduplicateParameters(result);

        return result;
    }

    /**
     * Extracts path parameters from a path pattern like /pets/{petId}.
     */
    private void extractPathParametersFromPattern(String pathPattern, ExtractedParameters result) {
        if (pathPattern == null) {
            return;
        }

        Matcher matcher = PATH_PARAM_PATTERN.matcher(pathPattern);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            ParameterInfo param = new ParameterInfo(paramName, ParameterInfo.Location.PATH);
            param.setRequired(true);
            param.setDataType("string");
            result.addParameter(param);
        }
    }

    /**
     * Extracts parameters from policies referenced in a flow.
     */
    private void extractParametersFromFlow(ApigeeBundle bundle, Flow flow, ExtractedParameters result) {
        if (flow == null) {
            return;
        }

        // Process request steps
        for (Step step : flow.getRequestSteps()) {
            extractParametersFromStep(bundle, step, result);
        }

        // Process response steps
        for (Step step : flow.getResponseSteps()) {
            extractParametersFromStep(bundle, step, result);
        }
    }

    /**
     * Extracts parameters from a specific flow step.
     */
    private void extractParametersFromStep(ApigeeBundle bundle, Step step, ExtractedParameters result) {
        String policyName = step.getEffectivePolicyName();
        Optional<Policy> policyOpt = bundle.getPolicy(policyName);
        
        if (policyOpt.isEmpty()) {
            return;
        }

        Policy policy = policyOpt.get();
        
        // Extract parameters from the policy
        for (ParameterInfo param : policy.getExtractedParameters()) {
            result.addParameter(param);
        }
    }

    /**
     * Extracts parameters from PreFlow and PostFlow policies.
     */
    public void extractGlobalParameters(ApigeeBundle bundle, ExtractedParameters result) {
        Optional<ProxyEndpoint> proxyEndpointOpt = bundle.getPrimaryProxyEndpoint();
        if (proxyEndpointOpt.isEmpty()) {
            return;
        }

        ProxyEndpoint proxyEndpoint = proxyEndpointOpt.get();

        // Extract from PreFlow
        if (proxyEndpoint.getPreFlow() != null) {
            extractParametersFromFlow(bundle, proxyEndpoint.getPreFlow(), result);
        }

        // Extract from PostFlow
        if (proxyEndpoint.getPostFlow() != null) {
            extractParametersFromFlow(bundle, proxyEndpoint.getPostFlow(), result);
        }
    }

    /**
     * Extracts all parameters from all policies in the bundle.
     * Useful for fallback extraction when flow-specific extraction yields nothing.
     */
    public ExtractedParameters extractAllParameters(ApigeeBundle bundle) {
        ExtractedParameters result = new ExtractedParameters();

        for (Policy policy : bundle.getPolicies().values()) {
            if (policy.getType() == Policy.PolicyType.EXTRACT_VARIABLES ||
                policy.getType() == Policy.PolicyType.ASSIGN_MESSAGE) {
                for (ParameterInfo param : policy.getExtractedParameters()) {
                    result.addParameter(param);
                }
            }
        }

        deduplicateParameters(result);
        return result;
    }

    /**
     * Infers parameters from query conditions in flows.
     * Looks for patterns like: request.queryparam.paramName
     */
    public void extractParametersFromConditions(ApigeeBundle bundle, ExtractedParameters result) {
        Optional<ProxyEndpoint> proxyEndpointOpt = bundle.getPrimaryProxyEndpoint();
        if (proxyEndpointOpt.isEmpty()) {
            return;
        }

        Set<String> queryParams = new HashSet<>();
        Set<String> headers = new HashSet<>();

        for (Flow flow : proxyEndpointOpt.get().getAllFlows()) {
            String condition = flow.getCondition();
            if (condition != null) {
                extractParamsFromConditionString(condition, queryParams, headers);
            }

            // Also check step conditions
            for (Step step : flow.getAllSteps()) {
                if (step.getCondition() != null) {
                    extractParamsFromConditionString(step.getCondition(), queryParams, headers);
                }
            }
        }

        // Add discovered query parameters
        for (String paramName : queryParams) {
            ParameterInfo param = new ParameterInfo(paramName, ParameterInfo.Location.QUERY);
            param.setDataType("string");
            result.addParameter(param);
        }

        // Add discovered headers
        for (String headerName : headers) {
            ParameterInfo param = new ParameterInfo(headerName, ParameterInfo.Location.HEADER);
            param.setDataType("string");
            result.addParameter(param);
        }
    }

    private void extractParamsFromConditionString(String condition, Set<String> queryParams, Set<String> headers) {
        // Pattern for query parameters: request.queryparam.paramName
        Pattern queryPattern = Pattern.compile("request\\.queryparam\\.([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
        Matcher queryMatcher = queryPattern.matcher(condition);
        while (queryMatcher.find()) {
            queryParams.add(queryMatcher.group(1));
        }

        // Pattern for headers: request.header.headerName
        Pattern headerPattern = Pattern.compile("request\\.header\\.([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
        Matcher headerMatcher = headerPattern.matcher(condition);
        while (headerMatcher.find()) {
            headers.add(headerMatcher.group(1));
        }
    }

    /**
     * Removes duplicate parameters based on name and location.
     */
    private void deduplicateParameters(ExtractedParameters result) {
        deduplicateList(result.getPathParameters());
        deduplicateList(result.getQueryParameters());
        deduplicateList(result.getHeaderParameters());
        deduplicateList(result.getBodyParameters());
    }

    private void deduplicateList(List<ParameterInfo> params) {
        Set<String> seen = new HashSet<>();
        Iterator<ParameterInfo> iterator = params.iterator();
        while (iterator.hasNext()) {
            ParameterInfo param = iterator.next();
            String key = param.getName().toLowerCase();
            if (seen.contains(key)) {
                iterator.remove();
            } else {
                seen.add(key);
            }
        }
    }
}
