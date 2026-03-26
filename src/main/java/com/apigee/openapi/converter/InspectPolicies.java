/*
 * Inspect proxy policies
 */
package com.apigee.openapi.converter;

import com.apigee.openapi.converter.model.ApigeeBundle;
import com.apigee.openapi.converter.model.Policy;
import com.apigee.openapi.converter.parser.BundleParser;

public class InspectPolicies {
    public static void main(String[] args) {
        try {
            ApigeeApiConfig config = ApigeeApiConfig.builder()
                .organization("apigee-new-491006")
                .serviceAccountKeyPath("C:\\Users\\Dinith Edirisinghe\\Downloads\\apigee-new-491006-3b31424504ec.json")
                .build();
            
            ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
            ApigeeManagementApiClient client = converter.createApiClient(config);
            
            System.out.println("=== Downloading and Inspecting Proxy ===\n");
            
            var bundleStream = client.downloadLatestBundle("JSONPlaceholder-Todo-API");
            
            // Use BundleParser directly
            BundleParser parser = new BundleParser();
            ApigeeBundle bundle = parser.parseZipStream(bundleStream);
            
            System.out.println("Proxy: " + bundle.getName());
            System.out.println("Base Path: " + bundle.getBasePath());
            System.out.println("\n=== Policies Found (" + bundle.getPolicies().size() + ") ===");
            
            for (Policy policy : bundle.getPolicies().values()) {
                System.out.println("\nPolicy Name: " + policy.getName());
                System.out.println("  Type: " + policy.getType());
                System.out.println("  Is Security: " + policy.isSecurityPolicy());
                System.out.println("  Display Name: " + policy.getDisplayName());
                
                // Check specific config values we care about for security
                if (policy.isSecurityPolicy()) {
                    System.out.println("  === Security Policy Details ===");
                    
                    // Try to get API key config
                    String apiKeyLocation = policy.getConfigValue("apiKeyLocation", String.class);
                    String apiKeyName = policy.getConfigValue("apiKeyName", String.class);
                    
                    if (apiKeyLocation != null) {
                        System.out.println("    API Key Location: " + apiKeyLocation);
                    }
                    if (apiKeyName != null) {
                        System.out.println("    API Key Name: " + apiKeyName);
                    }
                    
                    // OAuth info
                    String operation = policy.getConfigValue("operation", String.class);
                    if (operation != null) {
                        System.out.println("    OAuth Operation: " + operation);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
