package com.wenxt.leavemanagement.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CompOffRequestDTO {

    private Long employeeId;
    // Initializing to empty list prevents NullPointer if 'entries' is missing in JSON
    private List<CompOffEntry> entries = new ArrayList<>();

    public static class CompOffEntry {
        private LocalDate workedDate;
        private LocalDate plannedLeaveDate;
        private int days;

        // Getters and Setters
        public LocalDate getWorkedDate() { return workedDate; }
        public void setWorkedDate(LocalDate workedDate) { this.workedDate = workedDate; }

        public LocalDate getPlannedLeaveDate() { return plannedLeaveDate; }
        public void setPlannedLeaveDate(LocalDate plannedLeaveDate) { this.plannedLeaveDate = plannedLeaveDate; }

        public int getDays() { return days; }
        public void setDays(int days) { this.days = days; }
    }

    // Getters and Setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public List<CompOffEntry> getEntries() { return entries; }
    public void setEntries(List<CompOffEntry> entries) { this.entries = entries; }
}