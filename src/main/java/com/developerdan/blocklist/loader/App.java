package com.developerdan.blocklist.loader;

import com.developerdan.blocklist.loader.Entity.Blocklist;
import com.developerdan.blocklist.loader.Entity.Version;
import com.developerdan.blocklist.tools.BlocklistParser;
import com.developerdan.blocklist.tools.Domain;
import com.developerdan.blocklist.tools.DomainListParser;
import com.developerdan.blocklist.tools.HostsParser;

public class App {
    private static BlocklistClient client;

    public static void main(final String[] args) {
        client = new BlocklistClient();
        var lists = client.getLists();
        lists.stream()
                .parallel()
                .forEach(App::parseList);
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
        var blocklistParser = getParser(list.getFormat());
        var parsedList = blocklistParser.parseUrl(list.getDownloadUrl());
        var version = new Version(list, parsedList);
        var createdVersion = client.createVersion(version);
        client.createEntries(parsedList, createdVersion);
    }
}
