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
import com.apigee.openapi.converter.model.ProxyEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts API paths and operations from Apigee proxy configuration.
 * Implements fallback strategies for proxies without explicit conditional flows.
 */
public class PathExtractor {

    private static final Logger log = LoggerFactory.getLogger(PathExtractor.class);

    // Pattern to extract path parameters
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /**
     * Represents an extracted operation (path + method combination).
     */
    public static class ExtractedOperation {
        private String path;
        private String method;
        private String operationId;
        private String summary;
        private String description;
        private Flow sourceFlow;

        public ExtractedOperation(String path, String method) {
            this.path = path;
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getOperationId() {
            return operationId;
        }

        public void setOperationId(String operationId) {
            this.operationId = operationId;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Flow getSourceFlow() {
            return sourceFlow;
        }

        public void setSourceFlow(Flow sourceFlow) {
            this.sourceFlow = sourceFlow;
        }

        /**
         * Generates a unique key for this operation.
         */
        public String getKey() {
            return method.toUpperCase() + " " + path;
        }

        @Override
        public String toString() {
            return "ExtractedOperation{" +
                    "path='" + path + '\'' +
                    ", method='" + method + '\'' +
                    ", operationId='" + operationId + '\'' +
                    '}';
        }
    }

    /**
     * Result of path extraction.
     */
    public static class PathExtractionResult {
        private String basePath;
        private final List<ExtractedOperation> operations = new ArrayList<>();
        private final Set<String> extractedPaths = new LinkedHashSet<>();
        private boolean usedFallback = false;

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public List<ExtractedOperation> getOperations() {
            return operations;
        }

        public void addOperation(ExtractedOperation operation) {
            String key = operation.getKey();
            if (!extractedPaths.contains(key)) {
                operations.add(operation);
                extractedPaths.add(key);
            }
        }

        public Set<String> getUniquePaths() {
            Set<String> paths = new LinkedHashSet<>();
            for (ExtractedOperation op : operations) {
                paths.add(op.getPath());
            }
            return paths;
        }

        public boolean isUsedFallback() {
            return usedFallback;
        }

        public void setUsedFallback(boolean usedFallback) {
            this.usedFallback = usedFallback;
        }

        public boolean isEmpty() {
            return operations.isEmpty();
        }
    }

    /**
     * Extracts paths and operations from the bundle.
     */
    public PathExtractionResult extractPaths(ApigeeBundle bundle) {
        PathExtractionResult result = new PathExtractionResult();
        result.setBasePath(bundle.getBasePath());

        Optional<ProxyEndpoint> proxyEndpointOpt = bundle.getPrimaryProxyEndpoint();
        if (proxyEndpointOpt.isEmpty()) {
            log.warn("No proxy endpoint found, using fallback extraction");
            result.setUsedFallback(true);
            createFallbackOperation(bundle, result);
            return result;
        }

        ProxyEndpoint proxyEndpoint = proxyEndpointOpt.get();
        
        // Set base path from proxy endpoint if not already set
        if (result.getBasePath() == null && proxyEndpoint.getBasePath() != null) {
            result.setBasePath(proxyEndpoint.getBasePath());
        }

        // Extract from conditional flows
        extractFromConditionalFlows(proxyEndpoint, result);

        // If no operations were extracted, use fallback strategy
        if (result.isEmpty()) {
            log.info("No conditional flows found, using fallback extraction");
            result.setUsedFallback(true);
            createFallbackOperation(bundle, result);
        }

        return result;
    }

    /**
     * Extracts operations from conditional flows.
     */
    private void extractFromConditionalFlows(ProxyEndpoint proxyEndpoint, PathExtractionResult result) {
        for (Flow flow : proxyEndpoint.getConditionalFlows()) {
            if (!flow.isPathBasedFlow()) {
                log.debug("Skipping non-path-based flow: {}", flow.getName());
                continue;
            }

            String pathPattern = flow.getPathPattern();
            String httpMethod = flow.getHttpMethod();

            if (pathPattern == null) {
                log.debug("Flow has no path pattern: {}", flow.getName());
                continue;
            }

            // Normalize path
            pathPattern = normalizePath(pathPattern);

            // If no specific method, create operations for common methods
            if (httpMethod == null || httpMethod.isEmpty()) {
                // Check flow name for method hints
                httpMethod = inferMethodFromFlowName(flow.getName());
            }

            if (httpMethod != null && !httpMethod.isEmpty()) {
                ExtractedOperation operation = createOperation(pathPattern, httpMethod, flow);
                result.addOperation(operation);
            } else {
                // Create GET as default when method is unknown
                ExtractedOperation operation = createOperation(pathPattern, "GET", flow);
                result.addOperation(operation);
            }
        }
    }

    /**
     * Creates an operation from extracted data.
     */
    private ExtractedOperation createOperation(String path, String method, Flow flow) {
        ExtractedOperation operation = new ExtractedOperation(path, method.toUpperCase());
        operation.setSourceFlow(flow);

        // Generate operation ID
        String operationId = generateOperationId(flow, path, method);
        operation.setOperationId(operationId);

        // Use flow description if available
        if (flow.getDescription() != null && !flow.getDescription().isEmpty()) {
            operation.setSummary(flow.getDescription());
        } else if (flow.getName() != null && !flow.getName().isEmpty()) {
            operation.setSummary(humanizeName(flow.getName()));
        } else {
            operation.setSummary(method.toUpperCase() + " " + path);
        }

        return operation;
    }

    /**
     * Creates a fallback operation when no specific flows are found.
     */
    private void createFallbackOperation(ApigeeBundle bundle, PathExtractionResult result) {
        String basePath = result.getBasePath();
        if (basePath == null || basePath.isEmpty()) {
            basePath = "/" + (bundle.getName() != null ? bundle.getName() : "api");
        }

        // Create a generic catch-all operation
        ExtractedOperation operation = new ExtractedOperation("/", "GET");
        operation.setOperationId("getRoot");
        operation.setSummary("API root endpoint");
        operation.setDescription("Automatically generated fallback operation. The original proxy has no explicit conditional flows.");
        result.addOperation(operation);

        // If there are path parameters in policies, try to create more specific paths
        extractPathsFromPolicies(bundle, result);
    }

    /**
     * Extracts paths from policy configurations (fallback strategy).
     */
    private void extractPathsFromPolicies(ApigeeBundle bundle, PathExtractionResult result) {
        Set<String> discoveredPaths = new HashSet<>();

        // Look for path patterns in ExtractVariables policies
        for (var policy : bundle.getPolicies().values()) {
            for (var param : policy.getExtractedParameters()) {
                if (param.getLocation() == com.apigee.openapi.converter.model.Policy.ParameterInfo.Location.PATH) {
                    String pattern = param.getPattern();
                    if (pattern != null && pattern.startsWith("/") && !discoveredPaths.contains(pattern)) {
                        discoveredPaths.add(pattern);
                        
                        ExtractedOperation operation = new ExtractedOperation(pattern, "GET");
                        operation.setOperationId(generateOperationIdFromPath(pattern, "GET"));
                        operation.setSummary("Operation for " + pattern);
                        result.addOperation(operation);
                    }
                }
            }
        }
    }

    /**
     * Normalizes a path pattern.
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }

        // Ensure path starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // Remove trailing slash (unless it's just "/")
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * Infers HTTP method from flow name.
     */
    private String inferMethodFromFlowName(String flowName) {
        if (flowName == null) {
            return null;
        }

        String lowerName = flowName.toLowerCase();
        
        if (lowerName.startsWith("get") || lowerName.contains("list") || lowerName.contains("fetch") || lowerName.contains("read")) {
            return "GET";
        } else if (lowerName.startsWith("post") || lowerName.contains("create") || lowerName.contains("add")) {
            return "POST";
        } else if (lowerName.startsWith("put") || lowerName.contains("update") || lowerName.contains("modify")) {
            return "PUT";
        } else if (lowerName.startsWith("delete") || lowerName.contains("remove")) {
            return "DELETE";
        } else if (lowerName.startsWith("patch")) {
            return "PATCH";
        } else if (lowerName.startsWith("head")) {
            return "HEAD";
        } else if (lowerName.startsWith("options") || lowerName.equals("optionspreflight")) {
            return "OPTIONS";
        }

        return null;
    }

    /**
     * Generates an operation ID from flow, path, and method.
     */
    private String generateOperationId(Flow flow, String path, String method) {
        // Prefer flow name if it looks like an operation ID
        if (flow != null && flow.getName() != null && !flow.getName().isEmpty()) {
            String name = flow.getName();
            if (!name.contains(" ") && !name.contains("/")) {
                return toCamelCase(name);
            }
        }

        return generateOperationIdFromPath(path, method);
    }

    /**
     * Generates an operation ID from path and method.
     */
    private String generateOperationIdFromPath(String path, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.toLowerCase());

        // Process path segments
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            // Handle path parameters
            Matcher matcher = PATH_PARAM_PATTERN.matcher(segment);
            if (matcher.matches()) {
                sb.append("By").append(capitalize(matcher.group(1)));
            } else {
                sb.append(capitalize(segment));
            }
        }

        return sb.toString();
    }

    /**
     * Converts a string to camelCase.
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Already camelCase
        if (!input.contains("_") && !input.contains("-") && !input.contains(" ")) {
            return Character.toLowerCase(input.charAt(0)) + input.substring(1);
        }

        String[] parts = input.split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                sb.append(part.toLowerCase());
            } else {
                sb.append(capitalize(part));
            }
        }
        return sb.toString();
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    /**
     * Converts a technical name to human-readable format.
     */
    private String humanizeName(String name) {
        if (name == null) {
            return null;
        }

        // Split camelCase and underscores
        String result = name.replaceAll("([a-z])([A-Z])", "$1 $2")
                           .replace("_", " ")
                           .replace("-", " ");

        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        for (String word : result.split("\\s+")) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(capitalize(word));
        }

        return sb.toString();
    }
}
