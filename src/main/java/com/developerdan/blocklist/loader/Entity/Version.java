package com.developerdan.blocklist.loader.Entity;

import com.developerdan.blocklist.tools.ParsedList;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

public class Version {
    private UUID id;
    private UUID blocklistId;
    private long numEntries;
    private String rawSha256;
    private String parsedSha256;
    private Instant createdOn;
    private Instant lastSeen;

    @JsonIgnore
    private boolean newlyCreated = false;

    public Version() {
        // bean
    }

    public Version(UUID blocklistId, ParsedList parsedList, Instant createdOn) {
        this.blocklistId = blocklistId;
        this.rawSha256 = parsedList.getOriginalSha();
        this.parsedSha256 = parsedList.getParsedSha();
        this.numEntries = parsedList.getRecords().size();
        this.createdOn = createdOn;
        this.lastSeen = createdOn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBlocklistId() {
        return blocklistId;
    }

    public void setBlocklistId(UUID blocklistId) {
        this.blocklistId = blocklistId;
    }

    public long getNumEntries() {
        return numEntries;
    }

    public void setNumEntries(long numEntries) {
        this.numEntries = numEntries;
    }

    public String getRawSha256() {
        return rawSha256;
    }

    public void setRawSha256(String rawSha256) {
        this.rawSha256 = rawSha256;
    }

    public String getParsedSha256() {
        return parsedSha256;
    }

    public void setParsedSha256(String parsedSha256) {
        this.parsedSha256 = parsedSha256;
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }
}
