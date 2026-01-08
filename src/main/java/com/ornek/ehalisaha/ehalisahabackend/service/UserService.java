package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.AppUser;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.UserRole;
import com.ornek.ehalisaha.ehalisahabackend.dto.RegisterRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(AppUserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional
    public AppUser registerMember(RegisterRequest req) {
        repo.findByUsername(req.username()).ifPresent(x -> {
            throw new IllegalStateException("Username already exists");
        });

        AppUser u = new AppUser();
        u.setUsername(req.username());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(UserRole.MEMBER);
        u.setEnabled(true);

        return repo.save(u);
    }

    // İstersen ileride public owner register kapatırız, şimdilik helper:
    @Transactional
    public AppUser registerOwner(RegisterRequest req) {
        repo.findByUsername(req.username()).ifPresent(x -> {
            throw new IllegalStateException("Username already exists");
        });

        AppUser u = new AppUser();
        u.setUsername(req.username());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(UserRole.OWNER);
        u.setEnabled(true);

        return repo.save(u);
    }
}
