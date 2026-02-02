package com.wenxt.leavemanagement.controller;

import com.wenxt.leavemanagement.dto.CompOffRequestDTO;
import com.wenxt.leavemanagement.model.CompOff;
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

    // ✅ EARN COMPOFF
    // Inside CompOffController.java

    @PostMapping("/earn")
    public ResponseEntity<String> earnCompOff(@RequestBody CompOffRequestDTO request) {
        compOffService.earnBulkCompOff(request);
        return ResponseEntity.ok("Comp-Off credits added successfully");
    }

    // ✅ CHECK BALANCE
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<?> getBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(
                compOffService.getAvailableCompOffDays(employeeId)
        );
    }
}
