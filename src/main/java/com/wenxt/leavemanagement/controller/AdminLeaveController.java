package com.wenxt.leavemanagement.controller;

import com.wenxt.leavemanagement.dto.LeaveRequestDTO;
import com.wenxt.leavemanagement.dto.LeaveResponse;
import com.wenxt.leavemanagement.enums.LeaveType;
import com.wenxt.leavemanagement.model.LeaveApplication;
import com.wenxt.leavemanagement.service.LeaveApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/leaves")
public class AdminLeaveController {

    private final LeaveApplicationService leaveService;

    public AdminLeaveController(LeaveApplicationService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/record")
    public ResponseEntity<LeaveResponse> recordAdminLeave(@RequestBody LeaveRequestDTO dto) {
        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(dto.getEmployeeId());
        leave.setLeaveType(LeaveType.valueOf(dto.getLeaveType().toUpperCase()));
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setReason(dto.getReason());

        // 🛑 The Fix: Convert String to Enum
        if (dto.getHalfDayType() != null && !dto.getHalfDayType().isEmpty()) {
            try {
                // Converts "FIRST_HALF" (String) -> HalfDayType.FIRST_HALF (Enum)
                leave.setHalfDayType(com.wenxt.leavemanagement.enums.HalfDayType.valueOf(dto.getHalfDayType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handles cases where Postman sends an invalid string
                return ResponseEntity.badRequest().build();
            }
        }

        LeaveResponse response = leaveService.applyAdminLeave(leave, dto.isConfirmLossOfPay());
        return ResponseEntity.ok(response);
    }
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<String> cancelAdminLeave(@PathVariable Long id) {
        leaveService.cancelAdminLeave(id);
        return ResponseEntity.ok("Admin leave cancelled successfully.");
    }
}