package com.armongate.mobilepasssdk.service;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.armongate.mobilepasssdk.constant.Direction;
import com.armongate.mobilepasssdk.manager.ConfigurationManager;
import com.armongate.mobilepasssdk.model.request.RequestAccess;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class AccessPointService {

    public void remoteOpen(RequestAccess request, final BaseService.ServiceResultListener listener) {
        BaseService.getInstance().requestPost("api/v1/access", request, null, listener);
    }
}
