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

    // --- 🟢 EMPLOYEE LEAVE LOGIC ---
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

    // --- 🔴 ADMIN LEAVE LOGIC ---
    @Transactional
    public LeaveResponse applyAdminLeave(LeaveApplication leave, boolean isConfirmed) {
        validateDates(leave);
        BigDecimal calculatedDays = calculateLeaveDuration(leave);
        String warning = checkBalanceAndGetWarning(leave, calculatedDays);

        if (warning != null && !isConfirmed) {
            return new LeaveResponse(null, warning);
        }

        leave.setDays(calculatedDays);
        leave.setStatus(LeaveStatus.APPROVED); // Admin recorded leaves are auto-approved
        processAttachments(leave);

        LeaveApplication savedLeave = repository.save(leave);

        if (leave.getLeaveType() == LeaveType.COMP_OFF && warning == null) {
            compOffService.useCompOff(leave.getEmployeeId().longValue(), calculatedDays, savedLeave.getApplicationId());
        }

        return new LeaveResponse(savedLeave, null);
    }

    private String checkBalanceAndGetWarning(LeaveApplication leave, BigDecimal calculatedDays) {
        if (leave.getLeaveType() == LeaveType.COMP_OFF) {
            BigDecimal available = compOffService.getAvailableCompOffDays(leave.getEmployeeId().longValue());
            if (available.compareTo(calculatedDays) < 0) {
                return "There is no leave left (Available: " + available + "). If you take this, it will be added to the Loss of Pay. Do you wish to continue?";
            }
        }
        return null;
    }

    // --- 🛠️ CANCELLATION LOGIC ---

    /**
     * Cancel leave as an Admin.
     * Can cancel any leave regardless of current status.
     */
    @Transactional
    public void cancelAdminLeave(Long applicationId) {
        LeaveApplication leave = repository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));

        performCancellation(leave);
    }

    /**
     * Cancel leave as an Employee.
     * Includes security check to ensure they own the record and it's in a cancellable state.
     */
    @Transactional
    public void cancelEmployeeLeave(Long applicationId, Long employeeId) {
        LeaveApplication leave = repository.findById(applicationId)
                .orElseThrow(() -> new BadRequestException("Leave application not found with ID: " + applicationId));

        // Security Check
        if (!leave.getEmployeeId().equals(employeeId.intValue())) {
            throw new BadRequestException("You are not authorized to cancel this leave application.");
        }

        // State Check
        if (leave.getStatus() == LeaveStatus.REJECTED || leave.getStatus() == LeaveStatus.CANCELLED) {
            throw new BadRequestException("Leave is already " + leave.getStatus() + " and cannot be cancelled.");
        }

        performCancellation(leave);
    }

    /**
     * Shared logic to flip status and reverse Comp-Off usage.
     */
    private void performCancellation(LeaveApplication leave) {
        // 🔄 REVERSAL LOGIC: Restore credits only if the leave was APPROVED or PENDING and using Comp-Off
        if (leave.getLeaveType() == LeaveType.COMP_OFF &&
                (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.PENDING)) {

            List<CompOff> linkedCredits = compOffRepository.findByUsedLeaveApplicationId(leave.getApplicationId());
            for (CompOff credit : linkedCredits) {
                credit.setStatus(CompOffStatus.EARNED); // Back to usable
                credit.setUsedLeaveApplicationId(null); // Clear link
                compOffRepository.save(credit);
            }
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        repository.save(leave);
    }

    // --- 🛠️ HELPER METHODS ---

    private void validateDates(LeaveApplication leave) {
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
    }

    private void processAttachments(LeaveApplication leave) {
        if (leave.getAttachments() != null) {
            leave.getAttachments().forEach(attachment -> {
                String fileName = attachment.getFileUrl();
                String webUrl = "http://" + SERVER_IP + ":" + SERVER_PORT + "/uploads/leaves/" + fileName;
                attachment.setFileUrl(webUrl);
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