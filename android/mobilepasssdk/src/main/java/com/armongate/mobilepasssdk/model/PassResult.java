package com.armongate.mobilepasssdk.model;

public class PassResult {
    public boolean  success;
    public Integer  failCode;
    public Integer  direction;
    public String   clubId;
    public String   clubName;

    public PassResult(boolean success, Integer direction, String clubId, String clubName, Integer failCode) {
        this.success    = success;
        this.direction  = direction;
        this.clubId     = clubId;
        this.clubName   = clubName;
        this.failCode   = failCode;
    }
}
