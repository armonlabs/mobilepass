package com.armongate.mobilepasssdk.model.response;

import androidx.annotation.Nullable;

public class ResponseAccessPointListItem {
    /** Id */
    public @Nullable String i;
    /** Name */
    public @Nullable String n;
    /** QR Codes */
    public @Nullable ResponseAccessPointListQRCode[]    q;
    /** Terminals */
    public @Nullable ResponseAccessPointListTerminal[]  t;
    /** GeoLocation */
    public @Nullable ResponseAccessPointListGeoLocation g;

    /** QR Code Ids | Only for mapping on storage */
    public String[] d;
}
