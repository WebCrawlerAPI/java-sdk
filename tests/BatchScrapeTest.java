import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Mocked-HTTP tests for batch scrape (local HttpServer, no network). */
public class BatchScrapeTest {

    static final List<String> requests = new ArrayList<>();
    static final AtomicInteger polls = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        SimpleTestFramework.suite("Batch scrape tests");

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/batch/scrape", ex -> {
            String path = ex.getRequestURI().toString();
            String method = ex.getRequestMethod();
            String body = read(ex.getRequestBody());
            requests.add(method + " " + path + " " + body);
            String resp;
            int code = 200;
            if (method.equals("POST")) {
                resp = "{\"id\":\"b1\",\"status_url\":\"http://x/v2/batch/scrape/b1\",\"invalid_urls\":"
                        + "[{\"url\":\"not a url\",\"error_code\":\"invalid_request\",\"error_message\":\"invalid url\"}]}";
            } else if (method.equals("DELETE")) {
                resp = "{\"status\":\"canceled\"}";
            } else if (path.contains("/errors")) {
                resp = "{\"errors\":[{\"url\":\"https://a.com/x\",\"error_code\":\"webpage_non_success\",\"error_message\":\"bad {x}\"}],\"robots_blocked\":[\"https://a.com/r\"]}";
            } else if (path.startsWith("/v2/batch/scrape/missing")) {
                code = 404;
                resp = "{\"error_code\":\"not_found\",\"error_message\":\"batch not found\"}";
            } else if (polls.incrementAndGet() < 2) {
                resp = "{\"id\":\"b1\",\"status\":\"scraping\",\"total\":2,\"completed\":0,\"failed\":0,\"canceled\":0,\"data\":[],\"next\":\"\"}";
            } else {
                resp = "{\"id\":\"b1\",\"status\":\"completed\",\"total\":2,\"completed\":1,\"failed\":1,\"canceled\":0,\"data\":["
                        + "{\"url\":\"https://a.com/\",\"status\":\"done\",\"page_status_code\":200,\"title\":\"A {b}\",\"markdown_url\":\"https://d/a.md\",\"from_cache\":true},"
                        + "{\"url\":\"https://a.com/x\",\"status\":\"error\",\"error_code\":\"webpage_non_success\",\"error_message\":\"fail\"}],\"next\":\"cur2\"}";
            }
            byte[] out = resp.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
        try {
            WebCrawlerAPI client = new WebCrawlerAPI("test-key", "http://127.0.0.1:" + server.getAddress().getPort());

            WebCrawlerAPI.BatchScrapeOptions opts = new WebCrawlerAPI.BatchScrapeOptions("https://a.com/", "https://a.com/x", "not a url")
                    .outputFormats("markdown").maxConcurrency(5).webhookUrl("https://h.com/w")
                    .ignoreInvalidUrls(true).mainContentOnly(true).respectRobotsTxt(false).pollDelayMs(10);
            WebCrawlerAPI.BatchScrapeResult r = client.scrape(opts);

            SimpleTestFramework.section("scrape(list)");
            String post = requests.get(0);
            SimpleTestFramework.assertContains("POST path", post, "POST /v2/batch/scrape ");
            SimpleTestFramework.assertContains("urls sent", post, "\"urls\":[\"https://a.com/\",\"https://a.com/x\",\"not a url\"]");
            SimpleTestFramework.assertContains("max_concurrency sent", post, "\"max_concurrency\":5");
            SimpleTestFramework.assertContains("webhook sent", post, "\"webhook_url\":\"https://h.com/w\"");
            SimpleTestFramework.assertContains("formats sent", post, "\"output_formats\":[\"markdown\"]");
            SimpleTestFramework.assertEquals("status", "completed", r.status);
            SimpleTestFramework.assertEquals("polled until done", 2, polls.get());
            SimpleTestFramework.assertEquals("total", 2, r.total);
            SimpleTestFramework.assertEquals("failed", 1, r.failed);
            SimpleTestFramework.assertEquals("items", 2, r.data.size());
            SimpleTestFramework.assertEquals("item title with braces", "A {b}", r.data.get(0).title);
            SimpleTestFramework.assertEquals("item markdown url", "https://d/a.md", r.data.get(0).markdownUrl);
            SimpleTestFramework.assertTrue("item from cache", r.data.get(0).fromCache);
            SimpleTestFramework.assertEquals("item error code", "webpage_non_success", r.data.get(1).errorCode);
            SimpleTestFramework.assertEquals("next cursor", "cur2", r.next);
            SimpleTestFramework.assertEquals("invalid urls", 1, r.invalidUrls.size());
            SimpleTestFramework.assertEquals("invalid url code", "invalid_request", r.invalidUrls.get(0).errorCode);

            SimpleTestFramework.section("scrape(list, type) overload");
            WebCrawlerAPI.BatchScrapeResult r2 = client.scrape(Arrays.asList("https://a.com/"), "html");
            SimpleTestFramework.assertEquals("overload status", "completed", r2.status);
            SimpleTestFramework.assertContains("html format sent", requests.get(requests.size() - 2), "\"output_formats\":[\"html\"]");

            SimpleTestFramework.section("getBatch / errors / cancel");
            WebCrawlerAPI.BatchScrapeResult page = client.getBatch("b1", 50, "cur2");
            String get = requests.get(requests.size() - 1);
            SimpleTestFramework.assertContains("limit and cursor", get, "GET /v2/batch/scrape/b1?limit=50&cursor=cur2");
            SimpleTestFramework.assertEquals("page items", 2, page.data.size());
            WebCrawlerAPI.BatchScrapeErrors errs = client.getBatchErrors("b1");
            SimpleTestFramework.assertEquals("errors count", 1, errs.errors.size());
            SimpleTestFramework.assertEquals("error message", "bad {x}", errs.errors.get(0).errorMessage);
            SimpleTestFramework.assertEquals("robots blocked", "https://a.com/r", errs.robotsBlocked[0]);
            client.cancelBatch("b1");
            SimpleTestFramework.assertContains("cancel is DELETE", requests.get(requests.size() - 1), "DELETE /v2/batch/scrape/b1");

            SimpleTestFramework.section("errors");
            try {
                client.getBatch("missing", null, null);
                SimpleTestFramework.fail("404 throws", "no exception");
            } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
                SimpleTestFramework.assertEquals("404 code", "not_found", e.getErrorCode());
                SimpleTestFramework.assertEquals("404 status", 404, e.getStatusCode());
            }
            SimpleTestFramework.assertThrows("empty urls rejected", IllegalArgumentException.class, () -> {
                try {
                    client.scrapeBatchAsync(new WebCrawlerAPI.BatchScrapeOptions(new ArrayList<String>()));
                } catch (WebCrawlerAPI.WebCrawlerAPIException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            server.stop(0);
        }
        System.exit(SimpleTestFramework.printSummary());
    }

    static String read(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
