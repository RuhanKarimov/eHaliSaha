package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record MeResponse(Long id, String username, String role) {}

    @GetMapping("/api/me")
    public MeResponse me(Authentication auth) {
        Long id = null;

        Object p = auth.getPrincipal();
        if (p instanceof AppUserPrincipal ap) {
            id = ap.getId();
        }

        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");

        return new MeResponse(id, auth.getName(), role);
    }
}
