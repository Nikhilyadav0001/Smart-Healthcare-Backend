package com.smarthealth.userprofile.mapper;

import com.smarthealth.userprofile.dto.ProfileResponse;
import com.smarthealth.userprofile.model.ProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {
    public ProfileResponse toResponse(ProfileEntity e) {
        return new ProfileResponse(e.getUserId(), e.getEmail(), e.getFullName(), e.getDateOfBirth(),
                e.getGender(), e.getPhone(), e.getAssignedDoctorId());
    }
}
