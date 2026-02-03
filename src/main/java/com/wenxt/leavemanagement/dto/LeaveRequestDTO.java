package com.wenxt.leavemanagement.dto;

import java.time.LocalDate;

public class LeaveRequestDTO {

    private Long employeeId;
    private String leaveType;
    private LocalDate startDate; // Changed from fromDate to match controller
    private LocalDate endDate;   // Changed from toDate to match controller
    private String reason;
    private String halfDayType;
    private boolean confirmLossOfPay;

    // Getters and Setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; } // 👈 The symbol compiler was looking for
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getHalfDayType() { return halfDayType; }
    public void setHalfDayType(String halfDayType) { this.halfDayType = halfDayType; }

    public boolean isConfirmLossOfPay() { return confirmLossOfPay; }
    public void setConfirmLossOfPay(boolean confirmLossOfPay) { this.confirmLossOfPay = confirmLossOfPay; }
}