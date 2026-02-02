package com.wenxt.leavemanagement.repository;

import com.wenxt.leavemanagement.model.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
}
