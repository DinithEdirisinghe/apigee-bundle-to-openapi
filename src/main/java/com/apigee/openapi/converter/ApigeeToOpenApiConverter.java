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

import com.apigee.openapi.converter.generator.OpenApiGenerator;
import com.apigee.openapi.converter.model.ApigeeBundle;
import com.apigee.openapi.converter.output.OpenApiWriter;
import com.apigee.openapi.converter.output.OpenApiWriter.OpenApiWriterException;
import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;
import com.apigee.openapi.converter.parser.BundleParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for converting Apigee API proxy bundles to OpenAPI 3.0 specifications.
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Basic conversion from a directory:</h3>
 * <pre>{@code
 * ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
 * ConversionResult result = converter.convert(Path.of("./my-proxy"));
 * String yaml = result.getOpenAPI();
 * }</pre>
 * 
 * <h3>Conversion from a ZIP file with custom options:</h3>
 * <pre>{@code
 * ConversionOptions options = ConversionOptions.builder()
 *     .title("My API")
 *     .version("2.0.0")
 *     .outputFormat(OutputFormat.YAML)
 *     .serverUrl("https://api.example.com/v2")
 *     .build();
 *     
 * ConversionResult result = converter.convert(Path.of("./my-proxy.zip"), options);
 * }</pre>
 * 
 * <h3>Direct conversion to string:</h3>
 * <pre>{@code
 * String openApiYaml = converter.convertToString(
 *     Path.of("./my-proxy"), 
 *     OutputFormat.YAML
 * );
 * }</pre>
 * 
 * <h3>Conversion and save to file:</h3>
 * <pre>{@code
 * converter.convertAndSave(
 *     Path.of("./my-proxy"),
 *     Path.of("./openapi.yaml"),
 *     ConversionOptions.defaults()
 * );
 * }</pre>
 */
public class ApigeeToOpenApiConverter {

    private static final Logger log = LoggerFactory.getLogger(ApigeeToOpenApiConverter.class);

    private final BundleParser bundleParser;
    private final OpenApiGenerator openApiGenerator;
    private final OpenApiWriter openApiWriter;

    /**
     * Creates a new converter instance.
     */
    public ApigeeToOpenApiConverter() {
        this.bundleParser = new BundleParser();
        this.openApiGenerator = new OpenApiGenerator();
        this.openApiWriter = new OpenApiWriter();
    }

    /**
     * Converts an Apigee bundle to OpenAPI specification with default options.
     *
     * @param bundlePath Path to the bundle (ZIP file or directory)
     * @return Conversion result containing the OpenAPI specification
     * @throws ConversionException if conversion fails
     */
    public ConversionResult convert(Path bundlePath) throws ConversionException {
        return convert(bundlePath, ConversionOptions.defaults());
    }

    /**
     * Converts an Apigee bundle to OpenAPI specification with custom options.
     *
     * @param bundlePath Path to the bundle (ZIP file or directory)
     * @param options    Conversion options
     * @return Conversion result containing the OpenAPI specification
     * @throws ConversionException if conversion fails
     */
    public ConversionResult convert(Path bundlePath, ConversionOptions options) throws ConversionException {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        log.info("Starting conversion of: {}", bundlePath);

        try {
            // Parse the bundle
            ApigeeBundle bundle = bundleParser.parse(bundlePath);
            
            if (bundle.getProxyEndpoints().isEmpty()) {
                warnings.add("No proxy endpoints found in bundle");
            }

            // Generate OpenAPI spec
            OpenAPI openAPI = openApiGenerator.generate(bundle, options);

            // Count operations and paths
            int pathCount = openAPI.getPaths() != null ? openAPI.getPaths().size() : 0;
            int operationCount = countOperations(openAPI);

            // Determine if fallback was used
            boolean usedFallback = pathCount == 1 && 
                    openAPI.getPaths() != null && 
                    openAPI.getPaths().containsKey("/");

            if (usedFallback) {
                warnings.add("Used fallback extraction - proxy has no explicit conditional flows");
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Conversion completed in {}ms: {} paths, {} operations", 
                    duration, pathCount, operationCount);

            return ConversionResult.builder()
                    .openAPI(openAPI)
                    .bundleName(bundle.getName())
                    .pathCount(pathCount)
                    .operationCount(operationCount)
                    .usedFallbackExtraction(usedFallback)
                    .warnings(warnings)
                    .conversionTimeMs(duration)
                    .build();

        } catch (IOException e) {
            throw new ConversionException("Failed to read bundle: " + bundlePath, e);
        } catch (Exception e) {
            throw new ConversionException("Conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts an Apigee bundle and returns the OpenAPI spec as a string.
     *
     * @param bundlePath Path to the bundle
     * @param format     Output format (JSON or YAML)
     * @return OpenAPI specification as a string
     * @throws ConversionException if conversion fails
     */
    public String convertToString(Path bundlePath, OutputFormat format) throws ConversionException {
        ConversionOptions options = ConversionOptions.builder()
                .outputFormat(format)
                .build();
        ConversionResult result = convert(bundlePath, options);
        
        try {
            return openApiWriter.writeToString(result.getOpenAPI(), format);
        } catch (OpenApiWriterException e) {
            throw new ConversionException("Failed to serialize OpenAPI spec", e);
        }
    }

    /**
     * Converts an Apigee bundle and saves the result to a file.
     *
     * @param bundlePath Path to the bundle
     * @param outputPath Path for the output file
     * @param options    Conversion options
     * @return Conversion result
     * @throws ConversionException if conversion or writing fails
     */
    public ConversionResult convertAndSave(Path bundlePath, Path outputPath, ConversionOptions options) 
            throws ConversionException {
        ConversionResult result = convert(bundlePath, options);
        
        try {
            OutputFormat format = options.getOutputFormat();
            if (format == null) {
                format = openApiWriter.inferFormat(outputPath);
            }
            openApiWriter.writeToFile(result.getOpenAPI(), outputPath, format);
            return result;
        } catch (OpenApiWriterException e) {
            throw new ConversionException("Failed to write output file: " + outputPath, e);
        }
    }

    /**
     * Converts an Apigee bundle and saves the result to a file, inferring format from extension.
     *
     * @param bundlePath Path to the bundle
     * @param outputPath Path for the output file
     * @return Conversion result
     * @throws ConversionException if conversion or writing fails
     */
    public ConversionResult convertAndSave(Path bundlePath, Path outputPath) throws ConversionException {
        return convertAndSave(bundlePath, outputPath, ConversionOptions.defaults());
    }

    /**
     * Parses an Apigee bundle without generating OpenAPI spec.
     * Useful for inspection or custom processing.
     *
     * @param bundlePath Path to the bundle
     * @return Parsed ApigeeBundle
     * @throws ConversionException if parsing fails
     */
    public ApigeeBundle parseBundle(Path bundlePath) throws ConversionException {
        try {
            return bundleParser.parse(bundlePath);
        } catch (IOException e) {
            throw new ConversionException("Failed to parse bundle: " + bundlePath, e);
        }
    }

    /**
     * Generates OpenAPI spec from an already-parsed bundle.
     * Useful when you want to parse once and generate multiple specs.
     *
     * @param bundle  The parsed bundle
     * @param options Conversion options
     * @return Generated OpenAPI specification
     */
    public OpenAPI generateOpenApi(ApigeeBundle bundle, ConversionOptions options) {
        return openApiGenerator.generate(bundle, options);
    }

    /**
     * Writes an OpenAPI specification to a string.
     *
     * @param openAPI The OpenAPI specification
     * @param format  Output format
     * @return Serialized specification
     * @throws ConversionException if writing fails
     */
    public String writeToString(OpenAPI openAPI, OutputFormat format) throws ConversionException {
        try {
            return openApiWriter.writeToString(openAPI, format);
        } catch (OpenApiWriterException e) {
            throw new ConversionException("Failed to serialize OpenAPI spec", e);
        }
    }

    /**
     * Writes an OpenAPI specification to a file.
     *
     * @param openAPI The OpenAPI specification
     * @param path    Output file path
     * @param format  Output format
     * @throws ConversionException if writing fails
     */
    public void writeToFile(OpenAPI openAPI, Path path, OutputFormat format) throws ConversionException {
        try {
            openApiWriter.writeToFile(openAPI, path, format);
        } catch (OpenApiWriterException e) {
            throw new ConversionException("Failed to write file: " + path, e);
        }
    }

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

    /**
     * Exception thrown when conversion fails.
     */
    public static class ConversionException extends Exception {
        public ConversionException(String message) {
            super(message);
        }

        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
