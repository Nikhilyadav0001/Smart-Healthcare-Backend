package com.smarthealth.userprofile.service;

import com.smarthealth.userprofile.dto.ProfileResponse;
import com.smarthealth.userprofile.dto.UpdateProfileRequest;
import com.smarthealth.userprofile.exception.ProfileNotFoundException;
import com.smarthealth.userprofile.mapper.ProfileMapper;
import com.smarthealth.userprofile.model.ProfileEntity;
import com.smarthealth.userprofile.repository.ProfileRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final ProfileRepository repo;
    private final ProfileMapper mapper;

    public ProfileService(ProfileRepository repo, ProfileMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Cacheable(cacheNames = "profiles", key = "#userId")
    @Transactional(readOnly = true)
    public ProfileResponse get(String userId) {
        return repo.findById(userId).map(mapper::toResponse)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    @CachePut(cacheNames = "profiles", key = "#userId")
    @Transactional
    public ProfileResponse update(String userId, UpdateProfileRequest req) {
        ProfileEntity e = repo.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
        e.setFullName(req.fullName());
        e.setDateOfBirth(req.dateOfBirth());
        e.setGender(req.gender());
        e.setPhone(req.phone());
        return mapper.toResponse(repo.save(e));
    }
}