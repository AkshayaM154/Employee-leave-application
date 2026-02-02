package com.wenxt.leavemanagement.dto;

import java.time.LocalDate;
import java.util.List;

public class CompOffRequestDTO {

    private Long employeeId;
    private List<CompOffEntry> entries;

    public static class CompOffEntry {
        private LocalDate workedDate;
        private int days; // Usually 1

        // Getters and Setters
        public LocalDate getWorkedDate() { return workedDate; }
        public void setWorkedDate(LocalDate workedDate) { this.workedDate = workedDate; }
        public int getDays() { return days; }
        public void setDays(int days) { this.days = days; }
    }

    // Getters and Setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public List<CompOffEntry> getEntries() { return entries; }
    public void setEntries(List<CompOffEntry> entries) { this.entries = entries; }
}