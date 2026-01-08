package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class MeAliasController {

    @GetMapping("/api/auth/me")
    public Map<String, Object> authMe(@AuthenticationPrincipal AppUserPrincipal me) {
        // /api/me ile aynı formatta dön
        return Map.of(
                "id", me.getId(),
                "username", me.getUsername(),
                "role", me.getRole()
        );
    }
}
