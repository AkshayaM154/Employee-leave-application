package com.wenxt.leavemanagement.controller;

import com.wenxt.leavemanagement.dto.LeaveResponse;
import com.wenxt.leavemanagement.enums.LeaveType;
import com.wenxt.leavemanagement.model.LeaveApplication;
import com.wenxt.leavemanagement.model.LeaveAttachment;
import com.wenxt.leavemanagement.service.LeaveApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/leave")
public class LeaveApplicationControllerV2 {

    private final LeaveApplicationService service;

    @Value("${file.upload-dir:uploads/leaves}")
    private String uploadDir;

    public LeaveApplicationControllerV2(LeaveApplicationService service) {
        this.service = service;
    }

    @PostMapping(value = "/apply", consumes = "multipart/form-data")
    public LeaveResponse applyLeave(
            @RequestParam Long employeeId,
            @RequestParam String leaveType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reason,
            @RequestParam(required = false) String halfDayType,
            @RequestParam(defaultValue = "false") boolean confirmLossOfPay, // 👈 NEW PARAMETER
            @RequestParam(required = false) MultipartFile[] files
    ) throws IOException {

        LeaveType type;
        try {
            type = LeaveType.valueOf(leaveType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave type");
        }

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(type);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);

        if (halfDayType != null && !halfDayType.isEmpty()) {
            leave.setHalfDayType(com.wenxt.leavemanagement.enums.HalfDayType.valueOf(halfDayType.toUpperCase()));
        }

        // File handling logic...
        if (files != null && files.length > 0) {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            List<LeaveAttachment> attachments = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.write(uploadPath.resolve(uniqueName), file.getBytes());
                LeaveAttachment attachment = new LeaveAttachment();
                attachment.setFileUrl(uniqueName);
                attachment.setLeaveApplication(leave);
                attachments.add(attachment);
            }
            leave.setAttachments(attachments);
        }

        // 🔹 Pass the confirmation flag to the service
        LeaveResponse response = service.applyLeave(leave, confirmLossOfPay);

        // Clean up circular reference for JSON response
        if (response.getLeaveApplication() != null && response.getLeaveApplication().getAttachments() != null) {
            response.getLeaveApplication().getAttachments().forEach(a -> a.setLeaveApplication(null));
        }

        return response;
    }
}