package com.armongate.mobilepasssdk.model;

import androidx.annotation.Nullable;

import java.util.HashMap;

public class BLEDataContent {

    public int                      type;
    public int                      result;
    public HashMap<String, Object>  data;

    public BLEDataContent(int type, int result, @Nullable HashMap<String, Object> data) {
        this.type   = type;
        this.result = result;
        this.data   = data;
    }

}
