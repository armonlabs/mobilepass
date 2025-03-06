package com.armongate.mobilepasssdk.enums;

public enum AnalyticsOS {
    ANDROID("android"),
    IOS("ios");

    private final String value;

    AnalyticsOS(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}