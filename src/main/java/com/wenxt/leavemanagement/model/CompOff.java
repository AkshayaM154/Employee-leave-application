package com.wenxt.leavemanagement.model;

import com.wenxt.leavemanagement.enums.CompOffStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "comp_off")
public class CompOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    // The holiday/weekend the employee actually worked
    @Column(nullable = false)
    private LocalDate workedDate;

    /**
     * 1) Requesting for comp-off knowing wanted leave days
     * This field captures that "wanted" date.
     * Even if filled, status must remain PENDING until teammates approve.
     */
    // ✅ ADDED EXPLICIT COLUMN MAPPING
    @Column(name = "planned_leave_date")
    private LocalDate plannedLeaveDate;

    /**
     * Workflow Status:
     * - PENDING: Initial state for all requests.
     * - EARNED: Approved by teammates (Balance added).
     * - USED: Linked to a LeaveApplication and consumed.
     * - REJECTED: Denied by teammates.
     */
    @Enumerated(EnumType.STRING)
    private CompOffStatus status = CompOffStatus.PENDING;

    // The credit amount (1.0 for full day, 0.5 for half day)
    @Column(precision = 3, scale = 1)
    private BigDecimal days;

    /**
     * When the employee applies via the Leave Application dropdown,
     * this ID links this credit to that specific leave record.
     */
    // ✅ ADDED EXPLICIT COLUMN MAPPING
    @Column(name = "used_leave_application_id")
    private Long usedLeaveApplicationId;

    // Optional: Reason for working on a holiday
    private String description;
}