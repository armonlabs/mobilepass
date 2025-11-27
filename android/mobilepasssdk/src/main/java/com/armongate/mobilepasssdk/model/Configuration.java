package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.constant.ConfigurationDefaults;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;

public class Configuration {

    /**
     * API key for SDK authentication
     * Each authorized application receives a unique key
     */
    public String apiKey;

    /**
     * Member id that will be used for validation to pass
     */
    public String memberId;

    /**
     * Barcode id that received with Benefits System
     */
    public @Nullable String barcode;

    /**
     * URL of server that communicate between SDK, devices and validation server
     */
    public String serverUrl;

    /**
     * Language code to localize texts
     *
     * 'tr' - Turkish
     * 'en' - English
     */
    public @Nullable String language;

    /**
     * Bluetooth connection timeout in seconds
     *
     * default: 5 seconds
     */
    public @Nullable Integer connectionTimeout;

    /**
     * Location verification timeout in seconds (for remote access with location requirement)
     *
     * default: 30 seconds
     */
    public @Nullable Integer locationVerificationTimeout;

    /**
     * Flag to decide action when BLE is unavailable (disabled, missing permissions, etc.)
     *
     * "true" means continue to next action (e.g., remote access) if BLE requirements not met
     * "false" means wait for user to satisfy BLE requirements
     *
     * default: false
     */
    public @Nullable Boolean continueWithoutBLE;

    /**
     * Minimum level to be informed about logs
     *
     * default: LogLevel.INFO (2)
     */
    public @Nullable Integer logLevel;

    /**
     * Optional listener instance for MobilePass SDK callbacks
     */
    public @Nullable MobilePassDelegate listener;


    public String getLog() {
        return "ApiKey: " + (this.apiKey != null ? this.apiKey.substring(0, Math.min(8, this.apiKey.length())) + "..." : "Empty")
                + " | MemberId: " + (this.memberId != null ? this.memberId : "Empty")
                + " | Barcode: " + (this.barcode != null ? this.barcode : "Empty")
                + " | ContinueWithoutBLE: " + (this.continueWithoutBLE != null ? this.continueWithoutBLE : ConfigurationDefaults.ContinueWithoutBLE)
                + " | BLEConnectionTimeout: " + (this.connectionTimeout != null ? this.connectionTimeout : ConfigurationDefaults.BLEConnectionTimeout);
    }
}
