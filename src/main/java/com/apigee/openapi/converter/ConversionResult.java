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

import io.swagger.v3.oas.models.OpenAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of an Apigee to OpenAPI conversion.
 * Contains the generated OpenAPI specification and metadata about the conversion.
 */
public class ConversionResult {

    private final OpenAPI openAPI;
    private final String bundleName;
    private final int operationCount;
    private final int pathCount;
    private final boolean usedFallbackExtraction;
    private final List<String> warnings = new ArrayList<>();
    private final long conversionTimeMs;

    private ConversionResult(Builder builder) {
        this.openAPI = builder.openAPI;
        this.bundleName = builder.bundleName;
        this.operationCount = builder.operationCount;
        this.pathCount = builder.pathCount;
        this.usedFallbackExtraction = builder.usedFallbackExtraction;
        this.warnings.addAll(builder.warnings);
        this.conversionTimeMs = builder.conversionTimeMs;
    }

    /**
     * Creates a new builder for ConversionResult.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the generated OpenAPI specification.
     */
    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    /**
     * Returns the name of the converted bundle.
     */
    public String getBundleName() {
        return bundleName;
    }

    /**
     * Returns the number of operations in the generated spec.
     */
    public int getOperationCount() {
        return operationCount;
    }

    /**
     * Returns the number of unique paths in the generated spec.
     */
    public int getPathCount() {
        return pathCount;
    }

    /**
     * Returns whether fallback extraction was used.
     */
    public boolean isUsedFallbackExtraction() {
        return usedFallbackExtraction;
    }

    /**
     * Returns any warnings generated during conversion.
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Returns whether there were any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns the conversion time in milliseconds.
     */
    public long getConversionTimeMs() {
        return conversionTimeMs;
    }

    /**
     * Returns whether the conversion was successful.
     */
    public boolean isSuccessful() {
        return openAPI != null && pathCount > 0;
    }

    @Override
    public String toString() {
        return "ConversionResult{" +
                "bundleName='" + bundleName + '\'' +
                ", pathCount=" + pathCount +
                ", operationCount=" + operationCount +
                ", usedFallback=" + usedFallbackExtraction +
                ", warnings=" + warnings.size() +
                ", timeMs=" + conversionTimeMs +
                '}';
    }

    /**
     * Builder for ConversionResult.
     */
    public static class Builder {
        private OpenAPI openAPI;
        private String bundleName;
        private int operationCount;
        private int pathCount;
        private boolean usedFallbackExtraction;
        private final List<String> warnings = new ArrayList<>();
        private long conversionTimeMs;

        public Builder openAPI(OpenAPI openAPI) {
            this.openAPI = openAPI;
            return this;
        }

        public Builder bundleName(String bundleName) {
            this.bundleName = bundleName;
            return this;
        }

        public Builder operationCount(int operationCount) {
            this.operationCount = operationCount;
            return this;
        }

        public Builder pathCount(int pathCount) {
            this.pathCount = pathCount;
            return this;
        }

        public Builder usedFallbackExtraction(boolean usedFallbackExtraction) {
            this.usedFallbackExtraction = usedFallbackExtraction;
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings.addAll(warnings);
            return this;
        }

        public Builder conversionTimeMs(long conversionTimeMs) {
            this.conversionTimeMs = conversionTimeMs;
            return this;
        }

        public ConversionResult build() {
            return new ConversionResult(this);
        }
    }
}
