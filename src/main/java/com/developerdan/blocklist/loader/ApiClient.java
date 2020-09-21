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
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    protected HttpRequest.Builder buildHttpRequest(String url) {
        return HttpRequest
                .newBuilder()
                .header("User-Agent", "Blocklist Tools: List Loader")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization-Token", "ce2f3b4d-856a-4710-94db-b907e65a7bde")
                .uri(URI.create(url));
    }
}
