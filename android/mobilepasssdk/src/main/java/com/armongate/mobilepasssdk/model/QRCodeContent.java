package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListGeoLocation;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListTerminal;

public class QRCodeContent {
    public ResponseAccessPointListTerminal[]  terminals;
    public ResponseAccessPointListQRCode qrCode;
    public ResponseAccessPointListGeoLocation geoLocation;

    public QRCodeContent(ResponseAccessPointListQRCode qrCode, ResponseAccessPointListTerminal[] terminals, ResponseAccessPointListGeoLocation geoLocation) {
        this.terminals      = terminals;
        this.qrCode         = qrCode;
        this.geoLocation    = geoLocation;
    }
}
