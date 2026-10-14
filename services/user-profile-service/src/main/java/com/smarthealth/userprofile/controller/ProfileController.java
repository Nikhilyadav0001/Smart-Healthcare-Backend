package com.smarthealth.userprofile.controller;

import com.smarthealth.userprofile.dto.ProfileResponse;
import com.smarthealth.userprofile.dto.UpdateProfileRequest;
import com.smarthealth.userprofile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profiles")
public class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) { this.service = service; }

    // TEMPORARY: removed when the JWT resource server is added (rule 10).
    @GetMapping("/me")
    public ProfileResponse me(@RequestHeader("X-Dev-User-Id") String userId) {
        return service.get(userId);
    }

    @PutMapping("/me")
    public ProfileResponse update(@RequestHeader("X-Dev-User-Id") String userId,
                                  @Valid @RequestBody UpdateProfileRequest req) {
        return service.update(userId, req);
    }
}