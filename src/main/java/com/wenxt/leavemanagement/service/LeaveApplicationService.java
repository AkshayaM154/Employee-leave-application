package com.wenxt.leavemanagement.service;

import com.wenxt.leavemanagement.enums.LeaveStatus;
import com.wenxt.leavemanagement.enums.LeaveType;
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

    // Put your Mac's IP address here
    private final String SERVER_IP = "192.168.1.15";
    private final String SERVER_PORT = "8081";

    public LeaveApplicationService(LeaveApplicationRepository repository, HolidayChecker holidayChecker) {
        this.repository = repository;
        this.holidayChecker = holidayChecker;
    }

    @Transactional
    public LeaveApplication applyLeave(LeaveApplication leave) {
        // 1. Validation: End date must not be before start date
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        BigDecimal calculatedDays = BigDecimal.ZERO;

        // 2. Logic Update: Determine the value of each day based on LeaveType
        BigDecimal dayIncrement = (leave.getLeaveType() == LeaveType.HALF_DAY)
                ? new BigDecimal("0.5")
                : BigDecimal.ONE;

        // 3. Iterate through dates to calculate total working days
        LocalDate date = leave.getStartDate();
        while (!date.isAfter(leave.getEndDate())) {
            if (!holidayChecker.isNonWorkingDay(date)) {
                calculatedDays = calculatedDays.add(dayIncrement);
            }
            date = date.plusDays(1);
        }

        // 4. Validation: Ensure at least one working day is selected
        if (calculatedDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("The selected dates are non-working days.");
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);

        // --- NEW LOGIC START: Convert filenames to Web URLs ---
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(attachment -> {
                String fileName = attachment.getFileUrl();
                // Construct the public URL for the file
                String webUrl = "http://" + SERVER_IP + ":" + SERVER_PORT + "/" + fileName;
                attachment.setFileUrl(webUrl);
                attachment.setLeaveApplication(leave);
            });
        }
        // --- NEW LOGIC END ---

        return repository.save(leave);
    }
}