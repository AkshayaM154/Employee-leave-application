package com.wenxt.leavemanagement.service;

import com.wenxt.leavemanagement.dto.LeaveResponse;
import com.wenxt.leavemanagement.enums.LeaveStatus;
import com.wenxt.leavemanagement.enums.LeaveType;
import com.wenxt.leavemanagement.enums.CompOffStatus;
import com.wenxt.leavemanagement.exception.BadRequestException;
import com.wenxt.leavemanagement.model.LeaveApplication;
import com.wenxt.leavemanagement.model.CompOff;
import com.wenxt.leavemanagement.repository.LeaveApplicationRepository;
import com.wenxt.leavemanagement.repository.CompOffRepository;
import com.wenxt.leavemanagement.util.HolidayChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveApplicationService {

    private final LeaveApplicationRepository repository;
    private final HolidayChecker holidayChecker;
    private final CompOffService compOffService;
    private final CompOffRepository compOffRepository;

    private final String SERVER_IP = "192.168.1.15";
    private final String SERVER_PORT = "8081";

    public LeaveApplicationService(LeaveApplicationRepository repository,
                                   HolidayChecker holidayChecker,
                                   CompOffService compOffService,
                                   CompOffRepository compOffRepository) {
        this.repository = repository;
        this.holidayChecker = holidayChecker;
        this.compOffService = compOffService;
        this.compOffRepository = compOffRepository;
    }

    // --- 🟢 EMPLOYEE APPLY LEAVE ---
    @Transactional
    public LeaveResponse applyLeave(LeaveApplication leave, boolean isConfirmed) {
        validateDates(leave);
        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);

        if (warning != null && !isConfirmed) {
            return new LeaveResponse(null, warning);
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.PENDING);
        processAttachments(leave);

        LeaveApplication savedLeave = repository.save(leave);

        if (leave.getLeaveType() == LeaveType.COMP_OFF && warning == null) {
            compOffService.useCompOff(leave.getEmployeeId().longValue(), calculatedDays, savedLeave.getApplicationId());
        }

        return new LeaveResponse(savedLeave, null);
    }

    // --- 🔴 ADMIN APPLY LEAVE ---
    @Transactional
    public LeaveResponse applyAdminLeave(LeaveApplication leave, boolean isConfirmed) {
        validateDates(leave);
        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);

        if (warning != null && !isConfirmed) {
            return new LeaveResponse(null, warning);
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.APPROVED);
        processAttachments(leave);

        LeaveApplication savedLeave = repository.save(leave);

        if (leave.getLeaveType() == LeaveType.COMP_OFF && warning == null) {
            compOffService.useCompOff(leave.getEmployeeId().longValue(), calculatedDays, savedLeave.getApplicationId());
        }

        return new LeaveResponse(savedLeave, null);
    }

    // --- 🛠️ CANCELLATION LOGIC ---

    @Transactional
    public void cancelAdminLeave(Long applicationId) {
        LeaveApplication leave = repository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));
        performCancellation(leave);
    }

    @Transactional
    public void cancelEmployeeLeave(Long applicationId, Long employeeId) {
        LeaveApplication leave = repository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));

        if (!leave.getEmployeeId().equals(employeeId.intValue())) {
            throw new BadRequestException("Unauthorized: You cannot cancel another employee's leave.");
        }

        if (leave.getStatus() == LeaveStatus.REJECTED || leave.getStatus() == LeaveStatus.CANCELLED) {
            throw new BadRequestException("Leave is already finalized as " + leave.getStatus());
        }
        performCancellation(leave);
    }

    private void performCancellation(LeaveApplication leave) {
        // 🔄 REVERSAL: Restore Comp-Off credits if they were deducted for APPROVED or PENDING leaves
        if (leave.getLeaveType() == LeaveType.COMP_OFF &&
                (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.PENDING)) {

            List<CompOff> linkedCredits = compOffRepository.findByUsedLeaveApplicationId(leave.getApplicationId());
            for (CompOff credit : linkedCredits) {
                credit.setStatus(CompOffStatus.EARNED);
                credit.setUsedLeaveApplicationId(null);
                compOffRepository.save(credit);
            }
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        repository.save(leave);
    }

    // --- 🛠️ HELPERS ---

    private String checkBalanceAndGetWarning(LeaveApplication leave, BigDecimal calculatedDays) {
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId().longValue());
            if (available.compareTo(calculatedDays) < 0) {
                return "Insufficient balance. (Available: " + available + "). Proceed with Loss of Pay?";
            }
        }
        return null;
    }

    private void validateDates(LeaveApplication leave) {
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    private void processAttachments(LeaveApplication leave) {
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(attachment -> {
                attachment.setFileUrl("http://" + SERVER_IP + ":" + SERVER_PORT + "/uploads/leaves/" + attachment.getFileUrl());
                attachment.setLeaveApplication(leave);
            });
        }
    }

    public BigDecimal calculateLeaveDuration(LeaveApplication leave) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = leave.getStartDate();
        while (!date.isAfter(leave.getEndDate())) {
            if (!holidayChecker.isNonWorkingDay(date)) {
                BigDecimal inc = (leave.getLeaveType() == LeaveType.HALF_DAY || (leave.getHalfDayType() != null && date.equals(leave.getEndDate())))
                        ? new BigDecimal("0.5") : BigDecimal.ONE;
                total = total.add(inc);
            }
            date = date.plusDays(1);
        }
        if (total.compareTo(BigDecimal.ZERO) == 0) throw new BadRequestException("Selected dates are non-working days.");
        return total;
    }
}