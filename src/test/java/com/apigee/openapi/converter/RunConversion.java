package com.apigee.openapi.converter;

import com.apigee.openapi.converter.output.OpenApiWriter.OutputFormat;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Run this to convert JSONPlaceholder-Todo-API from Apigee and print the OpenAPI spec.
 * 
 * Usage: mvn test-compile exec:java -Dexec.mainClass="com.apigee.openapi.converter.RunConversion" -Dexec.classpathScope=test
 */
public class RunConversion {
    
    // === YOUR CONFIGURATION ===
    private static final String ORGANIZATION = "apigee-new-491006";
    private static final String PROXY_NAME = "JSONPlaceholder-Todo-API";
    private static final String PROXY_HOSTNAME = "130.211.6.177.nip.io";
    
    private static final String SERVICE_ACCOUNT_JSON = 
        "{" +
        "\"type\": \"service_account\"," +
        "\"project_id\": \"apigee-new-491006\"," +
        "\"private_key_id\": \"3b31424504eccf92e787fc63a290ed4a0727f483\"," +
        "\"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCr584g0YmAhrAm\\nkcpfbz0QbCHlscSQ8wbN6j+SRflLRWiotjzRC1KL3JZIpdKmYY9hCJCUAiEasNxG\\n1iYtSqIrINXT+WsKubZ5uYyEzbD4lZ7jpj7o+WBGUkRFuMMvrDDktjublvb8fYFF\\nppQU325yFA7EyZ661bLpiNCQP7dlM08CRcVQGVMdZuiiMPg3rePXpqgPziO7diuv\\n5E2w42o/AH1hfC7yNnIhuE3Wio2p4ISjfYWYCZprUBNR8dTT1ebzSgrDby9OVrTW\\nUBCrvIpxqIBsz0C7Er5+RycquZR+NkqXpBLjQzGr+D7tSeNtbEQM1QowrbFIkCnV\\nE0N9P1ZbAgMBAAECggEABrfP4+jHFV6YyrZphhJBp38k/KYrrDh7Jxfvn7vxUqI7\\nRVXzNCjbndCjOjfqGWDNqs6cC9S2I3tkQwvCa9mzUw588hmYt89rKgBOKrB5KTXR\\nAveFFN4OB6GBE90fxPYB5nuYAmwYvzmjutZC34/zGoKIfeNH5ehNDiMFIjtYjkat\\nyc8Pu2k0lrSzKfoCs+YDUo9YaBH/tgklN45QhfeuiTumR5KXzhNYqlhT0OBFsWom\\nPzWxE3RVXqH79Rm+6pIE4RyG8qcc6joiGtnuZGei4E1BFyEC7YImREaHXL//U/88\\nOI4SNyzSPl5jHYS7wpkCfg7jLRb/D2jKAS805uYeqQKBgQDd4vu9bydXPuA2h5CX\\nGBP40bMTtm8OAAvTQGgh/hsyjT3iCqo7SadhJ/zueodr2JEQWvybyw/sWimgjtl2\\nztEqPxWh5DiHd0qJ3LkWnw6C6kGbqQvGAK3wzlu1qsLTfn9qEHsCpjQ0/HtMxRVT\\n/+DujEBI6moDjNxx7/Z5B50JgwKBgQDGVamqm3dsT2ma9qzn3wJU6u4rLBF1c6EP\\n2TNLxa+x7jbNdfO3HoGyxBcl3YLVasXZzRDiwYXd8tQgdTGWYQQxQSIYbIa+5UC+\\nedgn8ewoBTK+rVSOwBVctny2n7Jgk6P/SPFg4IwJxvbbz1xlx8r2cFV13HICWqUI\\nnF/eSjfgSQKBgQDD08A5wBAIgAWCGsMRlr6JqwlRLIF26bOGDiHZXlrCksU66g6z\\npJ2EsbYZAyrhk5DzzlmDZgP3pNt4SJrEsGMJ1gNRjgoWBMJUSelQfbBg+j5NEzTA\\n23ioPnfSLZMk9U+eXE00WVtaKDrp1kasi/gIkN9revd1iqxEInR0/LMr4QKBgGXQ\\nVc/iDTVJsvG9DdfeMIk9LNR2w+OiXx0Op/mO2vmhSvrAQUNQ4s3NQgDLLL24GScM\\n0U1GgR0F6gVbhAApf4h1YjZdh/J5J3pgfClNVKjaukkOG6lIS+8TMwRhCKXxRK/p\\nVxgfTWQ7gkAmu8z3+mX2FrjxWImC1NONyt0HBpYZAoGATHQHT5qMmbUU1w/YRvkn\\nX+pTb73sYLLLe1wdpm+cszfY82k7tZlyS8KOD4ehf68rZfUHGeiGXQAD4YSNbZtV\\nOMFe7crHAFSTDCRW3aM0tcoiE9yU/6k6KHtLnNacncJ8rVnhk3InqLrHWpO4wL+d\\n69qchJAsG7xXNSWyYK/9sAg=\\n-----END PRIVATE KEY-----\\n\"," +
        "\"client_email\": \"aigeenew@apigee-new-491006.iam.gserviceaccount.com\"," +
        "\"client_id\": \"112221362416082930654\"," +
        "\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\"," +
        "\"token_uri\": \"https://oauth2.googleapis.com/token\"," +
        "\"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\"," +
        "\"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/aigeenew%40apigee-new-491006.iam.gserviceaccount.com\"," +
        "\"universe_domain\": \"googleapis.com\"" +
        "}";

    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║       Apigee to OpenAPI Converter                            ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Configuration:");
            System.out.println("  • Organization:  " + ORGANIZATION);
            System.out.println("  • Proxy Name:    " + PROXY_NAME);
            System.out.println("  • Proxy Host:    " + PROXY_HOSTNAME);
            System.out.println();

            // Build configuration
            ApigeeApiConfig config = ApigeeApiConfig.builder()
                .organization(ORGANIZATION)
                .proxyHostname(PROXY_HOSTNAME)
                .serviceAccountKeyJson(SERVICE_ACCOUNT_JSON)
                .build();

            System.out.println("✓ Configuration created");

            // Create converter
            ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
            System.out.println("✓ Converter initialized");
            System.out.println();
            System.out.println("Connecting to Apigee and downloading proxy bundle...");
            System.out.println("Using default options - extracting metadata from proxy...");

            // Convert using default options (extracts metadata from proxy)
            ConversionResult result = converter.convertFromApigee(config, PROXY_NAME, null, ConversionOptions.defaults());

            System.out.println();
            System.out.println("✓ Conversion successful!");
            System.out.println("  • Bundle:     " + result.getBundleName());
            System.out.println("  • Paths:      " + result.getPathCount());
            System.out.println("  • Operations: " + result.getOperationCount());
            System.out.println("  • Time:       " + result.getConversionTimeMs() + "ms");

            // Get both formats
            String yaml = converter.writeToString(result.getOpenAPI(), OutputFormat.YAML);
            String json = converter.writeToString(result.getOpenAPI(), OutputFormat.JSON);

            // Save to files
            Files.writeString(Path.of(PROXY_NAME + "-openapi.yaml"), yaml);
            Files.writeString(Path.of(PROXY_NAME + "-openapi.json"), json);
            System.out.println();
            System.out.println("✓ Files saved:");
            System.out.println("  • " + PROXY_NAME + "-openapi.yaml");
            System.out.println("  • " + PROXY_NAME + "-openapi.json");

            // Print the OpenAPI spec
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    OpenAPI Spec (YAML)                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println(yaml);

            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    OpenAPI Spec (JSON)                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println(json);

        } catch (Exception e) {
            System.err.println();
            System.err.println("╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║                         ERROR                                ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println();
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
