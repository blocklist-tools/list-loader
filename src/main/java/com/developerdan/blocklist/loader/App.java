package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.Entity.Blocklist;
import com.developerdan.blocklist.loader.Entity.HistoricalList;
import com.developerdan.blocklist.loader.Entity.Version;
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
import java.util.UUID;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static BlocklistClient client;
    private static boolean loadHistory = false;
    private static UUID blocklistId = null;
    private static Path historyLoadFilePath = null;

    public static void main(final String[] args) {
        parseArgs(args);
        client = new BlocklistClient();
        if (loadHistory) {
            loadHistory();
        } else {
            loadAllLists();
        }
    }

    private static void loadAllLists() {
        var lists = client.getLists();
        Collections.shuffle(lists);
        lists.forEach(App::parseCurrentList);
    }

    private static void loadHistory() {
        var list = client.getList(blocklistId);
        var versions = JsonBodyHandler.historicalLists(historyLoadFilePath);
        versions.forEach((history) -> parseHistoricalList(list, history));
    }

    private static BlocklistParser<Domain> getParser(String format) {
        if ("hosts".equals(format)) {
            return new HostsParser();
        } else if ("domain".equals(format)) {
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
            var version = new Version(blocklistId, parsedList, createdOn);
            createdVersion = client.createVersion(version);
            client.createEntries(parsedList, createdVersion);
        } catch (Throwable e) {
            LOGGER.error("Failed to parse {}, due to {}.", url, e.getMessage());
            if (createdVersion != null && createdVersion.isNewlyCreated()) {
                LOGGER.warn("Deleting list version {} for blocklist {}, due to error.", createdVersion.getId(), url);
                client.deleteVersion(createdVersion);
            }
        }
    }

    private static void parseArgs(final String[] args) {
        for(int i = 0; i< args.length; i++){
            if("--blocklist".equals(args[i])) {
                blocklistId = UUID.fromString(args[i+1]);
            }

            if("--import".equals(args[i])) {
                loadHistory = true;
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
