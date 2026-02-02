package com.wenxt.leavemanagement.controller;

import com.wenxt.leavemanagement.dto.CompOffRequestDTO;
import com.wenxt.leavemanagement.service.CompOffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compoff")
public class CompOffController {

    private final CompOffService compOffService;

    public CompOffController(CompOffService compOffService) {
        this.compOffService = compOffService;
    }

    // ✅ 1 & 2: REQUEST COMPOFF (Starts as PENDING)
    @PostMapping("/request")
    public ResponseEntity<String> requestCompOff(@RequestBody CompOffRequestDTO request) {
        // Renamed from createCompOffRequest to requestBulkCompOff
        compOffService.requestBulkCompOff(request);
        return ResponseEntity.ok("Comp-Off request submitted and is now PENDING approval.");
    }

    // ✅ TEAMMATE APPROVAL (The Gatekeeper)
    @PatchMapping("/approve/{id}")
    public ResponseEntity<String> approveCompOff(@PathVariable Long id) {
        // Renamed from approveRequest to approveCompOff
        compOffService.approveCompOff(id);
        return ResponseEntity.ok("Comp-Off credit has been approved and is now EARNED.");
    }

    // ✅ CHECK BALANCE
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<?> getBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(
                compOffService.getAvailableCompOffDays(employeeId)
        );
    }
}