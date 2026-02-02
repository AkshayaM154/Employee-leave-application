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
import java.time.LocalDate;
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

    // =====================================================
    // 1️⃣ EARN BULK COMPOFF (WORKED ON MULTIPLE HOLIDAYS)
    // =====================================================
    @Transactional
    public void earnBulkCompOff(CompOffRequestDTO request) {
        if (request.getEmployeeId() == null) {
            throw new BadRequestException("Employee ID is required");
        }

        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException("No worked dates provided");
        }

        for (CompOffRequestDTO.CompOffEntry entry : request.getEntries()) {
            LocalDate workedDate = entry.getWorkedDate();
            BigDecimal days = BigDecimal.valueOf(entry.getDays());

            // ✅ VALIDATE: MUST BE HOLIDAY / WEEKEND
            if (!holidayChecker.isNonWorkingDay(workedDate)) {
                throw new BadRequestException(
                        "Date " + workedDate + " is not a holiday or weekend. CompOff denied."
                );
            }

            // 🚫 VALIDATE: PREVENT DUPLICATE EARN FOR SAME DAY
            boolean alreadyEarned =
                    compOffRepository.existsByEmployeeIdAndWorkedDate(request.getEmployeeId(), workedDate);

            if (alreadyEarned) {
                throw new BadRequestException(
                        "CompOff already earned for date: " + workedDate
                );
            }

            // 💾 SAVE RECORD
            CompOff compOff = new CompOff();
            compOff.setEmployeeId(request.getEmployeeId());
            compOff.setWorkedDate(workedDate);
            compOff.setDays(days);
            compOff.setStatus(CompOffStatus.EARNED);

            compOffRepository.save(compOff);
        }
    }

    // =====================================================
    // 2️⃣ CHECK AVAILABLE COMPOFF BALANCE
    // =====================================================
    public BigDecimal getAvailableCompOffDays(Long employeeId) {
        if (employeeId == null) {
            throw new BadRequestException("Employee ID is required");
        }

        BigDecimal earned = compOffRepository.sumDaysByEmployeeAndStatus(
                employeeId, CompOffStatus.EARNED
        );

        BigDecimal used = compOffRepository.sumDaysByEmployeeAndStatus(
                employeeId, CompOffStatus.USED
        );

        earned = (earned != null) ? earned : BigDecimal.ZERO;
        used = (used != null) ? used : BigDecimal.ZERO;

        return earned.subtract(used);
    }

    // =====================================================
    // 3️⃣ USE COMPOFF (WHEN APPLYING LEAVE) - FIFO SPLIT LOGIC
    // =====================================================
    @Transactional
    public void useCompOff(Long employeeId,
                           BigDecimal daysToDeduct,
                           Long leaveApplicationId) {

        if (employeeId == null || daysToDeduct == null || leaveApplicationId == null) {
            throw new BadRequestException("Invalid CompOff usage request");
        }

        BigDecimal remaining = daysToDeduct;

        // Fetch earned records in FIFO (Oldest Worked Dates first)
        List<CompOff> earnedList = compOffRepository.findEarnedCompOffs(employeeId);

        for (CompOff compOff : earnedList) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal availableInRecord = compOff.getDays();

            if (availableInRecord.compareTo(remaining) <= 0) {
                // CASE A: Full record consumed
                compOff.setStatus(CompOffStatus.USED);
                compOff.setUsedLeaveApplicationId(leaveApplicationId);
                remaining = remaining.subtract(availableInRecord);
                compOffRepository.save(compOff);
            } else {
                // CASE B: Partial record consumed (Split logic)
                // 1. Create leftover record
                CompOff leftover = new CompOff();
                leftover.setEmployeeId(employeeId);
                leftover.setWorkedDate(compOff.getWorkedDate());
                leftover.setDays(availableInRecord.subtract(remaining));
                leftover.setStatus(CompOffStatus.EARNED);
                compOffRepository.save(leftover);

                // 2. Consume required amount from current record
                compOff.setDays(remaining);
                compOff.setStatus(CompOffStatus.USED);
                compOff.setUsedLeaveApplicationId(leaveApplicationId);
                compOffRepository.save(compOff);

                remaining = BigDecimal.ZERO;
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Insufficient balance. Missing: " + remaining + " days.");
        }
    }
}