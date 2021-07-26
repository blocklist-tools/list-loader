package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.entity.Version;
import com.developerdan.blocklist.tools.BlocklistParser;
import com.developerdan.blocklist.tools.Domain;
import com.developerdan.blocklist.tools.DomainListParser;
import com.developerdan.blocklist.tools.HostsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static BlocklistClient client;
    private static boolean IMPORT_NEW_LIST_ARG = false;
    private static UUID BLOCKLIST_ID_ARG = null;
    private static Path HISTORY_FILE_PATH_ARG = null;

    public static void main(final String[] args) {
        parseArgs(args);
        client = new BlocklistClient(new Configuration());
        if (IMPORT_NEW_LIST_ARG) {
            importNewList(BLOCKLIST_ID_ARG, HISTORY_FILE_PATH_ARG);
        } else {
            loadAllLists();
        }
    }

    private static void loadAllLists() {
        var lists = client.getLists();
        Collections.shuffle(lists);
        lists.forEach(list -> {
            var blocklistParser = getParser(list.getFormat());
            parseList(list.getName(), blocklistParser, list.getDownloadUrl(), list.getId(), null, 0);
        });
    }

    private static void importNewList(UUID blocklistId, Path historyFilePath) {
        var list = client.getList(blocklistId);
        var versions = JsonBodyHandler.historicalLists(historyFilePath);
        var versionCount = versions.size();
        AtomicInteger versionIndex = new AtomicInteger(1);
        versions.stream().sorted().forEachOrdered(history -> {
            LOGGER.info("History import: {} of {}", versionIndex.getAndIncrement(), versionCount);
            var blocklistParser = getParser(list.getFormat());
            var successful = parseList(list.getName(), blocklistParser, history.getUrl(), list.getId(), Instant.ofEpochSecond(history.getCommitEpoch()), 0);
            if (!successful) {
                throw new RuntimeException("Unable to load list " + list.getName() + ": " + history.getUrl());
            }
        });
    }

    private static BlocklistParser<Domain> getParser(String format) {
        if ("hosts".equals(format)) {
            return new HostsParser();
        }
        if ("domain".equals(format)) {
            return new DomainListParser();
        }
        throw new IllegalArgumentException("Unknown list format: " + format);
    }

    private static boolean parseList(String listName, BlocklistParser<Domain> parser, String url, UUID blocklistId, Instant createdOn, int attempt) {
        Version createdVersion = null;
        try {
            var parsedList = parser.parseUrl(url);
            if (parsedList.getRecords().isEmpty()) {
                LOGGER.warn("List {} is empty! Url: {}", listName, url);
            }
            var version = new Version(blocklistId, parsedList, createdOn, false);
            var optionalPreviousVersion= loadListsPreviousVersion(blocklistId);
            TreeSet<Domain> previousEntries = new TreeSet<>();
            var previousVersion = optionalPreviousVersion.orElse(null);
            if (previousVersion != null) {
                var previousParsedList = client.getFullList(optionalPreviousVersion.get());
                previousEntries.addAll(previousParsedList.getRecords());
            } else {
                LOGGER.warn("No previous version found for list {}", listName);
            }
            createdVersion = client.createVersion(version);
            var loadStarted = Instant.now();
            var noChanges = createEntryPeriods(listName, previousVersion, previousEntries, createdVersion, parsedList.getRecords());
            LOGGER.info("List loaded after {} seconds", (Instant.now().toEpochMilli() - loadStarted.toEpochMilli()) / 1000.0);
            if (noChanges) {
                LOGGER.warn("No change detected for list {}", listName);
                client.deleteVersion(createdVersion);
                if (previousVersion != null) {
                    previousVersion.setParsedSha256(version.getParsedSha256());
                    previousVersion.setLastSeen(createdOn);
                    client.updateVersion(previousVersion);
                }
            } else {
                createdVersion.setFullyLoaded(true);
                client.updateVersion(createdVersion);
            }
            return true;
        } catch (Throwable e) {
            LOGGER.error("Attempt {} Failed to parse {}, due to {}. Cause: {}", attempt, url, e.getMessage(), e.getCause());
            e.printStackTrace();
            if (createdVersion != null) {
                LOGGER.warn("Deleting list version {} for blocklist {}, due to error.", createdVersion.getId(), url);
                client.deleteVersion(createdVersion);
            }
            if (attempt < 3) {
                try {
                    Thread.sleep(90000);
                    return parseList(listName, parser, url, blocklistId, createdOn, attempt+1);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
            return false;
        }
    }

    private static boolean createEntryPeriods(String listName, Version previousVersion, NavigableSet<Domain> previousEntries, Version currentVersion, NavigableSet<Domain> currentEntries) {
        var apiRequests = new ArrayList<CompletableFuture<Boolean>>();
        TreeSet<Domain> allDomains = new TreeSet<>(previousEntries);
        allDomains.addAll(currentEntries);
        var addedCount = 0;
        var removedCount = 0;
        var unchangedCount = 0;
        for(Domain entry : allDomains) {
            if (!previousEntries.contains(entry)) {
                apiRequests.add(client.startEntryPeriod(currentVersion, entry));
                addedCount++;
            } else if (!currentEntries.contains(entry)) {
                apiRequests.add(client.endEntryPeriod(previousVersion, entry));
                removedCount++;
            } else {
                unchangedCount++;
            }
            if (apiRequests.size() > 80) {
                waitForRequestsToComplete(apiRequests);
                apiRequests = new ArrayList<>();
            }
        }
        LOGGER.info("Loaded version of {}. Added {}, removed {}, unmodified {}", listName, addedCount, removedCount, unchangedCount);
        waitForRequestsToComplete(apiRequests);

        return addedCount == 0 && removedCount == 0 && unchangedCount > 0;
    }

    private static void waitForRequestsToComplete(ArrayList<CompletableFuture<Boolean>> apiRequests) {
        try {
            CompletableFuture.allOf(apiRequests.toArray(new CompletableFuture[0])).join();
        } catch (Throwable e) {
            LOGGER.error("Failed updating entry periods: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static Optional<Version> loadListsPreviousVersion(UUID blocklistId) {
        var versions = client.getVersions(blocklistId);
        for (Version version : versions) {
            if (version.isFullyLoaded()) {
                return Optional.of(version);
            }
            client.deleteVersion(version);
        }
        return Optional.empty();
    }

    private static void parseArgs(final String[] args) {
        for(var i = 0; i< args.length; i++){
            if("--blocklist".equals(args[i])) {
                BLOCKLIST_ID_ARG = UUID.fromString(args[i+1]);
            }

            if("--import".equals(args[i])) {
                IMPORT_NEW_LIST_ARG = true;
                HISTORY_FILE_PATH_ARG = Paths.get(args[i+1]);
            }

            if("--help".equals(args[i])) {
                System.out.println("--blocklist [UUID]");
                System.out.println("--import [path]");
                System.out.println("--help");
                System.exit(0);
            }
        }
    }
}
