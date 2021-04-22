package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItem;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItemDeviceInfo;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItemQRCodeItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BLEScanConfiguration {

    public Map<String, DeviceConnectionInfo> deviceList;
    public String dataUserId;
    public int direction;
    public int deviceNumber;
    public int relayNumber;

    public BLEScanConfiguration(List<ResponseAccessPointItemDeviceInfo> devices, String userId, int deviceNumber, int direction, int relayNumber) {
        this.dataUserId     = userId;
        this.direction      = direction;
        this.deviceNumber   = deviceNumber;
        this.relayNumber    = relayNumber;

        this.deviceList = new HashMap<>();

        for (ResponseAccessPointItemDeviceInfo device : devices) {
            this.deviceList.put(device.id, new DeviceConnectionInfo(device.id, device.publicKey, device.hardwareId));
        }
    }

}
