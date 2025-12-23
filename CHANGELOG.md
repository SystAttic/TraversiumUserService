# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - Unreleased
### Added
- Prometheus metrics endpoint
- Add gRPC support for user service
- Add circuit breaker and retry logic for external service calls
- ELK stack integration
- Add examples to swagger documentation
- Support for config server
- Add documentation

## [1.1.0] - 2025-12-03
### Added
- Add multitenancy support
- Add healthcheck endpoint
- Add kafka for notifications and audit data

## [1.0.0] - 2025-11-06
### Added
- Initial release of Traversium User Service
- User authentication and authorization
- RESTful API for user management
- Docker support with multi-platform builds (amd64, arm64)
- GitHub Actions CI/CD pipeline

### Security
- Implemented JWT-based authentication