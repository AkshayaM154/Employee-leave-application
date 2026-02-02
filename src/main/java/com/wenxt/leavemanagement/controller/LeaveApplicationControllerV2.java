package com.wenxt.leavemanagement.controller;

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

    // ================= APPLY LEAVE =================
    @PostMapping(value = "/apply", consumes = "multipart/form-data")
    public LeaveApplication applyLeave(
            @RequestParam Integer employeeId,
            @RequestParam String leaveType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String reason,
            // --- ADD THESE TWO PARAMETERS ---
            @RequestParam(required = false) String halfDayType,
            @RequestParam(required = false) String compOffType,
            @RequestParam(required = false) MultipartFile[] files
    ) throws IOException {

        // ===== CASE-INSENSITIVE ENUM PARSING =====
        LeaveType type;
        try {
            type = LeaveType.valueOf(leaveType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave type");
        }

        // ===== CREATE LEAVE OBJECT =====
        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(type);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setReason(reason);

        // --- MANUALLY SET THE NEW TYPES ---
        if (halfDayType != null && !halfDayType.isEmpty()) {
            leave.setHalfDayType(com.wenxt.leavemanagement.enums.HalfDayType.valueOf(halfDayType.toUpperCase()));
        }
        if (compOffType != null && !compOffType.isEmpty()) {
            leave.setCompOffType(com.wenxt.leavemanagement.enums.CompOffType.valueOf(compOffType.toUpperCase()));
        }

        // ===== FILE HANDLING =====
        if (files != null && files.length > 0) {
            if (files.length > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 3 files allowed");
            }

            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            List<LeaveAttachment> attachments = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String filename = Paths.get(file.getOriginalFilename()).getFileName().toString();
                String uniqueName = UUID.randomUUID() + "_" + filename;
                Path path = uploadPath.resolve(uniqueName);
                Files.write(path, file.getBytes());

                // Create attachment
                LeaveAttachment attachment = new LeaveAttachment();

                // --- FIX: Remove the leading "/uploads/" here ---
                // The Service will add the full "http://IP:8081/uploads/" later.
                attachment.setFileUrl(uniqueName);

                attachment.setLeaveApplication(leave);
                attachments.add(attachment);
            }
            leave.setAttachments(attachments);
        }

        // ===== SAVE LEAVE =====
        LeaveApplication savedLeave = service.applyLeave(leave);

        if (savedLeave.getAttachments() != null) {
            savedLeave.getAttachments().forEach(a -> a.setLeaveApplication(null));
        }

        return savedLeave;
    }
}