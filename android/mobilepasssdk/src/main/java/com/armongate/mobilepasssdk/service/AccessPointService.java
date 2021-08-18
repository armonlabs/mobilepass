package com.armongate.mobilepasssdk.service;

import com.armongate.mobilepasssdk.model.request.RequestAccess;

public class AccessPointService {

    public void remoteOpen(RequestAccess request, final BaseService.ServiceResultListener listener) {
        BaseService.getInstance().requestPost("api/v2/access", request, null, listener);
    }
}
