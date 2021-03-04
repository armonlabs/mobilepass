package com.armongate.mobilepasssdk.service;

import com.armongate.mobilepasssdk.model.request.RequestPagination;
import com.armongate.mobilepasssdk.model.request.RequestSetUserData;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointList;
import com.google.gson.Gson;

public class DataService {

    public void sendUserInfo(RequestSetUserData request, final BaseService.ServiceResultListener listener) {
        BaseService.getInstance().requestPost("api/v1/setpublickey", request, null, listener);
    }

    public void getAccessList(RequestPagination request, final BaseService.ServiceResultListener<ResponseAccessPointList> listener) {
        BaseService.getInstance().requestGet("api/v1/listAccessPointsRequest?take=" + request.take + "&skip=" + request.skip, ResponseAccessPointList.class, listener);
    }
}
