package com.armongate.mobilepasssdk.model;

public class DeviceCapability {

    public boolean support;
    public boolean enabled;
    public boolean needAuthorize;

    public DeviceCapability(boolean support, boolean enabled, boolean needAuthorize) {
        this.support        = support;
        this.enabled        = enabled;
        this.needAuthorize  = needAuthorize;
    }

}
