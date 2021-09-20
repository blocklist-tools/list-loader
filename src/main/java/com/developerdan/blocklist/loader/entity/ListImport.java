package com.developerdan.blocklist.loader.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListImport {
    private HistoricalList[] versions;

    public HistoricalList[] getVersions() {
        return versions;
    }

    public void setVersions(HistoricalList[] versions) {
        this.versions = versions;
    }
}
