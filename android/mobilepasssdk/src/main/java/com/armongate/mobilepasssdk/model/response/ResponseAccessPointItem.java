package com.armongate.mobilepasssdk.model.response;

public class ResponseAccessPointItem {
    public String id;
    public String name;
    public ResponseAccessPointItemQRCodeItem[]  qrCodeData;
    public ResponseAccessPointItemGeoLocation   geoLocation;
    public ResponseAccessPointItemDeviceInfo[]  deviceInfo;
}
