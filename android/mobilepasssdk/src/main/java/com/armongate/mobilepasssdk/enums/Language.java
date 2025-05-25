package com.armongate.mobilepasssdk.enums;

public enum Language {
    TR("tr"),
    EN("en");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
