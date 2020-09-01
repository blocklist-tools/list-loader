package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.Entity.Blocklist;
import com.developerdan.blocklist.loader.Entity.Version;
import com.developerdan.blocklist.tools.BlocklistParser;
import com.developerdan.blocklist.tools.Domain;
import com.developerdan.blocklist.tools.DomainListParser;
import com.developerdan.blocklist.tools.HostsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static BlocklistClient client;

    public static void main(final String[] args) {
        client = new BlocklistClient();
        var lists = client.getLists();
        lists.forEach(App::parseList);
    }

    private static BlocklistParser<Domain> getParser(String format) {
        if ("hosts".equals(format)) {
            return new HostsParser();
        } else if ("domain".equals(format)) {
            return new DomainListParser();
        }
        throw new IllegalArgumentException("Unknown list format: " + format);
    }

    private static void parseList(Blocklist list) {
        Version createdVersion = null;
        try {
            var blocklistParser = getParser(list.getFormat());
            var parsedList = blocklistParser.parseUrl(list.getDownloadUrl());
            if (parsedList.getRecords().isEmpty()) {
                LOGGER.warn("List {} is empty!", list.getName());
            }
            LOGGER.info("List {} has {} entries.", list.getName(), parsedList.getRecords().size());
            var version = new Version(list, parsedList);
            createdVersion = client.createVersion(version);
            client.createEntries(parsedList, createdVersion);
        } catch (Throwable e) {
            LOGGER.error("Failed to parse {}, due to {}.", list.getName(), e.getMessage());
            if (createdVersion != null && createdVersion.isNewlyCreated()) {
                LOGGER.warn("Deleting list version {} for blocklist {}, due to error.", createdVersion.getId(), list.getName());
                client.deleteVersion(createdVersion);
            }
        }
    }
}
