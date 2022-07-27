package com.armongate.mobilepasssdk.model;

public class PassResult {
    public boolean  success;
    public Integer  direction;
    public String   clubId;
    public String   clubName;

    public PassResult(boolean success, Integer direction, String clubId, String clubName) {
        this.success    = success;
        this.direction  = direction;
        this.clubId     = clubId;
        this.clubName   = clubName;
    }
}
