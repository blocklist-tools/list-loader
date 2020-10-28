package com.developerdan.blocklist.loader;

public class Configuration {

    public String blocklistApiBaseUrl() {
        var baseUrl = System.getenv("BLOCKLIST_API_BASE_URL");
        return assertNotNullOrEmpty(baseUrl, "Missing BLOCKLIST_API_BASE_URL environment variable.");
    }

    public String blocklistApiAuthToken() {
        var baseUrl = System.getenv("BLOCKLIST_API_AUTH_TOKEN");
        return assertNotNullOrEmpty(baseUrl, "Missing BLOCKLIST_API_AUTH_TOKEN environment variable.");
    }

    private String assertNotNullOrEmpty(String value, String exceptionMessage) {
        if (value == null || value.isEmpty() || value.isBlank()) {
            throw new RuntimeException(exceptionMessage);
        }
        return value;
    }
}
