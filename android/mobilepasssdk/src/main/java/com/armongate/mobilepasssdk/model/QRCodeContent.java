package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItem;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointItemQRCodeItem;

import java.util.ArrayList;

public class QRCodeContent {
    public String code;
    public ResponseAccessPointItem accessPoint;
    public ResponseAccessPointItemQRCodeItem action;

    public QRCodeContent(String code, ResponseAccessPointItem accessPoint, ResponseAccessPointItemQRCodeItem action) {
        this.code           = code;
        this.accessPoint    = accessPoint;
        this.action         = action;

        this.accessPoint.qrCodeData = new ResponseAccessPointItemQRCodeItem[0];
    }
}
