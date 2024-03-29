package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.entity.HistoricalList;
import com.developerdan.blocklist.loader.entity.ListImport;
import com.developerdan.blocklist.loader.entity.Version;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JsonBodyHandler<W> implements HttpResponse.BodyHandler<W> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
    private final Class<W> wClass;

    public JsonBodyHandler(Class<W> wClass) {
        this.wClass = wClass;
    }

    @Override
    public HttpResponse.BodySubscriber<W> apply(HttpResponse.ResponseInfo responseInfo) {
        return asJSON(wClass);
    }

    public static <T> HttpResponse.BodySubscriber<T> asJSON(Class<T> targetType) {
        HttpResponse.BodySubscriber<String> upstream = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);

        return HttpResponse.BodySubscribers.mapping(
                upstream,
                (String body) -> {
                    try {
                        return MAPPER.readValue(body, targetType);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    public static HttpRequest.BodyPublisher requestFromVersion(Version version) {
        try {
            var body = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(version);
            return HttpRequest.BodyPublishers.ofString(body);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ex);
        }
    }

    public static List<HistoricalList> historicalLists(Path filePath) {
        try {
            var listImport = MAPPER.readValue(filePath.toFile(), ListImport.class);
            var versions = Arrays.asList(listImport.getVersions());
            Collections.sort(versions);
            return versions;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
