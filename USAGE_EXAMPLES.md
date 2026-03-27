# Usage Examples

## Quick Start: Convert from Apigee API (Recommended)

The easiest way to use this library is to connect directly to your Apigee organization:

```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import com.apigee.openapi.converter.ApigeeApiConfig;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // Configure Apigee connection
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .serviceAccountKeyPath("/path/to/service-account.json")
            .build();

        // Convert proxy to OpenAPI YAML - one line!
        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");
        
        System.out.println(yaml);
    }
}
```

## Apigee API Examples

### Example A1: Download and Convert Latest Revision
```java
import com.apigee.openapi.converter.*;
import java.nio.file.Path;

public class ApiExample1 {
    public static void main(String[] args) throws Exception {
        // Configure with service account
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .serviceAccountKeyPath("service-account.json")
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Download and convert
        ConversionResult result = converter.convertFromApigee(config, "weather-api");
        
        System.out.println("Bundle: " + result.getBundleName());
        System.out.println("Paths: " + result.getPathCount());
        System.out.println("Operations: " + result.getOperationCount());
        
        // Get as YAML string
        String yaml = converter.writeToString(result.getOpenAPI(), OutputFormat.YAML);
        System.out.println(yaml);
    }
}
```

### Example A2: Convert Specific Revision with Custom Options
```java
import com.apigee.openapi.converter.*;

public class ApiExample2 {
    public static void main(String[] args) throws Exception {
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .serviceAccountKeyPath("service-account.json")
            .build();

        ConversionOptions options = ConversionOptions.builder()
            .title("Weather API")
            .version("2.0.0")
            .description("Real-time weather data")
            .serverUrl("https://api.weather.example.com/v2")
            .contactEmail("api@weather.example.com")
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Convert specific revision "5"
        ConversionResult result = converter.convertFromApigee(
            config, 
            "weather-api", 
            "5",    // specific revision
            options
        );
        
        System.out.println("Converted revision 5: " + result.getPathCount() + " paths");
    }
}
```

### Example A3: List Proxies and Convert All
```java
import com.apigee.openapi.converter.*;
import java.nio.file.*;
import java.util.List;

public class ApiExample3 {
    public static void main(String[] args) throws Exception {
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .serviceAccountKeyPath("service-account.json")
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        ApigeeManagementApiClient client = converter.createApiClient(config);
        
        // List all proxies
        List<String> proxies = client.listProxies();
        System.out.println("Found " + proxies.size() + " proxies");
        
        // Convert each one
        Path outputDir = Path.of("openapi-specs");
        Files.createDirectories(outputDir);
        
        for (String proxyName : proxies) {
            try {
                converter.convertFromApigeeAndSave(
                    config,
                    proxyName,
                    outputDir.resolve(proxyName + ".yaml"),
                    ConversionOptions.defaults()
                );
                System.out.println("✅ Converted: " + proxyName);
            } catch (Exception e) {
                System.err.println("❌ Failed: " + proxyName + " - " + e.getMessage());
            }
        }
    }
}
```

### Example A4: Using Application Default Credentials
```java
import com.apigee.openapi.converter.*;

public class ApiExample4 {
    public static void main(String[] args) throws Exception {
        // Use GOOGLE_APPLICATION_CREDENTIALS env var or default credentials
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .useApplicationDefaultCredentials()
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");
        System.out.println(yaml);
    }
}
```

### Example A5: Using Service Account JSON String (from environment/secrets)
```java
import com.apigee.openapi.converter.*;

public class ApiExample5 {
    public static void main(String[] args) throws Exception {
        // Get JSON from environment variable or secrets manager
        String jsonKey = System.getenv("APIGEE_SERVICE_ACCOUNT_JSON");
        
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .serviceAccountKeyJson(jsonKey)
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");
        System.out.println(yaml);
    }
}
```

### Example A6: Using Service Account from Environment Variable (Simplified)
```java
import com.apigee.openapi.converter.*;

public class ApiExample6 {
    public static void main(String[] args) throws Exception {
        // Automatically reads from APIGEE_SERVICE_ACCOUNT_JSON environment variable
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-gcp-project")
            .serviceAccountKeyFromEnv()  // No need to manually read env var!
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");
        System.out.println(yaml);
    }
}
```

Set your environment variable before running:
```bash
# Linux/Mac
export APIGEE_SERVICE_ACCOUNT_JSON='{"type":"service_account","project_id":"my-project",...}'

# Windows (Command Prompt)
set APIGEE_SERVICE_ACCOUNT_JSON={"type":"service_account","project_id":"my-project",...}

# Windows (PowerShell)
$env:APIGEE_SERVICE_ACCOUNT_JSON='{"type":"service_account","project_id":"my-project",...}'
```

Or use a custom environment variable name:
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-gcp-project")
    .serviceAccountKeyFromEnv("MY_CUSTOM_SERVICE_ACCOUNT")
    .build();
```

---

## Local File Examples

### Example 1: Convert Bundle to YAML
```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import java.nio.file.Path;

public class Example1 {
    public static void main(String[] args) throws Exception {
        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Convert and save
        converter.convertAndSave(
            Path.of("my-proxy.zip"),
            Path.of("api-spec.yaml")
        );
        
        System.out.println("✅ Conversion complete! Check api-spec.yaml");
    }
}
```

### Example 2: Convert to String (JSON)
```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;

public class Example2 {
    public static void main(String[] args) throws Exception {
        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Get as JSON string
        String jsonSpec = converter.convertToString(
            Path.of("my-proxy-folder"),
            OutputFormat.JSON
        );
        
        System.out.println(jsonSpec);
    }
}
```

### Example 3: Custom Configuration
```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import com.apigee.openapi.converter.ConversionOptions;
import com.apigee.openapi.converter.ConversionResult;

public class Example3 {
    public static void main(String[] args) throws Exception {
        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Custom options
        ConversionOptions options = ConversionOptions.builder()
            .title("My Custom API")
            .version("2.0.0")
            .description("Generated from Apigee proxy")
            .serverUrl("https://api.mycompany.com")
            .contactEmail("api@mycompany.com")
            .build();
        
        // Convert with options
        ConversionResult result = converter.convert(
            Path.of("my-proxy.zip"),
            options
        );
        
        // Print stats
        System.out.println("Bundle: " + result.getBundleName());
        System.out.println("Paths: " + result.getPathCount());
        System.out.println("Operations: " + result.getOperationCount());
        System.out.println("Time: " + result.getConversionTimeMs() + "ms");
        
        // Save to file
        converter.writeToFile(
            result.getOpenAPI(),
            Path.of("custom-api.yaml"),
            OutputFormat.YAML
        );
    }
}
```

### Example 4: Batch Conversion
```java
import com.apigee.openapi.converter.ApigeeToOpenApiConverter;
import java.nio.file.*;
import java.util.stream.Stream;

public class Example4 {
    public static void main(String[] args) throws Exception {
        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        Path bundlesDir = Path.of("apigee-bundles");
        Path outputDir = Path.of("openapi-specs");
        Files.createDirectories(outputDir);
        
        // Convert all ZIP files in directory
        try (Stream<Path> files = Files.list(bundlesDir)) {
            files.filter(p -> p.toString().endsWith(".zip"))
                 .forEach(zipFile -> {
                     try {
                         String name = zipFile.getFileName().toString()
                                              .replace(".zip", ".yaml");
                         converter.convertAndSave(
                             zipFile,
                             outputDir.resolve(name)
                         );
                         System.out.println("✅ Converted: " + name);
                     } catch (Exception e) {
                         System.err.println("❌ Failed: " + zipFile + " - " + e.getMessage());
                     }
                 });
        }
    }
}
```

## Running Examples

### Using Maven
```bash
# Create a test class in src/test/java/examples/Example1.java
# Then run:
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass="examples.Example1"
```

### Using IDE
1. Open project in IntelliJ/Eclipse
2. Create new class in `src/test/java/examples/`
3. Copy example code
4. Right-click → Run

### As Dependency in Another Project
```xml
<!-- In your other project's pom.xml -->
<dependency>
    <groupId>com.apigee.openapi</groupId>
    <artifactId>apigee-bundle-to-openapi</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Input: Apigee Bundle Structure

Your Apigee bundle should look like this:
```
my-proxy/
└── apiproxy/
    ├── my-proxy.xml           # Main descriptor
    ├── proxies/
    │   └── default.xml        # Proxy endpoint (paths/methods)
    ├── targets/
    │   └── default.xml        # Backend URL
    └── policies/
        ├── VerifyAPIKey.xml   # Security policies
        └── ExtractVars.xml    # Parameter extraction
```

Or as a ZIP file containing the above structure.

## Output: OpenAPI Spec

The library generates OpenAPI 3.0.3 specifications with:
- ✅ API info (title, version, description)
- ✅ Server URLs
- ✅ Paths and operations (GET, POST, PUT, DELETE, etc.)
- ✅ Path parameters (e.g., `/pets/{petId}`)
- ✅ Query parameters
- ✅ Headers
- ✅ Security schemes (API Key, OAuth2, Basic Auth)
- ✅ Response definitions
- ✅ Tags for grouping

## Troubleshooting

### Issue: "No proxy endpoints found"
**Solution**: Make sure your bundle has `apiproxy/proxies/default.xml`

### Issue: "Only GET / operation generated"
**Solution**: Your proxy uses fallback extraction. Add flow conditions like:
```xml
<Condition>(proxy.pathsuffix MatchesPath "/users") and (request.verb = "GET")</Condition>
```

### Issue: "No parameters extracted"
**Solution**: Add ExtractVariables or AssignMessage policies to define parameters
