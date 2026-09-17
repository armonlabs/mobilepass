package com.armongate.mobilepasssdk.model;

import java.util.List;

public class PassFlowResult {
    public Integer  result;
    public Integer  direction;
    public String   clubId;
    public String   clubName;
    public List<PassFlowState> states;
    public String   message;
    public String   code;

    public PassFlowResult(Integer result, Integer direction, String clubId, String clubName, List<PassFlowState> states, String message, String code) {
        this.result     = result;
        this.direction  = direction;
        this.clubId     = clubId;
        this.clubName   = clubName;
        this.states     = states;
        this.message    = message;
        this.code       = code;
    }
}
