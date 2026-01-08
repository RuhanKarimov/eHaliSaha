package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.PricingRule;
import com.ornek.ehalisaha.ehalisahabackend.dto.PricingRuleUpsertRequest;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.PricingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerPricingController {

    private final PricingService pricingService;

    public OwnerPricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/pricing")
    public PricingRule upsert(@AuthenticationPrincipal AppUserPrincipal me,
                              @Valid @RequestBody PricingRuleUpsertRequest req) {
        return pricingService.upsert(me.getId(), req);
    }

    // ✅ list de owner check’li servis üzerinden gelsin
    @GetMapping("/pricing")
    public List<PricingRule> list(@AuthenticationPrincipal AppUserPrincipal me,
                                  @RequestParam Long pitchId) {
        return pricingService.listForOwner(me.getId(), pitchId);
    }
}
