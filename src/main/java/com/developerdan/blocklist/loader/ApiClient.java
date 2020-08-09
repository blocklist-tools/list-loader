package com.developerdan.blocklist.loader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class ApiClient {
    protected static HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    protected HttpRequest.Builder buildHttpRequest(String url) {
        return HttpRequest
                .newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:78.0) Gecko/20100101 Firefox/78.0")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .uri(URI.create(url));
    }
}
