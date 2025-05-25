package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.constant.ConfigurationDefaults;
import com.armongate.mobilepasssdk.delegate.MobilePassDelegate;

public class Configuration {

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
     * default: false
     */
    public @Nullable Boolean allowMockLocation;

    /**
     * Bluetooth connection timeout in seconds
     *
     * default: 5 seconds
     */
    public @Nullable Integer connectionTimeout;

    /**
     * Auto close timeout for screen after pass completed, nil means stay opened
     */
    public @Nullable Integer autoCloseTimeout;

    /**
     * Flag to decide action for disabled Bluetooth state
     *
     * "true" means wait user to enable Bluetooth
     * "false" means continue to next step
     *
     * default: false
     */
    public @Nullable Boolean waitBLEEnabled;

    /**
     * Close QR code scanner and give information if content is invalid to pass
     *
     * default: false
     */
    public @Nullable Boolean closeWhenInvalidQRCode;

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
        return "MemberId: " + (this.memberId != null ? this.memberId : "Empty")
                + " | Barcode: " + (this.barcode != null ? this.barcode : "Empty")
                + " | WaitBLEEnabled: " + (this.waitBLEEnabled != null ? this.waitBLEEnabled : ConfigurationDefaults.WaitBleEnabled)
                + " | BLEConnectionTimeout: " + (this.connectionTimeout != null ? this.connectionTimeout : ConfigurationDefaults.BLEConnectionTimeout)
                + " | AutoCloseTimeout: " + (this.autoCloseTimeout != null ? this.autoCloseTimeout : "null")
                + " | CloseWhenInvalidQRCode: " + (this.closeWhenInvalidQRCode != null ? this.closeWhenInvalidQRCode : ConfigurationDefaults.CloseWhenInvalidQRCode)
                + " | AllowMockLocation: " + (this.allowMockLocation != null ? this.allowMockLocation : ConfigurationDefaults.AllowMockLocation);
    }
}
