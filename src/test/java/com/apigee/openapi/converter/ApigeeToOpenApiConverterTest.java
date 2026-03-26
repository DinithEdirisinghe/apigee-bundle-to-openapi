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
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ApigeeToOpenApiConverter.
 */
class ApigeeToOpenApiConverterTest {

    private ApigeeToOpenApiConverter converter;
    private Path sampleBundlePath;

    @BeforeEach
    void setUp() {
        converter = new ApigeeToOpenApiConverter();
        sampleBundlePath = Paths.get("src/test/resources/sample-bundles/petstore");
    }

    @Test
    void convertBasicBundle_shouldReturnValidResult() throws Exception {
        // Given
        assertThat(Files.exists(sampleBundlePath)).isTrue();

        // When
        ConversionResult result = converter.convert(sampleBundlePath);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getBundleName()).isEqualTo("petstore");
        assertThat(result.getPathCount()).isGreaterThan(0);
        assertThat(result.getOperationCount()).isGreaterThan(0);
    }

    @Test
    void convertBundle_shouldExtractCorrectPaths() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        assertThat(openAPI.getPaths()).isNotNull();
        assertThat(openAPI.getPaths()).containsKey("/pets");
        assertThat(openAPI.getPaths()).containsKey("/pets/{petId}");
    }

    @Test
    void convertBundle_shouldExtractHttpMethods() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        PathItem petsPath = openAPI.getPaths().get("/pets");
        assertThat(petsPath).isNotNull();
        assertThat(petsPath.getGet()).isNotNull();
        assertThat(petsPath.getPost()).isNotNull();

        PathItem petByIdPath = openAPI.getPaths().get("/pets/{petId}");
        assertThat(petByIdPath).isNotNull();
        assertThat(petByIdPath.getGet()).isNotNull();
        assertThat(petByIdPath.getPut()).isNotNull();
        assertThat(petByIdPath.getDelete()).isNotNull();
    }

    @Test
    void convertBundle_shouldExtractSecuritySchemes() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("apiKey");
    }

    @Test
    void convertBundle_shouldSetCorrectOpenApiVersion() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        assertThat(openAPI.getOpenapi()).startsWith("3.0");
    }

    @Test
    void convertBundle_shouldIncludeServers() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        assertThat(openAPI.getServers()).isNotNull();
        assertThat(openAPI.getServers()).isNotEmpty();
    }

    @Test
    void convertWithCustomOptions_shouldApplyTitle() throws Exception {
        // Given
        ConversionOptions options = ConversionOptions.builder()
                .title("My Custom API")
                .version("2.0.0")
                .description("Custom description")
                .build();

        // When
        ConversionResult result = converter.convert(sampleBundlePath, options);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("My Custom API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("Custom description");
    }

    @Test
    void convertToString_shouldReturnJson() throws Exception {
        // When
        String json = converter.convertToString(sampleBundlePath, OutputFormat.JSON);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"openapi\"");
        assertThat(json).contains("\"3.0");
        assertThat(json).contains("\"paths\"");
    }

    @Test
    void convertToString_shouldReturnYaml() throws Exception {
        // When
        String yaml = converter.convertToString(sampleBundlePath, OutputFormat.YAML);

        // Then
        assertThat(yaml).isNotNull();
        assertThat(yaml).contains("openapi:");
        assertThat(yaml).contains("paths:");
    }

    @Test
    void convertAndSave_shouldWriteToFile(@TempDir Path tempDir) throws Exception {
        // Given
        Path outputPath = tempDir.resolve("output.yaml");

        // When
        ConversionResult result = converter.convertAndSave(sampleBundlePath, outputPath);

        // Then
        assertThat(result.isSuccessful()).isTrue();
        assertThat(Files.exists(outputPath)).isTrue();
        
        String content = Files.readString(outputPath);
        assertThat(content).contains("openapi:");
        assertThat(content).contains("/pets");
    }

    @Test
    void convertAndSave_shouldWriteJsonWhenSpecified(@TempDir Path tempDir) throws Exception {
        // Given
        Path outputPath = tempDir.resolve("output.json");
        ConversionOptions options = ConversionOptions.builder()
                .outputFormat(OutputFormat.JSON)
                .build();

        // When
        converter.convertAndSave(sampleBundlePath, outputPath, options);

        // Then
        String content = Files.readString(outputPath);
        assertThat(content).contains("\"openapi\"");
    }

    @Test
    void parseBundle_shouldReturnValidBundle() throws Exception {
        // When
        var bundle = converter.parseBundle(sampleBundlePath);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getName()).isEqualTo("petstore");
        assertThat(bundle.getProxyEndpoints()).isNotEmpty();
        assertThat(bundle.getTargetEndpoints()).isNotEmpty();
        assertThat(bundle.getPolicies()).isNotEmpty();
    }

    @Test
    void conversionResult_shouldIncludeConversionTime() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);

        // Then
        assertThat(result.getConversionTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void operations_shouldHaveOperationIds() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        openAPI.getPaths().forEach((path, pathItem) -> {
            if (pathItem.getGet() != null) {
                assertThat(pathItem.getGet().getOperationId()).isNotNull();
            }
            if (pathItem.getPost() != null) {
                assertThat(pathItem.getPost().getOperationId()).isNotNull();
            }
        });
    }

    @Test
    void operations_shouldHaveResponses() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        openAPI.getPaths().forEach((path, pathItem) -> {
            if (pathItem.getGet() != null) {
                assertThat(pathItem.getGet().getResponses()).isNotNull();
                assertThat(pathItem.getGet().getResponses()).containsKey("200");
            }
        });
    }

    @Test
    void pathParameters_shouldBeExtracted() throws Exception {
        // When
        ConversionResult result = converter.convert(sampleBundlePath);
        OpenAPI openAPI = result.getOpenAPI();

        // Then
        PathItem petByIdPath = openAPI.getPaths().get("/pets/{petId}");
        assertThat(petByIdPath).isNotNull();
        
        // Check that GET operation has petId parameter
        if (petByIdPath.getGet() != null && petByIdPath.getGet().getParameters() != null) {
            boolean hasPetIdParam = petByIdPath.getGet().getParameters().stream()
                    .anyMatch(p -> "petId".equals(p.getName()));
            assertThat(hasPetIdParam).isTrue();
        }
    }
}
