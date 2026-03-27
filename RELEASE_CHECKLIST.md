# Release Checklist for Version 1.1.0

## ✅ Pre-Release Tasks

### 1. Version Updates
- [x] Updated `pom.xml` version to 1.1.0
- [x] Updated README.md with new version in Maven/Gradle examples
- [x] Created CHANGELOG.md with release notes
- [x] Added "What's New" section to README.md

### 2. Documentation
- [x] README.md - Updated and reviewed
- [x] USAGE_EXAMPLES.md - Updated with new features
- [x] QUICK_REFERENCE.md - Complete reference guide
- [x] CHANGELOG.md - Detailed changelog
- [x] .env.example - Template for environment variables

### 3. Code Cleanup
- [x] Removed generated OpenAPI files (*.json, *.yaml)
- [x] Removed service-account.json (sensitive)
- [x] Removed temporary test files
- [x] Removed old batch scripts
- [x] Updated .gitignore

### 4. Testing
- [ ] Run all unit tests: `mvn test`
- [ ] Run integration tests
- [ ] Test ManualTest.java with real Apigee proxy
- [ ] Verify both JSON and YAML outputs
- [ ] Test environment variable authentication

### 5. Build
- [ ] Clean build: `mvn clean install`
- [ ] Verify JAR is created in target/
- [ ] Check no errors or warnings

### 6. Documentation Review
- [ ] All code examples compile
- [ ] All links work
- [ ] Version numbers consistent everywhere
- [ ] No typos

---

## 📦 Deployment Steps

### Step 1: Prepare Release Build
```bash
# Clean and build
mvn clean install

# Generate sources and javadoc
mvn clean source:jar javadoc:jar

# Sign artifacts (if publishing to Maven Central)
mvn gpg:sign-and-deploy-file
```

### Step 2: Tag Release in Git
```bash
git add .
git commit -m "Release version 1.1.0

- Added environment variable support for service accounts
- Enhanced JSON and YAML output methods
- Improved documentation
- See CHANGELOG.md for full details"

git tag -a v1.1.0 -m "Version 1.1.0"
git push origin main
git push origin v1.1.0
```

### Step 3: Deploy to Maven Central (if applicable)
```bash
# Using Maven Central deployment plugin
mvn clean deploy -P release

# Or manually upload to Sonatype OSSRH
# Follow instructions in PUBLISHING.md
```

### Step 4: Create GitHub Release
1. Go to: https://github.com/dinithedirisinghe/apigee-bundle-to-openapi/releases/new
2. Tag: v1.1.0
3. Title: "Version 1.1.0 - Environment Variable Support & Enhanced Output Methods"
4. Description: Copy from CHANGELOG.md
5. Attach: apigee-bundle-to-openapi-1.1.0.jar
6. Publish release

---

## 🎯 Post-Release Tasks

### 1. Verification
- [ ] Download from Maven Central (wait 2-4 hours for sync)
- [ ] Test in a fresh project
- [ ] Verify documentation on GitHub

### 2. Announcements
- [ ] Update GitHub README if needed
- [ ] Post release notes
- [ ] Update any external documentation

### 3. Next Version Prep
- [ ] Update pom.xml to 1.2.0-SNAPSHOT
- [ ] Create new section in CHANGELOG.md for unreleased changes

---

## 📋 Files to Include in Release

### Keep These Files:
```
.env.example
.gitignore
CHANGELOG.md
LICENSE
pom.xml
PUBLISHING.md
QUICK_REFERENCE.md
README.md
run-manual-test.bat
USAGE_EXAMPLES.md
src/
  ├── main/
  └── test/
```

### Exclude These (via .gitignore):
```
service-account.json
*-openapi.json
*-openapi.yaml
*.asc
target/
.idea/
*.iml
```

---

## 🔑 Key Features in This Release

### New Authentication Method
```java
// Before (manual env var)
String json = System.getenv("APIGEE_SERVICE_ACCOUNT_JSON");
config.serviceAccountKeyJson(json);

// After (automatic)
config.serviceAccountKeyFromEnv();
```

### Enhanced Output Methods
```java
// Now both JSON and YAML support all options
String json = converter.convertFromApigeeToJson(config, proxy, options);
String yaml = converter.convertFromApigeeToYaml(config, proxy, options);

// Specific revisions too
String json = converter.convertFromApigeeToJson(config, proxy, "5", options);
String yaml = converter.convertFromApigeeToYaml(config, proxy, "5", options);
```

---

## 📞 Support

If issues arise during deployment:
1. Check PUBLISHING.md for troubleshooting
2. Review Maven Central requirements
3. Verify GPG signing is working
4. Check all tests pass

---

## ✅ Final Checklist

Before deploying, ensure:
- [ ] All tests pass
- [ ] Documentation is up to date
- [ ] Version numbers are consistent
- [ ] CHANGELOG is complete
- [ ] No sensitive data in repository
- [ ] .gitignore is comprehensive
- [ ] Build artifacts are clean
- [ ] GPG signature works (if needed)

---

**Ready to deploy? Run the commands above!** 🚀
