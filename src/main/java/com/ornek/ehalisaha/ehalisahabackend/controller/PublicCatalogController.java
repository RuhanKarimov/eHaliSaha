package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.repository.DurationOptionRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {

    private final FacilityRepository facilityRepo;
    private final PitchRepository pitchRepo;
    private final DurationOptionRepository durationRepo;

    public PublicCatalogController(FacilityRepository facilityRepo,
                                   PitchRepository pitchRepo,
                                   DurationOptionRepository durationRepo) {
        this.facilityRepo = facilityRepo;
        this.pitchRepo = pitchRepo;
        this.durationRepo = durationRepo;
    }

    @GetMapping("/facilities")
    public List<Facility> facilities() {
        return facilityRepo.findAll();
    }

    @GetMapping("/facilities/{facilityId}/pitches")
    public List<Pitch> pitches(@PathVariable Long facilityId) {
        // Member tarafında sadece aktif pitch'ler gösterilsin
        return pitchRepo.findByFacilityIdAndActiveTrueOrderByIdAsc(facilityId);
    }

    @GetMapping("/durations")
    public List<DurationOption> durations() {
        return durationRepo.findAll();
    }
}
