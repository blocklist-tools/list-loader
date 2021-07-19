package com.developerdan.blocklist.loader.entity;

import java.util.Objects;

public class HistoricalList implements Comparable<HistoricalList> {
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

    @Override
    public int compareTo(HistoricalList other) {
        return Long.compare(this.getCommitEpoch(), other.getCommitEpoch());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof HistoricalList)) return false;
        HistoricalList that = (HistoricalList) o;
        return commitEpoch == that.commitEpoch && url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, commitEpoch);
    }
}
