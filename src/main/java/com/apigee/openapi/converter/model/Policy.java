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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class representing an Apigee Policy.
 * Subclasses represent specific policy types with their configurations.
 */
public class Policy {

    /**
     * Supported policy types for extraction.
     */
    public enum PolicyType {
        EXTRACT_VARIABLES,
        ASSIGN_MESSAGE,
        VERIFY_API_KEY,
        OAUTH_V2,
        BASIC_AUTH,
        QUOTA,
        SPIKE_ARREST,
        RESPONSE_CACHE,
        RAISE_FAULT,
        JAVASCRIPT,
        SERVICE_CALLOUT,
        CORS,
        XML_TO_JSON,
        JSON_TO_XML,
        REGEX_PROTECTION,
        UNKNOWN
    }

    private String name;
    private String displayName;
    private PolicyType type = PolicyType.UNKNOWN;
    private boolean enabled = true;
    private boolean continueOnError = false;
    
    // Generic storage for extracted configuration
    private final Map<String, Object> configuration = new HashMap<>();
    
    // Extracted parameters from policy
    private final List<ParameterInfo> extractedParameters = new ArrayList<>();

    public Policy() {}

    public Policy(String name, PolicyType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(PolicyType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfigValue(String key, Object value) {
        this.configuration.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, Class<T> type) {
        Object value = configuration.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public List<ParameterInfo> getExtractedParameters() {
        return extractedParameters;
    }

    public void addExtractedParameter(ParameterInfo parameter) {
        this.extractedParameters.add(parameter);
    }

    /**
     * Checks if this policy is a security-related policy.
     */
    public boolean isSecurityPolicy() {
        return type == PolicyType.VERIFY_API_KEY ||
               type == PolicyType.OAUTH_V2 ||
               type == PolicyType.BASIC_AUTH;
    }

    /**
     * Checks if this policy extracts variables.
     */
    public boolean isExtractVariablesPolicy() {
        return type == PolicyType.EXTRACT_VARIABLES;
    }

    /**
     * Checks if this policy modifies messages (headers, body).
     */
    public boolean isMessageModificationPolicy() {
        return type == PolicyType.ASSIGN_MESSAGE ||
               type == PolicyType.CORS ||
               type == PolicyType.XML_TO_JSON ||
               type == PolicyType.JSON_TO_XML;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", enabled=" + enabled +
                '}';
    }

    /**
     * Represents parameter information extracted from a policy.
     */
    public static class ParameterInfo {
        
        public enum Location {
            PATH,
            QUERY,
            HEADER,
            BODY,
            FORM
        }

        private String name;
        private Location location;
        private String variableName;
        private String pattern;
        private boolean required;
        private String dataType;
        private String description;

        public ParameterInfo() {}

        public ParameterInfo(String name, Location location) {
            this.name = name;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public String getVariableName() {
            return variableName;
        }

        public void setVariableName(String variableName) {
            this.variableName = variableName;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "ParameterInfo{" +
                    "name='" + name + '\'' +
                    ", location=" + location +
                    ", required=" + required +
                    '}';
        }
    }
}
