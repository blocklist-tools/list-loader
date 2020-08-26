package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.Entity.Blocklist;
import com.developerdan.blocklist.loader.Entity.Version;
import com.developerdan.blocklist.tools.Domain;
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
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BlocklistClient extends ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlocklistClient.class);
    private static final String BASE_URL = "http://clayface.local:8181";
    private final HttpClient httpClient;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    public BlocklistClient() {
        httpClient = buildHttpClient();
    }

    public List<Blocklist> getLists() {
        List<Blocklist> allLists = new ArrayList<>(200);
        List<Blocklist> page;
        int pageNumber = 0;

        do {
            page = getLists(pageNumber);
            allLists.addAll(page);
            pageNumber++;
        } while (!page.isEmpty());
        return allLists;
    }

    public List<Blocklist> getLists(int page) {
        var request = buildHttpRequest(BASE_URL + "/blocklists?page=" + page)
                .GET().build();
        LOGGER.info("Loading blocklists page {}", page);
        try {
            var response = httpClient.send(request, new JsonBodyHandler<>(Blocklist[].class));
            return Arrays.asList(response.body());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Version createVersion(Version version) {
        HttpResponse response;
        var request = buildHttpRequest(BASE_URL + "/versions")
                .POST(JsonBodyHandler.requestFromVersion(version)).build();
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ApiException(e);
        }
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new ApiException("Unable to create version. Api Status: " + response.statusCode() + ", body: " + response.body());
        }
        try {
            return MAPPER.readValue((String)response.body(), Version.class);
        } catch (JsonProcessingException e) {
            throw new ApiException("Unable to parse version entity from response body: " + response.body(), e);
        }
    }

    public void deleteVersion(Version version) {
        HttpResponse response;
        var request = buildHttpRequest(BASE_URL + "/versions/" + version.getId())
                .DELETE().build();
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ApiException(e);
        }
        if (response.statusCode() != 200) {
            throw new ApiException("Unable to delete version. Api Status: " + response.statusCode() + ", body: " + response.body());
        }
    }

    public void createEntries(ParsedList<Domain> parsedList, Version version) {
        var records = parsedList.getRecords().stream().parallel().map(Domain::toString).collect(Collectors.toList());
        batches(records, 1000)
                .forEach((domains) -> createEntriesBatch(domains, version));
    }

    private void createEntriesBatch(List<String> entries, Version version) {
        HttpResponse response;
        var request = buildHttpRequest(BASE_URL + "/versions/" + version.getId() + "/entries")
                .PUT(JsonBodyHandler.requestFromDomains(entries)).build();
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            throw new ApiException(e);
        }
        if (response.statusCode() != 201) {
            throw new ApiException("Unable to create entry. Api Status: " + response.statusCode() + ", body: " + response.body());
        }
    }

    private static <T> Stream<List<T>> batches(List<T> source, int length) {
        if (length <= 0)
            throw new IllegalArgumentException("length = " + length);
        int size = source.size();
        if (size <= 0)
            return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }
}
