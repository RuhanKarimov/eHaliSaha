package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import com.ornek.ehalisaha.ehalisahabackend.repository.DurationOptionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerCatalogController {

    private final DurationOptionRepository durationRepo;

    public OwnerCatalogController(DurationOptionRepository durationRepo) {
        this.durationRepo = durationRepo;
    }

    /**
     * Owner UI bazı ekranlarda /api/owner/durations çağırıyor.
     * Public endpoint zaten var, ama burada owner tarafı için de aynısını expose ederek
     * UI'nin 404/500'e düşmesini engelliyoruz.
     */
    @GetMapping("/durations")
    public List<DurationOption> durations() {
        return durationRepo.findAll();
    }
}
