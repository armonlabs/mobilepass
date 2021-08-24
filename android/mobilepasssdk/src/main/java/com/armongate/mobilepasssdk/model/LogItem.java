package com.armongate.mobilepasssdk.model;

import java.util.Date;

public class LogItem {
    public int      level;
    public Integer  code;
    public String   message;
    public Date     time;

    public LogItem(int level, Integer code, String message) {
        this.level      = level;
        this.code       = code;
        this.message    = message;
        this.time       = new Date();
    }
}
