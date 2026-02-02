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

    private Long employeeId;

    // The holiday / weekend worked
    private LocalDate workedDate;

    // Optional – employee may plan leave date
    private LocalDate plannedLeaveDate;

    @Enumerated(EnumType.STRING)
    private CompOffStatus status;

    // Usually 1 or 0.5
    private BigDecimal days;

    // Set when comp-off is consumed
    private Long usedLeaveApplicationId;
}
