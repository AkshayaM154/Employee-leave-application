package com.wenxt.leavemanagement.repository;

import com.wenxt.leavemanagement.enums.CompOffStatus;
import com.wenxt.leavemanagement.model.CompOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
    BigDecimal sumDaysByEmployeeAndStatus(Long employeeId, CompOffStatus status);

    // 📋 GET EARNED COMPOFFS (FIFO)
    @Query("""
        SELECT c FROM CompOff c
        WHERE c.employeeId = :employeeId
          AND c.status = 'EARNED'
        ORDER BY c.workedDate ASC
    """)
    List<CompOff> findEarnedCompOffs(Long employeeId);
}
