// PublicPriceController.java
package com.ornek.ehalisaha.ehalisahabackend.controller;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.Pitch;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.PricingRule;
import com.ornek.ehalisaha.ehalisahabackend.domain.entity.DurationOption;
import com.ornek.ehalisaha.ehalisahabackend.repository.DurationOptionRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PitchRepository;
import com.ornek.ehalisaha.ehalisahabackend.repository.PricingRuleRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/public")
public class PublicPriceController {

    public record PriceQuoteDto(
            Long pitchId,
            int baseMinutes,
            BigDecimal basePrice,
            String currency,
            int totalMinutes,
            BigDecimal totalPrice
    ) {}

    private final PitchRepository pitchRepo;
    private final DurationOptionRepository durationRepo;
    private final PricingRuleRepository pricingRepo;

    public PublicPriceController(PitchRepository pitchRepo,
                                 DurationOptionRepository durationRepo,
                                 PricingRuleRepository pricingRepo) {
        this.pitchRepo = pitchRepo;
        this.durationRepo = durationRepo;
        this.pricingRepo = pricingRepo;
    }

    @GetMapping("/pitches/{pitchId}/price")
    public PriceQuoteDto quote(@PathVariable Long pitchId,
                               @RequestParam(defaultValue = "60") int baseMinutes,
                               @RequestParam(required = false) Integer totalMinutes) {

        // pitch var mı?
        Pitch pitch = pitchRepo.findById(pitchId)
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found: " + pitchId));

        DurationOption baseOpt = durationRepo.findByMinutes(baseMinutes)
                .orElseThrow(() -> new IllegalArgumentException("DurationOption not found: " + baseMinutes));

        PricingRule pr = pricingRepo.findByPitchIdAndDurationOptionIdAndActiveTrue(pitch.getId(), baseOpt.getId())
                .orElseThrow(() -> new IllegalStateException("Pricing not set for baseMinutes=" + baseMinutes));

        int tm = (totalMinutes == null || totalMinutes <= 0) ? baseMinutes : totalMinutes;
        if (tm % baseMinutes != 0) throw new IllegalArgumentException("totalMinutes must be multiple of baseMinutes");

        int mul = tm / baseMinutes;
        BigDecimal total = pr.getPrice().multiply(BigDecimal.valueOf(mul));

        return new PriceQuoteDto(pitchId, baseMinutes, pr.getPrice(), pr.getCurrency(), tm, total);
    }
}
