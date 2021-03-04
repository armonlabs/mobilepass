package com.armongate.mobilepasssdk.manager;

import android.util.Log;

public class LogManager {

    public enum LogType {
        INFO,
        WARN,
        ERROR,
        DEBUG
    }

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
        log(LogType.INFO, message);
    }

    public void warn(String message) {
        log(LogType.WARN, message);
    }

    public void error(String message) {
        log(LogType.ERROR, message);
    }

    public void debug(String message) {
        log(LogType.DEBUG, message);
    }

    // Private Functions

    private void log(LogType type, String message) {
        String prefix = "";

        switch (type) {
            case INFO:
                prefix = "INFO";
                Log.i(LOG_TAG, prefix + " | " + message);
                break;
            case WARN:
                prefix = "WARN";
                Log.w(LOG_TAG, prefix + " | " + message);
                break;
            case ERROR:
                prefix = "ERROR";
                Log.e(LOG_TAG, prefix + " | " + message);
                break;
            case DEBUG:
                prefix = "DEBUG";
                Log.d(LOG_TAG, prefix + " | " + message);
                break;
        }
    }

}
