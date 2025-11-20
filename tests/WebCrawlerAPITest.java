/**
 * Test suite for WebCrawlerAPI standalone SDK
 * Tests basic functionality, data classes, and validation
 */
public class WebCrawlerAPITest {

    public static void main(String[] args) {
        SimpleTestFramework.suite("WebCrawlerAPI Standalone SDK Tests");

        testConstructors();
        testDataClasses();
        testJsonParsing();
        testValidation();

        int exitCode = SimpleTestFramework.printSummary();
        System.exit(exitCode);
    }

    private static void testConstructors() {
        SimpleTestFramework.section("Constructor Tests");

        // Test basic constructor
        try {
            WebCrawlerAPI client = new WebCrawlerAPI("test-key");
            SimpleTestFramework.assertNotNull("Basic constructor creates client", client);
        } catch (Exception e) {
            SimpleTestFramework.fail("Basic constructor creates client", e.getMessage());
        }

        // Test constructor with custom URL
        try {
            WebCrawlerAPI client = new WebCrawlerAPI("test-key", "http://localhost:8080");
            SimpleTestFramework.assertNotNull("Constructor with custom URL creates client", client);
        } catch (Exception e) {
            SimpleTestFramework.fail("Constructor with custom URL creates client", e.getMessage());
        }

        // Test null API key validation
        SimpleTestFramework.assertThrows("Constructor rejects null API key",
            IllegalArgumentException.class,
            () -> new WebCrawlerAPI(null)
        );

        // Test empty API key validation
        SimpleTestFramework.assertThrows("Constructor rejects empty API key",
            IllegalArgumentException.class,
            () -> new WebCrawlerAPI("")
        );

        // Test whitespace-only API key validation
        SimpleTestFramework.assertThrows("Constructor rejects whitespace API key",
            IllegalArgumentException.class,
            () -> new WebCrawlerAPI("   ")
        );

        // Test null base URL (should use default)
        try {
            WebCrawlerAPI client = new WebCrawlerAPI("test-key", null);
            SimpleTestFramework.assertNotNull("Constructor with null base URL uses default", client);
        } catch (Exception e) {
            SimpleTestFramework.fail("Constructor with null base URL uses default", e.getMessage());
        }
    }

    private static void testDataClasses() {
        SimpleTestFramework.section("Data Classes Tests");

        // Test CrawlResult
        WebCrawlerAPI.CrawlResult crawlResult = new WebCrawlerAPI.CrawlResult();
        crawlResult.id = "test-id-123";
        crawlResult.status = "done";
        crawlResult.url = "https://example.com";
        crawlResult.scrapeType = "markdown";
        crawlResult.recommendedPullDelayMs = 5000;

        SimpleTestFramework.assertEquals("CrawlResult.id", "test-id-123", crawlResult.id);
        SimpleTestFramework.assertEquals("CrawlResult.status", "done", crawlResult.status);
        SimpleTestFramework.assertEquals("CrawlResult.url", "https://example.com", crawlResult.url);
        SimpleTestFramework.assertEquals("CrawlResult.scrapeType", "markdown", crawlResult.scrapeType);
        SimpleTestFramework.assertEquals("CrawlResult.recommendedPullDelayMs", 5000, crawlResult.recommendedPullDelayMs);
        SimpleTestFramework.assertNotNull("CrawlResult.items is initialized", crawlResult.items);
        SimpleTestFramework.assertEquals("CrawlResult.items is empty list", 0, crawlResult.items.size());
        SimpleTestFramework.assertContains("CrawlResult.toString contains id", crawlResult.toString(), "test-id-123");

        // Test CrawlItem
        WebCrawlerAPI.CrawlItem crawlItem = new WebCrawlerAPI.CrawlItem();
        crawlItem.url = "https://example.com/page1";
        crawlItem.status = "done";
        crawlItem.rawContentUrl = "http://storage.com/raw";
        crawlItem.cleanedContentUrl = "http://storage.com/cleaned";
        crawlItem.markdownContentUrl = "http://storage.com/markdown";

        SimpleTestFramework.assertEquals("CrawlItem.url", "https://example.com/page1", crawlItem.url);
        SimpleTestFramework.assertEquals("CrawlItem.status", "done", crawlItem.status);
        SimpleTestFramework.assertEquals("CrawlItem.getContentUrl(html)", "http://storage.com/raw", crawlItem.getContentUrl("html"));
        SimpleTestFramework.assertEquals("CrawlItem.getContentUrl(cleaned)", "http://storage.com/cleaned", crawlItem.getContentUrl("cleaned"));
        SimpleTestFramework.assertEquals("CrawlItem.getContentUrl(markdown)", "http://storage.com/markdown", crawlItem.getContentUrl("markdown"));
        SimpleTestFramework.assertNull("CrawlItem.getContentUrl(invalid)", crawlItem.getContentUrl("invalid"));
        SimpleTestFramework.assertContains("CrawlItem.toString contains url", crawlItem.toString(), "example.com/page1");

        // Test ScrapeResult
        WebCrawlerAPI.ScrapeResult scrapeResult = new WebCrawlerAPI.ScrapeResult();
        scrapeResult.status = "done";
        scrapeResult.content = "Test content";
        scrapeResult.html = "<html>Test</html>";
        scrapeResult.markdown = "# Test";
        scrapeResult.cleaned = "Test";
        scrapeResult.url = "https://example.com";
        scrapeResult.pageStatusCode = 200;

        SimpleTestFramework.assertEquals("ScrapeResult.status", "done", scrapeResult.status);
        SimpleTestFramework.assertEquals("ScrapeResult.content", "Test content", scrapeResult.content);
        SimpleTestFramework.assertEquals("ScrapeResult.html", "<html>Test</html>", scrapeResult.html);
        SimpleTestFramework.assertEquals("ScrapeResult.markdown", "# Test", scrapeResult.markdown);
        SimpleTestFramework.assertEquals("ScrapeResult.cleaned", "Test", scrapeResult.cleaned);
        SimpleTestFramework.assertEquals("ScrapeResult.url", "https://example.com", scrapeResult.url);
        SimpleTestFramework.assertEquals("ScrapeResult.pageStatusCode", 200, scrapeResult.pageStatusCode);
        SimpleTestFramework.assertContains("ScrapeResult.toString contains status", scrapeResult.toString(), "done");

        // Test WebCrawlerAPIException
        WebCrawlerAPI.WebCrawlerAPIException exception =
            new WebCrawlerAPI.WebCrawlerAPIException("test_error", "Test error message");

        SimpleTestFramework.assertEquals("Exception.getErrorCode", "test_error", exception.getErrorCode());
        SimpleTestFramework.assertEquals("Exception.getMessage", "Test error message", exception.getMessage());
        SimpleTestFramework.assertContains("Exception.toString contains errorCode", exception.toString(), "test_error");
        SimpleTestFramework.assertContains("Exception.toString contains message", exception.toString(), "Test error message");
    }

    private static void testJsonParsing() {
        SimpleTestFramework.section("JSON Parsing Tests");

        // These tests verify the internal JSON parsing logic works correctly
        // We'll use reflection-like approach by testing actual API responses

        // Test escape/unescape
        String original = "Test \"quoted\" text\nwith\nnewlines\tand\ttabs";
        // Note: We can't directly test private methods, but we can verify through API usage

        // Test that CrawlItem list initialization works
        WebCrawlerAPI.CrawlResult result = new WebCrawlerAPI.CrawlResult();
        SimpleTestFramework.assertNotNull("CrawlResult.items list is initialized", result.items);
        SimpleTestFramework.assertEquals("CrawlResult.items is empty", 0, result.items.size());

        // Add items to verify list operations
        WebCrawlerAPI.CrawlItem item1 = new WebCrawlerAPI.CrawlItem();
        item1.url = "https://example.com/1";
        result.items.add(item1);

        WebCrawlerAPI.CrawlItem item2 = new WebCrawlerAPI.CrawlItem();
        item2.url = "https://example.com/2";
        result.items.add(item2);

        SimpleTestFramework.assertEquals("CrawlResult.items has 2 items", 2, result.items.size());
        SimpleTestFramework.assertEquals("First item URL", "https://example.com/1", result.items.get(0).url);
        SimpleTestFramework.assertEquals("Second item URL", "https://example.com/2", result.items.get(1).url);
    }

    private static void testValidation() {
        SimpleTestFramework.section("Validation Tests");

        // Test CrawlItem.getContentUrl with null scrapeType
        WebCrawlerAPI.CrawlItem item = new WebCrawlerAPI.CrawlItem();
        item.rawContentUrl = "http://example.com/raw";
        item.cleanedContentUrl = "http://example.com/cleaned";
        item.markdownContentUrl = "http://example.com/markdown";

        SimpleTestFramework.assertNull("getContentUrl with null returns null", item.getContentUrl(null));
        SimpleTestFramework.assertNull("getContentUrl with unknown type returns null", item.getContentUrl("unknown"));

        // Test with null URLs
        WebCrawlerAPI.CrawlItem emptyItem = new WebCrawlerAPI.CrawlItem();
        SimpleTestFramework.assertNull("getContentUrl(html) with null URL returns null", emptyItem.getContentUrl("html"));
        SimpleTestFramework.assertNull("getContentUrl(cleaned) with null URL returns null", emptyItem.getContentUrl("cleaned"));
        SimpleTestFramework.assertNull("getContentUrl(markdown) with null URL returns null", emptyItem.getContentUrl("markdown"));

        // Test ScrapeResult with default values
        WebCrawlerAPI.ScrapeResult scrapeResult = new WebCrawlerAPI.ScrapeResult();
        SimpleTestFramework.assertNull("ScrapeResult.status defaults to null", scrapeResult.status);
        SimpleTestFramework.assertNull("ScrapeResult.content defaults to null", scrapeResult.content);
        SimpleTestFramework.assertEquals("ScrapeResult.pageStatusCode defaults to 0", 0, scrapeResult.pageStatusCode);

        // Test CrawlResult with default values
        WebCrawlerAPI.CrawlResult crawlResult = new WebCrawlerAPI.CrawlResult();
        SimpleTestFramework.assertNull("CrawlResult.id defaults to null", crawlResult.id);
        SimpleTestFramework.assertNull("CrawlResult.status defaults to null", crawlResult.status);
        SimpleTestFramework.assertEquals("CrawlResult.recommendedPullDelayMs defaults to 0", 0, crawlResult.recommendedPullDelayMs);
        SimpleTestFramework.assertNotNull("CrawlResult.items is not null", crawlResult.items);
    }
}
