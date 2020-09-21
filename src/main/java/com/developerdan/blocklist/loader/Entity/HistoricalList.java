package com.developerdan.blocklist.loader.Entity;

public class HistoricalList {
    private String url;
    private long commitEpoch;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getCommitEpoch() {
        return commitEpoch;
    }

    public void setCommitEpoch(int commitEpoch) {
        this.commitEpoch = commitEpoch;
    }
}
