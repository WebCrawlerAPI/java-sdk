# Batch scrape

Scrape an explicit list of URLs in one async request. The SDK creates the batch, and `scrape(...)` polls until it finishes. Lower-level methods let you create, page through, inspect and cancel a batch yourself.

API reference: https://webcrawlerapi.com/docs/api/batch-scrape. Guide: https://webcrawlerapi.com/docs/guides/batch-scrape.

All types are nested in `WebCrawlerAPI` (for example `WebCrawlerAPI.BatchScrapeOptions`). All methods throw `WebCrawlerAPI.WebCrawlerAPIException`.

## Quick example

```java
import java.util.Arrays;

WebCrawlerAPI client = new WebCrawlerAPI("YOUR_API_KEY");

WebCrawlerAPI.BatchScrapeResult batch = client.scrape(
        Arrays.asList("https://example.com/", "https://example.com/pricing"),
        "markdown");

System.out.println(batch.status + " " + batch.completed + "/" + batch.total);
for (WebCrawlerAPI.BatchScrapeItem item : batch.data) {
    System.out.println(item.url + " -> " + item.status + " " + item.markdownUrl);
}
```

## Methods

### scrape(List&lt;String&gt;, String)

```java
BatchScrapeResult scrape(List<String> urls, String scrapeType) throws WebCrawlerAPIException
```

Scrapes the URLs as one batch and waits for it to finish.

- `urls` - URLs to scrape.
- `scrapeType` - one output format: `"markdown"`, `"cleaned"`, `"html"`, `"links"` or `"images"`. `null` or blank sends no `output_formats`, so the API default (`["markdown"]`) applies.

Returns a `BatchScrapeResult` (first page of items, see below). Uses the default polling settings (1000 polls, 2000 ms apart).

### scrape(List&lt;String&gt;, String[])

```java
BatchScrapeResult scrape(List<String> urls, String[] outputFormats) throws WebCrawlerAPIException
```

Same as above with several output formats. `null` uses the API default.

### scrape(BatchScrapeOptions)

```java
BatchScrapeResult scrape(BatchScrapeOptions options) throws WebCrawlerAPIException
```

Creates the batch with the full option set and polls until the status is `completed`, `failed` or `cancelled`. Each poll fetches the first page (`options.limit` items). The returned result has `invalidUrls` filled from the creation response.

Errors:
- `IllegalArgumentException` if `urls` is null or empty.
- `WebCrawlerAPIException` with code `timeout` if the batch is still running after `options.maxPolls` polls.
- `WebCrawlerAPIException` with code `interrupted` if the thread is interrupted while waiting.
- `WebCrawlerAPIException` with code `invalid_response` if the API response has no batch id.
- Other API or network errors are thrown as `WebCrawlerAPIException`.

If `scrape` times out, the batch keeps running on the server. Use `scrapeBatchAsync` plus `getBatch` if you need the id.

```java
WebCrawlerAPI.BatchScrapeResult batch = client.scrape(
        new WebCrawlerAPI.BatchScrapeOptions("https://example.com/", "https://example.com/pricing")
                .outputFormats("markdown", "cleaned")
                .mainContentOnly(true)
                .maxConcurrency(5));
```

#### BatchScrapeOptions

Constructors: `BatchScrapeOptions(List<String> urls)` and `BatchScrapeOptions(String... urls)`. Setters return the options object, so they chain.

| Setter | Request field | Description |
| --- | --- | --- |
| `outputFormats(String...)` | `output_formats` | `markdown`, `cleaned`, `html`, `links`, `images`. API default is `["markdown"]`. |
| `mainContentOnly(boolean)` | `main_content_only` | Extract only the main content. Not sent unless set. |
| `cleanSelectors(String)` | `clean_selectors` | CSS selectors to clean from the output. Not sent unless set. |
| `maxConcurrency(int)` | `max_concurrency` | Max URLs scraped in parallel. `0` or unset means no per-batch cap. |
| `ignoreInvalidUrls(boolean)` | `ignore_invalid_urls` | If `false`, any invalid URL fails the whole request with `400`. API default is `true`. |
| `webhookUrl(String)` | `webhook_url` | Public http(s) URL called once when the batch finishes. |
| `maxAge(int)` | `max_age` | Cache duration in seconds. The SDK always sends it and its default is `0` (bypass the cache). |
| `respectRobotsTxt(boolean)` | `respect_robots_txt` | Respect each website's robots.txt. Not sent unless set. |
| `limit(int)` | query `limit` | Items per page while polling, 1 to 500. Default is the API default of 100. |
| `maxPolls(int)` | - | Max status polls in `scrape(options)`. Default `1000`. |
| `pollDelayMs(int)` | - | Delay between polls in ms. Default `2000`. |

### scrapeBatchAsync

```java
BatchScrapeCreated scrapeBatchAsync(BatchScrapeOptions options) throws WebCrawlerAPIException
```

Creates the batch (`POST /v2/batch/scrape`) and returns immediately. Throws `IllegalArgumentException` if `urls` is null or empty, and `invalid_response` if no id comes back.

`BatchScrapeCreated` fields:
- `String id` - batch id.
- `String statusUrl` - status endpoint URL.
- `List<BatchScrapeInvalidUrl> invalidUrls` - rejected URLs, each with `url`, `errorCode`, `errorMessage`.

```java
WebCrawlerAPI.BatchScrapeCreated created = client.scrapeBatchAsync(
        new WebCrawlerAPI.BatchScrapeOptions("https://example.com/")
                .webhookUrl("https://your-server.com/webhook"));
System.out.println(created.id);
```

### getBatch

```java
BatchScrapeResult getBatch(String batchId, Integer limit, String cursor) throws WebCrawlerAPIException
```

Gets progress and one page of items (`GET /v2/batch/scrape/:id`).

- `batchId` - batch id.
- `limit` - items per page, 1 to 500. `null` uses the API default of 100.
- `cursor` - the `next` value of the previous page. `null` or empty for the first page.

`BatchScrapeResult` fields:
- `id`, `status` (`scraping`, `completed`, `failed` or `cancelled`)
- `total`, `completed`, `failed`, `canceled` - counters
- `List<BatchScrapeItem> data` - the page of items
- `String next` - cursor for the next page, empty on the last page
- `List<BatchScrapeInvalidUrl> invalidUrls` - only set by `scrape(...)`; empty from `getBatch`

`BatchScrapeItem` fields: `url`, `status` (for example `done` or `error`), `pageStatusCode`, `title`, `markdownUrl`, `cleanedUrl`, `htmlUrl` (present for finished items and requested formats), `fromCache`, `errorCode`, `errorMessage` (set for failed items; see https://webcrawlerapi.com/docs/errors).

### getBatchErrors

```java
BatchScrapeErrors getBatchErrors(String batchId) throws WebCrawlerAPIException
```

Lists failed URLs (`GET /v2/batch/scrape/:id/errors`).

`BatchScrapeErrors` fields:
- `List<BatchScrapeError> errors` - each with `url`, `errorCode`, `errorMessage`.
- `String[] robotsBlocked` - URLs blocked by robots.txt (when `respectRobotsTxt` is on).

```java
for (WebCrawlerAPI.BatchScrapeError e : client.getBatchErrors(batch.id).errors) {
    System.out.println(e.url + " " + e.errorCode);
}
```

### cancelBatch

```java
void cancelBatch(String batchId) throws WebCrawlerAPIException
```

Cancels a running batch (`DELETE /v2/batch/scrape/:id`). Items that did not start are marked `canceled` and are not charged. The API answers `400` if the batch already finished, which is thrown as `WebCrawlerAPIException`.

## Pagination

`scrape(...)` returns only the first page. If `batch.next` is not empty, fetch the rest:

```java
WebCrawlerAPI.BatchScrapeResult page = batch;
while (true) {
    for (WebCrawlerAPI.BatchScrapeItem item : page.data) {
        System.out.println(item.url + " " + item.markdownUrl);
    }
    if (page.next.isEmpty()) break;
    page = client.getBatch(batch.id, 500, page.next);
}
```

## Invalid URLs

URLs are normalized and deduplicated. Empty, malformed, non-public, duplicate and unresolvable URLs are returned in `invalidUrls` (error codes `invalid_request`, `name_not_resolved`) when `ignoreInvalidUrls` is true (the API default). With `ignoreInvalidUrls(false)` the request fails instead. A page whose content duplicates another page in the batch fails later as an item (`duplicate_item`), not in `invalidUrls`.

## Webhook or polling

- Polling: `scrape(...)` does it for you. For manual polling use `scrapeBatchAsync` and call `getBatch` until `status` is not `scraping`.
- Webhook: set `webhookUrl`. The API sends one `POST` with the same body as the status endpoint (first 100 items) when the batch finishes; fetch more pages with `getBatch` and `cursor`. See https://webcrawlerapi.com/docs/async-requests.

`prompt`, `response_schema` and `actions` are not supported in batch scrape.
