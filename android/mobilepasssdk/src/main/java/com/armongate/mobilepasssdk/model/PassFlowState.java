package com.armongate.mobilepasssdk.model;

import java.util.Date;

public class PassFlowState {

    public Integer  state;
    public String   data;
    public Date datetime;

    public PassFlowState(Integer state, String data) {
        this(state, data, new Date());
    }

    public PassFlowState(Integer state, String data, Date datetime) {
        this.state = state;
        this.data = data;
        this.datetime = datetime;
    }

    public Integer getState() {
        return state;
    }

    public String getData() {
        return data;
    }

    public Date getDatetime() {
        return datetime;
    }
}

