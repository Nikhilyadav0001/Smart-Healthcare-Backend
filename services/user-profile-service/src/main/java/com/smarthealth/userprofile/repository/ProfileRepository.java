package com.smarthealth.userprofile.repository;

import com.smarthealth.userprofile.model.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<ProfileEntity, String> {}