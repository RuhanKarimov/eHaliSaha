package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.PricingRule;
import com.ornek.ehalisaha.ehalisahabackend.dto.PricingRuleUpsertRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.DurationOptionRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PricingRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PricingService {

    private final PitchRepository pitchRepo;
    private final FacilityRepository facilityRepo;
    private final DurationOptionRepository durationRepo;
    private final PricingRuleRepository pricingRepo;
    private final AuditService audit;

    public PricingService(PitchRepository pitchRepo,
                          FacilityRepository facilityRepo,
                          DurationOptionRepository durationRepo,
                          PricingRuleRepository pricingRepo,
                          AuditService audit) {
        this.pitchRepo = pitchRepo;
        this.facilityRepo = facilityRepo;
        this.durationRepo = durationRepo;
        this.pricingRepo = pricingRepo;
        this.audit = audit;
    }

    @Transactional
    public PricingRule upsert(Long ownerUserId, PricingRuleUpsertRequest req) {
        Pitch pitch = pitchRepo.findById(req.pitchId())
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + req.pitchId()));

        Facility fac = facilityRepo.findById(pitch.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        if (!fac.getOwnerUserId().equals(ownerUserId)) {
            throw new SecurityException("Not your facility");
        }

        DurationOption d = durationRepo.findByMinutes(req.durationMinutes())
                .orElseThrow(() -> new IllegalArgumentException("Invalid duration: " + req.durationMinutes()));

        PricingRule pr = pricingRepo.findByPitchIdAndDurationOptionIdAndActiveTrue(pitch.getId(), d.getId())
                .orElseGet(() -> {
                    PricingRule x = new PricingRule();
                    x.setPitchId(pitch.getId());
                    x.setDurationOptionId(d.getId());
                    x.setActive(true);
                    return x;
                });

        pr.setPrice(req.price());
        pr.setActive(true);

        PricingRule saved = pricingRepo.save(pr);

        audit.log(ownerUserId, "PRICING_UPSERT", "PricingRule", saved.getId(),
                "pitchId=" + pitch.getId() + ", minutes=" + d.getMinutes() + ", price=" + saved.getPrice());

        return saved;
    }

    public List<PricingRule> listForOwner(Long ownerUserId, Long pitchId) {
        Pitch pitch = pitchRepo.findById(pitchId)
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + pitchId));

        Facility fac = facilityRepo.findById(pitch.getFacilityId())
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        if (!fac.getOwnerUserId().equals(ownerUserId)) {
            throw new SecurityException("Not your pitch/facility");
        }

        return pricingRepo.findByPitchId(pitchId);
    }
}
