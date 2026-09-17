package com.armongate.mobilepasssdk.enums;

import java.util.Locale;

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

    /**
     * Resolve a configured language value to a supported language.
     *
     * Applies exactly the rule the server applies to Accept-Language: anything
     * starting with "tr" is Turkish, everything else is English. Both the request
     * header and the Bluetooth language byte are derived from this, so the server
     * and the device can never answer the same pass attempt in different
     * languages - values like "en-US" or "EN" used to resolve to English on the
     * server and Turkish on the device.
     */
    public static Language normalized(String value) {
        String normalized = value != null ? value.trim().toLowerCase(Locale.ROOT) : "";
        return normalized.startsWith("tr") ? TR : EN;
    }
}
