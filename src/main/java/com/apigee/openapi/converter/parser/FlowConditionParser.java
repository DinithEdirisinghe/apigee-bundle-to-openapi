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
package com.apigee.openapi.converter.parser;

import com.apigee.openapi.converter.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Apigee flow condition strings.
 * Extracts path patterns and HTTP methods from flow conditions.
 */
public class FlowConditionParser {

    private static final Logger log = LoggerFactory.getLogger(FlowConditionParser.class);

    // Pattern for MatchesPath conditions: proxy.pathsuffix MatchesPath "/path/{param}"
    private static final Pattern MATCHES_PATH_PATTERN = Pattern.compile(
            "proxy\\.pathsuffix\\s+MatchesPath\\s+[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for simple path match: proxy.pathsuffix = "/path"
    private static final Pattern SIMPLE_PATH_PATTERN = Pattern.compile(
            "proxy\\.pathsuffix\\s*(?:=|==|Equals)\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for Like conditions: proxy.pathsuffix Like "/path/*"
    private static final Pattern LIKE_PATH_PATTERN = Pattern.compile(
            "proxy\\.pathsuffix\\s+(?:Like|JavaRegex|Matches)\\s+[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern for HTTP method: request.verb = "GET"
    private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile(
            "request\\.verb\\s*(?:=|==|Equals)\\s*[\"']?(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS|TRACE|CONNECT)[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    // Alternative method patterns
    private static final Pattern HTTP_METHOD_ALT_PATTERN = Pattern.compile(
            "request\\.verb\\s+(?:Equals|=)\\s+[\"']?(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS|TRACE|CONNECT)[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Parses a flow condition and extracts path pattern and HTTP method into the flow.
     *
     * @param condition The flow condition string
     * @param flow The flow to update with parsed values
     */
    public void parseCondition(String condition, Flow flow) {
        if (condition == null || condition.trim().isEmpty()) {
            return;
        }

        // Decode HTML entities
        String decodedCondition = decodeHtmlEntities(condition);

        // Extract path pattern
        String pathPattern = extractPathPattern(decodedCondition);
        if (pathPattern != null) {
            flow.setPathPattern(pathPattern);
        }

        // Extract HTTP method
        String httpMethod = extractHttpMethod(decodedCondition);
        if (httpMethod != null) {
            flow.setHttpMethod(httpMethod.toUpperCase());
        }

        log.debug("Parsed condition: path={}, method={}", pathPattern, httpMethod);
    }

    /**
     * Extracts the path pattern from a condition string.
     */
    public String extractPathPattern(String condition) {
        if (condition == null) {
            return null;
        }

        // Try MatchesPath pattern first (most specific)
        Matcher matchesPathMatcher = MATCHES_PATH_PATTERN.matcher(condition);
        if (matchesPathMatcher.find()) {
            return matchesPathMatcher.group(1);
        }

        // Try simple path pattern
        Matcher simplePathMatcher = SIMPLE_PATH_PATTERN.matcher(condition);
        if (simplePathMatcher.find()) {
            return simplePathMatcher.group(1);
        }

        // Try Like pattern
        Matcher likePathMatcher = LIKE_PATH_PATTERN.matcher(condition);
        if (likePathMatcher.find()) {
            String pattern = likePathMatcher.group(1);
            // Convert wildcard pattern to OpenAPI style
            return convertWildcardPattern(pattern);
        }

        return null;
    }

    /**
     * Extracts the HTTP method from a condition string.
     */
    public String extractHttpMethod(String condition) {
        if (condition == null) {
            return null;
        }

        Matcher methodMatcher = HTTP_METHOD_PATTERN.matcher(condition);
        if (methodMatcher.find()) {
            return methodMatcher.group(1).toUpperCase();
        }

        Matcher altMethodMatcher = HTTP_METHOD_ALT_PATTERN.matcher(condition);
        if (altMethodMatcher.find()) {
            return altMethodMatcher.group(1).toUpperCase();
        }

        return null;
    }

    /**
     * Converts wildcard patterns to OpenAPI-style path patterns.
     * For example: /api/* becomes /api/{param}
     */
    private String convertWildcardPattern(String pattern) {
        if (pattern == null) {
            return null;
        }

        // Convert ** to {segments}
        pattern = pattern.replace("**", "{segments}");
        
        // Convert * to {param} with unique naming
        int paramCount = 0;
        while (pattern.contains("*")) {
            String paramName = paramCount == 0 ? "param" : "param" + paramCount;
            pattern = pattern.replaceFirst("\\*", "{" + paramName + "}");
            paramCount++;
        }

        return pattern;
    }

    /**
     * Decodes HTML entities in condition strings.
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    /**
     * Checks if a condition is a path-based condition.
     */
    public boolean isPathBasedCondition(String condition) {
        if (condition == null) {
            return false;
        }
        String decoded = decodeHtmlEntities(condition);
        return decoded.contains("proxy.pathsuffix") || 
               decoded.contains("request.path");
    }

    /**
     * Checks if a condition includes an HTTP method constraint.
     */
    public boolean hasMethodConstraint(String condition) {
        if (condition == null) {
            return false;
        }
        String decoded = decodeHtmlEntities(condition);
        return decoded.contains("request.verb");
    }
}
