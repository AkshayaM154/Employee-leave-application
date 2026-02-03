package com.wenxt.leavemanagement.service;

import com.wenxt.leavemanagement.dto.CompOffRequestDTO;
import com.wenxt.leavemanagement.enums.CompOffStatus;
import com.wenxt.leavemanagement.exception.BadRequestException;
import com.wenxt.leavemanagement.model.CompOff;
import com.wenxt.leavemanagement.repository.CompOffRepository;
import com.wenxt.leavemanagement.util.HolidayChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CompOffService {

    private final CompOffRepository compOffRepository;
    private final HolidayChecker holidayChecker;

    public CompOffService(CompOffRepository compOffRepository,
                          HolidayChecker holidayChecker) {
        this.compOffRepository = compOffRepository;
        this.holidayChecker = holidayChecker;
    }

    /**
     * 1️⃣ REQUEST BULK COMPOFF
     * Handles both Admin (Auto-Approved) and Employee (Pending) requests.
     */
    @Transactional
    public void requestBulkCompOff(CompOffRequestDTO request, boolean isAdmin) {
        if (request.getEmployeeId() == null) {
            throw new BadRequestException("Employee ID is required");
        }

        for (CompOffRequestDTO.CompOffEntry entry : request.getEntries()) {
            // Validate that workedDate is a non-working day
            if (!holidayChecker.isNonWorkingDay(entry.getWorkedDate())) {
                throw new BadRequestException("Date " + entry.getWorkedDate() + " is not a holiday/weekend.");
            }

            // Prevent duplicate banking for the same worked date
            if (compOffRepository.existsByEmployeeIdAndWorkedDate(request.getEmployeeId(), entry.getWorkedDate())) {
                throw new BadRequestException("Comp-Off already banked for date: " + entry.getWorkedDate());
            }

            CompOff compOff = new CompOff();
            compOff.setEmployeeId(request.getEmployeeId());
            compOff.setWorkedDate(entry.getWorkedDate());
            compOff.setPlannedLeaveDate(entry.getPlannedLeaveDate());

            // Safety check for days: default to 1 if 0 or null
            BigDecimal daysCount = (entry.getDays() <= 0) ? BigDecimal.ONE : BigDecimal.valueOf(entry.getDays());
            compOff.setDays(daysCount);

            // ✅ LOGIC FIX: Admin entries go straight to EARNED, Employees stay PENDING
            compOff.setStatus(isAdmin ? CompOffStatus.EARNED : CompOffStatus.PENDING);

            compOffRepository.save(compOff);
        }
    }

    /**
     * 2️⃣ APPROVE COMPOFF
     * Manual gatekeeper for employee requests.
     */
    @Transactional
    public void approveCompOff(Long id) {
        CompOff compOff = compOffRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("CompOff record not found"));

        if (compOff.getStatus() != CompOffStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved.");
        }

        compOff.setStatus(CompOffStatus.EARNED);
        compOffRepository.save(compOff);
    }

    /**
     * 3️⃣ CHECK BALANCE (Earned - Used)
     * Note: PENDING records do not count toward available balance.
     */
    public BigDecimal getAvailableCompOffDays(Long employeeId) {
        if (employeeId == null) return BigDecimal.ZERO;

        BigDecimal earned = compOffRepository.sumDaysByEmployeeAndStatus(employeeId, CompOffStatus.EARNED);
        BigDecimal used = compOffRepository.sumDaysByEmployeeAndStatus(employeeId, CompOffStatus.USED);

        earned = (earned != null) ? earned : BigDecimal.ZERO;
        used = (used != null) ? used : BigDecimal.ZERO;

        return earned.subtract(used);
    }

    /**
     * 4️⃣ USE COMPOFF (FIFO Deduction)
     * Deducts from EARNED records and handles splitting if leave is partial.
     */
    @Transactional
    public void useCompOff(Long employeeId, BigDecimal daysToDeduct, Long leaveApplicationId) {
        BigDecimal remaining = daysToDeduct;

        // FIFO: Always use the oldest earned credits first
        List<CompOff> earnedList = compOffRepository.findByEmployeeIdAndStatusOrderByWorkedDateAsc(employeeId, CompOffStatus.EARNED);

        for (CompOff compOff : earnedList) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal available = compOff.getDays();

            if (available.compareTo(remaining) <= 0) {
                // Scenario A: Record is smaller than or equal to needed leave
                compOff.setStatus(CompOffStatus.USED);
                compOff.setUsedLeaveApplicationId(leaveApplicationId);
                remaining = remaining.subtract(available);
                compOffRepository.save(compOff);
            } else {
                // Scenario B: Split logic - Create a new EARNED record for the leftover balance
                CompOff leftover = new CompOff();
                leftover.setEmployeeId(employeeId);
                leftover.setWorkedDate(compOff.getWorkedDate());
                leftover.setPlannedLeaveDate(compOff.getPlannedLeaveDate());
                leftover.setDays(available.subtract(remaining));
                leftover.setStatus(CompOffStatus.EARNED);
                compOffRepository.save(leftover);

                // Mark current record as USED for the 'remaining' amount
                compOff.setDays(remaining);
                compOff.setStatus(CompOffStatus.USED);
                compOff.setUsedLeaveApplicationId(leaveApplicationId);
                compOffRepository.save(compOff);

                remaining = BigDecimal.ZERO;
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Insufficient balance to deduct " + daysToDeduct + " days.");
        }
    }
}