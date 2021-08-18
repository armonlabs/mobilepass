package com.armongate.mobilepasssdk.model;

import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListGeoLocation;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListQRCode;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointListTerminal;

public class QRCodeContent {
    public String                               accessPointId;
    public ResponseAccessPointListTerminal[]    terminals;
    public ResponseAccessPointListQRCode        qrCode;
    public ResponseAccessPointListGeoLocation   geoLocation;

    public QRCodeContent(String accessPointId, ResponseAccessPointListQRCode qrCode, ResponseAccessPointListTerminal[] terminals, ResponseAccessPointListGeoLocation geoLocation) {
        this.accessPointId  = accessPointId;
        this.terminals      = terminals;
        this.qrCode         = qrCode;
        this.geoLocation    = geoLocation;
    }
}
