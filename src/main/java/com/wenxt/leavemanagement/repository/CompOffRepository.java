package com.wenxt.leavemanagement.repository;

import com.wenxt.leavemanagement.enums.CompOffStatus;
import com.wenxt.leavemanagement.model.CompOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CompOffRepository extends JpaRepository<CompOff, Long> {

    boolean existsByEmployeeIdAndWorkedDate(Long employeeId, LocalDate workedDate);

    @Query("SELECT SUM(c.days) FROM CompOff c WHERE c.employeeId = :employeeId AND c.status = :status")
    BigDecimal sumDaysByEmployeeAndStatus(@Param("employeeId") Long employeeId, @Param("status") CompOffStatus status);

    List<CompOff> findByEmployeeIdAndStatusOrderByWorkedDateAsc(Long employeeId, CompOffStatus status);

    List<CompOff> findByStatus(CompOffStatus status);

    // 🔄 Find the exact Comp-Off records linked to a specific leave application for reversal
    List<CompOff> findByUsedLeaveApplicationId(Long applicationId);
}