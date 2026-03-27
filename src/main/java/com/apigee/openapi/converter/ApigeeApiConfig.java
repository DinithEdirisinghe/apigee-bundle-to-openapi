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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

/**
 * Configuration for connecting to the Apigee Management API.
 * 
 * <p>Supports both Apigee X/hybrid (Google Cloud) and Apigee Edge configurations.
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Using service account JSON file path:</h3>
 * <pre>{@code
 * ApigeeApiConfig config = ApigeeApiConfig.builder()
 *     .organization("my-org")
 *     .serviceAccountKeyPath(Path.of("/path/to/service-account.json"))
 *     .build();
 * }</pre>
 * 
 * <h3>Using service account JSON string:</h3>
 * <pre>{@code
 * ApigeeApiConfig config = ApigeeApiConfig.builder()
 *     .organization("my-org")
 *     .serviceAccountKeyJson(jsonString)
 *     .build();
 * }</pre>
 * 
 * <h3>Using service account JSON from environment variable:</h3>
 * <pre>{@code
 * // Reads from APIGEE_SERVICE_ACCOUNT_JSON by default
 * ApigeeApiConfig config = ApigeeApiConfig.builder()
 *     .organization("my-org")
 *     .serviceAccountKeyFromEnv()
 *     .build();
 * 
 * // Or specify a custom environment variable
 * ApigeeApiConfig config = ApigeeApiConfig.builder()
 *     .organization("my-org")
 *     .serviceAccountKeyFromEnv("MY_CUSTOM_ENV_VAR")
 *     .build();
 * }</pre>
 * 
 * <h3>Using pre-created GoogleCredentials:</h3>
 * <pre>{@code
 * GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
 * ApigeeApiConfig config = ApigeeApiConfig.builder()
 *     .organization("my-org")
 *     .credentials(creds)
 *     .build();
 * }</pre>
 */
public class ApigeeApiConfig {

    private static final String DEFAULT_APIGEE_API_BASE_URL = "https://apigee.googleapis.com/v1";
    private static final String APIGEE_API_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private final String organization;
    private final String baseUrl;
    private final GoogleCredentials credentials;
    private final String proxyHostname;

    private ApigeeApiConfig(Builder builder) {
        this.organization = Objects.requireNonNull(builder.organization, "organization is required");
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : DEFAULT_APIGEE_API_BASE_URL;
        this.credentials = Objects.requireNonNull(builder.credentials, "credentials are required");
        this.proxyHostname = builder.proxyHostname;
    }

    /**
     * Returns the Apigee organization name
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Returns the base URL for the Apigee Management API.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the Google credentials for authentication.
     */
    public GoogleCredentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the Apigee proxy hostname (e.g., "10.43.65.12.nip.io").
     * This is used to construct the server URL in the generated OpenAPI spec.
     * May be null if not set.
     */
    public String getProxyHostname() {
        return proxyHostname;
    }

    /**
     * Returns an access token for API authentication.
     * The token is automatically refreshed if expired.
     *
     * @return Access token string
     * @throws IOException if token retrieval fails
     */
    public String getAccessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    /**
     * Creates a new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ApigeeApiConfig.
     */
    public static class Builder {
        private String organization;
        private String baseUrl;
        private GoogleCredentials credentials;
        private String proxyHostname;

        private Builder() {
        }

        /**
         * Sets the Apigee organization name.
         * This is required.
         *
         * @param organization The organization name (e.g., "my-gcp-project" for Apigee X)
         * @return this builder
         */
        public Builder organization(String organization) {
            this.organization = organization;
            return this;
        }

        /**
         * Sets the Apigee proxy hostname for the generated OpenAPI spec.
         * This is the hostname where your Apigee gateway is accessible
         * (e.g., "10.43.65.12.nip.io" or "api.mycompany.com").
         * 
         * The server URL in the generated spec will be: https://{proxyHostname}{basePath}
         *
         * @param proxyHostname The Apigee proxy gateway hostname
         * @return this builder
         */
        public Builder proxyHostname(String proxyHostname) {
            this.proxyHostname = proxyHostname;
            return this;
        }

        /**
         * Sets a custom base URL for the Apigee Management API.
         * Defaults to "https://apigee.googleapis.com/v1" for Apigee X/hybrid.
         *
         * @param baseUrl The base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets credentials from a service account JSON key file path.
         *
         * @param keyPath Path to the service account JSON key file
         * @return this builder
         * @throws IOException if the file cannot be read
         */
        public Builder serviceAccountKeyPath(Path keyPath) throws IOException {
            try (InputStream is = Files.newInputStream(keyPath)) {
                this.credentials = ServiceAccountCredentials.fromStream(is)
                        .createScoped(Collections.singletonList(APIGEE_API_SCOPE));
            }
            return this;
        }

        /**
         * Sets credentials from a service account JSON key file path string.
         *
         * @param keyPath Path to the service account JSON key file
         * @return this builder
         * @throws IOException if the file cannot be read
         */
        public Builder serviceAccountKeyPath(String keyPath) throws IOException {
            return serviceAccountKeyPath(Path.of(keyPath));
        }

        /**
         * Sets credentials from a service account JSON key string.
         *
         * @param jsonKey The JSON key content as a string
         * @return this builder
         * @throws IOException if parsing fails
         */
        public Builder serviceAccountKeyJson(String jsonKey) throws IOException {
            try (InputStream is = new ByteArrayInputStream(jsonKey.getBytes(StandardCharsets.UTF_8))) {
                this.credentials = ServiceAccountCredentials.fromStream(is)
                        .createScoped(Collections.singletonList(APIGEE_API_SCOPE));
            }
            return this;
        }

        /**
         * Sets credentials from an InputStream containing service account JSON.
         *
         * @param jsonStream InputStream containing the JSON key
         * @return this builder
         * @throws IOException if parsing fails
         */
        public Builder serviceAccountKeyStream(InputStream jsonStream) throws IOException {
            this.credentials = ServiceAccountCredentials.fromStream(jsonStream)
                    .createScoped(Collections.singletonList(APIGEE_API_SCOPE));
            return this;
        }

        /**
         * Sets pre-created GoogleCredentials.
         * Use this for advanced scenarios like Application Default Credentials.
         *
         * @param credentials The GoogleCredentials instance
         * @return this builder
         */
        public Builder credentials(GoogleCredentials credentials) {
            this.credentials = credentials.createScoped(Collections.singletonList(APIGEE_API_SCOPE));
            return this;
        }

        /**
         * Uses Application Default Credentials.
         * This looks for credentials in standard locations:
         * - GOOGLE_APPLICATION_CREDENTIALS environment variable
         * - Cloud SDK credentials
         * - Compute Engine/GKE metadata service
         *
         * @return this builder
         * @throws IOException if credentials cannot be found
         */
        public Builder useApplicationDefaultCredentials() throws IOException {
            this.credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singletonList(APIGEE_API_SCOPE));
            return this;
        }

        /**
         * Sets credentials from an environment variable containing service account JSON.
         * Defaults to "APIGEE_SERVICE_ACCOUNT_JSON" if no variable name is specified.
         *
         * @param envVarName The name of the environment variable
         * @return this builder
         * @throws IOException if the environment variable is not set or parsing fails
         * @throws IllegalArgumentException if the environment variable is not set
         */
        public Builder serviceAccountKeyFromEnv(String envVarName) throws IOException {
            String jsonKey = System.getenv(envVarName);
            if (jsonKey == null || jsonKey.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Environment variable '" + envVarName + "' is not set or empty. " +
                    "Please set it with: export " + envVarName + "='<your-service-account-json>'"
                );
            }
            return serviceAccountKeyJson(jsonKey);
        }

        /**
         * Sets credentials from the APIGEE_SERVICE_ACCOUNT_JSON environment variable.
         * This is a convenience method for serviceAccountKeyFromEnv("APIGEE_SERVICE_ACCOUNT_JSON").
         *
         * @return this builder
         * @throws IOException if the environment variable is not set or parsing fails
         * @throws IllegalArgumentException if the environment variable is not set
         */
        public Builder serviceAccountKeyFromEnv() throws IOException {
            return serviceAccountKeyFromEnv("APIGEE_SERVICE_ACCOUNT_JSON");
        }

        /**
         * Builds the ApigeeApiConfig instance.
         *
         * @return The configured ApigeeApiConfig
         * @throws NullPointerException if required fields are missing
         */
        public ApigeeApiConfig build() {
            return new ApigeeApiConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ApigeeApiConfig{" +
                "organization='" + organization + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }
}
