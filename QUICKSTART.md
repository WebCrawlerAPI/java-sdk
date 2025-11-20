# WebCrawlerAPI Standalone Java SDK - Quick Start

## 1. Copy the file

Copy `WebCrawlerAPI.java` into your project:

```bash
# For example, into your src directory
cp WebCrawlerAPI.java /path/to/your/project/src/
```

## 2. Use it in your code

```java
public class MyApp {
    public static void main(String[] args) {
        // Create client
        WebCrawlerAPI client = new WebCrawlerAPI("your-api-key");

        try {
            // Scrape a page
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

## 3. Compile and run

```bash
# Compile
javac MyApp.java WebCrawlerAPI.java

# Run
java MyApp
```

## Try the Example

```bash
cd example/src

# Copy the SDK file
cp ../../WebCrawlerAPI.java .

# Compile
javac Example.java WebCrawlerAPI.java

# Run (replace with your API key)
API_KEY=your-api-key java Example

# Or for local testing
API_KEY=test-api-key API_BASE_URL=http://localhost:8080 java Example
```

## Using the compile-and-run script

```bash
cd example
chmod +x compile-and-run.sh
API_KEY=your-api-key ./compile-and-run.sh
```

That's it! No build tools, no dependencies, just Java.

## API Methods

### crawl() - Crawl multiple pages

```java
CrawlResult result = client.crawl(
    "https://example.com",  // URL to crawl
    "markdown",              // Content type: html, cleaned, markdown
    10                       // Maximum pages to crawl
);

for (CrawlItem item : result.items) {
    System.out.println(item.url);
}
```

### scrape() - Scrape one page (synchronous)

```java
ScrapeResult result = client.scrape(
    "https://example.com",  // URL to scrape
    "markdown"              // Content type
);

System.out.println(result.content);
```

### scrapeAsync() - Scrape one page (asynchronous)

```java
// Start scrape
String scrapeId = client.scrapeAsync(
    "https://example.com",
    "html"
);

// Check status later
ScrapeResult result = client.getScrape(scrapeId);
if ("done".equals(result.status)) {
    System.out.println(result.html);
}
```

For more details, see [README.md](README.md).
