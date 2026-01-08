package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.AppUser;
import com.ornek.ehalisaha.ehalisahabackend.dto.RegisterRequest;
import com.ornek.ehalisaha.ehalisahabackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicAuthController {

    private final UserService userService;

    public PublicAuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public AppUser register(@Valid @RequestBody RegisterRequest req) {
        return userService.registerMember(req);
    }

    @PostMapping("/register-owner")
    public AppUser registerOwner(@Valid @RequestBody RegisterRequest req) {
        return userService.registerOwner(req);
    }

}
