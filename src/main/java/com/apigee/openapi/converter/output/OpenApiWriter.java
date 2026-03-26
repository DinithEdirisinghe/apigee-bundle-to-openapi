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
package com.apigee.openapi.converter.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes OpenAPI specifications to various output formats (JSON, YAML) and destinations.
 */
public class OpenApiWriter {

    private static final Logger log = LoggerFactory.getLogger(OpenApiWriter.class);

    /**
     * Output format for the OpenAPI specification.
     */
    public enum OutputFormat {
        JSON,
        YAML
    }

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public OpenApiWriter() {
        // Configure JSON mapper
        this.jsonMapper = Json.mapper().copy();
        configureMapper(this.jsonMapper);

        // Configure YAML mapper
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES);
        this.yamlMapper = new ObjectMapper(yamlFactory);
        configureMapper(this.yamlMapper);
    }

    private void configureMapper(ObjectMapper mapper) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * Writes the OpenAPI specification to a string.
     *
     * @param openAPI The OpenAPI specification to write
     * @param format  The output format (JSON or YAML)
     * @return The serialized specification
     * @throws OpenApiWriterException if serialization fails
     */
    public String writeToString(OpenAPI openAPI, OutputFormat format) throws OpenApiWriterException {
        try {
            switch (format) {
                case JSON:
                    return Json.pretty(openAPI);
                case YAML:
                    return Yaml.pretty(openAPI);
                default:
                    throw new OpenApiWriterException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            throw new OpenApiWriterException("Failed to serialize OpenAPI specification", e);
        }
    }

    /**
     * Writes the OpenAPI specification to a file.
     *
     * @param openAPI The OpenAPI specification to write
     * @param path    The output file path
     * @param format  The output format (JSON or YAML)
     * @throws OpenApiWriterException if writing fails
     */
    public void writeToFile(OpenAPI openAPI, Path path, OutputFormat format) throws OpenApiWriterException {
        String content = writeToString(openAPI, format);
        
        try {
            // Create parent directories if they don't exist
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("Wrote OpenAPI specification to: {}", path);
        } catch (IOException e) {
            throw new OpenApiWriterException("Failed to write to file: " + path, e);
        }
    }

    /**
     * Writes the OpenAPI specification to a file, inferring format from file extension.
     *
     * @param openAPI The OpenAPI specification to write
     * @param path    The output file path
     * @throws OpenApiWriterException if writing fails
     */
    public void writeToFile(OpenAPI openAPI, Path path) throws OpenApiWriterException {
        OutputFormat format = inferFormat(path);
        writeToFile(openAPI, path, format);
    }

    /**
     * Writes the OpenAPI specification to an output stream.
     *
     * @param openAPI      The OpenAPI specification to write
     * @param outputStream The output stream
     * @param format       The output format (JSON or YAML)
     * @throws OpenApiWriterException if writing fails
     */
    public void writeToStream(OpenAPI openAPI, OutputStream outputStream, OutputFormat format) 
            throws OpenApiWriterException {
        String content = writeToString(openAPI, format);
        
        try {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new OpenApiWriterException("Failed to write to output stream", e);
        }
    }

    /**
     * Writes the OpenAPI specification to a writer.
     *
     * @param openAPI The OpenAPI specification to write
     * @param writer  The writer
     * @param format  The output format (JSON or YAML)
     * @throws OpenApiWriterException if writing fails
     */
    public void writeToWriter(OpenAPI openAPI, Writer writer, OutputFormat format) 
            throws OpenApiWriterException {
        String content = writeToString(openAPI, format);
        
        try {
            writer.write(content);
        } catch (IOException e) {
            throw new OpenApiWriterException("Failed to write to writer", e);
        }
    }

    /**
     * Infers the output format from a file path based on extension.
     *
     * @param path The file path
     * @return The inferred format
     */
    public OutputFormat inferFormat(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return OutputFormat.YAML;
        }
        return OutputFormat.JSON;
    }

    /**
     * Infers the output format from a file name based on extension.
     *
     * @param fileName The file name
     * @return The inferred format
     */
    public OutputFormat inferFormat(String fileName) {
        if (fileName == null) {
            return OutputFormat.JSON;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return OutputFormat.YAML;
        }
        return OutputFormat.JSON;
    }

    /**
     * Returns the appropriate file extension for a format.
     *
     * @param format The output format
     * @return The file extension (including the dot)
     */
    public String getExtension(OutputFormat format) {
        switch (format) {
            case YAML:
                return ".yaml";
            case JSON:
            default:
                return ".json";
        }
    }

    /**
     * Exception thrown when writing fails.
     */
    public static class OpenApiWriterException extends Exception {
        public OpenApiWriterException(String message) {
            super(message);
        }

        public OpenApiWriterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
