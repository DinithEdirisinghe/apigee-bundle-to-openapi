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
package com.apigee.openapi.converter.generator;

import com.apigee.openapi.converter.ConversionOptions;
import com.apigee.openapi.converter.extractor.ParameterExtractor;
import com.apigee.openapi.converter.extractor.ParameterExtractor.ExtractedParameters;
import com.apigee.openapi.converter.extractor.PathExtractor;
import com.apigee.openapi.converter.extractor.PathExtractor.ExtractedOperation;
import com.apigee.openapi.converter.extractor.PathExtractor.PathExtractionResult;
import com.apigee.openapi.converter.extractor.SecurityExtractor;
import com.apigee.openapi.converter.extractor.SecurityExtractor.SecurityExtractionResult;
import com.apigee.openapi.converter.extractor.SecurityExtractor.SecurityScheme;
import com.apigee.openapi.converter.model.ApigeeBundle;
import com.apigee.openapi.converter.model.Policy;
import com.apigee.openapi.converter.model.TargetEndpoint;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Generates OpenAPI 3.0 specification from extracted Apigee bundle data.
 */
public class OpenApiGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenApiGenerator.class);

    private static final String OPENAPI_VERSION = "3.0.3";

    private final PathExtractor pathExtractor;
    private final ParameterExtractor parameterExtractor;
    private final SecurityExtractor securityExtractor;

    public OpenApiGenerator() {
        this.pathExtractor = new PathExtractor();
        this.parameterExtractor = new ParameterExtractor();
        this.securityExtractor = new SecurityExtractor();
    }

    /**
     * Generates an OpenAPI specification from the Apigee bundle.
     *
     * @param bundle  The parsed Apigee bundle
     * @param options Conversion options
     * @return Generated OpenAPI specification
     */
    public OpenAPI generate(ApigeeBundle bundle, ConversionOptions options) {
        log.info("Generating OpenAPI 3.0 specification for: {}", bundle.getName());

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi(OPENAPI_VERSION);

        // Generate Info section
        openAPI.setInfo(generateInfo(bundle, options));

        // Generate Servers section
        openAPI.setServers(generateServers(bundle, options));

        // Extract and generate paths
        PathExtractionResult pathResult = pathExtractor.extractPaths(bundle);
        openAPI.setPaths(generatePaths(bundle, pathResult, options));

        // Extract and generate security
        SecurityExtractionResult securityResult = securityExtractor.extractSecurity(bundle);
        if (securityResult.hasSecuritySchemes()) {
            openAPI.setComponents(generateComponents(securityResult));
            openAPI.setSecurity(generateGlobalSecurity(securityResult));
        }

        // Add tags based on path grouping
        openAPI.setTags(generateTags(pathResult));

        log.info("Generated OpenAPI spec with {} paths and {} operations",
                openAPI.getPaths() != null ? openAPI.getPaths().size() : 0,
                countOperations(openAPI));

        return openAPI;
    }

    /**
     * Generates the Info section of the OpenAPI spec.
     */
    private Info generateInfo(ApigeeBundle bundle, ConversionOptions options) {
        Info info = new Info();

        // Title
        String title = options.getTitle();
        if (title == null || title.isEmpty()) {
            title = bundle.getName() != null ? bundle.getName() : "API";
            title = humanizeTitle(title);
        }
        info.setTitle(title);

        // Version
        String version = options.getVersion();
        if (version == null || version.isEmpty()) {
            version = bundle.getRevision() != null ? "1.0." + bundle.getRevision() : "1.0.0";
        }
        info.setVersion(version);

        // Description
        String description = options.getDescription();
        if (description == null || description.isEmpty()) {
            description = bundle.getDescription();
        }
        if (description == null || description.isEmpty()) {
            description = "API specification generated from Apigee proxy bundle: " + bundle.getName();
        }
        info.setDescription(description);

        // Optional contact and license
        if (options.getContactName() != null || options.getContactEmail() != null) {
            Contact contact = new Contact();
            contact.setName(options.getContactName());
            contact.setEmail(options.getContactEmail());
            contact.setUrl(options.getContactUrl());
            info.setContact(contact);
        }

        if (options.getLicenseName() != null) {
            License license = new License();
            license.setName(options.getLicenseName());
            license.setUrl(options.getLicenseUrl());
            info.setLicense(license);
        }

        return info;
    }

    /**
     * Generates the Servers section.
     */
    private List<Server> generateServers(ApigeeBundle bundle, ConversionOptions options) {
        List<Server> servers = new ArrayList<>();

        // Use provided server URL if available
        if (options.getServerUrl() != null && !options.getServerUrl().isEmpty()) {
            Server server = new Server();
            server.setUrl(options.getServerUrl());
            server.setDescription("API Server");
            servers.add(server);
            return servers;
        }

        // Try to extract from target endpoint
        Optional<TargetEndpoint> targetOpt = bundle.getPrimaryTargetEndpoint();
        if (targetOpt.isPresent()) {
            TargetEndpoint target = targetOpt.get();
            if (target.getUrl() != null) {
                Server server = new Server();
                server.setUrl(target.getUrl());
                server.setDescription("Backend Server");
                servers.add(server);
            }
        }

        // Add a placeholder server if none found
        if (servers.isEmpty()) {
            Server server = new Server();
            String basePath = bundle.getBasePath() != null ? bundle.getBasePath() : "/" + bundle.getName();
            server.setUrl("https://api.example.com" + basePath);
            server.setDescription("API Server (placeholder URL)");
            servers.add(server);
        }

        return servers;
    }

    /**
     * Generates the Paths section.
     */
    private Paths generatePaths(ApigeeBundle bundle, PathExtractionResult pathResult, ConversionOptions options) {
        Paths paths = new Paths();

        for (ExtractedOperation operation : pathResult.getOperations()) {
            String path = operation.getPath();
            
            // Get or create PathItem
            PathItem pathItem = paths.get(path);
            if (pathItem == null) {
                pathItem = new PathItem();
                paths.addPathItem(path, pathItem);
            }

            // Create Operation
            Operation op = createOperation(bundle, operation, options);

            // Set operation on correct HTTP method
            setOperationOnPathItem(pathItem, operation.getMethod(), op);
        }

        return paths;
    }

    /**
     * Creates an Operation from extracted operation data.
     */
    private Operation createOperation(ApigeeBundle bundle, ExtractedOperation extracted, ConversionOptions options) {
        Operation operation = new Operation();

        // Operation ID
        operation.setOperationId(extracted.getOperationId());

        // Summary and description
        operation.setSummary(extracted.getSummary());
        if (extracted.getDescription() != null) {
            operation.setDescription(extracted.getDescription());
        }

        // Tags
        String tag = extractTagFromPath(extracted.getPath());
        if (tag != null) {
            operation.setTags(Collections.singletonList(tag));
        }

        // Parameters
        List<Parameter> parameters = createParameters(bundle, extracted);
        if (!parameters.isEmpty()) {
            operation.setParameters(parameters);
        }

        // Responses
        operation.setResponses(createDefaultResponses(extracted.getMethod()));

        return operation;
    }

    /**
     * Creates parameters for an operation.
     */
    private List<Parameter> createParameters(ApigeeBundle bundle, ExtractedOperation extracted) {
        List<Parameter> parameters = new ArrayList<>();

        // Extract parameters using ParameterExtractor
        ExtractedParameters extractedParams = parameterExtractor.extractParameters(
                bundle, extracted.getSourceFlow(), extracted.getPath());

        // Path parameters
        for (Policy.ParameterInfo param : extractedParams.getPathParameters()) {
            PathParameter pathParam = new PathParameter();
            pathParam.setName(param.getName());
            pathParam.setRequired(true);
            pathParam.setSchema(createSchemaForParam(param));
            if (param.getDescription() != null) {
                pathParam.setDescription(param.getDescription());
            }
            parameters.add(pathParam);
        }

        // Query parameters
        for (Policy.ParameterInfo param : extractedParams.getQueryParameters()) {
            QueryParameter queryParam = new QueryParameter();
            queryParam.setName(param.getName());
            queryParam.setRequired(param.isRequired());
            queryParam.setSchema(createSchemaForParam(param));
            if (param.getDescription() != null) {
                queryParam.setDescription(param.getDescription());
            }
            parameters.add(queryParam);
        }

        // Header parameters
        for (Policy.ParameterInfo param : extractedParams.getHeaderParameters()) {
            // Skip common headers that are usually implicit
            if (isCommonHeader(param.getName())) {
                continue;
            }
            HeaderParameter headerParam = new HeaderParameter();
            headerParam.setName(param.getName());
            headerParam.setRequired(param.isRequired());
            headerParam.setSchema(createSchemaForParam(param));
            if (param.getDescription() != null) {
                headerParam.setDescription(param.getDescription());
            }
            parameters.add(headerParam);
        }

        return parameters;
    }

    /**
     * Creates a schema for a parameter.
     */
    @SuppressWarnings("rawtypes")
    private Schema createSchemaForParam(Policy.ParameterInfo param) {
        String dataType = param.getDataType();
        if (dataType == null) {
            dataType = "string";
        }

        switch (dataType.toLowerCase()) {
            case "integer":
            case "int":
            case "int32":
                return new Schema<Integer>().type("integer").format("int32");
            case "long":
            case "int64":
                return new Schema<Long>().type("integer").format("int64");
            case "number":
            case "float":
            case "double":
                return new Schema<Double>().type("number");
            case "boolean":
            case "bool":
                return new Schema<Boolean>().type("boolean");
            default:
                return new StringSchema();
        }
    }

    /**
     * Checks if a header is a common implicit header.
     */
    private boolean isCommonHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        String lower = headerName.toLowerCase();
        return lower.equals("content-type") ||
               lower.equals("accept") ||
               lower.equals("authorization") ||
               lower.equals("user-agent") ||
               lower.equals("host") ||
               lower.equals("content-length");
    }

    /**
     * Creates default responses for an operation.
     */
    private ApiResponses createDefaultResponses(String method) {
        ApiResponses responses = new ApiResponses();

        // Success response
        ApiResponse successResponse = new ApiResponse();
        switch (method.toUpperCase()) {
            case "POST":
                successResponse.setDescription("Created");
                responses.addApiResponse("201", successResponse);
                break;
            case "DELETE":
                successResponse.setDescription("No Content");
                responses.addApiResponse("204", successResponse);
                break;
            default:
                successResponse.setDescription("Successful response");
                successResponse.setContent(createJsonContent());
                responses.addApiResponse("200", successResponse);
                break;
        }

        // Error responses
        ApiResponse badRequest = new ApiResponse();
        badRequest.setDescription("Bad Request");
        responses.addApiResponse("400", badRequest);

        ApiResponse unauthorized = new ApiResponse();
        unauthorized.setDescription("Unauthorized");
        responses.addApiResponse("401", unauthorized);

        ApiResponse serverError = new ApiResponse();
        serverError.setDescription("Internal Server Error");
        responses.addApiResponse("500", serverError);

        return responses;
    }

    /**
     * Creates JSON content for responses.
     */
    @SuppressWarnings("rawtypes")
    private Content createJsonContent() {
        Content content = new Content();
        MediaType mediaType = new MediaType();
        mediaType.setSchema(new Schema<>().type("object"));
        content.addMediaType("application/json", mediaType);
        return content;
    }

    /**
     * Sets an operation on the appropriate HTTP method of a PathItem.
     */
    private void setOperationOnPathItem(PathItem pathItem, String method, Operation operation) {
        switch (method.toUpperCase()) {
            case "GET":
                pathItem.setGet(operation);
                break;
            case "POST":
                pathItem.setPost(operation);
                break;
            case "PUT":
                pathItem.setPut(operation);
                break;
            case "DELETE":
                pathItem.setDelete(operation);
                break;
            case "PATCH":
                pathItem.setPatch(operation);
                break;
            case "HEAD":
                pathItem.setHead(operation);
                break;
            case "OPTIONS":
                pathItem.setOptions(operation);
                break;
            case "TRACE":
                pathItem.setTrace(operation);
                break;
            default:
                log.warn("Unknown HTTP method: {}", method);
                pathItem.setGet(operation);
                break;
        }
    }

    /**
     * Generates Components section with security schemes.
     */
    private Components generateComponents(SecurityExtractionResult securityResult) {
        Components components = new Components();
        
        Map<String, io.swagger.v3.oas.models.security.SecurityScheme> schemes = new LinkedHashMap<>();
        
        for (SecurityScheme scheme : securityResult.getSchemes().values()) {
            io.swagger.v3.oas.models.security.SecurityScheme oasScheme = convertSecurityScheme(scheme);
            schemes.put(scheme.getName(), oasScheme);
        }
        
        if (!schemes.isEmpty()) {
            components.setSecuritySchemes(schemes);
        }
        
        return components;
    }

    /**
     * Converts internal SecurityScheme to OpenAPI SecurityScheme.
     */
    private io.swagger.v3.oas.models.security.SecurityScheme convertSecurityScheme(SecurityScheme scheme) {
        io.swagger.v3.oas.models.security.SecurityScheme oasScheme = 
                new io.swagger.v3.oas.models.security.SecurityScheme();

        switch (scheme.getType()) {
            case API_KEY:
                oasScheme.setType(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY);
                oasScheme.setName(scheme.getParameterName());
                oasScheme.setIn(convertSecuritySchemeIn(scheme.getIn()));
                break;
            case OAUTH2:
                oasScheme.setType(io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2);
                oasScheme.setFlows(createOAuthFlows(scheme));
                break;
            case BASIC_AUTH:
                oasScheme.setType(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP);
                oasScheme.setScheme("basic");
                break;
            case BEARER_TOKEN:
                oasScheme.setType(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP);
                oasScheme.setScheme("bearer");
                oasScheme.setBearerFormat("JWT");
                break;
        }

        if (scheme.getDescription() != null) {
            oasScheme.setDescription(scheme.getDescription());
        }

        return oasScheme;
    }

    private io.swagger.v3.oas.models.security.SecurityScheme.In convertSecuritySchemeIn(SecurityScheme.In in) {
        if (in == null) {
            return io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
        }
        switch (in) {
            case HEADER:
                return io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
            case QUERY:
                return io.swagger.v3.oas.models.security.SecurityScheme.In.QUERY;
            case COOKIE:
                return io.swagger.v3.oas.models.security.SecurityScheme.In.COOKIE;
            default:
                return io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER;
        }
    }

    private OAuthFlows createOAuthFlows(SecurityScheme scheme) {
        OAuthFlows flows = new OAuthFlows();
        Scopes scopes = new Scopes();
        
        for (Map.Entry<String, String> scope : scheme.getScopes().entrySet()) {
            scopes.addString(scope.getKey(), scope.getValue());
        }
        
        // Default scope if none specified
        if (scopes.size() == 0) {
            scopes.addString("read", "Read access");
            scopes.addString("write", "Write access");
        }

        for (String flowType : scheme.getFlows()) {
            switch (flowType) {
                case "authorizationCode":
                    OAuthFlow authCodeFlow = new OAuthFlow();
                    authCodeFlow.setAuthorizationUrl(scheme.getAuthorizationUrl());
                    authCodeFlow.setTokenUrl(scheme.getTokenUrl());
                    authCodeFlow.setScopes(scopes);
                    flows.setAuthorizationCode(authCodeFlow);
                    break;
                case "implicit":
                    OAuthFlow implicitFlow = new OAuthFlow();
                    implicitFlow.setAuthorizationUrl(scheme.getAuthorizationUrl());
                    implicitFlow.setScopes(scopes);
                    flows.setImplicit(implicitFlow);
                    break;
                case "clientCredentials":
                    OAuthFlow clientCredFlow = new OAuthFlow();
                    clientCredFlow.setTokenUrl(scheme.getTokenUrl());
                    clientCredFlow.setScopes(scopes);
                    flows.setClientCredentials(clientCredFlow);
                    break;
                case "password":
                    OAuthFlow passwordFlow = new OAuthFlow();
                    passwordFlow.setTokenUrl(scheme.getTokenUrl());
                    passwordFlow.setScopes(scopes);
                    flows.setPassword(passwordFlow);
                    break;
            }
        }

        return flows;
    }

    /**
     * Generates global security requirements.
     */
    private List<SecurityRequirement> generateGlobalSecurity(SecurityExtractionResult securityResult) {
        List<SecurityRequirement> security = new ArrayList<>();
        
        for (String schemeName : securityResult.getSchemes().keySet()) {
            SecurityRequirement requirement = new SecurityRequirement();
            requirement.addList(schemeName);
            security.add(requirement);
        }
        
        return security;
    }

    /**
     * Generates tags based on paths.
     */
    private List<io.swagger.v3.oas.models.tags.Tag> generateTags(PathExtractionResult pathResult) {
        Set<String> tagNames = new LinkedHashSet<>();
        
        for (ExtractedOperation op : pathResult.getOperations()) {
            String tag = extractTagFromPath(op.getPath());
            if (tag != null) {
                tagNames.add(tag);
            }
        }

        List<io.swagger.v3.oas.models.tags.Tag> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            io.swagger.v3.oas.models.tags.Tag tag = new io.swagger.v3.oas.models.tags.Tag();
            tag.setName(tagName);
            tag.setDescription(humanizeTitle(tagName) + " operations");
            tags.add(tag);
        }

        return tags.isEmpty() ? null : tags;
    }

    /**
     * Extracts a tag name from a path.
     */
    private String extractTagFromPath(String path) {
        if (path == null || path.equals("/")) {
            return "default";
        }

        // Get first segment of path
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (!segment.isEmpty() && !segment.startsWith("{")) {
                return segment;
            }
        }

        return "default";
    }

    /**
     * Humanizes a title string.
     */
    private String humanizeTitle(String title) {
        if (title == null) {
            return "API";
        }

        // Split camelCase and underscores
        String result = title.replaceAll("([a-z])([A-Z])", "$1 $2")
                           .replace("_", " ")
                           .replace("-", " ");

        // Capitalize first letter of each word
        StringBuilder sb = new StringBuilder();
        for (String word : result.split("\\s+")) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }

        return sb.length() > 0 ? sb.toString() : title;
    }

    /**
     * Counts total operations in the spec.
     */
    private int countOperations(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return 0;
        }

        int count = 0;
        for (PathItem pathItem : openAPI.getPaths().values()) {
            if (pathItem.getGet() != null) count++;
            if (pathItem.getPost() != null) count++;
            if (pathItem.getPut() != null) count++;
            if (pathItem.getDelete() != null) count++;
            if (pathItem.getPatch() != null) count++;
            if (pathItem.getHead() != null) count++;
            if (pathItem.getOptions() != null) count++;
            if (pathItem.getTrace() != null) count++;
        }

        return count;
    }
}
