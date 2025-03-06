package com.armongate.mobilepasssdk.enums;

public enum AnalyticsResult {
    SUCCESS("success"),
    FAIL("fail"),
    CANCEL("cancel");

    private final String value;

    AnalyticsResult(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}