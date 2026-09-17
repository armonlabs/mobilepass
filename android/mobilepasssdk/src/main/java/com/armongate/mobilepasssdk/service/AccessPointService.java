package com.armongate.mobilepasssdk.service;

import com.armongate.mobilepasssdk.model.request.RequestAccess;
import com.armongate.mobilepasssdk.model.response.ResponseMessage;

public class AccessPointService {

    /**
     * Request a remote door open.
     *
     * Response is parsed as {@link ResponseMessage} for both success and failure
     * so that the result code is available in either case: on success it arrives
     * in the response body, on failure in the resultCode parameter of
     * {@link BaseService.ServiceResultListener#onError(int, String, String)}.
     */
    public void remoteOpen(RequestAccess request, final BaseService.ServiceResultListener<ResponseMessage> listener) {
        BaseService.getInstance().requestPost("api/v2/access", request, ResponseMessage.class, listener);
    }
}
