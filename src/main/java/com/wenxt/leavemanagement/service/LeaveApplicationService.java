package com.wenxt.leavemanagement.service;

import com.wenxt.leavemanagement.enums.LeaveStatus;
import com.wenxt.leavemanagement.enums.LeaveType;
import com.wenxt.leavemanagement.exception.BadRequestException;
import com.wenxt.leavemanagement.model.LeaveApplication;
import com.wenxt.leavemanagement.repository.LeaveApplicationRepository;
import com.wenxt.leavemanagement.util.HolidayChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository repository;
    private final HolidayChecker holidayChecker;
    private final CompOffService compOffService;

    // Server Configuration
    private final String SERVER_IP = "192.168.1.15";
    private final String SERVER_PORT = "8081";

    public LeaveApplicationService(LeaveApplicationRepository repository,
                                   HolidayChecker holidayChecker,
                                   CompOffService compOffService) {
        this.repository = repository;
        this.holidayChecker = holidayChecker;
        this.compOffService = compOffService;
    }

    @Transactional
    public LeaveApplication applyLeave(LeaveApplication leave) {

        // 1. Basic Date Validation
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        // 2. PRE-CHECK: Preliminary Database Balance Check
        // We check if the employee has ANY balance before running calculation loops
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal initialAvailable = compOffService.getAvailableCompOffDays(leave.getEmployeeId().longValue());
            if (initialAvailable.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("No Comp-Off balance available in the system.");
            }
        }

        // 3. Dynamic Calculation Logic
        BigDecimal calculatedDays = calculateLeaveDuration(leave);

        // 4. Final Validation against Calculated Days
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal finalAvailable = compOffService.getAvailableCompOffDays(leave.getEmployeeId().longValue());
            if (finalAvailable.compareTo(calculatedDays) < 0) {
                throw new BadRequestException("Insufficient Comp-Off. Available: " + finalAvailable + ", Required: " + calculatedDays);
            }
        }

        // 5. Prepare Leave Application for Saving
        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);

        // 6. File URL Conversion
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(attachment -> {
                String fileName = attachment.getFileUrl();
                String webUrl = "http://" + SERVER_IP + ":" + SERVER_PORT + "/uploads/leaves/" + fileName;
                attachment.setFileUrl(webUrl);
                attachment.setLeaveApplication(leave);
            });
        }

        // 7. Save Application
        // Saving first gives us the Application ID needed for the deduction link
        LeaveApplication savedLeave = repository.save(leave);

        // 8. Final Deduction (Only for Comp-Off)
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            compOffService.useCompOff(leave.getEmployeeId().longValue(), calculatedDays, savedLeave.getApplicationId());
        }

        return savedLeave;
    }

    /**
     * Helper method to calculate duration based on working days and half-day rules.
     */
    private BigDecimal calculateLeaveDuration(LeaveApplication leave) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate = leave.getEndDate();

        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            if (!holidayChecker.isNonWorkingDay(date)) {

                BigDecimal dailyIncrement = BigDecimal.ONE;

                // Rule A: Every day in range is 0.5 if LeaveType is HALF_DAY
                if (leave.getLeaveType() == LeaveType.HALF_DAY) {
                    dailyIncrement = new BigDecimal("0.5");
                }
                // Rule B: Only the last day is 0.5 if halfDayType is set (for CASUAL/SICK etc)
                else if (leave.getHalfDayType() != null && date.equals(endDate)) {
                    dailyIncrement = new BigDecimal("0.5");
                }

                total = total.add(dailyIncrement);
            }
            date = date.plusDays(1);
        }

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("The selected dates are non-working days (weekends/holidays)");
        }

        return total;
    }
}