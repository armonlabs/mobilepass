package com.armongate.mobilepasssdk.service;

import com.armongate.mobilepasssdk.model.request.RequestPagination;
import com.armongate.mobilepasssdk.model.request.RequestSetUserData;
import com.armongate.mobilepasssdk.model.response.ResponseAccessPointList;

public class DataService {

    public void sendUserInfo(RequestSetUserData request, final BaseService.ServiceResultListener listener) {
        BaseService.getInstance().requestPost("api/v1/setpublickey", request, null, listener);
    }

    public void getAccessList(RequestPagination pagination, Long syncDate, final BaseService.ServiceResultListener<ResponseAccessPointList> listener) {
        String url = "api/v2/listAccessPointsRequest?t=" + pagination.t + "&s=" + pagination.s;

        if (syncDate != null) {
            url = url + "&d=" + syncDate.toString();
        }

        BaseService.getInstance().requestGet(url, ResponseAccessPointList.class, listener);
    }
}
