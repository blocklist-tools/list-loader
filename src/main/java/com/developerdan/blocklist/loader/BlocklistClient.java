package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.entity.Blocklist;
import com.developerdan.blocklist.loader.entity.Version;
import com.developerdan.blocklist.tools.Domain;
import com.developerdan.blocklist.tools.DomainListParser;
import com.developerdan.blocklist.tools.ParsedList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BlocklistClient extends ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlocklistClient.class);
    private final Configuration configuration;
    private final HttpClient httpClient;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    public BlocklistClient(Configuration configuration) {
        this.configuration = configuration;
        httpClient = buildHttpClient();
    }

    public List<Blocklist> getLists() {
        List<Blocklist> allLists = new ArrayList<>(200);
        List<Blocklist> page;
        var pageNumber = 0;
        do {
            page = getLists(pageNumber);
            allLists.addAll(page);
            pageNumber++;
        } while (!page.isEmpty());
        return allLists;
    }

    public List<Blocklist> getLists(int page) {
        var url = buildUrl("/blocklists?page=" + page);
        var request = buildHttpRequest(url)
                .GET().build();
        LOGGER.debug("Loading blocklists page {}", page);
        try {
            var response = httpClient.send(request, new JsonBodyHandler<>(Blocklist[].class));
            return Arrays.asList(response.body());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Blocklist getList(UUID id) {
        var url = buildUrl("/blocklists/" + id);
        var request = buildHttpRequest(url)
                .GET().build();
        LOGGER.debug("Loading blocklist page {}", id);
        try {
            var response = httpClient.send(request, new JsonBodyHandler<>(Blocklist.class));
            return response.body();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Version[] getVersions(UUID blocklistId) {
        var url = buildUrl("/blocklists/" + blocklistId + "/versions");
        var request = buildHttpRequest(url)
                .GET().build();
        LOGGER.trace("Loading versions for blocklist {}", blocklistId);
        try {
            var response = httpClient.send(request, new JsonBodyHandler<>(Version[].class));
            return response.body();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Version createVersion(Version version) {
        HttpResponse response;
        var request = buildHttpRequest(buildUrl("/versions"))
                .POST(JsonBodyHandler.requestFromVersion(version)).build();
        try {
            LOGGER.trace("Creating new blocklist version for blocklist {}.", version.getBlocklistId());
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ApiException(e);
        }
        if (response.statusCode() != 201) {
            throw new ApiException("Unable to create version. Api Status: " + response.statusCode() + ", body: " + response.body());
        }
        try {
            return MAPPER.readValue((String)response.body(), Version.class);
        } catch (JsonProcessingException e) {
            throw new ApiException("Unable to parse version entity from response body: " + response.body(), e);
        }
    }

    public Version updateVersion(Version version) {
        HttpResponse response;
        var request = buildHttpRequest(buildUrl("/versions"))
                .PUT(JsonBodyHandler.requestFromVersion(version)).build();
        try {
            LOGGER.trace("Updating blocklist version {}.", version.getId());
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ApiException(e);
        }
        if (response.statusCode() != 200) {
            throw new ApiException("Unable to update version. Api Status: " + response.statusCode() + ", body: " + response.body());
        }
        try {
            return MAPPER.readValue((String)response.body(), Version.class);
        } catch (JsonProcessingException e) {
            throw new ApiException("Unable to parse version entity from response body: " + response.body(), e);
        }
    }

    public void deleteVersion(Version version) {
        HttpResponse response;
        var url = buildUrl("/versions/" + version.getId());
        var request = buildHttpRequest(url)
                .DELETE().build();
        try {
            LOGGER.warn("Deleting blocklist version {}.", version.getId());
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ApiException(e);
        }
        if (response.statusCode() != 200) {
            throw new ApiException("Unable to delete version. Api Status: " + response.statusCode() + ", body: " + response.body());
        }
    }

    public CompletableFuture<Boolean> startEntryPeriod(Version initialVersion, Domain domain) {
        var url = buildUrl("/blocklists/" + initialVersion.getBlocklistId() + "/versions/" + initialVersion.getId() + "/entries");
        var request = buildHttpRequest(url)
                .POST(HttpRequest.BodyPublishers.ofString(domain.toString())).build();
        LOGGER.trace("Creating new entry period for blocklist {}, domain {}", initialVersion.getBlocklistId(), domain);
        var future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        return future.thenApply(response -> {
            if (response.statusCode() != 201) {
                throw new ApiException("Unable to start entry period. Api Status: " + response.statusCode() + ", body: " + response.body());
            }
            return true;
        });
    }

    public CompletableFuture<Boolean> endEntryPeriod(Version lastIncludedVersion, Domain domain) {
        var url = buildUrl("/blocklists/" + lastIncludedVersion.getBlocklistId() + "/versions/" + lastIncludedVersion.getId() + "/entries");
        var request = buildHttpRequest(url)
                .PUT(HttpRequest.BodyPublishers.ofString(domain.toString())).build();
        LOGGER.trace("Ending entry period for blocklist {}, domain {}", lastIncludedVersion.getBlocklistId(), domain);
        var future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        return future.thenApply(response -> {
            if (response.statusCode() != 201) {
                throw new ApiException("Unable to end entry period " + lastIncludedVersion.getId() + ". Api Status: " + response.statusCode() + ", body: " + response.body());
            }
            return true;
        });
    }

    public ParsedList<Domain> getFullList(Version version) {
        var url = buildUrl("/versions/" + version.getId() + "/entries");
        var request = buildHttpRequest(url)
                .GET().build();
        LOGGER.trace("Loading entries for version {}", version.getId());
        try {
            var parser = new DomainListParser();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            var parsedList = parser.parseStream(response.body());
            parsedList.setOriginalSha(version.getRawSha256());
            return parsedList;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String buildUrl(String url) {
        return configuration.blocklistApiBaseUrl() + url;
    }

    @Override
    protected HttpRequest.Builder buildHttpRequest(String url) {
        return super.buildHttpRequest(url)
                .header("Authorization-Token", configuration.blocklistApiAuthToken())
                .timeout(Duration.ofSeconds(120));
    }
}
