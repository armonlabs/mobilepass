package com.armongate.mobilepasssdk.manager;

import com.armongate.mobilepasssdk.model.PassFlowState;

import java.util.ArrayList;
import java.util.List;

public class PassFlowManager {

    // Singleton

    private static PassFlowManager instance = null;
    private PassFlowManager() { }

    public static PassFlowManager getInstance() {
        if (instance == null) {
            instance = new PassFlowManager();
        }

        return instance;
    }

    // Fields

    private List<PassFlowState> states = new ArrayList<>();

    // Public Functions

    public void clearStates() {
        states.clear();
    }

    public List<PassFlowState> getStates() {
        return this.states;
    }

    public void addToStates(Integer state) {
        this.states.add(new PassFlowState(state, null));
    }

    public void addToStates(Integer state, String data) {
        this.states.add(new PassFlowState(state, data));
    }

}
