package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MembershipRequest;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.MembershipService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerMembershipController {

    private final MembershipService membershipService;

    public OwnerMembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // ✅ facilityId istemez: owner’ın tüm pending istekleri
    @GetMapping("/membership-requests")
    public List<MembershipRequest> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return membershipService.listRequestsForOwner(me.getId());
    }

    // ✅ FIX: service MembershipRequest dönüyorsa controller da MembershipRequest dönmeli
    @PostMapping("/membership-requests/{requestId}/approve")
    public MembershipRequest approve(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long requestId) {
        return membershipService.approve(me.getId(), requestId);
    }

    @PostMapping("/membership-requests/{requestId}/reject")
    public MembershipRequest reject(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long requestId) {
        return membershipService.reject(me.getId(), requestId);
    }
}
