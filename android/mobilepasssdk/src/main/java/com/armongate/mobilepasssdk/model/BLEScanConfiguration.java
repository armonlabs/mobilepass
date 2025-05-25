package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.enums.Language;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListTerminal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BLEScanConfiguration {

    public Map<String, DeviceConnectionInfo> deviceList;
    public String dataUserId;
    public String dataUserBarcode;
    public String qrCodeId;
    public String hardwareId;
    public int direction;
    public int deviceNumber;
    public int relayNumber;
    public Language language;

    public BLEScanConfiguration(List<ResponseAccessPointListTerminal> devices, String userId, String userBarcode, String qrCodeId, String hardwareId, int direction, int relayNumber, Language language) {
        this.dataUserId         = userId;
        this.dataUserBarcode    = userBarcode;
        this.qrCodeId           = qrCodeId;
        this.hardwareId         = hardwareId;
        this.direction          = direction;
        this.deviceNumber       = 0; // Default value
        this.relayNumber        = relayNumber;
        this.language           = language;

        this.deviceList = new HashMap<>();

        for (ResponseAccessPointListTerminal device : devices) {
            if (device.i != null && !device.i.isEmpty() && device.p != null && !device.p.isEmpty()) {
                this.deviceList.put(device.i.toLowerCase(), new DeviceConnectionInfo(device.i, device.p));
            }
        }
    }

}
