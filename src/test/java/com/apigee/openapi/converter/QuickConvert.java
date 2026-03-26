package com.apigee.openapi.converter;

import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;

import java.nio.file.Path;

/**
 * Simple example to convert your Apigee bundle to OpenAPI.
 * 
 * To run: mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass="com.apigee.openapi.converter.QuickConvert"
 */
public class QuickConvert {

    public static void main(String[] args) {
        try {
            System.out.println("🚀 Starting Apigee to OpenAPI conversion...\n");
            
            // Create converter
            ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
            
            // Your bundle path
            Path bundlePath = Path.of("C:\\Users\\Dinith Edirisinghe\\Downloads\\JSONPlaceholder-Todo-API_rev20_2026_03_26.zip");
            
            // Output paths (will be created in Downloads folder)
            Path yamlOutput = Path.of("C:\\Users\\Dinith Edirisinghe\\Downloads\\todo-api-openapi.yaml");
            Path jsonOutput = Path.of("C:\\Users\\Dinith Edirisinghe\\Downloads\\todo-api-openapi.json");
            
            System.out.println("📦 Reading bundle: " + bundlePath.getFileName());
            System.out.println("⏳ Converting...\n");
            
            // Convert to YAML
            ConversionResult result = converter.convertAndSave(bundlePath, yamlOutput);
            
            // Also save as JSON
            converter.writeToFile(result.getOpenAPI(), jsonOutput, OutputFormat.JSON);
            
            // Print results
            System.out.println("✅ Conversion successful!\n");
            System.out.println("📊 Statistics:");
            System.out.println("   Bundle name: " + result.getBundleName());
            System.out.println("   API paths: " + result.getPathCount());
            System.out.println("   Operations: " + result.getOperationCount());
            System.out.println("   Conversion time: " + result.getConversionTimeMs() + "ms");
            
            if (result.isUsedFallbackExtraction()) {
                System.out.println("   ⚠️  Used fallback extraction (no explicit flow conditions found)");
            }
            
            if (result.hasWarnings()) {
                System.out.println("\n⚠️  Warnings:");
                result.getWarnings().forEach(w -> System.out.println("   - " + w));
            }
            
            System.out.println("\n📄 Output files created:");
            System.out.println("   YAML: " + yamlOutput);
            System.out.println("   JSON: " + jsonOutput);
            
            System.out.println("\n🎉 Done! You can now:");
            System.out.println("   1. Open the YAML/JSON file in any text editor");
            System.out.println("   2. Import it into Swagger Editor (https://editor.swagger.io/)");
            System.out.println("   3. Use it with Postman or other API tools");
            
        } catch (Exception e) {
            System.err.println("\n❌ Conversion failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
