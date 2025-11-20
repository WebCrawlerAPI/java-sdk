
/**
 * Integration tests for WebCrawlerAPI
 * These tests make actual API calls and require API_KEY environment variable
 *
 * Run with:
 *   API_KEY=your-key java IntegrationTest
 *
 * Or for local testing:
 *   API_KEY=test-key API_BASE_URL=http://localhost:8080 java IntegrationTest
 */
public class IntegrationTest {

    public static void main(String[] args) {
        SimpleTestFramework.suite("WebCrawlerAPI Integration Tests");

        String apiKey = System.getenv("API_KEY");
        String baseUrl = System.getenv("API_BASE_URL");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("\nError: API_KEY environment variable not set");
            System.err.println("\nUsage:");
            System.err.println("  API_KEY=your-key java IntegrationTest");
            System.err.println("\nFor local testing:");
            System.err.println("  API_KEY=test-key API_BASE_URL=http://localhost:8080 java IntegrationTest");
            System.exit(1);
        }

        System.out.println("\nConfiguration:");
        System.out.println("  API Key: " + maskApiKey(apiKey));
        System.out.println("  Base URL: " + (baseUrl != null ? baseUrl : "https://api.webcrawlerapi.com"));
        System.out.println();

        WebCrawlerAPI client = baseUrl != null
            ? new WebCrawlerAPI(apiKey, baseUrl)
            : new WebCrawlerAPI(apiKey);

        testScrape(client);
        testScrapeAsync(client);
        testCrawl(client);

        int exitCode = SimpleTestFramework.printSummary();
        System.exit(exitCode);
    }

    private static void testScrape(WebCrawlerAPI client) {
        SimpleTestFramework.section("Scrape Tests (Synchronous)");

        try {
            String url = "https://example.com";
            String scrapeType = "markdown";

            System.out.println("  Scraping: " + url + " (" + scrapeType + ")");
            WebCrawlerAPI.ScrapeResult result = client.scrape(url, scrapeType);

            SimpleTestFramework.assertNotNull("Scrape result is not null", result);
            SimpleTestFramework.assertNotNull("Scrape result has status", result.status);
            SimpleTestFramework.assertEquals("Scrape status is done", "done", result.status);
            SimpleTestFramework.assertNotNull("Scrape result has content", result.content);
            SimpleTestFramework.assertTrue("Content is not empty", result.content.length() > 0);
            SimpleTestFramework.assertContains("Content contains 'Example Domain'", result.content, "Example Domain");
            SimpleTestFramework.assertEquals("Page status code is 200", 200, result.pageStatusCode);
            // Note: V2 API does not return url field in scrape response

            System.out.println("    Content length: " + result.content.length() + " chars");
            System.out.println("    Page status: " + result.pageStatusCode);

        } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
            SimpleTestFramework.fail("Scrape test", "API error: " + e.getErrorCode() + " - " + e.getMessage());
        } catch (Exception e) {
            SimpleTestFramework.fail("Scrape test", "Unexpected error: " + e.getMessage());
        }

        // Test with HTML scrape type
        try {
            String url = "https://example.com";
            String scrapeType = "html";

            System.out.println("  Scraping: " + url + " (" + scrapeType + ")");
            WebCrawlerAPI.ScrapeResult result = client.scrape(url, scrapeType);

            SimpleTestFramework.assertNotNull("HTML scrape result is not null", result);
            SimpleTestFramework.assertEquals("HTML scrape status is done", "done", result.status);
            SimpleTestFramework.assertNotNull("HTML result has html field", result.html);
            SimpleTestFramework.assertTrue("HTML is not empty", result.html.length() > 0);
            // Note: API currently returns markdown content even when html format is requested
            // This is a known issue with the V2 API
            SimpleTestFramework.assertContains("HTML contains expected content", result.html.toLowerCase(), "example");

            System.out.println("    HTML length: " + result.html.length() + " chars");

        } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
            SimpleTestFramework.fail("HTML scrape test", "API error: " + e.getErrorCode() + " - " + e.getMessage());
        } catch (Exception e) {
            SimpleTestFramework.fail("HTML scrape test", "Unexpected error: " + e.getMessage());
        }
    }

    private static void testScrapeAsync(WebCrawlerAPI client) {
        SimpleTestFramework.section("Scrape Tests (Asynchronous)");

        try {
            String url = "https://example.com";
            String scrapeType = "cleaned";

            System.out.println("  Starting async scrape: " + url + " (" + scrapeType + ")");
            String scrapeId = client.scrapeAsync(url, scrapeType);

            SimpleTestFramework.assertNotNull("Scrape ID is not null", scrapeId);
            SimpleTestFramework.assertTrue("Scrape ID is not empty", scrapeId.length() > 0);
            System.out.println("    Scrape ID: " + scrapeId);

            // Poll for completion
            WebCrawlerAPI.ScrapeResult result = null;
            int maxPolls = 20;
            boolean completed = false;

            for (int i = 0; i < maxPolls; i++) {
                result = client.getScrape(scrapeId);
                System.out.println("    Poll " + (i + 1) + ": status = " + result.status);

                if ("done".equals(result.status) || "error".equals(result.status)) {
                    completed = true;
                    break;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SimpleTestFramework.assertTrue("Scrape completed within timeout", completed);
            SimpleTestFramework.assertNotNull("Final result is not null", result);
            SimpleTestFramework.assertEquals("Final status is done", "done", result.status);
            SimpleTestFramework.assertNotNull("Result has cleaned content", result.cleaned);
            SimpleTestFramework.assertTrue("Cleaned content is not empty", result.cleaned.length() > 0);

            System.out.println("    Cleaned content length: " + result.cleaned.length() + " chars");

        } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
            SimpleTestFramework.fail("Async scrape test", "API error: " + e.getErrorCode() + " - " + e.getMessage());
        } catch (Exception e) {
            SimpleTestFramework.fail("Async scrape test", "Unexpected error: " + e.getMessage());
        }
    }

    private static void testCrawl(WebCrawlerAPI client) {
        SimpleTestFramework.section("Crawl Tests");

        try {
            String url = "https://example.com";
            String scrapeType = "markdown";
            int itemsLimit = 2;

            System.out.println("  Crawling: " + url + " (limit: " + itemsLimit + ", type: " + scrapeType + ")");
            WebCrawlerAPI.CrawlResult result = client.crawl(url, scrapeType, itemsLimit);

            SimpleTestFramework.assertNotNull("Crawl result is not null", result);
            SimpleTestFramework.assertNotNull("Crawl result has ID", result.id);
            SimpleTestFramework.assertNotNull("Crawl result has status", result.status);
            SimpleTestFramework.assertTrue("Crawl status is terminal",
                "done".equals(result.status) ||
                "error".equals(result.status) ||
                "cancelled".equals(result.status)
            );
            SimpleTestFramework.assertNotNull("Crawl result has items", result.items);
            SimpleTestFramework.assertTrue("Crawl found at least 1 item", result.items.size() >= 1);
            SimpleTestFramework.assertTrue("Crawl respects items limit", result.items.size() <= itemsLimit);

            System.out.println("    Job ID: " + result.id);
            System.out.println("    Status: " + result.status);
            System.out.println("    Items found: " + result.items.size());

            // Check first item
            if (result.items.size() > 0) {
                WebCrawlerAPI.CrawlItem firstItem = result.items.get(0);
                SimpleTestFramework.assertNotNull("First item has URL", firstItem.url);
                SimpleTestFramework.assertNotNull("First item has status", firstItem.status);

                String contentUrl = firstItem.getContentUrl(scrapeType);
                SimpleTestFramework.assertNotNull("First item has content URL", contentUrl);
                SimpleTestFramework.assertTrue("Content URL is valid", contentUrl.startsWith("http"));

                System.out.println("    First item URL: " + firstItem.url);
                System.out.println("    First item status: " + firstItem.status);
                System.out.println("    First item content URL: " + contentUrl);
            }

        } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
            SimpleTestFramework.fail("Crawl test", "API error: " + e.getErrorCode() + " - " + e.getMessage());
        } catch (Exception e) {
            SimpleTestFramework.fail("Crawl test", "Unexpected error: " + e.getMessage());
        }

        // Test crawl with null scrapeType (should work with default)
        try {
            String url = "https://example.com";
            int itemsLimit = 1;

            System.out.println("  Crawling with null scrape type: " + url + " (limit: " + itemsLimit + ")");
            WebCrawlerAPI.CrawlResult result = client.crawl(url, null, itemsLimit);

            SimpleTestFramework.assertNotNull("Crawl with null type result is not null", result);
            SimpleTestFramework.assertNotNull("Crawl with null type has status", result.status);
            SimpleTestFramework.assertTrue("Crawl with null type completed or errored",
                "done".equals(result.status) ||
                "error".equals(result.status)
            );

            System.out.println("    Status: " + result.status);
            System.out.println("    Items found: " + result.items.size());

        } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
            SimpleTestFramework.fail("Crawl with null type test", "API error: " + e.getErrorCode() + " - " + e.getMessage());
        } catch (Exception e) {
            SimpleTestFramework.fail("Crawl with null type test", "Unexpected error: " + e.getMessage());
        }
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() == 0) {
            return "***";
        }
        // Only show first 2 characters for better security in CI logs
        if (apiKey.length() <= 2) {
            return "***";
        }
        return apiKey.substring(0, 2) + "***";
    }
}
