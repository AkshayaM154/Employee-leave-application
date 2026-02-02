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

    // Put your Mac's IP address here
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

        BigDecimal calculatedDays = BigDecimal.ZERO;
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate = leave.getEndDate();

        // 2. Dynamic Calculation Logic
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            if (!holidayChecker.isNonWorkingDay(date)) {

                BigDecimal dailyIncrement = BigDecimal.ONE;

                // Rule A: If the leaveType itself is "HALF_DAY", every day in the range is 0.5
                if (leave.getLeaveType() == LeaveType.HALF_DAY) {
                    dailyIncrement = new BigDecimal("0.5");
                }
                // Rule B: If it's a multi-day leave (e.g., CASUAL) but halfDayType is set,
                // we treat ONLY the last day as 0.5.
                else if (leave.getHalfDayType() != null && date.equals(endDate)) {
                    dailyIncrement = new BigDecimal("0.5");
                }

                calculatedDays = calculatedDays.add(dailyIncrement);
            }
            date = date.plusDays(1);
        }

        // 3. Ensure we have at least some time requested
        if (calculatedDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("The selected dates are non-working days");
        }

        // 4. CompOff Balance Check (Uses the new calculated total)
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId().longValue());
            if (available.compareTo(calculatedDays) < 0) {
                throw new BadRequestException("Insufficient CompOff. Available: " + available + ", Required: " + calculatedDays);
            }
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);

        // 5. File URL Conversion
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(attachment -> {
                String fileName = attachment.getFileUrl();
                String webUrl = "http://" + SERVER_IP + ":" + SERVER_PORT + "/uploads/leaves/" + fileName;
                attachment.setFileUrl(webUrl);
                attachment.setLeaveApplication(leave);
            });
        }

        // 6. Save Application
        LeaveApplication savedLeave = repository.save(leave);

        // 7. Deduct from CompOff records if leaveType is COMP_OFF
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            compOffService.useCompOff(leave.getEmployeeId().longValue(), calculatedDays, savedLeave.getApplicationId());
        }

        return savedLeave;
    }
}