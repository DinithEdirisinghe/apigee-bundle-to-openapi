# Publishing Guide: Apigee Bundle to OpenAPI Converter

## Pre-Publishing Checklist

### 1. Clean Up Test Files
Remove test files from the main source directory:
```bash
del "src\main\java\com\apigee\openapi\converter\ApigeeApiTest.java"
del "src\main\java\com\apigee\openapi\converter\InspectPolicies.java"
```

### 2. Verify Build
```bash
mvn clean install
```

Ensure:
- ✅ All tests pass
- ✅ No compilation errors
- ✅ JAR file is created in `target/`

### 3. Update Version (if needed)
Edit `pom.xml` and update version number:
```xml
<version>1.0.0</version>  <!-- Change if needed -->
```

---

## Publishing Options

You have **3 options** for sharing this library:

---

## Option 1: Maven Central (Recommended for Public Use)

**Best for:** Making the library available to everyone via Maven/Gradle

### Step 1: Create Sonatype JIRA Account
1. Go to https://issues.sonatype.org/
2. Click "Sign up" (top right)
3. Create account with your email

### Step 2: Request Namespace
1. Log in to Sonatype JIRA
2. Click "Create" → Select "Community Support - Open Source Project Repository Hosting"
3. Fill in:
   - **Summary:** Request for com.apigee.openapi namespace
   - **Group Id:** `com.apigee.openapi`
   - **Project URL:** Your GitHub repo URL
   - **SCM URL:** Your GitHub repo URL (e.g., https://github.com/yourusername/apigee-bundle-to-openapi)
   - **Username:** Your Sonatype username
4. Submit and wait for approval (usually 1-2 business days)
5. They may ask you to verify domain ownership

### Step 3: Set Up GPG Signing
```bash
# Install GPG (if not installed)
# Windows: Download from https://gpg4win.org/

# Generate GPG key
gpg --gen-key
# Follow prompts (use your email, create passphrase)

# List keys to get your key ID
gpg --list-keys
# Copy the key ID (8-character code)

# Distribute public key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### Step 4: Configure Maven Settings
Create/edit `C:\Users\YOUR_USERNAME\.m2\settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_PASSWORD</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

### Step 5: Update pom.xml for Publishing
Add to your `pom.xml` (after `</dependencies>`):

```xml
<distributionManagement>
  <snapshotRepository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </snapshotRepository>
  <repository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
  </repository>
</distributionManagement>

<build>
  <plugins>
    <!-- Existing plugins stay here -->
    
    <!-- GPG Plugin for Signing -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-gpg-plugin</artifactId>
      <version>3.1.0</version>
      <executions>
        <execution>
          <id>sign-artifacts</id>
          <phase>verify</phase>
          <goals>
            <goal>sign</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    
    <!-- Nexus Staging Plugin -->
    <plugin>
      <groupId>org.sonatype.plugins</groupId>
      <artifactId>nexus-staging-maven-plugin</artifactId>
      <version>1.6.13</version>
      <extensions>true</extensions>
      <configuration>
        <serverId>ossrh</serverId>
        <nexusUrl>https://oss.sonatype.org/</nexusUrl>
        <autoReleaseAfterClose>true</autoReleaseAfterClose>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Step 6: Deploy to Maven Central
```bash
# Deploy to staging
mvn clean deploy

# If autoReleaseAfterClose=false, manually release:
# 1. Go to https://oss.sonatype.org/
# 2. Log in
# 3. Click "Staging Repositories"
# 4. Find your repository
# 5. Click "Close" → "Release"
```

### Step 7: Wait and Verify
- **Central Sync:** 2-4 hours
- **Search Availability:** Up to 24 hours
- Check at: https://search.maven.org/artifact/com.apigee.openapi/apigee-bundle-to-openapi

---

## Option 2: GitHub Packages (Easier, GitHub Users Only)

**Best for:** Sharing with developers who use GitHub

### Step 1: Create GitHub Personal Access Token
1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Give it a name: "Maven Package Publishing"
4. Check scopes:
   - ✅ `write:packages`
   - ✅ `read:packages`
5. Click "Generate token"
6. **Copy the token** (you can't see it again!)

### Step 2: Update pom.xml
Add after `</dependencies>`:

```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/YOUR_USERNAME/apigee-bundle-to-openapi</url>
  </repository>
</distributionManagement>
```

### Step 3: Configure Maven Settings
Edit `C:\Users\YOUR_USERNAME\.m2\settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

### Step 4: Publish
```bash
mvn clean deploy
```

### Step 5: Users Access It
Users need to add to their `pom.xml`:
```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/YOUR_USERNAME/apigee-bundle-to-openapi</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.apigee.openapi</groupId>
    <artifactId>apigee-bundle-to-openapi</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

And configure their `~/.m2/settings.xml` with their own GitHub token.

---

## Option 3: Local/Corporate Maven Repository

**Best for:** Internal company use

### Using JFrog Artifactory
1. Get Artifactory URL from your IT team
2. Add to `pom.xml`:
```xml
<distributionManagement>
  <repository>
    <id>artifactory</id>
    <url>https://your-company.jfrog.io/artifactory/libs-release-local</url>
  </repository>
</distributionManagement>
```

3. Configure `~/.m2/settings.xml` with credentials
4. Deploy: `mvn clean deploy`

### Using Nexus Repository
Similar to Artifactory - get URL and credentials from IT.

---

## Post-Publishing Tasks

### 1. Create GitHub Release
1. Go to your GitHub repository
2. Click "Releases" → "Create a new release"
3. Tag: `v1.0.0`
4. Title: `Apigee Bundle to OpenAPI Converter v1.0.0`
5. Description:
```markdown
## Features
- Convert Apigee proxy bundles to OpenAPI 3.0
- Direct download from Apigee Management API
- Support for JWT/OKTA token validation
- Extracts API Key, OAuth2, and Basic Auth security
- YAML and JSON output

## Installation
Maven:
...xml...

Gradle:
...groovy...

## Usage
See [README.md](README.md) and [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)
```
6. Attach files: `target/apigee-bundle-to-openapi-1.0.0.jar`
7. Click "Publish release"

### 2. Update README Badges (Optional)
Add to top of README.md:
```markdown
[![Maven Central](https://img.shields.io/maven-central/v/com.apigee.openapi/apigee-bundle-to-openapi.svg)](https://search.maven.org/artifact/com.apigee.openapi/apigee-bundle-to-openapi)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
```

### 3. Announce
- Tweet/LinkedIn post
- Post on relevant forums/Slack channels
- Add to Apigee community resources

---

## Troubleshooting

### "401 Unauthorized" during deploy
- Check Maven settings.xml credentials
- Verify token/password hasn't expired
- For Maven Central: Ensure JIRA ticket is resolved

### "GPG signing failed"
```bash
# List keys
gpg --list-keys

# Test signing
gpg --sign test.txt

# If passphrase wrong, re-configure in settings.xml
```

### "Failed to deploy artifact"
- Check internet connection
- Verify repository URL is correct
- Check if version already exists (can't redeploy same version)

### Users can't find the package
- Maven Central: Wait 2-24 hours
- GitHub Packages: Ensure they have access to the repository
- Check version number matches

---

## Quick Decision Guide

**Choose Maven Central if:**
- ✅ Library is open source
- ✅ You want maximum reach
- ✅ You have time for setup (2-3 days initial)

**Choose GitHub Packages if:**
- ✅ You want quick publishing (minutes)
- ✅ Your users are on GitHub
- ✅ You're okay with GitHub-only distribution

**Choose Corporate Maven if:**
- ✅ Internal company library
- ✅ Company has Artifactory/Nexus
- ✅ You want access control

---

## My Recommendation

For this library, I recommend **Maven Central** because:
1. It's an open-source utility library
2. Apigee users worldwide could benefit
3. Proper Maven Central hosting adds credibility
4. No restrictions on who can use it

**Timeline:**
- Day 1: Submit Sonatype JIRA ticket, set up GPG
- Day 2-3: Wait for namespace approval
- Day 4: Deploy and release
- Day 5: Available to all Maven/Gradle users

---

## Need Help?

Common issues:
- **GPG:** https://central.sonatype.org/publish/requirements/gpg/
- **Maven Central Guide:** https://central.sonatype.org/publish/publish-guide/
- **GitHub Packages:** https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry

Good luck! 🚀
