package com.ornek.ehalisaha.ehalisahabackend.service;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.dto.FacilityCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.dto.PitchCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.repository.FacilityRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OwnerOpsService {

    private final FacilityRepository facilityRepo;
    private final PitchRepository pitchRepo;
    private final AuditService audit;

    public OwnerOpsService(FacilityRepository facilityRepo, PitchRepository pitchRepo, AuditService audit) {
        this.facilityRepo = facilityRepo;
        this.pitchRepo = pitchRepo;
        this.audit = audit;
    }
    private static String key(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    @Transactional
    public Facility createFacility(Long ownerUserId, FacilityCreateRequest req) {
        String nameKey = key(req.name());
        if (facilityRepo.existsByOwnerUserIdAndNameKey(ownerUserId, nameKey)) {
            throw new IllegalStateException("Bu isimde bir tesis zaten var: " + req.name());
        }

        Facility f = new Facility();
        f.setOwnerUserId(ownerUserId);
        f.setName(req.name());
        f.setAddress(req.address());
        f.setActive(true);

        Facility saved = facilityRepo.save(f);
        audit.log(ownerUserId, "FACILITY_CREATE", "Facility", saved.getId(), "name=" + saved.getName());
        return saved;
    }

    public List<Facility> myFacilities(Long ownerUserId) {
        return facilityRepo.findByOwnerUserId(ownerUserId);
    }

    @Transactional
    public Pitch createPitch(Long ownerUserId, Long facilityId, PitchCreateRequest req) {
        String pitchKey = key(req.name());
        if (pitchRepo.existsByFacilityIdAndNameKey(facilityId, pitchKey)) {
            throw new IllegalStateException("Bu tesiste bu isimde saha zaten var: " + req.name());
        }

        Facility f = facilityRepo.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        if (!f.getOwnerUserId().equals(ownerUserId)) {
            throw new SecurityException("Not your facility");
        }

        // İstersen body’de facilityId gönderirse tutarlılık kontrolü:
        if (req.facilityId() != null && !req.facilityId().equals(facilityId)) {
            throw new IllegalArgumentException("facilityId in body does not match path");
        }

        Pitch p = new Pitch();
        p.setFacilityId(facilityId);
        p.setName(req.name());
        p.setActive(true);

        Pitch saved = pitchRepo.save(p);
        audit.log(ownerUserId, "PITCH_CREATE", "Pitch", saved.getId(),
                "facilityId=" + facilityId + ", name=" + saved.getName());
        return saved;
    }

    public List<Pitch> myPitches(Long ownerUserId, Long facilityId) {
        Facility f = facilityRepo.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        if (!f.getOwnerUserId().equals(ownerUserId)) {
            throw new SecurityException("Not your facility");
        }

        return pitchRepo.findByFacilityId(facilityId);
    }
}
