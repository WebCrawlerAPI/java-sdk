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
 * - Supports crawl(), crawlAsync(), scrape(), scrapeAsync(), getJob(), getJobMarkdown(),
 *   markdown() and the agent (runAgent()) endpoints
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
    private static final int INITIAL_CRAWL_POLL_DELAY_MS = 2000;
    private static final int DEFAULT_AGENT_POLL_DELAY_MS = 2000;
    private static final int DEFAULT_MAX_POLLS = 100;
    private static final int DEFAULT_MAX_AGE = 0;
    private static final String SDK_VERSION = "1.1.0-standalone";
    private static final String[] AGENT_TERMINAL_STATUSES = {"done", "error", "canceled"};

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
     * Start a crawl job asynchronously (returns immediately with the job ID). Mirrors the
     * JS SDK's {@code crawlAsync()} and supports the full {@link CrawlOptions} parameter set
     * (whitelist/blacklist regexp, webhook, robots.txt, max_depth, keep_query_params, etc).
     *
     * @param options Crawl request options
     * @return Job ID that can be used with {@link #getJob(String)}
     * @throws WebCrawlerAPIException if the request fails
     */
    public String crawlAsync(CrawlOptions options) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v1/crawl", "POST", buildCrawlRequestJson(options));
        String jobId = extractJsonValue(response, "id");

        if (jobId == null || jobId.isEmpty()) {
            throw new WebCrawlerAPIException("invalid_response", "Failed to get job ID from response");
        }

        return jobId;
    }

    /**
     * Crawl a website using the full {@link CrawlOptions} parameter set, polling until the job
     * reaches a terminal state (any status other than {@code new}/{@code in_progress}).
     *
     * @param options Crawl request options
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails or polling times out
     */
    public CrawlResult crawl(CrawlOptions options) throws WebCrawlerAPIException {
        return crawl(options, DEFAULT_MAX_POLLS);
    }

    /**
     * Crawl a website using the full {@link CrawlOptions} parameter set, with a custom max
     * number of polling attempts.
     *
     * @param options Crawl request options
     * @param maxPolls Maximum number of polling attempts
     * @return CrawlResult containing job details and crawled items
     * @throws WebCrawlerAPIException if the request fails or polling times out
     */
    public CrawlResult crawl(CrawlOptions options, int maxPolls) throws WebCrawlerAPIException {
        String jobId = crawlAsync(options);

        int delayMs = INITIAL_CRAWL_POLL_DELAY_MS;
        for (int i = 0; i < maxPolls; i++) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WebCrawlerAPIException("interrupted", "Polling was interrupted");
            }

            CrawlResult result = getJob(jobId);
            if (isTerminalJobStatus(result.status)) {
                return result;
            }
            if (result.recommendedPullDelayMs > 0) {
                delayMs = result.recommendedPullDelayMs;
            }
        }

        throw new WebCrawlerAPIException("timeout", "Crawling took too long, please retry or increase the number of polling retries");
    }

    /**
     * Fetch the current state of a crawl job by ID.
     *
     * @param jobId The job ID returned from {@link #crawlAsync(CrawlOptions)}
     * @return CrawlResult containing the current job state
     * @throws WebCrawlerAPIException if the request fails
     */
    public CrawlResult getJob(String jobId) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v1/job/" + jobId, "GET", null);
        return parseCrawlResult(response);
    }

    /**
     * Get the URL of the aggregated markdown content for a finished crawl job.
     *
     * @param jobId The job ID
     * @return JobMarkdownResponse containing the content URL
     * @throws WebCrawlerAPIException if the request fails
     */
    public JobMarkdownResponse getJobMarkdown(String jobId) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v1/job/" + jobId + "/markdown", "GET", null);
        JobMarkdownResponse result = new JobMarkdownResponse();
        result.contentUrl = extractJsonValue(response, "content_url");
        return result;
    }

    /**
     * Fetch the aggregated markdown content for a finished crawl job directly (raw text, not JSON).
     *
     * @param jobId The job ID
     * @return The raw markdown content
     * @throws WebCrawlerAPIException if the request fails
     */
    public String getJobMarkdownContent(String jobId) throws WebCrawlerAPIException {
        return sendRequest(baseUrl + "/v1/job/" + jobId + "/markdown/content", "GET", null);
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
     * Scrape a single page using the full {@link ScrapeOptions} parameter set (webhook_url,
     * clean_selectors, actions, prompt, response_schema, main_content_only, max_age).
     *
     * @param options Scrape request options
     * @return ScrapeResult containing the scraped content
     * @throws WebCrawlerAPIException if the request fails
     */
    public ScrapeResult scrape(ScrapeOptions options) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v2/scrape", "POST", buildScrapeRequestJson(options));
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
     * Start a scrape job asynchronously using the full {@link ScrapeOptions} parameter set.
     *
     * @param options Scrape request options
     * @return Scrape ID that can be used to check status later
     * @throws WebCrawlerAPIException if the request fails
     */
    public String scrapeAsync(ScrapeOptions options) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v2/scrape?async=true", "POST", buildScrapeRequestJson(options));
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

    /**
     * Extract cleaned article markdown from a webpage (the standalone {@code /markdown} endpoint).
     *
     * @param url The URL of the webpage to extract markdown from
     * @return MarkdownResponse containing the extracted markdown
     * @throws WebCrawlerAPIException if the request fails
     */
    public MarkdownResponse markdown(String url) throws WebCrawlerAPIException {
        StringBuilder jsonBody = new StringBuilder("{");
        appendField(jsonBody, "url", url);
        jsonBody.append("}");

        String response = sendRequest(baseUrl + "/markdown", "POST", jsonBody.toString());
        MarkdownResponse result = new MarkdownResponse();
        result.success = extractJsonBoolean(response, "success");
        result.markdown = extractJsonValue(response, "markdown");
        return result;
    }

    /**
     * Start an AI agent run asynchronously (returns immediately with the run's current state).
     *
     * @param request Agent run request
     * @return AgentRun with the initial run state
     * @throws WebCrawlerAPIException if the request fails
     */
    public AgentRun runAgentAsync(AgentRunRequest request) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v1/agent", "POST", buildAgentRunRequestJson(request));
        return parseAgentRun(response);
    }

    /**
     * Fetch the current state of an agent run by ID.
     *
     * @param jobId The agent run ID
     * @return AgentRun with the current run state
     * @throws WebCrawlerAPIException if the request fails
     */
    public AgentRun getAgentJob(String jobId) throws WebCrawlerAPIException {
        String response = sendRequest(baseUrl + "/v1/agent/job/" + jobId, "GET", null);
        return parseAgentRun(response);
    }

    /**
     * Run an AI agent task and poll until it reaches a terminal state (done/error/canceled).
     *
     * @param request Agent run request
     * @return AgentRun with the final run state
     * @throws WebCrawlerAPIException if the request fails or polling times out
     */
    public AgentRun runAgent(AgentRunRequest request) throws WebCrawlerAPIException {
        return runAgent(request, DEFAULT_MAX_POLLS);
    }

    /**
     * Run an AI agent task and poll until it reaches a terminal state (done/error/canceled),
     * with a custom max number of polling attempts.
     *
     * @param request Agent run request
     * @param maxPolls Maximum number of polling attempts
     * @return AgentRun with the final run state
     * @throws WebCrawlerAPIException if the request fails or polling times out
     */
    public AgentRun runAgent(AgentRunRequest request, int maxPolls) throws WebCrawlerAPIException {
        AgentRun run = runAgentAsync(request);

        if (run.id == null || run.id.isEmpty()) {
            throw new WebCrawlerAPIException("invalid_response", "Failed to fetch agent job status");
        }

        if (isTerminalAgentStatus(run.status)) {
            return run;
        }

        for (int i = 0; i < maxPolls; i++) {
            try {
                Thread.sleep(DEFAULT_AGENT_POLL_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WebCrawlerAPIException("interrupted", "Polling was interrupted");
            }

            AgentRun agentJob = getAgentJob(run.id);
            if (isTerminalAgentStatus(agentJob.status)) {
                return agentJob;
            }
        }

        throw new WebCrawlerAPIException("timeout", "Agent run took too long, please retry or increase the number of polling retries");
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
                    errorMessage = extractJsonValue(response.toString(), "error");
                }
                if (errorMessage == null) {
                    errorMessage = extractJsonValue(response.toString(), "message");
                }
                if (errorMessage == null) {
                    errorMessage = "Request failed with status " + statusCode;
                }
                throw new WebCrawlerAPIException(errorCode != null ? errorCode : "unknown_error", errorMessage, statusCode);
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
        result.orgId = extractJsonValue(json, "org_id");
        result.status = extractJsonValue(json, "status");
        result.url = extractJsonValue(json, "url");
        result.scrapeType = extractJsonValue(json, "scrape_type");
        result.outputFormats = extractJsonStringArray(json, "output_formats");
        result.whitelistRegexp = extractJsonValue(json, "whitelist_regexp");
        result.blacklistRegexp = extractJsonValue(json, "blacklist_regexp");
        result.itemsLimit = extractJsonInt(json, "items_limit");
        result.maxDepth = extractJsonInt(json, "max_depth", null);
        result.createdAt = extractJsonValue(json, "created_at");
        result.finishedAt = extractJsonValue(json, "finished_at");
        result.updatedAt = extractJsonValue(json, "updated_at");
        result.webhookUrl = extractJsonValue(json, "webhook_url");
        result.recommendedPullDelayMs = extractJsonInt(json, "recommended_pull_delay_ms");
        result.items = extractJobItems(json);
        return result;
    }

    private ScrapeResult parseScrapeResult(String json) {
        ScrapeResult result = new ScrapeResult();
        result.success = extractJsonBoolean(json, "success");
        result.status = extractJsonValue(json, "status");
        result.html = extractJsonValue(json, "raw_content");
        result.markdown = extractJsonValue(json, "markdown");
        result.cleaned = extractJsonValue(json, "cleaned_content");
        result.url = extractJsonValue(json, "url");
        result.pageStatusCode = extractJsonInt(json, "page_status_code");
        result.pageTitle = extractJsonValue(json, "page_title");
        result.links = extractJsonStringArray(json, "links");
        result.structuredDataJson = extractJsonRawObject(json, "structured_data");

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

    private AgentRun parseAgentRun(String json) {
        AgentRun run = new AgentRun();
        run.id = extractJsonValue(json, "id");
        run.status = extractJsonValue(json, "status");
        run.prompt = extractJsonValue(json, "prompt");
        run.model = extractJsonValue(json, "model");
        run.dataJson = extractJsonRawValue(json, "data");
        run.errorReason = extractJsonValue(json, "error_reason");
        run.balanceUsedUsd = extractJsonDouble(json, "balance_used_usd");
        String maxSpend = extractJsonValue(json, "max_spend_usd");
        run.maxSpendUsd = maxSpend != null ? Double.valueOf(maxSpend) : null;
        run.urls = extractJsonStringArray(json, "urls");
        run.createdAt = extractJsonValue(json, "created_at");
        run.updatedAt = extractJsonValue(json, "updated_at");
        run.success = extractJsonBoolean(json, "success");
        return run;
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
                    item.id = extractJsonValue(itemJson, "id");
                    item.jobId = extractJsonValue(itemJson, "job_id");
                    item.url = extractJsonValue(itemJson, "original_url");
                    item.status = extractJsonValue(itemJson, "status");
                    item.pageStatusCode = extractJsonInt(itemJson, "page_status_code");
                    item.title = extractJsonValue(itemJson, "title");
                    item.lastError = extractJsonValue(itemJson, "last_error");
                    item.createdAt = extractJsonValue(itemJson, "created_at");
                    item.updatedAt = extractJsonValue(itemJson, "updated_at");
                    item.cost = extractJsonDouble(itemJson, "cost");
                    item.referredUrl = extractJsonValue(itemJson, "referred_url");
                    item.links = extractJsonValue(itemJson, "links");
                    item.depth = extractJsonInt(itemJson, "depth", null);
                    item.rawContentUrl = extractJsonValue(itemJson, "raw_content_url");
                    item.cleanedContentUrl = extractJsonValue(itemJson, "cleaned_content_url");
                    item.markdownContentUrl = extractJsonValue(itemJson, "markdown_content_url");
                    item.errorCode = extractJsonValue(itemJson, "error_code");
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

    /**
     * Like {@link #extractJsonInt(String, String)} but returns null (instead of 0) when the
     * field is absent, so optional numeric fields (e.g. max_depth, depth) can be distinguished
     * from an explicit 0.
     */
    private Integer extractJsonInt(String json, String key, Integer defaultValue) {
        String value = extractJsonValue(json, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double extractJsonDouble(String json, String key) {
        String value = extractJsonValue(json, key);
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean extractJsonBoolean(String json, String key) {
        return "true".equals(extractJsonValue(json, key));
    }

    /**
     * Extracts the raw JSON text (not unescaped) of an object/array value, used for fields with
     * no fixed shape (structured_data, agent data, response_schema, output_schema).
     * Returns null if the key is missing or its value is not an object/array.
     */
    private String extractJsonRawObject(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int start = keyIndex + searchKey.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) {
            return null;
        }

        char open = json.charAt(start);
        if (open != '{' && open != '[') {
            return null;
        }
        char close = (open == '{') ? '}' : ']';

        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return json.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    /**
     * Extracts the raw JSON text of a value that may be an object/array/string/number/boolean/null.
     */
    private String extractJsonRawValue(String json, String key) {
        String rawObject = extractJsonRawObject(json, key);
        if (rawObject != null) {
            return rawObject;
        }
        return extractJsonValue(json, key);
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

    private static String buildJsonStringArray(String[] values) {
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

    /**
     * Mirrors the JS SDK's crawl polling condition: terminal as soon as the job status is
     * neither "new" nor "in_progress" (used by the {@link CrawlOptions}-based crawl()).
     */
    private boolean isTerminalJobStatus(String status) {
        return !JobStatus.NEW.equals(status) && !JobStatus.IN_PROGRESS.equals(status);
    }

    private boolean isTerminalAgentStatus(String status) {
        if (status == null) {
            return false;
        }
        for (String terminal : AGENT_TERMINAL_STATUSES) {
            if (terminal.equals(status)) {
                return true;
            }
        }
        return false;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\\\", "\\");
    }

    // ==================== Request Body Builders ====================

    private static void appendField(StringBuilder json, String key, String value) {
        if (value == null) return;
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":\"").append(escapeJson(value)).append("\"");
    }

    private static void appendRawField(StringBuilder json, String key, String rawJsonValue) {
        if (rawJsonValue == null) return;
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":").append(rawJsonValue);
    }

    private static void appendIntField(StringBuilder json, String key, int value) {
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":").append(value);
    }

    private static void appendIntFieldOptional(StringBuilder json, String key, Integer value) {
        if (value == null) return;
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":").append(value);
    }

    private static void appendDoubleField(StringBuilder json, String key, double value) {
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":").append(value);
    }

    private static void appendBoolField(StringBuilder json, String key, Boolean value) {
        if (value == null) return;
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":").append(value);
    }

    private static void appendStringArrayField(StringBuilder json, String key, String[] values) {
        if (values == null || values.length == 0) return;
        if (json.length() > 1) json.append(",");
        json.append("\"").append(key).append("\":").append(buildJsonStringArray(values));
    }

    private static String buildScrapeRequestJson(ScrapeOptions options) {
        StringBuilder json = new StringBuilder("{");
        appendField(json, "url", options.url);
        appendStringArrayField(json, "output_formats", options.outputFormats);
        appendField(json, "webhook_url", options.webhookUrl);
        appendField(json, "clean_selectors", options.cleanSelectors);
        if (options.actions != null && !options.actions.isEmpty()) {
            StringBuilder actionsJson = new StringBuilder("[");
            for (int i = 0; i < options.actions.size(); i++) {
                if (i > 0) actionsJson.append(",");
                actionsJson.append(options.actions.get(i).toJson());
            }
            actionsJson.append("]");
            appendRawField(json, "actions", actionsJson.toString());
        }
        appendField(json, "prompt", options.prompt);
        appendRawField(json, "response_schema", options.responseSchemaJson);
        appendBoolField(json, "main_content_only", options.mainContentOnly);
        appendIntField(json, "max_age", options.maxAge);
        json.append("}");
        return json.toString();
    }

    private static String buildCrawlRequestJson(CrawlOptions options) {
        StringBuilder json = new StringBuilder("{");
        appendField(json, "url", options.url);
        // The JS SDK defaults output_formats to ["markdown"] when not provided; mirror that here.
        String[] outputFormats = (options.outputFormats != null && options.outputFormats.length > 0)
            ? options.outputFormats
            : new String[]{"markdown"};
        appendStringArrayField(json, "output_formats", outputFormats);
        appendIntFieldOptional(json, "items_limit", options.itemsLimit);
        appendField(json, "whitelist_regexp", options.whitelistRegexp);
        appendField(json, "blacklist_regexp", options.blacklistRegexp);
        appendField(json, "webhook_url", options.webhookUrl);
        appendBoolField(json, "respect_robots_txt", options.respectRobotsTxt);
        appendBoolField(json, "main_content_only", options.mainContentOnly);
        appendIntFieldOptional(json, "max_depth", options.maxDepth);
        appendIntField(json, "max_age", options.maxAge);
        appendBoolField(json, "keep_query_params", options.keepQueryParams);
        json.append("}");
        return json.toString();
    }

    private static String buildAgentRunRequestJson(AgentRunRequest request) {
        StringBuilder json = new StringBuilder("{");
        appendField(json, "prompt", request.prompt);
        appendStringArrayField(json, "urls", request.urls);
        appendBoolField(json, "seed_urls_only", request.seedUrlsOnly);
        appendRawField(json, "output_schema", request.outputSchemaJson);
        appendField(json, "model", request.model);
        appendDoubleField(json, "max_spend_usd", request.maxSpendUsd);
        json.append("}");
        return json.toString();
    }

    // ==================== Public Classes ====================

    /**
     * Job status constants, matching the JS SDK's {@code JobStatus}.
     */
    public static final class JobStatus {
        public static final String IN_PROGRESS = "in_progress";
        public static final String NEW = "new";
        public static final String DONE = "done";

        private JobStatus() {
        }
    }

    /**
     * Known API error codes, matching the JS SDK's {@code ErrorCode}.
     */
    public static final class ErrorCode {
        public static final String BLOCKED_BY_ROBOTS_TXT = "blocked_by_robots_txt";

        private ErrorCode() {
        }
    }

    /**
     * Full parameter set for {@link #crawl(CrawlOptions)} / {@link #crawlAsync(CrawlOptions)},
     * mirroring the JS SDK's {@code CrawlRequest}.
     */
    public static class CrawlOptions {
        public String url;
        public String[] outputFormats;
        public Integer itemsLimit;
        public String whitelistRegexp;
        public String blacklistRegexp;
        public String webhookUrl;
        public Boolean respectRobotsTxt;
        public Boolean mainContentOnly;
        public Integer maxDepth;
        public int maxAge = DEFAULT_MAX_AGE;
        public Boolean keepQueryParams;

        public CrawlOptions(String url) {
            this.url = url;
        }

        public CrawlOptions outputFormats(String... outputFormats) {
            this.outputFormats = outputFormats;
            return this;
        }

        public CrawlOptions itemsLimit(int itemsLimit) {
            this.itemsLimit = itemsLimit;
            return this;
        }

        public CrawlOptions whitelistRegexp(String whitelistRegexp) {
            this.whitelistRegexp = whitelistRegexp;
            return this;
        }

        public CrawlOptions blacklistRegexp(String blacklistRegexp) {
            this.blacklistRegexp = blacklistRegexp;
            return this;
        }

        public CrawlOptions webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public CrawlOptions respectRobotsTxt(boolean respectRobotsTxt) {
            this.respectRobotsTxt = respectRobotsTxt;
            return this;
        }

        public CrawlOptions mainContentOnly(boolean mainContentOnly) {
            this.mainContentOnly = mainContentOnly;
            return this;
        }

        public CrawlOptions maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public CrawlOptions maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public CrawlOptions keepQueryParams(boolean keepQueryParams) {
            this.keepQueryParams = keepQueryParams;
            return this;
        }
    }

    /**
     * Full parameter set for {@link #scrape(ScrapeOptions)} / {@link #scrapeAsync(ScrapeOptions)},
     * mirroring the JS SDK's {@code ScrapeRequest}.
     */
    public static class ScrapeOptions {
        public String url;
        public String[] outputFormats;
        public String webhookUrl;
        public String cleanSelectors;
        public List<Action> actions;
        public String prompt;
        /** JSON Schema (OpenAI Structured Outputs format), as a raw JSON object string, e.g. {"type":"object",...}. */
        public String responseSchemaJson;
        public Boolean mainContentOnly;
        public int maxAge = DEFAULT_MAX_AGE;

        public ScrapeOptions(String url) {
            this.url = url;
        }

        public ScrapeOptions outputFormats(String... outputFormats) {
            this.outputFormats = outputFormats;
            return this;
        }

        public ScrapeOptions webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public ScrapeOptions cleanSelectors(String cleanSelectors) {
            this.cleanSelectors = cleanSelectors;
            return this;
        }

        public ScrapeOptions actions(List<Action> actions) {
            this.actions = actions;
            return this;
        }

        public ScrapeOptions prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public ScrapeOptions responseSchemaJson(String responseSchemaJson) {
            this.responseSchemaJson = responseSchemaJson;
            return this;
        }

        public ScrapeOptions mainContentOnly(boolean mainContentOnly) {
            this.mainContentOnly = mainContentOnly;
            return this;
        }

        public ScrapeOptions maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }
    }

    /**
     * Base class for scrape actions (mirrors the JS SDK's {@code Action} union).
     */
    public static abstract class Action {
        public final String type;

        protected Action(String type) {
            this.type = type;
        }

        abstract String toJson();
    }

    /**
     * Uploads the scraped content to S3 (mirrors the JS SDK's {@code UploadS3Action}).
     */
    public static class UploadS3Action extends Action {
        public String path;
        public String accessKeyId;
        public String secretAccessKey;
        public String bucket;
        public String endpoint;

        public UploadS3Action(String path, String accessKeyId, String secretAccessKey, String bucket) {
            super("upload_s3");
            this.path = path;
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.bucket = bucket;
        }

        public UploadS3Action endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        String toJson() {
            StringBuilder json = new StringBuilder("{");
            appendField(json, "type", type);
            appendField(json, "path", path);
            appendField(json, "access_key_id", accessKeyId);
            appendField(json, "secret_access_key", secretAccessKey);
            appendField(json, "bucket", bucket);
            appendField(json, "endpoint", endpoint);
            json.append("}");
            return json.toString();
        }
    }

    /**
     * Result of a crawl operation / a job's current state (mirrors the JS SDK's {@code Job}).
     */
    public static class CrawlResult {
        public String id;
        public String orgId;
        public String status;
        public String url;
        public String[] outputFormats = new String[0];
        public String scrapeType;
        public String whitelistRegexp;
        public String blacklistRegexp;
        public int itemsLimit;
        public Integer maxDepth;
        public String createdAt;
        public String finishedAt;
        public String updatedAt;
        public String webhookUrl;
        public int recommendedPullDelayMs;
        public List<CrawlItem> items = new ArrayList<>();

        @Override
        public String toString() {
            return "CrawlResult{id='" + id + "', status='" + status + "', items=" + items.size() + "}";
        }
    }

    /**
     * Individual crawled page item (mirrors the JS SDK's {@code JobItem}).
     */
    public static class CrawlItem {
        public String id;
        public String jobId;
        public String url;
        public String status;
        public int pageStatusCode;
        public String title;
        public String lastError;
        public String createdAt;
        public String updatedAt;
        public double cost;
        public String referredUrl;
        public String links;
        public Integer depth;
        public String rawContentUrl;
        public String cleanedContentUrl;
        public String markdownContentUrl;
        public String errorCode;

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
     * Result of a scrape operation (mirrors the JS SDK's {@code ScrapeResponse}).
     */
    public static class ScrapeResult {
        public boolean success;
        public String status;
        public String content;
        public String html;
        public String markdown;
        public String cleaned;
        public String url;
        public int pageStatusCode;
        public String pageTitle;
        public String[] links = new String[0];
        /** Raw JSON of the structured_data field (present when a response_schema/prompt was used). */
        public String structuredDataJson;

        @Override
        public String toString() {
            return "ScrapeResult{status='" + status + "', url='" + url + "'}";
        }
    }

    /**
     * Response of {@link #getJobMarkdown(String)} (mirrors the JS SDK's {@code JobMarkdownResponse}).
     */
    public static class JobMarkdownResponse {
        public String contentUrl;
    }

    /**
     * Response of {@link #markdown(String)} (mirrors the JS SDK's {@code MarkdownResponse}).
     */
    public static class MarkdownResponse {
        public boolean success;
        public String markdown;
    }

    /**
     * Request for {@link #runAgent(AgentRunRequest)} / {@link #runAgentAsync(AgentRunRequest)},
     * mirroring the JS SDK's {@code AgentRunRequest}.
     */
    public static class AgentRunRequest {
        public String prompt;
        public String[] urls;
        public Boolean seedUrlsOnly;
        /** Optional JSON Schema for structured agent output, as a raw JSON object string. */
        public String outputSchemaJson;
        public String model;
        public double maxSpendUsd;

        public AgentRunRequest(String prompt, double maxSpendUsd) {
            this.prompt = prompt;
            this.maxSpendUsd = maxSpendUsd;
        }

        public AgentRunRequest urls(String... urls) {
            this.urls = urls;
            return this;
        }

        public AgentRunRequest seedUrlsOnly(boolean seedUrlsOnly) {
            this.seedUrlsOnly = seedUrlsOnly;
            return this;
        }

        public AgentRunRequest outputSchemaJson(String outputSchemaJson) {
            this.outputSchemaJson = outputSchemaJson;
            return this;
        }

        public AgentRunRequest model(String model) {
            this.model = model;
            return this;
        }
    }

    /**
     * Result of an agent run (mirrors the JS SDK's {@code AgentRun}).
     */
    public static class AgentRun {
        public String id;
        public String status;
        public String prompt;
        public String model;
        /** Raw JSON of the run's output data (shape depends on outputSchemaJson, if any). */
        public String dataJson;
        public String errorReason;
        public double balanceUsedUsd;
        public Double maxSpendUsd;
        public String[] urls = new String[0];
        public String createdAt;
        public String updatedAt;
        public boolean success;

        @Override
        public String toString() {
            return "AgentRun{id='" + id + "', status='" + status + "'}";
        }
    }

    /**
     * Exception thrown by WebCrawlerAPI operations
     */
    public static class WebCrawlerAPIException extends Exception {
        private final String errorCode;
        private final int statusCode;

        public WebCrawlerAPIException(String errorCode, String message) {
            this(errorCode, message, 0);
        }

        public WebCrawlerAPIException(String errorCode, String message, int statusCode) {
            super(message);
            this.errorCode = errorCode;
            this.statusCode = statusCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        /** HTTP status code of the failed response, or 0 if not applicable (e.g. network errors). */
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String toString() {
            return "WebCrawlerAPIException{errorCode='" + errorCode + "', statusCode=" + statusCode + ", message='" + getMessage() + "'}";
        }
    }
}
