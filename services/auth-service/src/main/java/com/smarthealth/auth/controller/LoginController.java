package com.smarthealth.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
}
