# WebCrawlerAPI Standalone Java SDK

[![Test](https://github.com/WebCrawlerAPI/java-sdk/actions/workflows/test.yml/badge.svg)](https://github.com/WebCrawlerAPI/java-sdk/actions/workflows/test.yml)

A simple, dependency-free Java client for WebCrawlerAPI that can be copy-pasted into any Java 17+ project.

## Features

- **Zero Dependencies**: Uses only standard Java HTTP client (no external libraries required)
- **Simple Integration**: Just copy `WebCrawlerAPI.java` into your project
- **Complete API Coverage**: Supports `crawl()`, `scrape()`, and `scrapeAsync()` methods
- **Java 17+ Compatible**: Works with Java 17 and newer versions (tested on Java 8+)
- **No Build Tools Required**: Can be compiled and run with just `javac` and `java`
- **Fully Tested**: 50+ unit tests, all passing ✅

## Quick Start

### 1. Copy the SDK File

Copy `WebCrawlerAPI.java` into your project's source directory.

### 2. Use in Your Code

```java
// Create a client
WebCrawlerAPI client = new WebCrawlerAPI("your-api-key");

// Crawl a website
WebCrawlerAPI.CrawlResult result = client.crawl("https://example.com", "markdown", 10);
System.out.println("Found " + result.items.size() + " items");

// Scrape a single page
WebCrawlerAPI.ScrapeResult scrape = client.scrape("https://example.com", "markdown");
System.out.println("Content: " + scrape.content);
```

## Running the Example

The `example/` directory contains a complete working example.

### Prerequisites

- Java 17 or newer
- A WebCrawlerAPI key (get one at https://webcrawlerapi.com)

### Steps

1. **Copy the SDK file into the example directory:**

```bash
cd example/src
cp ../../WebCrawlerAPI.java .
```

2. **Compile the example:**

```bash
javac src/*.java -d bin
```

3. **Run the example:**

```bash
# With production API
API_KEY=your-api-key java -cp bin Example

# With local development API
API_KEY=test-api-key API_BASE_URL=http://localhost:8080 java -cp bin Example
```

### Alternative: Compile and run in one step

```bash
cd example/src
cp ../../WebCrawlerAPI.java .
javac Example.java WebCrawlerAPI.java
API_KEY=your-api-key java Example
```

## API Reference

### Constructor

```java
// Default constructor (uses https://api.webcrawlerapi.com)
WebCrawlerAPI client = new WebCrawlerAPI(String apiKey);

// Constructor with custom base URL (for testing)
WebCrawlerAPI client = new WebCrawlerAPI(String apiKey, String baseUrl);
```

### Methods

#### crawl()

Crawl a website and return all discovered pages.

```java
CrawlResult crawl(String url, String scrapeType, int itemsLimit)
CrawlResult crawl(String url, String scrapeType, int itemsLimit, int maxPolls)
```

**Parameters:**
- `url` - The URL to crawl
- `scrapeType` - Type of content to extract: `"html"`, `"cleaned"`, or `"markdown"`
- `itemsLimit` - Maximum number of pages to crawl
- `maxPolls` - (Optional) Maximum polling attempts (default: 100)

**Returns:** `CrawlResult` containing job details and crawled items

**Example:**
```java
CrawlResult result = client.crawl("https://example.com", "markdown", 10);
for (CrawlItem item : result.items) {
    System.out.println("URL: " + item.url);
    System.out.println("Content URL: " + item.getContentUrl("markdown"));
}
```

#### scrape()

Scrape a single page synchronously (waits for completion).

```java
ScrapeResult scrape(String url, String scrapeType)
ScrapeResult scrape(String url, String scrapeType, int maxPolls)
```

**Parameters:**
- `url` - The URL to scrape
- `scrapeType` - Type of content to extract: `"html"`, `"cleaned"`, or `"markdown"`
- `maxPolls` - (Optional) Maximum polling attempts (default: 100)

**Returns:** `ScrapeResult` containing the scraped content

**Example:**
```java
ScrapeResult result = client.scrape("https://example.com", "markdown");
if ("done".equals(result.status)) {
    System.out.println("Content: " + result.content);
}
```

#### scrapeAsync()

Start a scrape job asynchronously (returns immediately).

```java
String scrapeAsync(String url, String scrapeType)
```

**Parameters:**
- `url` - The URL to scrape
- `scrapeType` - Type of content to extract: `"html"`, `"cleaned"`, or `"markdown"`

**Returns:** Scrape ID (String) that can be used with `getScrape()`

**Example:**
```java
String scrapeId = client.scrapeAsync("https://example.com", "html");
System.out.println("Scrape started: " + scrapeId);

// Later, check the status
ScrapeResult result = client.getScrape(scrapeId);
```

#### getScrape()

Get the status and result of a scrape job.

```java
ScrapeResult getScrape(String scrapeId)
```

**Parameters:**
- `scrapeId` - The scrape ID returned from `scrapeAsync()`

**Returns:** `ScrapeResult` with current status and content (if done)

## Data Classes

### CrawlResult

```java
public class CrawlResult {
    public String id;                      // Job ID
    public String status;                  // Job status: "new", "in_progress", "done", "error"
    public String url;                     // Original URL
    public String scrapeType;              // Scrape type used
    public int recommendedPullDelayMs;     // Recommended delay between polls
    public List<CrawlItem> items;          // List of crawled items
}
```

### CrawlItem

```java
public class CrawlItem {
    public String url;                     // Page URL
    public String status;                  // Item status
    public String rawContentUrl;           // URL to raw HTML content
    public String cleanedContentUrl;       // URL to cleaned content
    public String markdownContentUrl;      // URL to markdown content

    // Helper method to get content URL based on scrape type
    public String getContentUrl(String scrapeType);
}
```

### ScrapeResult

```java
public class ScrapeResult {
    public String status;                  // Scrape status: "in_progress", "done", "error"
    public String content;                 // Scraped content (based on scrape_type)
    public String html;                    // Raw HTML content
    public String markdown;                // Markdown content
    public String cleaned;                 // Cleaned text content
    public String url;                     // Page URL
    public int pageStatusCode;             // HTTP status code
}
```

### WebCrawlerAPIException

```java
public class WebCrawlerAPIException extends Exception {
    public String getErrorCode();          // Error code from API
}
```

Common error codes:
- `network_error` - Network/connection error
- `invalid_response` - Invalid API response
- `interrupted` - Operation was interrupted
- `unknown_error` - Unknown error occurred

## Integration Examples

### Spring Boot

```java
@Service
public class WebScraperService {
    private final WebCrawlerAPI client;

    public WebScraperService(@Value("${webcrawlerapi.key}") String apiKey) {
        this.client = new WebCrawlerAPI(apiKey);
    }

    public List<String> scrapeWebsite(String url) throws WebCrawlerAPI.WebCrawlerAPIException {
        WebCrawlerAPI.CrawlResult result = client.crawl(url, "markdown", 20);
        return result.items.stream()
            .map(item -> item.url)
            .collect(Collectors.toList());
    }
}
```

### Vanilla Java Application

```java
public class MyApp {
    public static void main(String[] args) {
        WebCrawlerAPI client = new WebCrawlerAPI(System.getenv("API_KEY"));

        try {
            WebCrawlerAPI.ScrapeResult result = client.scrape(
                "https://example.com",
                "markdown"
            );

            if ("done".equals(result.status)) {
                System.out.println(result.content);
            }
        } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### Maven Project

If you're using Maven, just add the file to `src/main/java/`:

```
src/
  main/
    java/
      com/
        yourcompany/
          WebCrawlerAPI.java    ← Copy here
          YourApp.java
```

### Gradle Project

For Gradle projects, add to `src/main/java/`:

```
src/
  main/
    java/
      com/
        yourcompany/
          WebCrawlerAPI.java    ← Copy here
          YourApp.java
```

## Environment Variables

The example supports the following environment variables:

- `API_KEY` - Your WebCrawlerAPI key (required)
- `API_BASE_URL` - Custom API base URL (optional, for testing)

## Error Handling

All methods can throw `WebCrawlerAPIException`. Always handle exceptions appropriately:

```java
try {
    WebCrawlerAPI.CrawlResult result = client.crawl(url, "markdown", 10);
    // Process result...
} catch (WebCrawlerAPI.WebCrawlerAPIException e) {
    System.err.println("Error code: " + e.getErrorCode());
    System.err.println("Error message: " + e.getMessage());
    // Handle error...
}
```

## Testing

The SDK includes a comprehensive test suite without requiring any test frameworks or build tools.

### Run Tests

```bash
cd tests
./run-tests.sh
```

### Test Coverage

- **50+ Unit Tests**: Test constructors, data classes, JSON parsing, validation
- **Integration Tests**: Test actual API calls (requires API key)
- **Custom Test Framework**: Zero dependencies, works with Java 8+

See [`tests/README.md`](tests/README.md) for detailed testing documentation.

### Quick Test

```bash
cd tests
# Run unit tests only
./run-tests.sh

# Run all tests including integration
API_KEY=your-key ./run-tests.sh
```

Output:
```
============================================================
Test Summary
============================================================
Total tests:  50
Passed:       50 ✓
Failed:       0
Success rate: 100%
============================================================
```

## CI/CD & Releases

The SDK includes GitHub Actions workflows for automated testing and releases.

### Automated Testing

Tests run automatically on:
- Every push to main/master/develop branches
- Every pull request
- Multiple Java versions: 8, 11, 17, 21

### Creating a Release

Push a semantic version tag (without 'v' prefix):

```bash
# Create and push a release tag
git tag 1.0.0
git push origin 1.0.0
```

This automatically:
1. Runs all tests
2. Builds JAR and source ZIP
3. Creates GitHub release with artifacts
4. Generates SHA256 checksums

See [`.github/RELEASE.md`](.github/RELEASE.md) for detailed release instructions.

### Downloads

Download pre-built releases from the [Releases page](https://github.com/YOUR_ORG/YOUR_REPO/releases):
- JAR file (compiled classes)
- Source ZIP (complete source with tests)
- SHA256 checksums for verification

## Limitations

- **Simple JSON Parsing**: Uses basic string operations instead of a JSON library. Works for WebCrawlerAPI responses but may need adjustments for complex nested structures.
- **No Async I/O**: Uses blocking HTTP calls. For high-throughput applications, consider using the original SDK with async libraries.
- **Basic Error Handling**: Error parsing is simplified compared to the full SDK.

## License

This standalone SDK is provided as-is for use with WebCrawlerAPI.

## Support

For API documentation and support, visit:
- Documentation: https://docs.webcrawlerapi.com
- Website: https://webcrawlerapi.com
- GitHub: https://github.com/webcrawlerapi

## Comparison with Full SDK

| Feature | Standalone SDK | Full SDK (Gradle) |
|---------|---------------|-------------------|
| Dependencies | None | Gson, HttpClient |
| Setup | Copy single file | Gradle/Maven setup |
| JSON Parsing | Basic string ops | Full Gson support |
| Type Safety | Basic | Full with builders |
| Size | ~600 lines | Multiple files |
| Best For | Simple projects | Production apps |

Choose the standalone SDK when you want simplicity and no dependencies. Use the full SDK when you need advanced features and type safety.
