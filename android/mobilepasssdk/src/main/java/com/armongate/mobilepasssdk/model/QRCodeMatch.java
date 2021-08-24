package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;

public class QRCodeMatch {
    public String                           accessPointId;
    public ResponseAccessPointListQRCode    qrCode;

    public QRCodeMatch(String accessPointId, ResponseAccessPointListQRCode qrCode) {
        this.accessPointId = accessPointId;
        this.qrCode = qrCode;
    }
}
