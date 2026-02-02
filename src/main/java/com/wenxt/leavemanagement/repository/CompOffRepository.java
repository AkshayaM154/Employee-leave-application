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

    // 🔒 DUPLICATE CHECK
    boolean existsByEmployeeIdAndWorkedDate(Long employeeId, LocalDate workedDate);

    // 📊 SUM BY STATUS
    @Query("""
        SELECT SUM(c.days)
        FROM CompOff c
        WHERE c.employeeId = :employeeId
          AND c.status = :status
    """)
    BigDecimal sumDaysByEmployeeAndStatus(@Param("employeeId") Long employeeId, @Param("status") CompOffStatus status);

    // 📋 GET EARNED COMPOFFS (FIFO)
    // Renamed to match the Service call for FIFO logic
    List<CompOff> findByEmployeeIdAndStatusOrderByWorkedDateAsc(Long employeeId, CompOffStatus status);

    // 👥 TEAMMATE VIEW: Find all pending requests across the company
    List<CompOff> findByStatus(CompOffStatus status);
}