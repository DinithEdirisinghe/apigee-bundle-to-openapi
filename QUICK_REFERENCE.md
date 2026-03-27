# Quick Reference: Service Account Configuration & Output Formats

## 🔐 Service Account Authentication (5 Options)

### 1. File Path (Local Development)
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyPath("/path/to/service-account.json")
    .build();
```

### 2. JSON String (Secrets Managers)
```java
String json = loadFromSecretsManager(); // Your implementation
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyJson(json)
    .build();
```

### 3. Environment Variable (RECOMMENDED - Docker/K8s/CI/CD)
```java
// Reads from APIGEE_SERVICE_ACCOUNT_JSON automatically
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyFromEnv()  // ✨ NEW!
    .build();
```

Set before running:
```bash
# Linux/Mac
export APIGEE_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'

# Windows (CMD)
set APIGEE_SERVICE_ACCOUNT_JSON={"type":"service_account",...}

# Windows (PowerShell)
$env:APIGEE_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
```

### 4. Custom Environment Variable
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyFromEnv("MY_CUSTOM_VAR_NAME")  // ✨ NEW!
    .build();
```

### 5. Application Default Credentials (GCP Environments)
```java
// Uses GOOGLE_APPLICATION_CREDENTIALS or GCP metadata service
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .useApplicationDefaultCredentials()
    .build();
```

---

## 📄 Output Format Methods

### Simple Conversions (Latest Revision, Default Options)

```java
ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();

// YAML format
String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");

// JSON format
String json = converter.convertFromApigeeToJson(config, "my-proxy");
```

### With Custom Options (Latest Revision)

```java
ConversionOptions options = ConversionOptions.builder()
    .title("My API")
    .version("2.0.0")
    .description("API description")
    .serverUrl("https://api.example.com")
    .contactEmail("api@example.com")
    .build();

// YAML with options ✨ NEW!
String yaml = converter.convertFromApigeeToYaml(config, "my-proxy", options);

// JSON with options ✨ NEW!
String json = converter.convertFromApigeeToJson(config, "my-proxy", options);
```

### Specific Revision with Custom Options

```java
// YAML - specific revision ✨ NEW!
String yaml = converter.convertFromApigeeToYaml(config, "my-proxy", "5", options);

// JSON - specific revision ✨ NEW!
String json = converter.convertFromApigeeToJson(config, "my-proxy", "5", options);
```

---

## 🚀 Complete Examples

### Example 1: Quick YAML Conversion (Environment Variable)
```java
public class QuickYaml {
    public static void main(String[] args) throws Exception {
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-org")
            .serviceAccountKeyFromEnv()
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        String yaml = converter.convertFromApigeeToYaml(config, "my-proxy");
        
        System.out.println(yaml);
    }
}
```

### Example 2: Quick JSON Conversion (Environment Variable)
```java
public class QuickJson {
    public static void main(String[] args) throws Exception {
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-org")
            .serviceAccountKeyFromEnv()
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        String json = converter.convertFromApigeeToJson(config, "my-proxy");
        
        System.out.println(json);
    }
}
```

### Example 3: Both Formats with Custom Options
```java
public class BothFormats {
    public static void main(String[] args) throws Exception {
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-org")
            .serviceAccountKeyFromEnv()
            .build();

        ConversionOptions options = ConversionOptions.builder()
            .title("Payment API")
            .version("3.0.0")
            .description("Payment processing")
            .serverUrl("https://api.payments.com")
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Get both formats
        String yaml = converter.convertFromApigeeToYaml(config, "payment-api", options);
        String json = converter.convertFromApigeeToJson(config, "payment-api", options);
        
        System.out.println("YAML:\n" + yaml);
        System.out.println("\nJSON:\n" + json);
    }
}
```

### Example 4: Specific Revision
```java
public class SpecificRevision {
    public static void main(String[] args) throws Exception {
        ApigeeApiConfig config = ApigeeApiConfig.builder()
            .organization("my-org")
            .serviceAccountKeyPath("service-account.json")
            .build();

        ConversionOptions options = ConversionOptions.builder()
            .title("User API v2")
            .version("2.5.0")
            .build();

        ApigeeToOpenApiConverter converter = new ApigeeToOpenApiConverter();
        
        // Convert revision 7 to YAML
        String yaml = converter.convertFromApigeeToYaml(config, "user-api", "7", options);
        System.out.println(yaml);
    }
}
```

---

## 🐳 Docker Usage

**Dockerfile:**
```dockerfile
FROM openjdk:11
COPY target/my-app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
```

**Run:**
```bash
docker run \
  -e APIGEE_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}' \
  my-image
```

**Java Code (no changes needed):**
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyFromEnv()  // Automatically reads from Docker env
    .build();
```

---

## ☸️ Kubernetes Usage

**Create Secret:**
```bash
kubectl create secret generic apigee-creds \
  --from-literal=service-account-json='{"type":"service_account",...}'
```

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: apigee-converter
spec:
  template:
    spec:
      containers:
      - name: converter
        image: my-converter:latest
        env:
        - name: APIGEE_SERVICE_ACCOUNT_JSON
          valueFrom:
            secretKeyRef:
              name: apigee-creds
              key: service-account-json
```

**Java Code (no changes needed):**
```java
ApigeeApiConfig config = ApigeeApiConfig.builder()
    .organization("my-org")
    .serviceAccountKeyFromEnv()  // Automatically reads from K8s secret
    .build();
```

---

## 📊 Method Comparison Table

| Scenario | Method | Format |
|----------|--------|--------|
| Quick YAML, default options | `convertFromApigeeToYaml(config, proxy)` | YAML |
| Quick JSON, default options | `convertFromApigeeToJson(config, proxy)` | JSON |
| YAML with custom metadata | `convertFromApigeeToYaml(config, proxy, options)` | YAML |
| JSON with custom metadata | `convertFromApigeeToJson(config, proxy, options)` | JSON |
| YAML specific revision | `convertFromApigeeToYaml(config, proxy, "5", options)` | YAML |
| JSON specific revision | `convertFromApigeeToJson(config, proxy, "5", options)` | JSON |
| Need result object | `convertFromApigee(config, proxy)` | Result |
| Save to file | `convertFromApigeeAndSave(config, proxy, path, options)` | File |

---

## ✅ Best Practices

1. **Use environment variables for production** - More secure than files
2. **Use custom options** - Add proper metadata to your OpenAPI specs
3. **Choose the right format** - YAML for human reading, JSON for API consumption
4. **Handle exceptions** - Wrap calls in try-catch blocks
5. **Cache credentials** - Reuse `ApigeeApiConfig` instances

---

## 🆕 What's New

✨ **Environment Variable Support**
- `serviceAccountKeyFromEnv()` - Auto-reads from default env var
- `serviceAccountKeyFromEnv("VAR_NAME")` - Custom env var name

✨ **Enhanced JSON/YAML Methods**
- Overloaded methods support custom options
- Overloaded methods support specific revisions
- Equal support for both output formats

✨ **Better Documentation**
- Comprehensive examples
- Docker/Kubernetes guides
- `.env.example` template file
