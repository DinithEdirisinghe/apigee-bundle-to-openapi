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
import com.apigee.openapi.converter.model.Policy.PolicyType;
import com.apigee.openapi.converter.model.ProxyEndpoint;
import com.apigee.openapi.converter.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Extracts security scheme information from Apigee policies.
 * Supports API Key, OAuth2, and Basic Authentication detection.
 */
public class SecurityExtractor {

    private static final Logger log = LoggerFactory.getLogger(SecurityExtractor.class);

    /**
     * Represents an extracted security scheme.
     */
    public static class SecurityScheme {
        
        public enum Type {
            API_KEY,
            OAUTH2,
            BASIC_AUTH,
            BEARER_TOKEN
        }

        public enum In {
            HEADER,
            QUERY,
            COOKIE
        }

        private String name;
        private Type type;
        private In in;
        private String parameterName;
        private String description;
        
        // OAuth2 specific
        private Set<String> flows = new HashSet<>();
        private String authorizationUrl;
        private String tokenUrl;
        private Map<String, String> scopes = new LinkedHashMap<>();

        public SecurityScheme(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public In getIn() {
            return in;
        }

        public void setIn(In in) {
            this.in = in;
        }

        public String getParameterName() {
            return parameterName;
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Set<String> getFlows() {
            return flows;
        }

        public void addFlow(String flow) {
            this.flows.add(flow);
        }

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public void setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public Map<String, String> getScopes() {
            return scopes;
        }

        public void addScope(String scope, String description) {
            this.scopes.put(scope, description);
        }

        @Override
        public String toString() {
            return "SecurityScheme{" +
                    "name='" + name + '\'' +
                    ", type=" + type +
                    ", in=" + in +
                    ", parameterName='" + parameterName + '\'' +
                    '}';
        }
    }

    /**
     * Result of security extraction for the bundle.
     */
    public static class SecurityExtractionResult {
        private final Map<String, SecurityScheme> schemes = new LinkedHashMap<>();
        private final Map<String, List<String>> operationSecurity = new LinkedHashMap<>();

        public Map<String, SecurityScheme> getSchemes() {
            return schemes;
        }

        public void addScheme(SecurityScheme scheme) {
            schemes.put(scheme.getName(), scheme);
        }

        public Map<String, List<String>> getOperationSecurity() {
            return operationSecurity;
        }

        public void addOperationSecurity(String operationId, String schemeName) {
            operationSecurity.computeIfAbsent(operationId, k -> new ArrayList<>()).add(schemeName);
        }

        public boolean hasSecuritySchemes() {
            return !schemes.isEmpty();
        }

        public List<String> getSecurityForOperation(String operationId) {
            return operationSecurity.getOrDefault(operationId, Collections.emptyList());
        }
    }

    /**
     * Extracts all security schemes from the bundle.
     */
    public SecurityExtractionResult extractSecurity(ApigeeBundle bundle) {
        SecurityExtractionResult result = new SecurityExtractionResult();

        // Extract from policies
        for (Policy policy : bundle.getPolicies().values()) {
            extractSecurityFromPolicy(policy, result);
        }

        // Map security to operations based on flow steps
        mapSecurityToOperations(bundle, result);

        return result;
    }

    /**
     * Extracts security scheme from a policy.
     */
    private void extractSecurityFromPolicy(Policy policy, SecurityExtractionResult result) {
        switch (policy.getType()) {
            case VERIFY_API_KEY:
                extractApiKeySecurity(policy, result);
                break;
            case VERIFY_JWT:
                extractJwtSecurity(policy, result);
                break;
            case OAUTH_V2:
                extractOAuth2Security(policy, result);
                break;
            case BASIC_AUTH:
                extractBasicAuthSecurity(policy, result);
                break;
            default:
                // Check for OAuth token verification in other policies
                if (isTokenVerificationPolicy(policy)) {
                    extractBearerTokenSecurity(policy, result);
                }
                break;
        }
    }

    private void extractApiKeySecurity(Policy policy, SecurityExtractionResult result) {
        String location = policy.getConfigValue("apiKeyLocation", String.class);
        String paramName = policy.getConfigValue("apiKeyName", String.class);

        SecurityScheme scheme = new SecurityScheme("apiKey", SecurityScheme.Type.API_KEY);
        
        if ("header".equals(location)) {
            scheme.setIn(SecurityScheme.In.HEADER);
            scheme.setParameterName(paramName != null ? paramName : "X-API-Key");
        } else {
            scheme.setIn(SecurityScheme.In.QUERY);
            scheme.setParameterName(paramName != null ? paramName : "apikey");
        }

        scheme.setDescription("API Key authentication");
        result.addScheme(scheme);

        log.debug("Extracted API Key security: {}", scheme);
    }

    private void extractJwtSecurity(Policy policy, SecurityExtractionResult result) {
        String location = policy.getConfigValue("tokenLocation", String.class);
        String headerName = policy.getConfigValue("tokenHeaderName", String.class);
        String paramName = policy.getConfigValue("tokenParamName", String.class);
        String issuer = policy.getConfigValue("issuer", String.class);

        // Use apiKey type with the actual header name from the policy
        SecurityScheme scheme = new SecurityScheme("ApiKeyAuth", SecurityScheme.Type.API_KEY);
        
        if ("header".equals(location)) {
            scheme.setIn(SecurityScheme.In.HEADER);
            scheme.setParameterName(headerName != null ? headerName : "Authorization");
        } else if ("query".equals(location)) {
            scheme.setIn(SecurityScheme.In.QUERY);
            scheme.setParameterName(paramName != null ? paramName : "token");
        } else {
            // Default to header
            scheme.setIn(SecurityScheme.In.HEADER);
            scheme.setParameterName(headerName != null ? headerName : "Authorization");
        }

        // Build description
        StringBuilder desc = new StringBuilder("JWT token authentication");
        if (issuer != null) {
            desc.append(" (Issuer: ").append(issuer).append(")");
        }
        scheme.setDescription(desc.toString());
        
        result.addScheme(scheme);

        log.debug("Extracted JWT security: {}", scheme);
    }

    @SuppressWarnings("unchecked")
    private void extractOAuth2Security(Policy policy, SecurityExtractionResult result) {
        SecurityScheme scheme = new SecurityScheme("oauth2", SecurityScheme.Type.OAUTH2);
        
        String operation = policy.getConfigValue("operation", String.class);
        List<String> grantTypes = policy.getConfigValue("supportedGrantTypes", List.class);

        // Map Apigee grant types to OAuth2 flows
        if (grantTypes != null) {
            for (String grantType : grantTypes) {
                switch (grantType.toLowerCase()) {
                    case "authorization_code":
                        scheme.addFlow("authorizationCode");
                        break;
                    case "implicit":
                        scheme.addFlow("implicit");
                        break;
                    case "client_credentials":
                        scheme.addFlow("clientCredentials");
                        break;
                    case "password":
                        scheme.addFlow("password");
                        break;
                }
            }
        }

        // Default to client credentials if no grant types specified
        if (scheme.getFlows().isEmpty()) {
            scheme.addFlow("clientCredentials");
        }

        // Set placeholder URLs
        scheme.setAuthorizationUrl("/oauth/authorize");
        scheme.setTokenUrl("/oauth/token");
        scheme.setDescription("OAuth 2.0 authentication");

        result.addScheme(scheme);
        log.debug("Extracted OAuth2 security: {}", scheme);
    }

    private void extractBasicAuthSecurity(Policy policy, SecurityExtractionResult result) {
        SecurityScheme scheme = new SecurityScheme("basicAuth", SecurityScheme.Type.BASIC_AUTH);
        scheme.setDescription("Basic HTTP authentication");
        result.addScheme(scheme);

        log.debug("Extracted Basic Auth security: {}", scheme);
    }

    private void extractBearerTokenSecurity(Policy policy, SecurityExtractionResult result) {
        // Check if we already have OAuth2 scheme
        if (result.getSchemes().containsKey("oauth2")) {
            return;
        }

        SecurityScheme scheme = new SecurityScheme("bearerAuth", SecurityScheme.Type.BEARER_TOKEN);
        scheme.setIn(SecurityScheme.In.HEADER);
        scheme.setParameterName("Authorization");
        scheme.setDescription("Bearer token authentication");
        result.addScheme(scheme);

        log.debug("Extracted Bearer Token security: {}", scheme);
    }

    private boolean isTokenVerificationPolicy(Policy policy) {
        String name = policy.getName();
        if (name == null) {
            return false;
        }
        String lowerName = name.toLowerCase();
        return lowerName.contains("verify") && 
               (lowerName.contains("token") || lowerName.contains("access") || lowerName.contains("oauth"));
    }

    /**
     * Maps security schemes to specific operations based on flow steps.
     */
    private void mapSecurityToOperations(ApigeeBundle bundle, SecurityExtractionResult result) {
        Optional<ProxyEndpoint> proxyEndpointOpt = bundle.getPrimaryProxyEndpoint();
        if (proxyEndpointOpt.isEmpty()) {
            return;
        }

        ProxyEndpoint proxyEndpoint = proxyEndpointOpt.get();
        Set<String> globalSecurityPolicies = new HashSet<>();

        // Check PreFlow for global security
        if (proxyEndpoint.getPreFlow() != null) {
            for (Step step : proxyEndpoint.getPreFlow().getRequestSteps()) {
                if (isSecurityStep(bundle, step)) {
                    globalSecurityPolicies.add(step.getEffectivePolicyName());
                }
            }
        }

        // Map security to individual operations
        for (Flow flow : proxyEndpoint.getConditionalFlows()) {
            String operationId = generateOperationId(flow);
            
            // Add global security
            for (String policyName : globalSecurityPolicies) {
                String schemeName = getSchemeNameForPolicy(bundle, policyName, result);
                if (schemeName != null) {
                    result.addOperationSecurity(operationId, schemeName);
                }
            }

            // Add flow-specific security
            for (Step step : flow.getRequestSteps()) {
                if (isSecurityStep(bundle, step)) {
                    String schemeName = getSchemeNameForPolicy(bundle, step.getEffectivePolicyName(), result);
                    if (schemeName != null) {
                        result.addOperationSecurity(operationId, schemeName);
                    }
                }
            }
        }
    }

    private boolean isSecurityStep(ApigeeBundle bundle, Step step) {
        Optional<Policy> policyOpt = bundle.getPolicy(step.getEffectivePolicyName());
        return policyOpt.isPresent() && policyOpt.get().isSecurityPolicy();
    }

    private String getSchemeNameForPolicy(ApigeeBundle bundle, String policyName, SecurityExtractionResult result) {
        Optional<Policy> policyOpt = bundle.getPolicy(policyName);
        if (policyOpt.isEmpty()) {
            return null;
        }

        Policy policy = policyOpt.get();
        switch (policy.getType()) {
            case VERIFY_API_KEY:
                return "apiKey";
            case VERIFY_JWT:
                return "ApiKeyAuth";
            case OAUTH_V2:
                return "oauth2";
            case BASIC_AUTH:
                return "basicAuth";
            default:
                if (isTokenVerificationPolicy(policy)) {
                    return result.getSchemes().containsKey("oauth2") ? "oauth2" : "bearerAuth";
                }
                return null;
        }
    }

    private String generateOperationId(Flow flow) {
        if (flow.getName() != null && !flow.getName().isEmpty()) {
            return flow.getName();
        }
        String method = flow.getHttpMethod() != null ? flow.getHttpMethod() : "operation";
        String path = flow.getPathPattern() != null ? flow.getPathPattern().replace("/", "_") : "";
        return method.toLowerCase() + path;
    }
}
