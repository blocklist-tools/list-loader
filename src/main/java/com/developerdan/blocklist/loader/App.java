package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.entity.Blocklist;
import com.developerdan.blocklist.loader.entity.HistoricalList;
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
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static BlocklistClient client;
    private static boolean importNewList = false;
    private static UUID blocklistId = null;
    private static Path historyLoadFilePath = null;

    public static void main(final String[] args) {
        parseArgs(args);
        client = new BlocklistClient(new Configuration());
        if (importNewList) {
            importNewList(blocklistId);
        } else {
            loadAllLists();
        }
    }

    private static void loadAllLists() {
        var lists = client.getLists();
        Collections.shuffle(lists);
        lists.forEach(App::parseCurrentList);
    }

    private static void importNewList(UUID blocklistId) {
        var list = client.getList(blocklistId);
        var versions = JsonBodyHandler.historicalLists(historyLoadFilePath);
        versions.stream().sorted().forEach(history -> parseHistoricalList(list, history));
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

    private static void parseCurrentList(Blocklist list) {
        var blocklistParser = getParser(list.getFormat());
        parseList(blocklistParser, list.getDownloadUrl(), list.getId(), null);
    }

    private static void parseHistoricalList(Blocklist list, HistoricalList history) {
        var blocklistParser = getParser(list.getFormat());
        parseList(blocklistParser, history.getUrl(), list.getId(), Instant.ofEpochSecond(history.getCommitEpoch()));
    }

    private static void parseList(BlocklistParser<Domain> parser, String url, UUID blocklistId, Instant createdOn) {
        Version createdVersion = null;
        try {
            var parsedList = parser.parseUrl(url);
            if (parsedList.getRecords().isEmpty()) {
                LOGGER.warn("List {} is empty!", url);
            }
            LOGGER.info("List {} has {} entries.", url, parsedList.getRecords().size());
            var version = new Version(blocklistId, parsedList, createdOn, false);
            var optionalPreviousVersion= loadListsPreviousVersion(blocklistId);
            TreeSet<Domain> previousEntries = new TreeSet<>();
            var previousVersion = optionalPreviousVersion.orElse(null);
            if (previousVersion != null) {
                var previousParsedList = client.getFullList(optionalPreviousVersion.get());
                previousEntries.addAll(previousParsedList.getRecords());
            }
            createdVersion = client.createVersion(version);
            createEntryPeriods(previousVersion, previousEntries, createdVersion, parsedList.getRecords());
            createdVersion.setFullyLoaded(true);
            client.updateVersion(createdVersion);
        } catch (Throwable e) {
            LOGGER.error("Failed to parse {}, due to {}.", url, e.getMessage());
            if (createdVersion != null) {
                LOGGER.warn("Deleting list version {} for blocklist {}, due to error.", createdVersion.getId(), url);
                client.deleteVersion(createdVersion);
            }
        }
    }

    private static void createEntryPeriods(Version previousVersion, NavigableSet<Domain> previousEntries, Version currentVersion, NavigableSet<Domain> currentEntries) {
        TreeSet<Domain> allDomains = new TreeSet<>(previousEntries);
        allDomains.addAll(currentEntries);
        var addedCount = 0;
        var removedCount = 0;
        var unchangedCount = 0;
        for(Domain entry : allDomains) {
            if (!previousEntries.contains(entry)) {
                client.startEntryPeriod(currentVersion, entry);
                addedCount++;
            } else if (!currentEntries.contains(entry)) {
                client.endEntryPeriod(previousVersion, entry);
                removedCount++;
            } else {
                unchangedCount++;
            }
        }
        LOGGER.info("Updating list entries. New: {}. Removed: {}. Unchanged: {}", addedCount, removedCount, unchangedCount);
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
                blocklistId = UUID.fromString(args[i+1]);
            }

            if("--import".equals(args[i])) {
                importNewList = true;
                historyLoadFilePath = Paths.get(args[i+1]);
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
