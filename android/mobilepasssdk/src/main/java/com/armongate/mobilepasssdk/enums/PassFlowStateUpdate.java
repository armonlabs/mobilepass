package com.armongate.mobilepasssdk.enums;

import androidx.annotation.Nullable;

import com.armongate.mobilepasssdk.model.PassFlowResult;

/**
 * Updates to the overall pass flow state
 * Consolidates progress updates and completion
 */
public class PassFlowStateUpdate {
    private final Type type;
    private final Integer state;
    private final String message;
    private final PassFlowResult result;

    public enum Type {
        STATE_CHANGED,  // Progress update
        COMPLETED       // Final result
    }

    private PassFlowStateUpdate(Type type, @Nullable Integer state, @Nullable String message, @Nullable PassFlowResult result) {
        this.type = type;
        this.state = state;
        this.message = message;
        this.result = result;
    }

    public static PassFlowStateUpdate stateChanged(int state, @Nullable String message) {
        return new PassFlowStateUpdate(Type.STATE_CHANGED, state, message, null);
    }

    public static PassFlowStateUpdate completed(PassFlowResult result) {
        return new PassFlowStateUpdate(Type.COMPLETED, null, null, result);
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public Integer getState() {
        return state;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public PassFlowResult getResult() {
        return result;
    }
}

