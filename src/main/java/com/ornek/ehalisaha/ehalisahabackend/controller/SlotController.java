package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.security.AppUserPrincipal;
import com.ornek.ehalisaha.ehalisahabackend.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/api/public/facilities/{facilityId}/slots")
    public List<SlotService.SlotDto> publicSlots(@PathVariable Long facilityId) {
        return slotService.publicSlots(facilityId);
    }

    // ✅ Owner ekranı: aktif + kapalı tüm slotlar
    @GetMapping("/api/owner/facilities/{facilityId}/slots")
    @PreAuthorize("hasRole('OWNER')")
    public List<SlotService.SlotDto> ownerSlots(@AuthenticationPrincipal AppUserPrincipal me,
                                                @PathVariable Long facilityId) {
        return slotService.ownerSlots(me.getId(), facilityId);
    }

    @PutMapping("/api/owner/facilities/{facilityId}/slots")
    @PreAuthorize("hasRole('OWNER')")
    public List<SlotService.SlotDto> replaceSlots(@AuthenticationPrincipal AppUserPrincipal me,
                                                  @PathVariable Long facilityId,
                                                  @Valid @RequestBody List<SlotService.SlotDto> slots) {
        return slotService.ownerReplaceSlots(me.getId(), facilityId, slots);
    }
}
