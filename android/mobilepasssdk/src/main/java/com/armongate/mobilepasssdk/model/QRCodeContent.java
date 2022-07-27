package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.constant.LogCodes;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListClubInfo;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListGeoLocation;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QRCodeContent {
    public @Nullable String                             accessPointId;
    public @Nullable ResponseAccessPointListTerminal[]  terminals;
    public @Nullable ResponseAccessPointListQRCode      qrCode;
    public @Nullable ResponseAccessPointListGeoLocation geoLocation;
    public @Nullable ResponseAccessPointListClubInfo    clubInfo;
    public Boolean                                      valid;

    public QRCodeContent(@Nullable String accessPointId, @Nullable ResponseAccessPointListQRCode qrCode, @Nullable ResponseAccessPointListTerminal[] terminals, @Nullable ResponseAccessPointListGeoLocation geoLocation, @Nullable ResponseAccessPointListClubInfo clubInfo) {
        this.accessPointId  = accessPointId;
        this.terminals      = terminals;
        this.qrCode         = qrCode;
        this.geoLocation    = geoLocation;
        this.clubInfo       = clubInfo;

        this.valid = validateQRCodeContent();
    }

    private Boolean validateQRCodeContent() {
        if (this.qrCode == null) {
            LogManager.getInstance().warn("QR code content has missing configuration details", LogCodes.PASSFLOW_QRCODE_VALIDATION_CONFIG);
            return false;
        }

        if (this.qrCode.q == null || this.qrCode.q.isEmpty()) {
            LogManager.getInstance().warn("QR code content has invalid data", LogCodes.PASSFLOW_QRCODE_VALIDATION_DATA);
            return false;
        }

        if (this.qrCode.i == null || this.qrCode.i.isEmpty()) {
            LogManager.getInstance().warn("[" + this.qrCode.q + "]  QR code content has missing id value", LogCodes.PASSFLOW_QRCODE_VALIDATION_ID);
            return false;
        }

        if (this.qrCode.d == null || this.qrCode.r == null || this.qrCode.h == null || this.qrCode.h.isEmpty()) {
            LogManager.getInstance().warn("[" + this.qrCode.q + "] QR code content has missing connection detail in direction, relay number or hardware id", LogCodes.PASSFLOW_QRCODE_VALIDATION_DOOR_DETAILS);
            return false;
        }

        if (this.qrCode.t == null || this.qrCode.t < 1 || this.qrCode.t > 4) {
            LogManager.getInstance().warn("[" + this.qrCode.q + "] QR code content has invalid trigger type", LogCodes.PASSFLOW_QRCODE_VALIDATION_TRIGGERTYPE);
            return false;
        }

        if (this.qrCode.v != null && this.qrCode.v && this.geoLocation != null && (this.geoLocation.la == null || this.geoLocation.lo == null || this.geoLocation.r == null)) {
            LogManager.getInstance().warn("[" + this.qrCode.q + "] QR code content has invalid location details", LogCodes.PASSFLOW_QRCODE_VALIDATION_LOCATION);
            return false;
        }

        List<ResponseAccessPointListTerminal> terminals = this.terminals != null ? new ArrayList<>(Arrays.asList(this.terminals)) : new ArrayList<ResponseAccessPointListTerminal>();
        boolean hasInvalidTerminal = false;
        for (ResponseAccessPointListTerminal terminal :
                terminals) {
            if (terminal.i == null || terminal.i.isEmpty() || terminal.p == null || terminal.p.isEmpty()) {
                hasInvalidTerminal = true;
                break;
            }
        }

        if (hasInvalidTerminal) {
            LogManager.getInstance().warn("[" + this.qrCode.q + "] QR code content has invalid terminal details", LogCodes.PASSFLOW_QRCODE_VALIDATION_TERMINAL);
            return false;
        }

        if (this.accessPointId == null || this.accessPointId.isEmpty()) {
            LogManager.getInstance().warn("[" + this.qrCode.q + "] QR code content has invalid access point matching", LogCodes.PASSFLOW_QRCODE_VALIDATION_ACCESSPOINT_ID);
            return false;
        }

        return true;
    }
}
