package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Facility;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.dto.FacilityCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.dto.PitchCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.OwnerOpsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerOpsController {

    private final OwnerOpsService service;

    public OwnerOpsController(OwnerOpsService service) {
        this.service = service;
    }

    @PostMapping("/facilities")
    public Facility createFacility(@AuthenticationPrincipal AppUserPrincipal me,
                                   @Valid @RequestBody FacilityCreateRequest req) {
        return service.createFacility(me.getId(), req);
    }

    @GetMapping("/facilities")
    public List<Facility> myFacilities(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.myFacilities(me.getId());
    }

    @PostMapping("/facilities/{facilityId}/pitches")
    public Pitch createPitch(@AuthenticationPrincipal AppUserPrincipal me,
                             @PathVariable Long facilityId,
                             @Valid @RequestBody PitchCreateRequest req) {
        return service.createPitch(me.getId(), facilityId, req);
    }

    @GetMapping("/facilities/{facilityId}/pitches")
    public List<Pitch> pitches(@AuthenticationPrincipal AppUserPrincipal me,
                               @PathVariable Long facilityId) {
        return service.myPitches(me.getId(), facilityId);
    }
}
