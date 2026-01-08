package com.ornek.ehalisaha.ehalisahabackend.repository;

import com.ornek.ehalisaha.ehalisahabackend.domain.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    Optional<PricingRule> findByPitchIdAndDurationOptionIdAndActiveTrue(Long pitchId, Long durationOptionId);
    List<PricingRule> findByPitchId(Long pitchId);
}
