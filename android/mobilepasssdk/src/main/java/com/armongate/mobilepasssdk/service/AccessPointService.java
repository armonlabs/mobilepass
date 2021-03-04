package com.armongate.mobilepasssdk.service;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.armongate.mobilepasssdk.constant.Direction;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class AccessPointService {

    public void remoteOpen(String accessPointId, int direction, final BaseService.ServiceResultListener listener) {
        BaseService.getInstance().requestGet("api/v1/access?accessPointId=" + accessPointId + "&direction=" + direction, null, listener);
    }
}
