package com.wenxt.leavemanagement.enums;

public enum CompOffStatus {
    PENDING,    // Requested, waiting for approval
    APPROVED,   // Approved by manager
    EARNED,     // Added to comp-off balance
    USED        // Used in leave application
}
