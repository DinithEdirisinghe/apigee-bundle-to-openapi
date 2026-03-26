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

import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;

/**
 * Configuration options for the Apigee to OpenAPI conversion.
 * Use the builder pattern to create instances.
 */
public class ConversionOptions {

    // Output settings (null means infer from file extension)
    private OutputFormat outputFormat = null;
    
    // Info section
    private String title;
    private String version;
    private String description;
    
    // Contact info
    private String contactName;
    private String contactEmail;
    private String contactUrl;
    
    // License info
    private String licenseName;
    private String licenseUrl;
    
    // Server info
    private String serverUrl;
    private String proxyHostname;
    private boolean useBackendUrl = false;  // Default: do NOT use backend URL
    
    // Conversion behavior
    private boolean includeDefaultResponses = true;
    private boolean includeSecuritySchemes = true;
    private boolean generateOperationIds = true;
    private boolean useFallbackExtraction = true;
    
    private ConversionOptions() {}

    /**
     * Creates a new builder for ConversionOptions.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates ConversionOptions with default settings.
     */
    public static ConversionOptions defaults() {
        return new Builder().build();
    }

    // Getters

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactUrl() {
        return contactUrl;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProxyHostname() {
        return proxyHostname;
    }

    public boolean isUseBackendUrl() {
        return useBackendUrl;
    }

    public boolean isIncludeDefaultResponses() {
        return includeDefaultResponses;
    }

    public boolean isIncludeSecuritySchemes() {
        return includeSecuritySchemes;
    }

    public boolean isGenerateOperationIds() {
        return generateOperationIds;
    }

    public boolean isUseFallbackExtraction() {
        return useFallbackExtraction;
    }

    /**
     * Builder for ConversionOptions.
     */
    public static class Builder {
        private final ConversionOptions options = new ConversionOptions();

        /**
         * Sets the output format (JSON or YAML).
         */
        public Builder outputFormat(OutputFormat format) {
            options.outputFormat = format;
            return this;
        }

        /**
         * Sets the API title.
         */
        public Builder title(String title) {
            options.title = title;
            return this;
        }

        /**
         * Sets the API version.
         */
        public Builder version(String version) {
            options.version = version;
            return this;
        }

        /**
         * Sets the API description.
         */
        public Builder description(String description) {
            options.description = description;
            return this;
        }

        /**
         * Sets the contact name.
         */
        public Builder contactName(String contactName) {
            options.contactName = contactName;
            return this;
        }

        /**
         * Sets the contact email.
         */
        public Builder contactEmail(String contactEmail) {
            options.contactEmail = contactEmail;
            return this;
        }

        /**
         * Sets the contact URL.
         */
        public Builder contactUrl(String contactUrl) {
            options.contactUrl = contactUrl;
            return this;
        }

        /**
         * Sets the license name.
         */
        public Builder licenseName(String licenseName) {
            options.licenseName = licenseName;
            return this;
        }

        /**
         * Sets the license URL.
         */
        public Builder licenseUrl(String licenseUrl) {
            options.licenseUrl = licenseUrl;
            return this;
        }

        /**
         * Sets the server URL.
         * This takes precedence over proxyHostname.
         */
        public Builder serverUrl(String serverUrl) {
            options.serverUrl = serverUrl;
            return this;
        }

        /**
         * Sets the Apigee proxy hostname (e.g., "10.43.65.12.nip.io" or "api.example.com").
         * The final server URL will be: https://{proxyHostname}{basePath}
         * This is useful when you want to generate a spec that points to the Apigee gateway
         * rather than the backend.
         */
        public Builder proxyHostname(String proxyHostname) {
            options.proxyHostname = proxyHostname;
            return this;
        }

        /**
         * Sets whether to use the backend/target URL as the server URL.
         * Default is FALSE - backend URLs are typically private/internal.
         * Only set to true if your backend is publicly accessible.
         */
        public Builder useBackendUrl(boolean useBackendUrl) {
            options.useBackendUrl = useBackendUrl;
            return this;
        }

        /**
         * Sets whether to include default responses for operations.
         */
        public Builder includeDefaultResponses(boolean include) {
            options.includeDefaultResponses = include;
            return this;
        }

        /**
         * Sets whether to include security schemes.
         */
        public Builder includeSecuritySchemes(boolean include) {
            options.includeSecuritySchemes = include;
            return this;
        }

        /**
         * Sets whether to generate operation IDs.
         */
        public Builder generateOperationIds(boolean generate) {
            options.generateOperationIds = generate;
            return this;
        }

        /**
         * Sets whether to use fallback extraction for proxies without conditional flows.
         */
        public Builder useFallbackExtraction(boolean use) {
            options.useFallbackExtraction = use;
            return this;
        }

        /**
         * Builds the ConversionOptions.
         */
        public ConversionOptions build() {
            return options;
        }
    }

    @Override
    public String toString() {
        return "ConversionOptions{" +
                "outputFormat=" + outputFormat +
                ", title='" + title + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
