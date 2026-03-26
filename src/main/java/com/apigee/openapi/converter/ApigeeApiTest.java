/*
 * Test class for Apigee API integration
 */
package com.apigee.openapi.converter;

import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;
import java.nio.file.Path;

public class ApigeeApiTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Apigee API Integration Test ===\n");
            
            // Configure with service account
            ApigeeApiConfig config = ApigeeApiConfig.builder()
                .organization("apigee-new-491006")
                .serviceAccountKeyPath("C:\\Users\\Dinith Edirisinghe\\Downloads\\apigee-new-491006-3b31424504ec.json")
                .build();
            
            System.out.println("✓ Config created: " + config);
            
            // Create converter
            ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
            
            // Create API client to list info first
            ApigeeManagementApiClient client = converter.createApiClient(config);
            
            System.out.println("\n--- Fetching proxy info ---");
            String latestRevision = client.getLatestRevision("JSONPlaceholder-Todo-API");
            System.out.println("✓ Latest revision: " + latestRevision);
            
            System.out.println("\n--- Converting proxy to OpenAPI ---");
            ConversionResult result = converter.convertFromApigee(config, "JSONPlaceholder-Todo-API");
            
            System.out.println("\n=== Conversion Result ===");
            System.out.println("Bundle Name: " + result.getBundleName());
            System.out.println("Paths: " + result.getPathCount());
            System.out.println("Operations: " + result.getOperationCount());
            System.out.println("Conversion Time: " + result.getConversionTimeMs() + "ms");
            
            if (result.hasWarnings()) {
                System.out.println("\nWarnings:");
                result.getWarnings().forEach(w -> System.out.println("  - " + w));
            }
            
            // Get YAML output
            String yaml = converter.writeToString(result.getOpenAPI(), OutputFormat.YAML);
            
            System.out.println("\n=== Generated OpenAPI Spec (YAML) ===\n");
            System.out.println(yaml);
            
            // Save to file
            Path outputPath = Path.of("JSONPlaceholder-Todo-API-openapi.yaml");
            converter.writeToFile(result.getOpenAPI(), outputPath, OutputFormat.YAML);
            System.out.println("\n✓ Saved to: " + outputPath.toAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
