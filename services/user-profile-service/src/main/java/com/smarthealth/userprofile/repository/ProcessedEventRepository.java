package com.smarthealth.userprofile.repository;

import com.smarthealth.userprofile.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

}