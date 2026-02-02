package com.wenxt.leavemanagement.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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

@Getter @Setter
@Entity
@Table(name = "emp_leave_application")
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @NotNull
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    private HalfDayType halfDayType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private BigDecimal days;

    @NotNull
    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    @OneToMany(mappedBy = "leaveApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LeaveAttachment> attachments = new ArrayList<>();
}