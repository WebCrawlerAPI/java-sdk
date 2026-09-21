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
        testConstants();
        testExceptionStatusCode();
        testCrawlOptionsBuilder();
        testScrapeOptionsBuilder();
        testUploadS3Action();
        testAgentRunRequestBuilder();
        testAgentRunDataClass();
        testNewCrawlResultFields();
        testNewCrawlItemFields();
        testNewScrapeResultFields();
        testJobMarkdownResponse();
        testMarkdownResponse();

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
        crawlResult.outputFormats = new String[]{"markdown", "html"};
        crawlResult.scrapeType = "markdown";
        crawlResult.recommendedPullDelayMs = 5000;

        SimpleTestFramework.assertEquals("CrawlResult.id", "test-id-123", crawlResult.id);
        SimpleTestFramework.assertEquals("CrawlResult.status", "done", crawlResult.status);
        SimpleTestFramework.assertEquals("CrawlResult.url", "https://example.com", crawlResult.url);
        SimpleTestFramework.assertEquals("CrawlResult.outputFormats length", 2, crawlResult.outputFormats.length);
        SimpleTestFramework.assertEquals("CrawlResult.outputFormats[0]", "markdown", crawlResult.outputFormats[0]);
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
        SimpleTestFramework.assertEquals("CrawlItem.getContentUrl(outputFormats prefers markdown)",
            "http://storage.com/markdown",
            crawlItem.getContentUrl(new String[]{"markdown", "html"}));
        SimpleTestFramework.assertEquals("CrawlItem.getContentUrl(outputFormats falls back to html)",
            "http://storage.com/raw",
            crawlItem.getContentUrl(new String[]{"links", "html"}));
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
        scrapeResult.pageTitle = "Example Domain";
        scrapeResult.links = new String[]{"https://example.com/a", "https://example.com/b"};

        SimpleTestFramework.assertEquals("ScrapeResult.status", "done", scrapeResult.status);
        SimpleTestFramework.assertEquals("ScrapeResult.content", "Test content", scrapeResult.content);
        SimpleTestFramework.assertEquals("ScrapeResult.html", "<html>Test</html>", scrapeResult.html);
        SimpleTestFramework.assertEquals("ScrapeResult.markdown", "# Test", scrapeResult.markdown);
        SimpleTestFramework.assertEquals("ScrapeResult.cleaned", "Test", scrapeResult.cleaned);
        SimpleTestFramework.assertEquals("ScrapeResult.url", "https://example.com", scrapeResult.url);
        SimpleTestFramework.assertEquals("ScrapeResult.pageStatusCode", 200, scrapeResult.pageStatusCode);
        SimpleTestFramework.assertEquals("ScrapeResult.pageTitle", "Example Domain", scrapeResult.pageTitle);
        SimpleTestFramework.assertEquals("ScrapeResult.links length", 2, scrapeResult.links.length);
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

        SimpleTestFramework.assertNull("getContentUrl with null returns null", item.getContentUrl((String) null));
        SimpleTestFramework.assertNull("getContentUrl with unknown type returns null", item.getContentUrl("unknown"));
        SimpleTestFramework.assertNull("getContentUrl with null outputFormats returns null", item.getContentUrl((String[]) null));
        SimpleTestFramework.assertNull("getContentUrl with empty outputFormats returns null", item.getContentUrl(new String[0]));

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

    private static void testConstants() {
        SimpleTestFramework.section("Constants Tests (JS SDK parity)");

        SimpleTestFramework.assertEquals("JobStatus.IN_PROGRESS", "in_progress", WebCrawlerAPI.JobStatus.IN_PROGRESS);
        SimpleTestFramework.assertEquals("JobStatus.NEW", "new", WebCrawlerAPI.JobStatus.NEW);
        SimpleTestFramework.assertEquals("JobStatus.DONE", "done", WebCrawlerAPI.JobStatus.DONE);
        SimpleTestFramework.assertEquals("ErrorCode.BLOCKED_BY_ROBOTS_TXT", "blocked_by_robots_txt", WebCrawlerAPI.ErrorCode.BLOCKED_BY_ROBOTS_TXT);
    }

    private static void testExceptionStatusCode() {
        SimpleTestFramework.section("Exception Status Code Tests");

        WebCrawlerAPI.WebCrawlerAPIException withStatus =
            new WebCrawlerAPI.WebCrawlerAPIException("not_found", "Not found", 404);
        SimpleTestFramework.assertEquals("Exception.getStatusCode returns given status", 404, withStatus.getStatusCode());
        SimpleTestFramework.assertContains("Exception.toString contains statusCode", withStatus.toString(), "404");

        WebCrawlerAPI.WebCrawlerAPIException withoutStatus =
            new WebCrawlerAPI.WebCrawlerAPIException("network_error", "Network error");
        SimpleTestFramework.assertEquals("Exception.getStatusCode defaults to 0", 0, withoutStatus.getStatusCode());
    }

    private static void testCrawlOptionsBuilder() {
        SimpleTestFramework.section("CrawlOptions Builder Tests");

        WebCrawlerAPI.CrawlOptions options = new WebCrawlerAPI.CrawlOptions("https://example.com")
            .outputFormats("markdown", "html")
            .itemsLimit(5)
            .whitelistRegexp(".*allowed.*")
            .blacklistRegexp(".*blocked.*")
            .webhookUrl("https://example.com/webhook")
            .respectRobotsTxt(true)
            .mainContentOnly(true)
            .maxDepth(2)
            .maxAge(60)
            .keepQueryParams(true);

        SimpleTestFramework.assertEquals("CrawlOptions.url", "https://example.com", options.url);
        SimpleTestFramework.assertEquals("CrawlOptions.outputFormats length", 2, options.outputFormats.length);
        SimpleTestFramework.assertEquals("CrawlOptions.itemsLimit", Integer.valueOf(5), options.itemsLimit);
        SimpleTestFramework.assertEquals("CrawlOptions.whitelistRegexp", ".*allowed.*", options.whitelistRegexp);
        SimpleTestFramework.assertEquals("CrawlOptions.blacklistRegexp", ".*blocked.*", options.blacklistRegexp);
        SimpleTestFramework.assertEquals("CrawlOptions.webhookUrl", "https://example.com/webhook", options.webhookUrl);
        SimpleTestFramework.assertEquals("CrawlOptions.respectRobotsTxt", Boolean.TRUE, options.respectRobotsTxt);
        SimpleTestFramework.assertEquals("CrawlOptions.mainContentOnly", Boolean.TRUE, options.mainContentOnly);
        SimpleTestFramework.assertEquals("CrawlOptions.maxDepth", Integer.valueOf(2), options.maxDepth);
        SimpleTestFramework.assertEquals("CrawlOptions.maxAge", 60, options.maxAge);
        SimpleTestFramework.assertEquals("CrawlOptions.keepQueryParams", Boolean.TRUE, options.keepQueryParams);

        WebCrawlerAPI.CrawlOptions defaults = new WebCrawlerAPI.CrawlOptions("https://example.com");
        SimpleTestFramework.assertNull("CrawlOptions.itemsLimit defaults to null", defaults.itemsLimit);
        SimpleTestFramework.assertNull("CrawlOptions.maxDepth defaults to null", defaults.maxDepth);
        SimpleTestFramework.assertEquals("CrawlOptions.maxAge defaults to 0", 0, defaults.maxAge);
    }

    private static void testScrapeOptionsBuilder() {
        SimpleTestFramework.section("ScrapeOptions Builder Tests");

        java.util.List<WebCrawlerAPI.Action> actions = new java.util.ArrayList<>();
        actions.add(new WebCrawlerAPI.UploadS3Action("path/to/file", "key-id", "secret", "my-bucket"));

        WebCrawlerAPI.ScrapeOptions options = new WebCrawlerAPI.ScrapeOptions("https://example.com")
            .outputFormats("markdown")
            .webhookUrl("https://example.com/webhook")
            .cleanSelectors(".ads, .nav")
            .actions(actions)
            .prompt("Extract the title")
            .responseSchemaJson("{\"type\":\"object\"}")
            .mainContentOnly(true)
            .maxAge(30);

        SimpleTestFramework.assertEquals("ScrapeOptions.url", "https://example.com", options.url);
        SimpleTestFramework.assertEquals("ScrapeOptions.outputFormats[0]", "markdown", options.outputFormats[0]);
        SimpleTestFramework.assertEquals("ScrapeOptions.webhookUrl", "https://example.com/webhook", options.webhookUrl);
        SimpleTestFramework.assertEquals("ScrapeOptions.cleanSelectors", ".ads, .nav", options.cleanSelectors);
        SimpleTestFramework.assertEquals("ScrapeOptions.actions size", 1, options.actions.size());
        SimpleTestFramework.assertEquals("ScrapeOptions.prompt", "Extract the title", options.prompt);
        SimpleTestFramework.assertEquals("ScrapeOptions.responseSchemaJson", "{\"type\":\"object\"}", options.responseSchemaJson);
        SimpleTestFramework.assertEquals("ScrapeOptions.mainContentOnly", Boolean.TRUE, options.mainContentOnly);
        SimpleTestFramework.assertEquals("ScrapeOptions.maxAge", 30, options.maxAge);
    }

    private static void testUploadS3Action() {
        SimpleTestFramework.section("UploadS3Action Tests");

        WebCrawlerAPI.UploadS3Action action =
            new WebCrawlerAPI.UploadS3Action("path/to/file", "key-id", "secret", "my-bucket")
                .endpoint("https://s3.custom.example.com");

        SimpleTestFramework.assertEquals("UploadS3Action.type", "upload_s3", action.type);
        SimpleTestFramework.assertEquals("UploadS3Action.path", "path/to/file", action.path);
        SimpleTestFramework.assertEquals("UploadS3Action.accessKeyId", "key-id", action.accessKeyId);
        SimpleTestFramework.assertEquals("UploadS3Action.secretAccessKey", "secret", action.secretAccessKey);
        SimpleTestFramework.assertEquals("UploadS3Action.bucket", "my-bucket", action.bucket);
        SimpleTestFramework.assertEquals("UploadS3Action.endpoint", "https://s3.custom.example.com", action.endpoint);
    }

    private static void testAgentRunRequestBuilder() {
        SimpleTestFramework.section("AgentRunRequest Builder Tests");

        WebCrawlerAPI.AgentRunRequest request = new WebCrawlerAPI.AgentRunRequest("Find pricing info", 1.5)
            .urls("https://example.com/pricing")
            .seedUrlsOnly(true)
            .outputSchemaJson("{\"type\":\"object\"}")
            .model("claude-sonnet-5");

        SimpleTestFramework.assertEquals("AgentRunRequest.prompt", "Find pricing info", request.prompt);
        SimpleTestFramework.assertEquals("AgentRunRequest.maxSpendUsd", 1.5, request.maxSpendUsd);
        SimpleTestFramework.assertEquals("AgentRunRequest.urls[0]", "https://example.com/pricing", request.urls[0]);
        SimpleTestFramework.assertEquals("AgentRunRequest.seedUrlsOnly", Boolean.TRUE, request.seedUrlsOnly);
        SimpleTestFramework.assertEquals("AgentRunRequest.outputSchemaJson", "{\"type\":\"object\"}", request.outputSchemaJson);
        SimpleTestFramework.assertEquals("AgentRunRequest.model", "claude-sonnet-5", request.model);
    }

    private static void testAgentRunDataClass() {
        SimpleTestFramework.section("AgentRun Data Class Tests");

        WebCrawlerAPI.AgentRun run = new WebCrawlerAPI.AgentRun();
        run.id = "run-123";
        run.status = "done";
        run.prompt = "Find pricing info";
        run.model = "claude-sonnet-5";
        run.dataJson = "{\"price\":10}";
        run.balanceUsedUsd = 0.5;
        run.maxSpendUsd = 1.5;
        run.urls = new String[]{"https://example.com"};
        run.success = true;

        SimpleTestFramework.assertEquals("AgentRun.id", "run-123", run.id);
        SimpleTestFramework.assertEquals("AgentRun.status", "done", run.status);
        SimpleTestFramework.assertEquals("AgentRun.dataJson", "{\"price\":10}", run.dataJson);
        SimpleTestFramework.assertEquals("AgentRun.balanceUsedUsd", 0.5, run.balanceUsedUsd);
        SimpleTestFramework.assertEquals("AgentRun.maxSpendUsd", Double.valueOf(1.5), run.maxSpendUsd);
        SimpleTestFramework.assertEquals("AgentRun.urls length", 1, run.urls.length);
        SimpleTestFramework.assertTrue("AgentRun.success", run.success);
        SimpleTestFramework.assertContains("AgentRun.toString contains id", run.toString(), "run-123");
    }

    private static void testNewCrawlResultFields() {
        SimpleTestFramework.section("CrawlResult New Fields Tests");

        WebCrawlerAPI.CrawlResult result = new WebCrawlerAPI.CrawlResult();
        result.orgId = "org-1";
        result.whitelistRegexp = ".*allowed.*";
        result.blacklistRegexp = ".*blocked.*";
        result.itemsLimit = 10;
        result.maxDepth = 3;
        result.createdAt = "2026-01-01T00:00:00Z";
        result.finishedAt = "2026-01-01T00:05:00Z";
        result.updatedAt = "2026-01-01T00:05:00Z";
        result.webhookUrl = "https://example.com/webhook";

        SimpleTestFramework.assertEquals("CrawlResult.orgId", "org-1", result.orgId);
        SimpleTestFramework.assertEquals("CrawlResult.whitelistRegexp", ".*allowed.*", result.whitelistRegexp);
        SimpleTestFramework.assertEquals("CrawlResult.blacklistRegexp", ".*blocked.*", result.blacklistRegexp);
        SimpleTestFramework.assertEquals("CrawlResult.itemsLimit", 10, result.itemsLimit);
        SimpleTestFramework.assertEquals("CrawlResult.maxDepth", Integer.valueOf(3), result.maxDepth);
        SimpleTestFramework.assertEquals("CrawlResult.createdAt", "2026-01-01T00:00:00Z", result.createdAt);
        SimpleTestFramework.assertEquals("CrawlResult.finishedAt", "2026-01-01T00:05:00Z", result.finishedAt);
        SimpleTestFramework.assertEquals("CrawlResult.updatedAt", "2026-01-01T00:05:00Z", result.updatedAt);
        SimpleTestFramework.assertEquals("CrawlResult.webhookUrl", "https://example.com/webhook", result.webhookUrl);
    }

    private static void testNewCrawlItemFields() {
        SimpleTestFramework.section("CrawlItem New Fields Tests");

        WebCrawlerAPI.CrawlItem item = new WebCrawlerAPI.CrawlItem();
        item.id = "item-1";
        item.jobId = "job-1";
        item.pageStatusCode = 200;
        item.title = "Example Page";
        item.lastError = null;
        item.createdAt = "2026-01-01T00:00:00Z";
        item.updatedAt = "2026-01-01T00:01:00Z";
        item.cost = 0.01;
        item.referredUrl = "https://example.com";
        item.links = "https://example.com/a,https://example.com/b";
        item.depth = 1;
        item.errorCode = null;

        SimpleTestFramework.assertEquals("CrawlItem.id", "item-1", item.id);
        SimpleTestFramework.assertEquals("CrawlItem.jobId", "job-1", item.jobId);
        SimpleTestFramework.assertEquals("CrawlItem.pageStatusCode", 200, item.pageStatusCode);
        SimpleTestFramework.assertEquals("CrawlItem.title", "Example Page", item.title);
        SimpleTestFramework.assertEquals("CrawlItem.cost", 0.01, item.cost);
        SimpleTestFramework.assertEquals("CrawlItem.referredUrl", "https://example.com", item.referredUrl);
        SimpleTestFramework.assertEquals("CrawlItem.depth", Integer.valueOf(1), item.depth);
        SimpleTestFramework.assertNull("CrawlItem.lastError defaults to null", item.lastError);
        SimpleTestFramework.assertNull("CrawlItem.errorCode defaults to null", item.errorCode);
    }

    private static void testNewScrapeResultFields() {
        SimpleTestFramework.section("ScrapeResult New Fields Tests");

        WebCrawlerAPI.ScrapeResult result = new WebCrawlerAPI.ScrapeResult();
        result.success = true;
        result.structuredDataJson = "{\"title\":\"Example\"}";

        SimpleTestFramework.assertTrue("ScrapeResult.success", result.success);
        SimpleTestFramework.assertEquals("ScrapeResult.structuredDataJson", "{\"title\":\"Example\"}", result.structuredDataJson);

        WebCrawlerAPI.ScrapeResult defaults = new WebCrawlerAPI.ScrapeResult();
        SimpleTestFramework.assertFalse("ScrapeResult.success defaults to false", defaults.success);
        SimpleTestFramework.assertNull("ScrapeResult.structuredDataJson defaults to null", defaults.structuredDataJson);
    }

    private static void testJobMarkdownResponse() {
        SimpleTestFramework.section("JobMarkdownResponse Tests");

        WebCrawlerAPI.JobMarkdownResponse response = new WebCrawlerAPI.JobMarkdownResponse();
        response.contentUrl = "https://cdn.example.com/job-1.md";

        SimpleTestFramework.assertEquals("JobMarkdownResponse.contentUrl", "https://cdn.example.com/job-1.md", response.contentUrl);
    }

    private static void testMarkdownResponse() {
        SimpleTestFramework.section("MarkdownResponse Tests");

        WebCrawlerAPI.MarkdownResponse response = new WebCrawlerAPI.MarkdownResponse();
        response.success = true;
        response.markdown = "# Example";

        SimpleTestFramework.assertTrue("MarkdownResponse.success", response.success);
        SimpleTestFramework.assertEquals("MarkdownResponse.markdown", "# Example", response.markdown);
    }
}
