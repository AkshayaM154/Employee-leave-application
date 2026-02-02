package com.wenxt.leavemanagement.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.wenxt.leavemanagement.enums.CompOffType;
import com.wenxt.leavemanagement.enums.HalfDayType;
import com.wenxt.leavemanagement.enums.LeaveStatus;
import com.wenxt.leavemanagement.enums.LeaveType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "emp_leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @NotNull
    private Integer employeeId;

    @Enumerated(EnumType.STRING)
    private HalfDayType halfDayType;

    @Enumerated(EnumType.STRING)
    private CompOffType compOffType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type")
    private LeaveType leaveType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal days;

    @NotNull
    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    // Add this to your existing LeaveApplication class
    @Column(name = "holiday_work_date")
    private LocalDate holidayWorkDate;

    // Duplicate halfDayType removed from here to fix compilation error

    @OneToMany(mappedBy = "leaveApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LeaveAttachment> attachments = new ArrayList<>();

    public void addAttachment(LeaveAttachment attachment) {
        if (attachments == null) attachments = new ArrayList<>();
        attachments.add(attachment);
        attachment.setLeaveApplication(this);
    }


}