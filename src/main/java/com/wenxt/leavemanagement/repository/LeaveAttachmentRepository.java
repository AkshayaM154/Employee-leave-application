package com.wenxt.leavemanagement.repository;

import com.wenxt.leavemanagement.model.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {
}
