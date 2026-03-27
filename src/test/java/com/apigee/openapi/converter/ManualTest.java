package com.apigee.openapi.converter;

import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual test to convert JSONPlaceholder-Todo-API proxy from Apigee.
 * Run this to generate both JSON and YAML OpenAPI specs.
 */
public class ManualTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Starting Apigee to OpenAPI Conversion ===\n");
            
            // Service account JSON from file
            String serviceAccountJson = new String(Files.readAllBytes(Path.of("service-account.json")));
            if (serviceAccountJson == null || serviceAccountJson.isEmpty()) {
                System.err.println("ERROR: Could not read service-account.json file!");
                System.err.println("Please ensure service-account.json exists in the project root.");
                System.exit(1);
            }
            
            // Configuration
            String organization = "apigee-new-491006";
            String proxyName = "JSONPlaceholder-Todo-API";
            String proxyHostname = "130.211.6.177.nip.io";
            
            System.out.println("Configuration:");
            System.out.println("  Organization: " + organization);
            System.out.println("  Proxy Name: " + proxyName);
            System.out.println("  Proxy Hostname: " + proxyHostname);
            System.out.println();
            
            // Build Apigee API config using environment variable
            ApigeeApiConfig config = ApigeeApiConfig.builder()
                .organization(organization)
                .proxyHostname(proxyHostname)
                .serviceAccountKeyJson(serviceAccountJson)
                .build();
            
            System.out.println("✓ Apigee API config created successfully");
            
            // Create converter
            ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
            System.out.println("✓ Converter created");
            
            // Use default options - this will extract metadata from the proxy itself
            ConversionOptions options = ConversionOptions.defaults();
            
            System.out.println("\n=== Converting from Apigee ===");
            System.out.println("Using default options - extracting metadata from proxy bundle...");
            
            // Convert to get result with stats
            ConversionResult result = converter.convertFromApigee(config, proxyName, null, options);
            
            System.out.println("\n✓ Conversion completed!");
            System.out.println("  Bundle: " + result.getBundleName());
            System.out.println("  Paths: " + result.getPathCount());
            System.out.println("  Operations: " + result.getOperationCount());
            System.out.println("  Time: " + result.getConversionTimeMs() + "ms");
            
            if (result.hasWarnings()) {
                System.out.println("\n⚠ Warnings:");
                result.getWarnings().forEach(w -> System.out.println("  - " + w));
            }
            
            // Generate both JSON and YAML using the new methods
            System.out.println("\n=== Generating Output Formats ===");
            
            // YAML format
            String yaml = converter.writeToString(result.getOpenAPI(), OutputFormat.YAML);
            Path yamlPath = Path.of(proxyName + "-openapi.yaml");
            Files.writeString(yamlPath, yaml);
            System.out.println("✓ YAML saved to: " + yamlPath.toAbsolutePath());
            
            // JSON format
            String json = converter.writeToString(result.getOpenAPI(), OutputFormat.JSON);
            Path jsonPath = Path.of(proxyName + "-openapi.json");
            Files.writeString(jsonPath, json);
            System.out.println("✓ JSON saved to: " + jsonPath.toAbsolutePath());
            
            // Also test the new convenience methods
            System.out.println("\n=== Testing New Convenience Methods ===");
            
            String yamlDirect = converter.convertFromApigeeToYaml(config, proxyName, options);
            System.out.println("✓ convertFromApigeeToYaml() - " + yamlDirect.length() + " chars");
            
            String jsonDirect = converter.convertFromApigeeToJson(config, proxyName, options);
            System.out.println("✓ convertFromApigeeToJson() - " + jsonDirect.length() + " chars");
            
            // Print full YAML output
            System.out.println("\n=== YAML Output (Complete) ===");
            System.out.println(yaml);
            
            // Print full JSON output
            System.out.println("\n=== JSON Output (Complete) ===");
            System.out.println(json);
            
            System.out.println("\n=== SUCCESS! ===");
            System.out.println("Both JSON and YAML files have been generated successfully.");
            
        } catch (Exception e) {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
