package com.ornek.ehalisaha.ehalisahabackend.config;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.AppUser;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import com.ornek.ehalisaha.ehalisahabackend.domain.enums.UserRole;
import com.ornek.ehalisaha.ehalisahabackend.repository.AppUserRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.DurationOptionRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PricingRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedConfig {

    @Bean
    CommandLineRunner seed(DurationOptionRepository durationRepo,
                           AppUserRepository userRepo,
                           PricingRuleRepository pricingRepo,
                           PasswordEncoder encoder) {
        return args -> {
            // önce pricing temizle (FK güvenliği)
            pricingRepo.deleteAll();

            durationRepo.findByMinutes(60).orElseGet(() -> durationRepo.save(d(60, "1 saat")));


            // demo users
            seedUser(userRepo, encoder, "owner1", "owner123", UserRole.OWNER);
            seedUser(userRepo, encoder, "member1", "member123", UserRole.MEMBER);
        };
    }


    private static DurationOption d(int minutes, String label) {
        DurationOption x = new DurationOption();
        x.setMinutes(minutes);
        x.setLabel(label);
        return x;
    }

    private static void seedUser(AppUserRepository repo, PasswordEncoder encoder,
                                 String username, String rawPassword, UserRole role) {

        AppUser u = repo.findByUsername(username).orElse(null);

        if (u == null) {
            AppUser nu = new AppUser();
            nu.setUsername(username);
            nu.setPasswordHash(encoder.encode(rawPassword));
            nu.setRole(role);
            nu.setEnabled(true);
            repo.save(nu);
            return;
        }

        boolean changed = false;

        if (u.getRole() != role) {
            u.setRole(role);
            changed = true;
        }

        if (u.getPasswordHash() == null || !encoder.matches(rawPassword, u.getPasswordHash())) {
            u.setPasswordHash(encoder.encode(rawPassword));
            changed = true;
        }

        if (!Boolean.TRUE.equals(u.getEnabled())) {
            u.setEnabled(true);
            changed = true;
        }

        if (changed) {
            repo.save(u);
        }
    }
}
