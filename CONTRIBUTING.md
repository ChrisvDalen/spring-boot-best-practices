# Contributing to Spring Boot Best Practices

Thank you for your interest in contributing! This document explains the process for contributing to this project.

## Code of Conduct

By participating in this project, you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

### Reporting Bugs

Before opening a bug report, please search [existing issues](https://github.com/ChrisvDalen/spring-boot-best-practices/issues) to avoid duplicates.

When reporting a bug, include:
- A clear, descriptive title
- Steps to reproduce the issue
- Expected vs. actual behavior
- Spring Boot version, Java version, and OS

### Suggesting Enhancements

Open an issue with the `enhancement` label. Describe:
- The problem your suggestion would solve
- Your proposed solution
- Any alternatives you considered

### Submitting Pull Requests

1. **Fork** the repository and create your branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Write tests** for any new functionality.

3. **Ensure all tests pass**:
   ```bash
   ./mvnw test
   ```

4. **Follow the code style** — the project uses standard Java conventions enforced by Checkstyle.

5. **Commit** with a clear message following [Conventional Commits](https://www.conventionalcommits.org/):
   ```
   feat: add caching configuration example
   fix: correct property binding for nested objects
   docs: update README with Docker instructions
   ```

6. **Open a pull request** against `main`. Fill in the pull request template completely.

## Development Setup

```bash
git clone https://github.com/ChrisvDalen/spring-boot-best-practices.git
cd spring-boot-best-practices
./mvnw install -DskipTests
```

## Coding Standards

- Follow standard Java naming conventions
- Keep methods small and focused
- Write Javadoc for public APIs
- Prefer constructor injection over field injection
- Write both unit and integration tests for new features

## Questions?

See [SUPPORT.md](SUPPORT.md) for how to get help.
