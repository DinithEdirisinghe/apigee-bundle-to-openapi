package com.apigee.openapi.converter;

import com.apigee.openapi.converter.output.OpenApiWriter;
import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple converter to transform existing YAML to JSON to demonstrate both formats.
 */
public class YamlToJsonConverter {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Converting YAML to JSON ===\n");
            
            Path yamlPath = Path.of("JSONPlaceholder-Todo-API-openapi.yaml");
            Path jsonPath = Path.of("JSONPlaceholder-Todo-API-openapi.json");
            
            if (!Files.exists(yamlPath)) {
                System.err.println("ERROR: YAML file not found: " + yamlPath);
                System.exit(1);
            }
            
            System.out.println("Reading: " + yamlPath.toAbsolutePath());
            
            // Parse YAML
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            OpenAPI openAPI = parser.read(yamlPath.toString());
            
            if (openAPI == null) {
                System.err.println("ERROR: Failed to parse YAML file");
                System.exit(1);
            }
            
            System.out.println("✓ YAML parsed successfully");
            System.out.println("  Title: " + openAPI.getInfo().getTitle());
            System.out.println("  Version: " + openAPI.getInfo().getVersion());
            System.out.println("  Paths: " + openAPI.getPaths().size());
            
            // Write as JSON
            OpenApiWriter writer = new OpenApiWriter();
            String json = writer.writeToString(openAPI, OutputFormat.JSON);
            
            Files.writeString(jsonPath, json);
            
            System.out.println("\n✓ JSON created: " + jsonPath.toAbsolutePath());
            System.out.println("  Size: " + json.length() + " characters");
            
            // Show preview
            System.out.println("\n=== JSON Preview (first 800 chars) ===");
            System.out.println(json.substring(0, Math.min(800, json.length())));
            System.out.println("...\n");
            
            System.out.println("✅ SUCCESS! Both YAML and JSON versions are now available.");
            
        } catch (Exception e) {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
