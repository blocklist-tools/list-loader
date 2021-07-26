package com.developerdan.blocklist.loader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class ApiClient {
    protected static HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(120))
                .build();
    }

    protected HttpRequest.Builder buildHttpRequest(String url) {
        return HttpRequest
                .newBuilder()
                .timeout(Duration.ofSeconds(120))
                .header("User-Agent", "Blocklist Tools: List Loader")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .uri(URI.create(url));
    }
}
