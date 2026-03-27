# Apigee Bundle to OpenAPI Converter

A production-quality Java library that converts Apigee API proxy bundles (ZIP files or extracted directories) into OpenAPI 3.0 specifications.

## 🆕 What's New in v1.1.0

- **🔐 Environment Variable Support** - New `serviceAccountKeyFromEnv()` method for easier credential management
- **📄 Enhanced JSON/YAML Methods** - Both formats now support custom options and specific revisions
- **📚 Improved Documentation** - Added QUICK_REFERENCE.md and comprehensive examples
- **🐳 Better Container Support** - Perfect for Docker, Kubernetes, and CI/CD pipelines

See [CHANGELOG.md](CHANGELOG.md) for full details.

---

## Features

- **OpenAPI 3.0 Output**: Generates OpenAPI 3.0.x specifications (not legacy Swagger 2.0)
- **Multiple Output Formats**: Supports both JSON and YAML output
- **Offline Operation**: Works entirely from local files - no Apigee connection required
- **Apigee API Integration**: Download bundles directly from Apigee using service account credentials
- **Smart Path Extraction**: Extracts paths from conditional flows with fallback strategies
- **Parameter Inference**: Extracts query parameters, headers, and path parameters from policy XMLs
- **Security Detection**: Automatically detects API Key, OAuth2, and Basic Auth from policies
- **Java API**: Designed for programmatic use as a library

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.dinithedirisinghe</groupId>
    <artifactId>apigee-bundle-to-openapi</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.dinithedirisinghe:apigee-bundle-to-openapi:1.1.0'
```

## Quick Start

### Option 1: Convert from Local File/Directory

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

### Option 2: Convert Directly from Apigee API (Recommended)

No need to manually download the proxy bundle! The library can fetch it directly from Apigee using your service account credentials:

```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import com.apigee.openapi.converter.ApigeeApiConfig;
import com.apigee.openapi.converter.ConversionResult;

// Configure Apigee API connection with service account
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-gcp-project")                           // Your Apigee org
    .serviceAccountKeyPath("/path/to/service-account.json")   // GCP service account JSON
    .build();

// Create converter and convert directly from Apigee
ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
ConversionResult result = converter.convertFromApigee(config, "my-proxy");

// Get the OpenAPI spec as YAML or JSON
String yaml = converter.writeToString(result.getOpenAPI(), OutputFormat.YAML);
String json = converter.writeToString(result.getOpenAPI(), OutputFormat.JSON);
```

### Even Simpler - One-liner Conversions

```java
// Get as YAML string
String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");

// Get as JSON string
String json = converter.convertFromApigeeToJson(config, "my-proxy");

// With custom options
ConversionOptions options = ConversionOptions.builder()
    .title("My API")
    .version("2.0.0")
    .serverUrl("https://api.example.com")
    .build();

String yaml = converter.convertFromApigeeToYaml(config, "my-proxy", options);
String json = converter.convertFromApigeeToJson(config, "my-proxy", options);

// Specific revision with options
String yaml = converter.convertFromApigeeToYaml(config, "my-proxy", "5", options);
String json = converter.convertFromApigeeToJson(config, "my-proxy", "5", options);
```

## Apigee API Integration

### Authentication Options

#### Service Account JSON File
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyPath("/path/to/service-account.json")
    .build();
```

#### Service Account JSON String
```java
String jsonKey = "{ \"type\": \"service_account\", ... }";

ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyJson(jsonKey)
    .build();
```

#### Service Account JSON from Environment Variable
```java
// Reads from APIGEE_SERVICE_ACCOUNT_JSON by default
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyFromEnv()
    .build();

// Or specify a custom environment variable name
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyFromEnv("MY_SERVICE_ACCOUNT_JSON")
    .build();
```

Set the environment variable:
```bash
# Linux/Mac
export APIGEE_SERVICE_ACCOUNT_JSON='{"type":"service_account","project_id":"...","private_key":"..."}'

# Windows
set APIGEE_SERVICE_ACCOUNT_JSON={"type":"service_account","project_id":"...","private_key":"..."}
```

#### Application Default Credentials
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .useApplicationDefaultCredentials()  // Uses GOOGLE_APPLICATION_CREDENTIALS env var
    .build();
```

### Convert Specific Revision

```java
// Convert a specific revision instead of latest
ConversionResult result = converter.convertFromApigee(config, "my-proxy", "5", options);
```

### List Proxies and Revisions

```java
// Create API client for management operations
ApigeeManagementApiClient client = converter.createApiClient(config);

// List all proxies in the organization
List<String> proxies = client.listProxies();
System.out.println("Proxies: " + proxies);

// List revisions for a specific proxy
List<String> revisions = client.listRevisions("my-proxy");
System.out.println("Revisions: " + revisions);

// Get the latest revision number
String latest = client.getLatestRevision("my-proxy");
System.out.println("Latest revision: " + latest);
```

### Convert and Save to File

```java
// Convert from Apigee and save directly to file
converter.convertFromApigeeAndSave(
    config,
    "my-proxy",
    Path.of("./output/my-proxy-openapi.yaml"),
    ConversionOptions.defaults()
);
```

## Local File Conversion

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

## Service Account Setup

To use the Apigee API integration, you need a GCP service account with the following roles:

1. **Create a service account** in your GCP project
2. **Grant roles**:
   - `roles/apigee.apiAdmin` - For reading proxy configurations
   - Or `roles/apigee.readOnlyAdmin` - For read-only access
3. **Download the JSON key file**
4. **Use the key file** with `ApigeeApiConfig.builder().serviceAccountKeyPath(...)`

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
- [Google Auth Library](https://github.com/googleapis/google-auth-library-java) - Service account authentication
- [SLF4J](https://www.slf4j.org/) - Logging facade

## License

Apache License, Version 2.0

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Support

This is not an officially supported Google product.
