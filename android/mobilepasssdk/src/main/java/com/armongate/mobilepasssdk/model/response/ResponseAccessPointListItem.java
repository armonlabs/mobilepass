package com.armongate.mobilepasssdk.model.response;

public class ResponseAccessPointListItem {
    /** Name */
    public String n;
    /** QR Codes */
    public ResponseAccessPointListQRCode[]    q;
    /** Terminals */
    public ResponseAccessPointListTerminal[]  t;
    /** GeoLocation */
    public ResponseAccessPointListGeoLocation g;
}
