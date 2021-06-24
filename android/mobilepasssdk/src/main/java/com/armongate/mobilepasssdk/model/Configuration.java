package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

public class Configuration {

    /**
     * Member id that will be used for validation to pass
     */
    public String memberId;

    /**
     * URL of server that communicate between SDK, devices and validation server
     */
    public String serverUrl;

    /**
     * Information message for QR Code reader that will be shown at top of screen
     */
    public @Nullable String qrCodeMessage;

    /**
     * OAuth token value of current user's session to validate
     */
    public @Nullable String token;

    /**
     * Language code to localize texts
     *
     * 'tr' - Turkish
     * 'en' - English
     */
    public @Nullable String language;

    /**
     * Determines usage of mock location in flow
     *
     * @default false
     */
    public @Nullable Boolean allowMockLocation;

    /**
     * Bluetooth connection timeout in seconds
     *
     * @default 5 seconds
     */
    public @Nullable Integer connectionTimeout;

    /**
     * Auto close timeout for screen after pass completed, nil means stay opened
     */
    public @Nullable Integer autoCloseTimeout;
}
