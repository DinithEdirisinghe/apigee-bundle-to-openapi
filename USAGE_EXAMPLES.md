# Usage Examples

## Basic Usage

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
