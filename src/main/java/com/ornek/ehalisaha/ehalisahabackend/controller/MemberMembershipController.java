package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.MembershipRequest;
import com.ornek.ehalisaha.ehalisahabackend.dto.MembershipRequestCreateRequest;
import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.MembershipService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
public class MemberMembershipController {

    public record MembershipRequestDto(
            Long id,
            Long userId,
            Long facilityId,
            String status
    ) {}

    public record MembershipStatusDto(
            Long facilityId,
            boolean member,
            String membershipStatus,
            Long membershipId,
            String requestStatus
    ) {}

    private final MembershipService membershipService;

    public MemberMembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/membership-requests")
    public MembershipRequestDto create(@AuthenticationPrincipal AppUserPrincipal me,
                                       @RequestBody MembershipRequestCreateRequest body) {
        MembershipRequest req = membershipService.createRequest(me.getId(), body.facilityId());
        return new MembershipRequestDto(req.getId(), req.getUserId(), req.getFacilityId(), req.getStatus().name());
    }

    // ✅ UI'ın "onaylandı mı?" bilgisini çekmesi için
    @GetMapping("/membership-status")
    public MembershipStatusDto status(@AuthenticationPrincipal AppUserPrincipal me,
                                      @RequestParam Long facilityId) {
        var st = membershipService.getMemberFacilityStatus(me.getId(), facilityId);

        boolean active = st.membershipStatus() != null && "ACTIVE".equalsIgnoreCase(st.membershipStatus().name());

        return new MembershipStatusDto(
                st.facilityId(),
                active,
                st.membershipStatus() == null ? null : st.membershipStatus().name(),
                st.membershipId(),
                st.requestStatus() == null ? null : st.requestStatus().name()
        );
    }
}
