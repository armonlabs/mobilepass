package com.armongate.mobilepasssdk.manager;

import android.util.Log;

import com.armongate.mobilepasssdk.constant.LogLevel;
import com.armongate.mobilepasssdk.model.LogItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LogManager {

    private final List<String> logItems = new ArrayList<>();

    // Singleton

    private static LogManager instance = null;
    private LogManager() { }

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }

        return instance;
    }

    // Constants

    private static final String LOG_TAG = "MobilePass";

    // Public Functions

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message, Integer code) {
        log(LogLevel.WARN, message, code);
    }

    public void error(String message, Integer code) {
        log(LogLevel.ERROR, message, code);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public List<String> getLogs() {
        return this.logItems;
    }

    // Private Functions

    private void log(int type, String message, Integer code) {
        String prefix = "";

        switch (type) {
            case LogLevel.INFO:
                prefix = "INFO";
                break;
            case LogLevel.WARN:
                prefix = "WARN";
                break;
            case LogLevel.ERROR:
                prefix = "ERROR";
                break;
            case LogLevel.DEBUG:
                prefix = "DEBUG";
                break;
        }

        Log.i(LOG_TAG, prefix + " | " + message);
        logItems.add(now() + " | " + message);

        if (type >= ConfigurationManager.getInstance().getLogLevel()) {
            DelegateManager.getInstance().onLogItemCreated(new LogItem(type, code, message));
        }
    }

    private String now() {
        DateFormat df = new SimpleDateFormat("HH:mm:ss:SSS");
        Date today = Calendar.getInstance().getTime();

        return df.format(today);
    }

}
