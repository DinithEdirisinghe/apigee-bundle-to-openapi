/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apigee.openapi.converter.parser;

import com.apigee.openapi.converter.model.ApigeeBundle;
import com.apigee.openapi.converter.model.Policy;
import com.apigee.openapi.converter.model.ProxyEndpoint;
import com.apigee.openapi.converter.model.TargetEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Main parser for Apigee API proxy bundles.
 * Supports loading from both ZIP files and extracted directories.
 */
public class BundleParser {

    private static final Logger log = LoggerFactory.getLogger(BundleParser.class);

    private final ProxyEndpointParser proxyEndpointParser;
    private final TargetEndpointParser targetEndpointParser;
    private final PolicyParser policyParser;
    private final ProxyDescriptorParser proxyDescriptorParser;

    public BundleParser() {
        this.proxyEndpointParser = new ProxyEndpointParser();
        this.targetEndpointParser = new TargetEndpointParser();
        this.policyParser = new PolicyParser();
        this.proxyDescriptorParser = new ProxyDescriptorParser();
    }

    /**
     * Parses an Apigee bundle from a file path (ZIP or directory).
     *
     * @param path Path to the bundle (ZIP file or directory)
     * @return Parsed ApigeeBundle
     * @throws IOException if parsing fails
     */
    public ApigeeBundle parse(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return parseDirectory(path);
        } else if (Files.isRegularFile(path)) {
            return parseZipFile(path);
        } else {
            throw new IOException("Path does not exist or is not accessible: " + path);
        }
    }

    /**
     * Parses an Apigee bundle from a ZIP file.
     */
    public ApigeeBundle parseZipFile(Path zipPath) throws IOException {
        log.info("Parsing Apigee bundle from ZIP: {}", zipPath);
        
        ApigeeBundle bundle = new ApigeeBundle();
        
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            // Find the apiproxy root
            String apiproxyRoot = findApiproxyRoot(zipFile);
            
            // Parse main proxy descriptor
            parseProxyDescriptor(bundle, zipFile, apiproxyRoot);
            
            // Parse proxy endpoints
            parseProxyEndpoints(bundle, zipFile, apiproxyRoot);
            
            // Parse target endpoints
            parseTargetEndpoints(bundle, zipFile, apiproxyRoot);
            
            // Parse policies
            parsePolicies(bundle, zipFile, apiproxyRoot);
        }
        
        log.info("Parsed bundle: {}", bundle);
        return bundle;
    }

    /**
     * Parses an Apigee bundle from a ZIP InputStream.
     * This is useful when downloading bundles directly from the Apigee Management API.
     *
     * @param zipInputStream InputStream containing the ZIP bundle
     * @return Parsed ApigeeBundle
     * @throws IOException if parsing fails
     */
    public ApigeeBundle parseZipStream(InputStream zipInputStream) throws IOException {
        log.info("Parsing Apigee bundle from InputStream");
        
        ApigeeBundle bundle = new ApigeeBundle();
        
        // We need to read the stream into memory to process multiple passes
        // (first pass to find apiproxy root, second to parse contents)
        byte[] zipBytes = readAllBytes(zipInputStream);
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            // First pass: find the apiproxy root
            String apiproxyRoot = findApiproxyRootFromStream(zipBytes);
            
            // Process entries
            parseFromZipBytes(bundle, zipBytes, apiproxyRoot);
        }
        
        log.info("Parsed bundle from stream: {}", bundle);
        return bundle;
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private String findApiproxyRootFromStream(byte[] zipBytes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith("/apiproxy/") || name.equals("apiproxy/")) {
                    return name;
                }
                if (name.contains("/apiproxy/proxies/") || name.startsWith("apiproxy/proxies/")) {
                    int idx = name.indexOf("apiproxy/");
                    return name.substring(0, idx + "apiproxy/".length());
                }
                zis.closeEntry();
            }
        }
        // Default to "apiproxy/" if not found
        return "apiproxy/";
    }

    private void parseFromZipBytes(ApigeeBundle bundle, byte[] zipBytes, String apiproxyRoot) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                
                // Read entry content
                byte[] entryBytes = readEntryBytes(zis);
                InputStream entryStream = new ByteArrayInputStream(entryBytes);
                
                // Parse proxy descriptor (XML at root of apiproxy)
                if (name.startsWith(apiproxyRoot) && name.endsWith(".xml")) {
                    String relativePath = name.substring(apiproxyRoot.length());
                    
                    if (!relativePath.contains("/") && !relativePath.isEmpty()) {
                        // Proxy descriptor at root
                        try {
                            proxyDescriptorParser.parse(entryStream, bundle);
                            if (bundle.getName() == null) {
                                bundle.setName(relativePath.replace(".xml", ""));
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse proxy descriptor: {}", name, e);
                        }
                    } else if (name.startsWith(apiproxyRoot + "proxies/")) {
                        // Proxy endpoint
                        try {
                            entryStream = new ByteArrayInputStream(entryBytes);
                            ProxyEndpoint endpoint = proxyEndpointParser.parse(entryStream);
                            bundle.addProxyEndpoint(endpoint);
                            if (bundle.getBasePath() == null && endpoint.getBasePath() != null) {
                                bundle.setBasePath(endpoint.getBasePath());
                            }
                            log.debug("Parsed proxy endpoint: {}", endpoint.getName());
                        } catch (Exception e) {
                            log.warn("Failed to parse proxy endpoint: {}", name, e);
                        }
                    } else if (name.startsWith(apiproxyRoot + "targets/")) {
                        // Target endpoint
                        try {
                            entryStream = new ByteArrayInputStream(entryBytes);
                            TargetEndpoint endpoint = targetEndpointParser.parse(entryStream);
                            bundle.addTargetEndpoint(endpoint);
                            log.debug("Parsed target endpoint: {}", endpoint.getName());
                        } catch (Exception e) {
                            log.warn("Failed to parse target endpoint: {}", name, e);
                        }
                    } else if (name.startsWith(apiproxyRoot + "policies/")) {
                        // Policy
                        try {
                            entryStream = new ByteArrayInputStream(entryBytes);
                            Policy policy = policyParser.parse(entryStream);
                            bundle.addPolicy(policy);
                            log.debug("Parsed policy: {} ({})", policy.getName(), policy.getType());
                        } catch (Exception e) {
                            log.warn("Failed to parse policy: {}", name, e);
                        }
                    }
                }
                
                zis.closeEntry();
            }
        }
    }

    private byte[] readEntryBytes(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = zis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /**
     * Parses an Apigee bundle from an extracted directory.
     */
    public ApigeeBundle parseDirectory(Path dirPath) throws IOException {
        log.info("Parsing Apigee bundle from directory: {}", dirPath);
        
        ApigeeBundle bundle = new ApigeeBundle();
        
        // Find the apiproxy directory
        Path apiproxyPath = findApiproxyDirectory(dirPath);
        
        // Parse main proxy descriptor
        parseProxyDescriptorFromDir(bundle, apiproxyPath);
        
        // Parse proxy endpoints
        parseProxyEndpointsFromDir(bundle, apiproxyPath);
        
        // Parse target endpoints
        parseTargetEndpointsFromDir(bundle, apiproxyPath);
        
        // Parse policies
        parsePoliciesFromDir(bundle, apiproxyPath);
        
        log.info("Parsed bundle: {}", bundle);
        return bundle;
    }

    private String findApiproxyRoot(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith("/apiproxy/") || name.equals("apiproxy/")) {
                return name;
            }
            if (name.contains("/apiproxy/proxies/") || name.startsWith("apiproxy/proxies/")) {
                int idx = name.indexOf("apiproxy/");
                return name.substring(0, idx + "apiproxy/".length());
            }
        }
        // Default to "apiproxy/" if not found
        return "apiproxy/";
    }

    private Path findApiproxyDirectory(Path dirPath) throws IOException {
        // Check if this IS the apiproxy directory
        if (dirPath.getFileName().toString().equals("apiproxy") &&
            Files.isDirectory(dirPath.resolve("proxies"))) {
            return dirPath;
        }
        
        // Check for apiproxy subdirectory
        Path apiproxyPath = dirPath.resolve("apiproxy");
        if (Files.isDirectory(apiproxyPath)) {
            return apiproxyPath;
        }
        
        // Search one level deep
        try (var stream = Files.list(dirPath)) {
            Optional<Path> found = stream
                    .filter(Files::isDirectory)
                    .filter(p -> Files.isDirectory(p.resolve("apiproxy")))
                    .map(p -> p.resolve("apiproxy"))
                    .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
        }
        
        throw new IOException("Could not find apiproxy directory in: " + dirPath);
    }

    private void parseProxyDescriptor(ApigeeBundle bundle, ZipFile zipFile, String apiproxyRoot) {
        // Look for the proxy descriptor XML file
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(apiproxyRoot) && name.endsWith(".xml") && !entry.isDirectory()) {
                String relativePath = name.substring(apiproxyRoot.length());
                // Proxy descriptor is at root of apiproxy, not in subdirectories
                if (!relativePath.contains("/") && !relativePath.isEmpty()) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        proxyDescriptorParser.parse(is, bundle);
                        if (bundle.getName() == null) {
                            bundle.setName(relativePath.replace(".xml", ""));
                        }
                        return;
                    } catch (Exception e) {
                        log.warn("Failed to parse proxy descriptor: {}", name, e);
                    }
                }
            }
        }
    }

    private void parseProxyDescriptorFromDir(ApigeeBundle bundle, Path apiproxyPath) {
        try (var stream = Files.list(apiproxyPath)) {
            stream.filter(p -> p.toString().endsWith(".xml"))
                  .filter(Files::isRegularFile)
                  .findFirst()
                  .ifPresent(xmlFile -> {
                      try (InputStream is = Files.newInputStream(xmlFile)) {
                          proxyDescriptorParser.parse(is, bundle);
                          if (bundle.getName() == null) {
                              String fileName = xmlFile.getFileName().toString();
                              bundle.setName(fileName.replace(".xml", ""));
                          }
                      } catch (Exception e) {
                          log.warn("Failed to parse proxy descriptor: {}", xmlFile, e);
                      }
                  });
        } catch (IOException e) {
            log.warn("Failed to list apiproxy directory", e);
        }
    }

    private void parseProxyEndpoints(ApigeeBundle bundle, ZipFile zipFile, String apiproxyRoot) {
        String proxiesPath = apiproxyRoot + "proxies/";
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(proxiesPath) && name.endsWith(".xml") && !entry.isDirectory()) {
                try (InputStream is = zipFile.getInputStream(entry)) {
                    ProxyEndpoint endpoint = proxyEndpointParser.parse(is);
                    bundle.addProxyEndpoint(endpoint);
                    
                    // Update bundle basePath from proxy endpoint
                    if (bundle.getBasePath() == null && endpoint.getBasePath() != null) {
                        bundle.setBasePath(endpoint.getBasePath());
                    }
                    log.debug("Parsed proxy endpoint: {}", endpoint.getName());
                } catch (Exception e) {
                    log.warn("Failed to parse proxy endpoint: {}", name, e);
                }
            }
        }
    }

    private void parseProxyEndpointsFromDir(ApigeeBundle bundle, Path apiproxyPath) {
        Path proxiesDir = apiproxyPath.resolve("proxies");
        if (!Files.isDirectory(proxiesDir)) {
            log.warn("No proxies directory found");
            return;
        }
        
        try (var stream = Files.list(proxiesDir)) {
            stream.filter(p -> p.toString().endsWith(".xml"))
                  .filter(Files::isRegularFile)
                  .forEach(xmlFile -> {
                      try (InputStream is = Files.newInputStream(xmlFile)) {
                          ProxyEndpoint endpoint = proxyEndpointParser.parse(is);
                          bundle.addProxyEndpoint(endpoint);
                          
                          if (bundle.getBasePath() == null && endpoint.getBasePath() != null) {
                              bundle.setBasePath(endpoint.getBasePath());
                          }
                          log.debug("Parsed proxy endpoint: {}", endpoint.getName());
                      } catch (Exception e) {
                          log.warn("Failed to parse proxy endpoint: {}", xmlFile, e);
                      }
                  });
        } catch (IOException e) {
            log.warn("Failed to list proxies directory", e);
        }
    }

    private void parseTargetEndpoints(ApigeeBundle bundle, ZipFile zipFile, String apiproxyRoot) {
        String targetsPath = apiproxyRoot + "targets/";
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(targetsPath) && name.endsWith(".xml") && !entry.isDirectory()) {
                try (InputStream is = zipFile.getInputStream(entry)) {
                    TargetEndpoint endpoint = targetEndpointParser.parse(is);
                    bundle.addTargetEndpoint(endpoint);
                    log.debug("Parsed target endpoint: {}", endpoint.getName());
                } catch (Exception e) {
                    log.warn("Failed to parse target endpoint: {}", name, e);
                }
            }
        }
    }

    private void parseTargetEndpointsFromDir(ApigeeBundle bundle, Path apiproxyPath) {
        Path targetsDir = apiproxyPath.resolve("targets");
        if (!Files.isDirectory(targetsDir)) {
            log.warn("No targets directory found");
            return;
        }
        
        try (var stream = Files.list(targetsDir)) {
            stream.filter(p -> p.toString().endsWith(".xml"))
                  .filter(Files::isRegularFile)
                  .forEach(xmlFile -> {
                      try (InputStream is = Files.newInputStream(xmlFile)) {
                          TargetEndpoint endpoint = targetEndpointParser.parse(is);
                          bundle.addTargetEndpoint(endpoint);
                          log.debug("Parsed target endpoint: {}", endpoint.getName());
                      } catch (Exception e) {
                          log.warn("Failed to parse target endpoint: {}", xmlFile, e);
                      }
                  });
        } catch (IOException e) {
            log.warn("Failed to list targets directory", e);
        }
    }

    private void parsePolicies(ApigeeBundle bundle, ZipFile zipFile, String apiproxyRoot) {
        String policiesPath = apiproxyRoot + "policies/";
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(policiesPath) && name.endsWith(".xml") && !entry.isDirectory()) {
                try (InputStream is = zipFile.getInputStream(entry)) {
                    Policy policy = policyParser.parse(is);
                    bundle.addPolicy(policy);
                    log.debug("Parsed policy: {} ({})", policy.getName(), policy.getType());
                } catch (Exception e) {
                    log.warn("Failed to parse policy: {}", name, e);
                }
            }
        }
    }

    private void parsePoliciesFromDir(ApigeeBundle bundle, Path apiproxyPath) {
        Path policiesDir = apiproxyPath.resolve("policies");
        if (!Files.isDirectory(policiesDir)) {
            log.debug("No policies directory found");
            return;
        }
        
        try (var stream = Files.list(policiesDir)) {
            stream.filter(p -> p.toString().endsWith(".xml"))
                  .filter(Files::isRegularFile)
                  .forEach(xmlFile -> {
                      try (InputStream is = Files.newInputStream(xmlFile)) {
                          Policy policy = policyParser.parse(is);
                          bundle.addPolicy(policy);
                          log.debug("Parsed policy: {} ({})", policy.getName(), policy.getType());
                      } catch (Exception e) {
                          log.warn("Failed to parse policy: {}", xmlFile, e);
                      }
                  });
        } catch (IOException e) {
            log.warn("Failed to list policies directory", e);
        }
    }
}
