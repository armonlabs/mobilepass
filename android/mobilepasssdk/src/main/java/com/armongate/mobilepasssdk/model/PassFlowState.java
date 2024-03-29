package com.armongate.mobilepasssdk.model;

public class PassFlowState {

    public Integer  state;
    public String   data;

    public PassFlowState(Integer state, String data) {
        this.state = state;
        this.data = data;
    }

    public Integer getState() {
        return state;
    }

    public String getData() {
        return data;
    }
}

