# WebCrawlerAPI Standalone SDK - Tests

Comprehensive test suite for the standalone Java SDK without using Maven, Gradle, or any test frameworks.

## Test Structure

### Test Framework

**`SimpleTestFramework.java`** - Lightweight test framework with zero dependencies

Features:
- ✅ Basic assertions: `assertTrue`, `assertFalse`, `assertEquals`, `assertNotNull`
- ✅ String assertions: `assertContains`
- ✅ Exception testing: `assertThrows`
- ✅ Test organization: `suite()`, `section()`
- ✅ Failure tracking and summary reports
- ✅ Exit codes for CI/CD integration

### Test Suites

#### 1. Unit Tests (`WebCrawlerAPITest.java`)

Tests core SDK functionality without making API calls:

- **Constructor Tests** (6 tests)
  - Basic constructor
  - Constructor with custom URL
  - API key validation (null, empty, whitespace)
  - Null base URL handling

- **Data Classes Tests** (27 tests)
  - CrawlResult fields and methods
  - CrawlItem fields and `getContentUrl()` helper
  - ScrapeResult fields
  - WebCrawlerAPIException

- **JSON Parsing Tests** (5 tests)
  - List initialization
  - Item parsing
  - Array operations

- **Validation Tests** (12 tests)
  - Null handling
  - Default values
  - Edge cases

**Total: 50 unit tests**

#### 2. Integration Tests (`IntegrationTest.java`)

Tests actual API interactions (requires API key):

- **Scrape Tests (Synchronous)**
  - Markdown scraping
  - HTML scraping
  - Content validation

- **Scrape Tests (Asynchronous)**
  - Async job creation
  - Polling mechanism
  - Status checking

- **Crawl Tests**
  - Multi-page crawling
  - Items limit enforcement
  - Job status tracking
  - Null scrape type handling

## Running Tests

### Quick Start

```bash
cd tests
./run-tests.sh
```

### Run Unit Tests Only

```bash
cd tests
./run-tests.sh
# Skips integration tests if API_KEY is not set
```

### Run All Tests (Including Integration)

```bash
cd tests
API_KEY=your-api-key ./run-tests.sh
```

### Run with Local API

```bash
cd tests
API_KEY=test-key API_BASE_URL=http://localhost:8080 ./run-tests.sh
```

### Manual Test Execution

```bash
# Compile
javac -d bin SimpleTestFramework.java
javac -d bin WebCrawlerAPI.java
javac -cp bin -d bin WebCrawlerAPITest.java IntegrationTest.java

# Run unit tests
java -cp bin WebCrawlerAPITest

# Run integration tests
API_KEY=your-key java -cp bin IntegrationTest
```

## Test Output

### Successful Run

```
============================================================
WebCrawlerAPI Standalone SDK Tests
============================================================

Constructor Tests
-----------------
  ✓ Basic constructor creates client
  ✓ Constructor with custom URL creates client
  ...

============================================================
Test Summary
============================================================
Total tests:  50
Passed:       50 ✓
Failed:       0
Success rate: 100%
============================================================
```

### With Failures

```
  ✗ Test name (expected: value, got: othervalue)

============================================================
Test Summary
============================================================
Total tests:  50
Passed:       45 ✓
Failed:       5 ✗
Success rate: 90%

Failures:
  - FAILED: Test name - Expected: value, but was: othervalue
============================================================
```

## CI/CD Integration

The test runner returns appropriate exit codes:

- `0` - All tests passed
- `1` - One or more tests failed

Example GitHub Actions:

```yaml
- name: Run Tests
  run: |
    cd sdk/java/standalone-sdk/tests
    ./run-tests.sh
```

Example GitLab CI:

```yaml
test:
  script:
    - cd sdk/java/standalone-sdk/tests
    - ./run-tests.sh
```

## Adding New Tests

### Unit Test Example

```java
private static void testMyFeature() {
    SimpleTestFramework.section("My Feature Tests");

    // Test something
    SimpleTestFramework.assertEquals("Feature works", "expected", "actual");
    SimpleTestFramework.assertTrue("Condition is true", someCondition);

    // Test exception
    SimpleTestFramework.assertThrows("Throws on invalid input",
        IllegalArgumentException.class,
        () -> myMethod(null)
    );
}
```

Then add to main():

```java
public static void main(String[] args) {
    SimpleTestFramework.suite("My Test Suite");

    testMyFeature();  // Add your test here

    int exitCode = SimpleTestFramework.printSummary();
    System.exit(exitCode);
}
```

### Integration Test Example

```java
private static void testNewEndpoint(WebCrawlerAPI client) {
    SimpleTestFramework.section("New Endpoint Tests");

    try {
        // Make API call
        var result = client.someMethod();

        // Validate results
        SimpleTestFramework.assertNotNull("Result is not null", result);
        SimpleTestFramework.assertEquals("Status is correct", "done", result.status);

    } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
        SimpleTestFramework.fail("Test failed", "API error: " + e.getMessage());
    }
}
```

## Test Coverage

### Covered Features

✅ Constructor validation
✅ Data class initialization
✅ JSON parsing (basic)
✅ Error handling
✅ HTTP requests (integration)
✅ Polling mechanism
✅ Content type handling
✅ Status code validation

### Not Covered

❌ Network error scenarios (would require mocking)
❌ Timeout handling (would require slow server)
❌ Large response handling
❌ Concurrent requests
❌ Memory/performance testing

## Requirements

- Java 8 or newer
- No external dependencies
- For integration tests: Valid API key

## File Structure

```
tests/
├── README.md                    # This file
├── run-tests.sh                 # Test runner script
├── SimpleTestFramework.java     # Test framework
├── WebCrawlerAPITest.java       # Unit tests
├── IntegrationTest.java         # Integration tests
├── WebCrawlerAPI.java           # SDK copy (auto-copied by script)
└── bin/                         # Compiled classes (generated)
    ├── SimpleTestFramework.class
    ├── WebCrawlerAPI*.class
    ├── WebCrawlerAPITest.class
    └── IntegrationTest.class
```

## Troubleshooting

### Compilation Errors

**Problem**: "cannot find symbol: class SimpleTestFramework"

**Solution**: Ensure compilation order is correct. The script compiles in this order:
1. SimpleTestFramework.java
2. WebCrawlerAPI.java
3. Test files (with -cp bin)

### Java Version Errors

**Problem**: "Java 17 or newer is required"

**Solution**: Install Java 17+ or update `JAVA_HOME`:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Integration Tests Fail

**Problem**: "Error: API_KEY environment variable not set"

**Solution**: Set API_KEY before running:
```bash
API_KEY=your-key ./run-tests.sh
```

**Problem**: Integration tests timeout or fail

**Solution**:
- Check your API key is valid
- Verify network connectivity
- For local testing, ensure local API is running

### Permission Denied

**Problem**: "Permission denied: ./run-tests.sh"

**Solution**: Make script executable:
```bash
chmod +x run-tests.sh
```

## Best Practices

1. **Run tests before commits**: Ensure changes don't break existing functionality
2. **Add tests for new features**: Every new method should have corresponding tests
3. **Test edge cases**: Null values, empty strings, invalid inputs
4. **Keep tests fast**: Unit tests should complete in seconds
5. **Use descriptive test names**: Clear test names make failures easier to debug

## Comparison with Standard Test Frameworks

| Feature | This Framework | JUnit 5 | TestNG |
|---------|---------------|---------|--------|
| Dependencies | None | JUnit libs | TestNG libs |
| Build tool | None | Maven/Gradle | Maven/Gradle |
| Setup time | Instant | Minutes | Minutes |
| Learning curve | Minimal | Moderate | Moderate |
| IDE integration | Basic | Excellent | Excellent |
| Assertions | Basic | Extensive | Extensive |
| Best for | Simple projects | Production | Enterprise |

## Contributing

When adding tests:

1. Follow existing test structure
2. Use clear, descriptive test names
3. Add comments for complex test logic
4. Ensure tests are independent (don't rely on execution order)
5. Clean up resources in tests (if any)

## Support

For issues with the test framework or SDK:
- Check existing tests for examples
- Review the SimpleTestFramework source
- See main SDK README for API documentation

---

**Test Status**: ✅ 50/50 unit tests passing (100%)

Last Updated: 2025-11-19
