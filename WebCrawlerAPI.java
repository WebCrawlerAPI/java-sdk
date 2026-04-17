import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone WebCrawlerAPI Java Client
 *
 * A simple, dependency-free Java client for WebCrawlerAPI that can be copy-pasted into any Java 17+ project.
 *
 * Features:
 * - No external dependencies (uses standard Java HTTP client)
 * - Simple JSON parsing using basic string operations
 * - Supports crawl(), scrape(), and scrapeAsync() methods
 * - Automatic polling for job completion
 *
 * Example usage:
 * <pre>
 * WebCrawlerAPI client = new WebCrawlerAPI("your-api-key");
 *
 * // Crawl a website
 * CrawlResult result = client.crawl("https://example.com", new String[]{"markdown"}, 10);
 * System.out.println("Found " + result.items.size() + " items");
 *
 * // Scrape a single page
 * ScrapeResult scrape = client.scrape("https://example.com", new String[]{"markdown"});
 * System.out.println("Content: " + scrape.content);
 * </pre>
 */
public class WebCrawlerAPI {

    private static final String DEFAULT_BASE_URL = "https://api.webcrawlerapi.com";
    private static final int DEFAULT_POLL_DELAY_MS = 5000;
    private static final int DEFAULT_MAX_POLLS = 100;
    private static final int DEFAULT_MAX_AGE = 0;
    private static final String SDK_VERSION = "1.0.0-standalone";

    private final String apiKey;
    private final String baseUrl;

    /**
     * Creates a new WebCrawlerAPI client with default base URL
     * @param apiKey Your WebCrawlerAPI key
     */
    public WebCrawlerAPI(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    /**
     * Creates a new WebCrawlerAPI client with custom base URL
     * @param apiKey Your WebCrawlerAPI key
     * @param baseUrl Custom base URL (e.g., http://localhost:8080 for testing)
     */
    public WebCrawlerAPI(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
    }

    /**
     * Crawl a website and return all discovered pages
     *
     * @param url The URL to crawl
     * @param scrapeType Type of content to extract: "html", "cleaned", or "markdown"
     * @param itemsLimit Maximum number of pages to crawl
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult crawl(String url, String scrapeType, int itemsLimit) throws WebCrawlerAPIException {
        return crawl(url, toOutputFormats(scrapeType), itemsLimit, DEFAULT_MAX_POLLS, DEFAULT_MAX_AGE);
    }

    /**
     * Crawl a website and return all discovered pages
     *
     * @param url The URL to crawl
     * @param outputFormats Content formats to extract
     * @param itemsLimit Maximum number of pages to crawl
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult crawl(String url, String[] outputFormats, int itemsLimit) throws WebCrawlerAPIException {
        return crawl(url, outputFormats, itemsLimit, DEFAULT_MAX_POLLS, DEFAULT_MAX_AGE);
    }

    /**
     * Crawl a website with custom max polls
     *
     * @param url The URL to crawl
     * @param scrapeType Type of content to extract
     * @param itemsLimit Maximum number of pages to crawl
     * @param maxPolls Maximum number of polling attempts
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult crawl(String url, String scrapeType, int itemsLimit, int maxPolls) throws WebCrawlerAPIException {
        return crawl(url, toOutputFormats(scrapeType), itemsLimit, maxPolls, DEFAULT_MAX_AGE);
    }

    /**
     * Crawl a website with custom max polls
     *
     * @param url The URL to crawl
     * @param outputFormats Content formats to extract
     * @param itemsLimit Maximum number of pages to crawl
     * @param maxPolls Maximum number of polling attempts
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult crawl(String url, String[] outputFormats, int itemsLimit, int maxPolls) throws WebCrawlerAPIException {
        return crawl(url, outputFormats, itemsLimit, maxPolls, DEFAULT_MAX_AGE);
    }

    /**
     * Crawl a website with custom max polls and max_age cache control
     *
     * @param url The URL to crawl
     * @param scrapeType Type of content to extract
     * @param itemsLimit Maximum number of pages to crawl
     * @param maxPolls Maximum number of polling attempts
     * @param maxAge Cache age in seconds (0 to bypass cache)
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult crawl(String url, String scrapeType, int itemsLimit, int maxPolls, int maxAge) throws WebCrawlerAPIException {
        return crawl(url, toOutputFormats(scrapeType), itemsLimit, maxPolls, maxAge);
    }

    /**
     * Crawl a website with custom max polls and max_age cache control
     *
     * @param url The URL to crawl
     * @param outputFormats Content formats to extract
     * @param itemsLimit Maximum number of pages to crawl
     * @param maxPolls Maximum number of polling attempts
     * @param maxAge Cache age in seconds (0 to bypass cache)
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult crawl(String url, String[] outputFormats, int itemsLimit, int maxPolls, int maxAge) throws WebCrawlerAPIException {
        // Build request body
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"url\":\"").append(escapeJson(url)).append("\"");
        if (outputFormats != null && outputFormats.length > 0) {
            jsonBody.append(",\"output_formats\":").append(buildJsonStringArray(outputFormats));
        }
        jsonBody.append(",\"items_limit\":").append(itemsLimit);
        jsonBody.append(",\"max_age\":").append(maxAge);
        jsonBody.append("}");

        // Start crawl job
        String response = sendRequest(baseUrl + "/v1/crawl", "POST", jsonBody.toString());
        String jobId = extractJsonValue(response, "id");

        if (jobId == null || jobId.isEmpty()) {
            throw new WebCrawlerAPIException("invalid_response", "Failed to get job ID from response");
        }

        // Poll for completion
        CrawlResult result = null;
        for (int i = 0; i < maxPolls; i++) {
            String jobResponse = sendRequest(baseUrl + "/v1/job/" + jobId, "GET", null);
            result = parseCrawlResult(jobResponse);

            if (isTerminalStatus(result.status)) {
                return result;
            }

            // Use recommended delay if available
            int delayMs = result.recommendedPullDelayMs > 0
                ? result.recommendedPullDelayMs
                : DEFAULT_POLL_DELAY_MS;

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WebCrawlerAPIException("interrupted", "Polling was interrupted");
            }
        }

        return result;
    }

    /**
     * Scrape a single page (synchronous - waits for completion)
     *
     * @param url The URL to scrape
     * @param scrapeType Type of content to extract: "html", "cleaned", or "markdown"
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(String url, String scrapeType) throws WebCrawlerAPIException {
        return scrape(url, toOutputFormats(scrapeType), DEFAULT_MAX_POLLS, DEFAULT_MAX_AGE);
    }

    /**
     * Scrape a single page synchronously.
     *
     * @param url The URL to scrape
     * @param outputFormats Content formats to extract
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(String url, String[] outputFormats) throws WebCrawlerAPIException {
        return scrape(url, outputFormats, DEFAULT_MAX_POLLS, DEFAULT_MAX_AGE);
    }

    /**
     * Scrape a single page with custom max polls
     *
     * @param url The URL to scrape
     * @param scrapeType Type of content to extract
     * @param maxPolls Maximum number of polling attempts
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(String url, String scrapeType, int maxPolls) throws WebCrawlerAPIException {
        return scrape(url, toOutputFormats(scrapeType), maxPolls, DEFAULT_MAX_AGE);
    }

    /**
     * Scrape a single page with custom max polls.
     *
     * @param url The URL to scrape
     * @param outputFormats Content formats to extract
     * @param maxPolls Deprecated for the sync endpoint. Kept for backward compatibility.
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(String url, String[] outputFormats, int maxPolls) throws WebCrawlerAPIException {
        return scrape(url, outputFormats, maxPolls, DEFAULT_MAX_AGE);
    }

    /**
     * Scrape a single page with custom max polls and max_age cache control
     *
     * @param url The URL to scrape
     * @param scrapeType Type of content to extract
     * @param maxPolls Maximum number of polling attempts
     * @param maxAge Cache age in seconds (0 to bypass cache)
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(String url, String scrapeType, int maxPolls, int maxAge) throws WebCrawlerAPIException {
        return scrape(url, toOutputFormats(scrapeType), maxPolls, maxAge);
    }

    /**
     * Scrape a single page with custom max_age cache control.
     *
     * @param url The URL to scrape
     * @param outputFormats Content formats to extract
     * @param maxPolls Deprecated for the sync endpoint. Kept for backward compatibility.
     * @param maxAge Cache age in seconds (0 to bypass cache)
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(String url, String[] outputFormats, int maxPolls, int maxAge) throws WebCrawlerAPIException {
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"url\":\"").append(escapeJson(url)).append("\"");
        if (outputFormats != null && outputFormats.length > 0) {
            jsonBody.append(",\"output_formats\":").append(buildJsonStringArray(outputFormats));
        }
        jsonBody.append(",\"max_age\":").append(maxAge);
        jsonBody.append("}");

        String response = sendRequest(baseUrl + "/v2/scrape", "POST", jsonBody.toString());
        return parseScrapeResult(response);
    }

    /**
     * Start a scrape job asynchronously (returns immediately with scrape ID)
     *
     * @param url The URL to scrape
     * @param scrapeType Type of content to extract
     * @return Scrape ID that can be used to check status later
     * @throws WebCrawlerAPIException if the request fails
     */
    public String scrapeAsync(String url, String scrapeType) throws WebCrawlerAPIException {
        return scrapeAsync(url, toOutputFormats(scrapeType), DEFAULT_MAX_AGE);
    }

    /**
     * Start a scrape job asynchronously (returns immediately with scrape ID)
     *
     * @param url The URL to scrape
     * @param outputFormats Content formats to extract
     * @return Scrape ID that can be used to check status later
     * @throws WebCrawlerAPIException if the request fails
     */
    public String scrapeAsync(String url, String[] outputFormats) throws WebCrawlerAPIException {
        return scrapeAsync(url, outputFormats, DEFAULT_MAX_AGE);
    }

    /**
     * Start a scrape job asynchronously with max_age cache control
     *
     * @param url The URL to scrape
     * @param scrapeType Type of content to extract
     * @param maxAge Cache age in seconds (0 to bypass cache)
     * @return Scrape ID that can be used to check status later
     * @throws WebCrawlerAPIException if the request fails
     */
    public String scrapeAsync(String url, String scrapeType, int maxAge) throws WebCrawlerAPIException {
        return scrapeAsync(url, toOutputFormats(scrapeType), maxAge);
    }

    /**
     * Start a scrape job asynchronously with max_age cache control
     *
     * @param url The URL to scrape
     * @param outputFormats Content formats to extract
     * @param maxAge Cache age in seconds (0 to bypass cache)
     * @return Scrape ID that can be used to check status later
     * @throws WebCrawlerAPIException if the request fails
     */
    public String scrapeAsync(String url, String[] outputFormats, int maxAge) throws WebCrawlerAPIException {
        // Build request body
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"url\":\"").append(escapeJson(url)).append("\"");
        if (outputFormats != null && outputFormats.length > 0) {
            jsonBody.append(",\"output_formats\":").append(buildJsonStringArray(outputFormats));
        }
        jsonBody.append(",\"max_age\":").append(maxAge);
        jsonBody.append("}");

        String response = sendRequest(baseUrl + "/v2/scrape?async=true", "POST", jsonBody.toString());
        String scrapeId = extractJsonValue(response, "id");

        if (scrapeId == null || scrapeId.isEmpty()) {
            throw new WebCrawlerAPIException("invalid_response", "Failed to get scrape ID from response");
        }

        return scrapeId;
    }

    /**
     * Get the status of a scrape job
     *
     * @param scrapeId The scrape ID returned from scrapeAsync
     * @return ScrapeResult containing current status and content (if done)
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult getScrape(String scrapeId) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v2/scrape/" + scrapeId, "GET", null);
        return parseScrapeResult(response);
    }

    // ==================== Private Helper Methods ====================

    private String sendRequest(String urlString, String method, String body) throws WebCrawlerAPIException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "WebCrawlerAPI-Java-Standalone/" + SDK_VERSION);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            conn.setRequestProperty("Pragma", "no-cache");
            conn.setRequestProperty("Expires", "0");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            if (body != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int statusCode = conn.getResponseCode();

            // Read response
            BufferedReader reader;
            if (statusCode >= 200 && statusCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (statusCode >= 200 && statusCode < 300) {
                return response.toString();
            } else {
                // Parse error response
                String errorCode = extractJsonValue(response.toString(), "error_code");
                String errorMessage = extractJsonValue(response.toString(), "error_message");
                if (errorMessage == null) {
                    errorMessage = extractJsonValue(response.toString(), "message");
                }
                if (errorMessage == null) {
                    errorMessage = "Request failed with status " + statusCode;
                }
                throw new WebCrawlerAPIException(errorCode != null ? errorCode : "unknown_error", errorMessage);
            }

        } catch (WebCrawlerAPIException e) {
            throw e;
        } catch (Exception e) {
            throw new WebCrawlerAPIException("network_error", "Network error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private CrawlResult parseCrawlResult(String json) {
        CrawlResult result = new CrawlResult();
        result.id = extractJsonValue(json, "id");
        result.status = extractJsonValue(json, "status");
        result.url = extractJsonValue(json, "url");
        result.scrapeType = extractJsonValue(json, "scrape_type");
        result.outputFormats = extractJsonStringArray(json, "output_formats");
        result.recommendedPullDelayMs = extractJsonInt(json, "recommended_pull_delay_ms");
        result.items = extractJobItems(json);
        return result;
    }

    private ScrapeResult parseScrapeResult(String json) {
        ScrapeResult result = new ScrapeResult();
        result.status = extractJsonValue(json, "status");
        result.html = extractJsonValue(json, "raw_content");
        result.markdown = extractJsonValue(json, "markdown");
        result.cleaned = extractJsonValue(json, "cleaned_content");
        result.url = extractJsonValue(json, "url");
        result.pageStatusCode = extractJsonInt(json, "page_status_code");
        result.pageTitle = extractJsonValue(json, "page_title");
        result.links = extractJsonStringArray(json, "links");

        // Map content field based on available data
        // V2 API returns specific fields (markdown, raw_content, cleaned_content) instead of generic "content"
        if (result.markdown != null) {
            result.content = result.markdown;
        } else if (result.html != null) {
            result.content = result.html;
        } else if (result.cleaned != null) {
            result.content = result.cleaned;
        }

        if (result.status == null && (result.content != null || result.pageStatusCode > 0)) {
            result.status = "done";
        }

        return result;
    }

    private List<CrawlItem> extractJobItems(String json) {
        List<CrawlItem> items = new ArrayList<>();

        // Find job_items array
        int itemsStart = json.indexOf("\"job_items\":");
        if (itemsStart == -1) {
            return items;
        }

        int arrayStart = json.indexOf("[", itemsStart);
        if (arrayStart == -1) {
            return items;
        }

        // Simple array parsing - find matching ]
        int depth = 0;
        int arrayEnd = arrayStart;
        for (int i = arrayStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    arrayEnd = i;
                    break;
                }
            }
        }

        String itemsJson = json.substring(arrayStart + 1, arrayEnd);

        // Parse individual items
        int itemStart = 0;
        depth = 0;
        int objStart = -1;

        for (int i = 0; i < itemsJson.length(); i++) {
            char c = itemsJson.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String itemJson = itemsJson.substring(objStart, i + 1);
                    CrawlItem item = new CrawlItem();
                    item.url = extractJsonValue(itemJson, "original_url");
                    item.status = extractJsonValue(itemJson, "status");
                    item.rawContentUrl = extractJsonValue(itemJson, "raw_content_url");
                    item.cleanedContentUrl = extractJsonValue(itemJson, "cleaned_content_url");
                    item.markdownContentUrl = extractJsonValue(itemJson, "markdown_content_url");
                    items.add(item);
                    objStart = -1;
                }
            }
        }

        return items;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int valueStart = keyIndex + searchKey.length();

        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        char firstChar = json.charAt(valueStart);

        // String value
        if (firstChar == '"') {
            int valueEnd = valueStart + 1;
            boolean escaped = false;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == '\\' && !escaped) {
                    escaped = true;
                } else if (c == '"' && !escaped) {
                    return unescapeJson(json.substring(valueStart + 1, valueEnd));
                } else {
                    escaped = false;
                }
                valueEnd++;
            }
            return null;
        }

        // Null value
        if (json.startsWith("null", valueStart)) {
            return null;
        }

        // Number, boolean, etc - find end
        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                break;
            }
            valueEnd++;
        }

        return json.substring(valueStart, valueEnd).trim();
    }

    private int extractJsonInt(String json, String key) {
        String value = extractJsonValue(json, key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String[] extractJsonStringArray(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return new String[0];
        }

        int arrayStart = json.indexOf("[", keyIndex);
        if (arrayStart == -1) {
            return new String[0];
        }

        int depth = 0;
        int arrayEnd = -1;
        for (int i = arrayStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    arrayEnd = i;
                    break;
                }
            }
        }

        if (arrayEnd == -1) {
            return new String[0];
        }

        String arrayJson = json.substring(arrayStart + 1, arrayEnd);
        List<String> values = new ArrayList<>();
        int index = 0;

        while (index < arrayJson.length()) {
            while (index < arrayJson.length() && (Character.isWhitespace(arrayJson.charAt(index)) || arrayJson.charAt(index) == ',')) {
                index++;
            }

            if (index >= arrayJson.length() || arrayJson.charAt(index) != '"') {
                break;
            }

            int valueEnd = index + 1;
            boolean escaped = false;
            while (valueEnd < arrayJson.length()) {
                char c = arrayJson.charAt(valueEnd);
                if (c == '\\' && !escaped) {
                    escaped = true;
                } else if (c == '"' && !escaped) {
                    values.add(unescapeJson(arrayJson.substring(index + 1, valueEnd)));
                    index = valueEnd + 1;
                    break;
                } else {
                    escaped = false;
                }
                valueEnd++;
            }

            if (valueEnd >= arrayJson.length()) {
                break;
            }
        }

        return values.toArray(new String[0]);
    }

    private String[] toOutputFormats(String scrapeType) {
        if (scrapeType == null || scrapeType.trim().isEmpty()) {
            return null;
        }
        return new String[]{scrapeType};
    }

    private String buildJsonStringArray(String[] values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(values[i])).append("\"");
        }
        json.append("]");
        return json.toString();
    }

    private boolean isTerminalStatus(String status) {
        return "done".equals(status) || "error".equals(status) || "cancelled".equals(status);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\\\", "\\");
    }

    // ==================== Public Classes ====================

    /**
     * Result of a crawl operation
     */
    public static class CrawlResult {
        public String id;
        public String status;
        public String url;
        public String[] outputFormats = new String[0];
        public String scrapeType;
        public int recommendedPullDelayMs;
        public List<CrawlItem> items = new ArrayList<>();

        @Override
        public String toString() {
            return "CrawlResult{id='" + id + "', status='" + status + "', items=" + items.size() + "}";
        }
    }

    /**
     * Individual crawled page item
     */
    public static class CrawlItem {
        public String url;
        public String status;
        public String rawContentUrl;
        public String cleanedContentUrl;
        public String markdownContentUrl;

        /**
         * Get the content URL based on output format priority
         */
        public String getContentUrl(String[] outputFormats) {
            if (outputFormats == null || outputFormats.length == 0) {
                return null;
            }

            for (String outputFormat : outputFormats) {
                if ("markdown".equals(outputFormat) && markdownContentUrl != null) return markdownContentUrl;
                if ("cleaned".equals(outputFormat) && cleanedContentUrl != null) return cleanedContentUrl;
                if ("html".equals(outputFormat) && rawContentUrl != null) return rawContentUrl;
            }
            return null;
        }

        /**
         * Get the content URL based on the scrape type
         */
        public String getContentUrl(String scrapeType) {
            if ("html".equals(scrapeType)) return rawContentUrl;
            if ("cleaned".equals(scrapeType)) return cleanedContentUrl;
            if ("markdown".equals(scrapeType)) return markdownContentUrl;
            return null;
        }

        @Override
        public String toString() {
            return "CrawlItem{url='" + url + "', status='" + status + "'}";
        }
    }

    /**
     * Result of a scrape operation
     */
    public static class ScrapeResult {
        public String status;
        public String content;
        public String html;
        public String markdown;
        public String cleaned;
        public String url;
        public int pageStatusCode;
        public String pageTitle;
        public String[] links = new String[0];

        @Override
        public String toString() {
            return "ScrapeResult{status='" + status + "', url='" + url + "'}";
        }
    }

    /**
     * Exception thrown by WebCrawlerAPI operations
     */
    public static class WebCrawlerAPIException extends Exception {
        private final String errorCode;

        public WebCrawlerAPIException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        @Override
        public String toString() {
            return "WebCrawlerAPIException{errorCode='" + errorCode + "', message='" + getMessage() + "'}";
        }
    }
}
