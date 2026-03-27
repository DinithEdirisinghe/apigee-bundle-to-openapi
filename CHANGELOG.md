# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-03-27

### Added
- **Environment Variable Support for Service Account JSON**
  - New `serviceAccountKeyFromEnv()` method to read from `APIGEE_SERVICE_ACCOUNT_JSON` environment variable
  - New `serviceAccountKeyFromEnv(String varName)` method to read from custom environment variables
  - Improved error messages when environment variables are not set
  - Perfect for Docker, Kubernetes, and CI/CD deployments

- **Enhanced JSON Output Methods**
  - Added `convertFromApigeeToJson(config, proxyName, options)` - with custom options
  - Added `convertFromApigeeToJson(config, proxyName, revision, options)` - with specific revision and options
  - Now JSON methods have equal functionality to YAML methods

- **Enhanced YAML Output Methods**
  - Added `convertFromApigeeToYaml(config, proxyName, options)` - with custom options
  - Added `convertFromApigeeToYaml(config, proxyName, revision, options)` - with specific revision and options

- **Documentation**
  - Added `QUICK_REFERENCE.md` - Quick reference guide for authentication and output formats
  - Added `.env.example` - Template for environment variable configuration
  - Added comprehensive examples for all authentication methods

### Changed
- Updated README.md with environment variable examples
- Enhanced USAGE_EXAMPLES.md with more comprehensive examples
- Improved API documentation in all classes

### Fixed
- Fixed Java 11 compatibility issues in test files
- Fixed hardcoded values in test files to use dynamic proxy names

## [1.0.0] - 2024-XX-XX

### Added
- Initial release
- Convert Apigee proxy bundles (ZIP or directory) to OpenAPI 3.0 specifications
- Support for both JSON and YAML output formats
- Direct Apigee API integration with service account authentication
- Smart path extraction from conditional flows
- Parameter inference from policy XMLs
- Security scheme detection (API Key, OAuth2, Basic Auth)
- Offline operation from local files
- Comprehensive Java API

[1.1.0]: https://github.com/dinithedirisinghe/apigee-bundle-to-openapi/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/dinithedirisinghe/apigee-bundle-to-openapi/releases/tag/v1.0.0
