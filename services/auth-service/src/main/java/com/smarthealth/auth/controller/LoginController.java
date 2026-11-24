package com.smarthealth.auth.controller;

import com.smarthealth.auth.model.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        return "login";
    }

    @GetMapping({"/auth/me", "/me"})
    @ResponseBody
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        String email = authentication.getName();
        String name = email != null && email.contains("@") ? email.split("@")[0] : "User";
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .toList();

        if (principal instanceof AuthenticatedUser authUser) {
            email = authUser.getUsername();
            name = email != null && email.contains("@") ? email.split("@")[0] : "User";
        } else if (principal instanceof OAuth2User oauth2User) {
            if (oauth2User.getAttribute("email") != null) {
                email = oauth2User.getAttribute("email");
            }
            if (oauth2User.getAttribute("name") != null) {
                name = oauth2User.getAttribute("name");
            } else if (oauth2User.getAttribute("login") != null) {
                name = oauth2User.getAttribute("login");
            } else if (email != null && email.contains("@")) {
                name = email.split("@")[0];
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("email", email);
        resp.put("name", name);
        resp.put("fullName", name);
        resp.put("roles", roles);
        resp.put("role", roles.contains("ADMIN") ? "ADMIN" : (roles.contains("DOCTOR") ? "DOCTOR" : "PATIENT"));
        return ResponseEntity.ok(resp);
    }
}
