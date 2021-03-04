package com.armongate.mobilepasssdk.model;

import java.util.ArrayList;
import java.util.List;

public class BLEScanConfiguration {

    public List<String> uuidFilter;
    public String devicePublicKey;
    public String dataUserId;
    public String dataHardwareId;
    public int deviceNumber;
    public int relayNumber;

    public BLEScanConfiguration(List<String> uuidFilter, String devicePublicKey, String userId, String hardwareId, int deviceNumber, int relayNumber) {
        this.uuidFilter         = uuidFilter;
        this.devicePublicKey    = devicePublicKey;
        this.dataUserId         = userId;
        this.dataHardwareId     = hardwareId;
        this.deviceNumber       = deviceNumber;
        this.relayNumber        = relayNumber;
    }

}
