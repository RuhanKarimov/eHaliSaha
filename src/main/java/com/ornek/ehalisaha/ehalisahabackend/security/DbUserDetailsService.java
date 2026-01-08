package com.ornek.ehalisaha.ehalisahabackend.security;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.AppUser;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.UserRole;
import com.ornek.ehalisaha.ehalisahabackend.repository.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final AppUserRepository repo;

    public DbUserDetailsService(AppUserRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        UserRole role = u.getRole();
        return new AppUserPrincipal(u.getId(), u.getUsername(), u.getPasswordHash(), role);
    }
}
