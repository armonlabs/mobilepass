package com.armongate.mobilepasssdk.model.response;

public class ResponseAccessPointListItem {
    /** Id */
    public String i;
    /** Name */
    public String n;
    /** QR Codes */
    public ResponseAccessPointListQRCode[]    q;
    /** Terminals */
    public ResponseAccessPointListTerminal[]  t;
    /** GeoLocation */
    public ResponseAccessPointListGeoLocation g;

    /** QR Code Ids | Only for mapping on storage */
    public String[] d;
}
