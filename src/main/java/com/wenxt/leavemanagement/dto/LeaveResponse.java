package com.wenxt.leavemanagement.dto;

import com.wenxt.leavemanagement.model.LeaveApplication;

public class LeaveResponse {
    private LeaveApplication leaveApplication;
    private String warningMessage;

    public LeaveResponse(LeaveApplication leaveApplication, String warningMessage) {
        this.leaveApplication = leaveApplication;
        this.warningMessage = warningMessage;
    }

    // Getters and Setters
    public LeaveApplication getLeaveApplication() { return leaveApplication; }
    public void setLeaveApplication(LeaveApplication leaveApplication) { this.leaveApplication = leaveApplication; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}