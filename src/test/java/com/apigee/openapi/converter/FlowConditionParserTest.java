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

import com.apigee.openapi.converter.parser.FlowConditionParser;
import com.apigee.openapi.converter.model.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FlowConditionParser.
 */
class FlowConditionParserTest {

    private FlowConditionParser parser;

    @BeforeEach
    void setUp() {
        parser = new FlowConditionParser();
    }

    @Test
    void extractPathPattern_withMatchesPath() {
        // Given
        String condition = "(proxy.pathsuffix MatchesPath \"/pets/{petId}\") and (request.verb = \"GET\")";

        // When
        String path = parser.extractPathPattern(condition);

        // Then
        assertThat(path).isEqualTo("/pets/{petId}");
    }

    @Test
    void extractPathPattern_withSimpleEquals() {
        // Given
        String condition = "proxy.pathsuffix = \"/status\"";

        // When
        String path = parser.extractPathPattern(condition);

        // Then
        assertThat(path).isEqualTo("/status");
    }

    @Test
    void extractHttpMethod_withEquals() {
        // Given
        String condition = "(proxy.pathsuffix MatchesPath \"/pets\") and (request.verb = \"POST\")";

        // When
        String method = parser.extractHttpMethod(condition);

        // Then
        assertThat(method).isEqualTo("POST");
    }

    @Test
    void extractHttpMethod_caseInsensitive() {
        // Given
        String condition = "request.verb = \"get\"";

        // When
        String method = parser.extractHttpMethod(condition);

        // Then
        assertThat(method).isEqualTo("GET");
    }

    @Test
    void parseCondition_shouldUpdateFlow() {
        // Given
        String condition = "(proxy.pathsuffix MatchesPath \"/users/{userId}\") and (request.verb = \"DELETE\")";
        Flow flow = new Flow();

        // When
        parser.parseCondition(condition, flow);

        // Then
        assertThat(flow.getPathPattern()).isEqualTo("/users/{userId}");
        assertThat(flow.getHttpMethod()).isEqualTo("DELETE");
    }

    @Test
    void parseCondition_withHtmlEntities() {
        // Given
        String condition = "(proxy.pathsuffix MatchesPath &quot;/pets&quot;) and (request.verb = &quot;GET&quot;)";
        Flow flow = new Flow();

        // When
        parser.parseCondition(condition, flow);

        // Then
        assertThat(flow.getPathPattern()).isEqualTo("/pets");
        assertThat(flow.getHttpMethod()).isEqualTo("GET");
    }

    @Test
    void isPathBasedCondition_shouldReturnTrue() {
        assertThat(parser.isPathBasedCondition("proxy.pathsuffix = \"/test\"")).isTrue();
        assertThat(parser.isPathBasedCondition("proxy.pathsuffix MatchesPath \"/test\"")).isTrue();
    }

    @Test
    void isPathBasedCondition_shouldReturnFalse() {
        assertThat(parser.isPathBasedCondition("request.verb = \"GET\"")).isFalse();
        assertThat(parser.isPathBasedCondition(null)).isFalse();
    }

    @Test
    void hasMethodConstraint_shouldReturnTrue() {
        assertThat(parser.hasMethodConstraint("request.verb = \"GET\"")).isTrue();
        assertThat(parser.hasMethodConstraint("(path) and (request.verb = \"POST\")")).isTrue();
    }

    @Test
    void hasMethodConstraint_shouldReturnFalse() {
        assertThat(parser.hasMethodConstraint("proxy.pathsuffix = \"/test\"")).isFalse();
        assertThat(parser.hasMethodConstraint(null)).isFalse();
    }

    @Test
    void extractPathPattern_withLikeOperator() {
        // Given
        String condition = "proxy.pathsuffix Like \"/api/*\"";

        // When
        String path = parser.extractPathPattern(condition);

        // Then
        assertThat(path).isEqualTo("/api/{param}");
    }

    @Test
    void extractHttpMethod_allMethods() {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};
        
        for (String method : methods) {
            String condition = "request.verb = \"" + method + "\"";
            assertThat(parser.extractHttpMethod(condition)).isEqualTo(method);
        }
    }
}
