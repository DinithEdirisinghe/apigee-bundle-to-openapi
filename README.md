# Apigee Bundle to OpenAPI Converter

A production-quality Java library that converts Apigee API proxy bundles (ZIP files or extracted directories) into OpenAPI 3.0 specifications.

## Features

- **OpenAPI 3.0 Output**: Generates OpenAPI 3.0.x specifications (not legacy Swagger 2.0)
- **Multiple Output Formats**: Supports both JSON and YAML output
- **Offline Operation**: Works entirely from local files - no Apigee connection required
- **Smart Path Extraction**: Extracts paths from conditional flows with fallback strategies
- **Parameter Inference**: Extracts query parameters, headers, and path parameters from policy XMLs
- **Security Detection**: Automatically detects API Key, OAuth2, and Basic Auth from policies
- **Java API**: Designed for programmatic use as a library

## Installation

### Maven

```xml
<dependency>
    <groupId>com.apigee.openapi</groupId>
    <artifactId>apigee-bundle-to-openapi</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.apigee.openapi:apigee-bundle-to-openapi:1.0.0'
```

## Quick Start

### Basic Conversion

```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import com.apigee.openapi.converter.ConversionResult;
import java.nio.file.Path;

// Create converter
ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();

// Convert from directory or ZIP file
ConversionResult result = converter.convert(Path.of("./my-proxy"));

// Get the OpenAPI specification
OpenAPI openAPI = result.getOpenAPI();
System.out.println("Converted " + result.getPathCount() + " paths");
```

### Convert to YAML String

```java
import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;

String yaml = converter.convertToString(
    Path.of("./my-proxy"), 
    OutputFormat.YAML
);
System.out.println(yaml);
```

### Convert and Save to File

```java
converter.convertAndSave(
    Path.of("./my-proxy"),
    Path.of("./openapi.yaml")
);
```

### Custom Options

```java
import com.apigee.openapi.converter.ConversionOptions;

ConversionOptions options = ConversionOptions.builder()
    .title("My API")
    .version("2.0.0")
    .description("API converted from Apigee proxy")
    .serverUrl("https://api.example.com/v2")
    .contactName("API Team")
    .contactEmail("api@example.com")
    .licenseName("Apache 2.0")
    .outputFormat(OutputFormat.YAML)
    .build();

ConversionResult result = converter.convert(Path.of("./my-proxy"), options);
```

### Working with the Result

```java
ConversionResult result = converter.convert(path);

// Check if conversion was successful
if (result.isSuccessful()) {
    System.out.println("Bundle: " + result.getBundleName());
    System.out.println("Paths: " + result.getPathCount());
    System.out.println("Operations: " + result.getOperationCount());
    System.out.println("Time: " + result.getConversionTimeMs() + "ms");
}

// Check for warnings
if (result.hasWarnings()) {
    result.getWarnings().forEach(System.out::println);
}

// Check if fallback extraction was used
if (result.isUsedFallbackExtraction()) {
    System.out.println("Note: Proxy has no conditional flows, used fallback extraction");
}
```

### Parse Bundle for Inspection

```java
// Parse without generating OpenAPI (for custom processing)
ApigeeBundle bundle = converter.parseBundle(Path.of("./my-proxy"));

System.out.println("Proxy: " + bundle.getName());
System.out.println("Base Path: " + bundle.getBasePath());
System.out.println("Endpoints: " + bundle.getProxyEndpoints().size());
System.out.println("Policies: " + bundle.getPolicies().size());

// Then generate OpenAPI when ready
OpenAPI openAPI = converter.generateOpenApi(bundle, ConversionOptions.defaults());
```

## Supported Input Formats

- **ZIP files**: Standard Apigee proxy bundle exports
- **Directories**: Extracted proxy bundles (must contain `apiproxy/` directory structure)

### Expected Bundle Structure

```
my-proxy/
└── apiproxy/
    ├── my-proxy.xml          # Proxy descriptor
    ├── proxies/
    │   └── default.xml       # Proxy endpoint(s)
    ├── targets/
    │   └── default.xml       # Target endpoint(s)
    └── policies/
        ├── VerifyAPIKey.xml  # Policy files
        └── ...
```

## What Gets Extracted

### From Proxy Endpoints
- Base path
- Conditional flows (paths and HTTP methods)
- Route rules

### From Flow Conditions
- Path patterns (e.g., `/pets/{petId}`)
- HTTP methods (GET, POST, PUT, DELETE, etc.)
- Operation names/IDs

### From Policies
- **ExtractVariables**: Path parameters, query parameters, headers
- **AssignMessage**: Headers, query parameters
- **VerifyAPIKey**: API Key security scheme
- **OAuthV2**: OAuth 2.0 security scheme
- **BasicAuthentication**: Basic Auth security scheme

### From Target Endpoints
- Backend server URLs

## Improvements Over Original Node.js Tool

This library addresses the limitations of the original `apigee2openapi` Node.js tool:

| Feature | Original Tool | This Library |
|---------|---------------|--------------|
| Output Spec | OpenAPI 2.0 (Swagger) | OpenAPI 3.0.x |
| Output Format | JSON only | JSON & YAML |
| No Flows Handling | None | Fallback extraction |
| Parameter Extraction | None | From policy XMLs |
| Connectivity | Requires Apigee API | Offline only |
| Usage | CLI only | Java library API |

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building)

## Building from Source

```bash
cd apigee-bundle-to-openapi
mvn clean install
```

### Running Tests

```bash
mvn test
```

## Dependencies

- [Swagger Parser](https://github.com/swagger-api/swagger-parser) - OpenAPI model and utilities
- [Jackson](https://github.com/FasterXML/jackson) - JSON/YAML processing
- [SLF4J](https://www.slf4j.org/) - Logging facade

## License

Apache License, Version 2.0

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Support

This is not an officially supported Google product.
