package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario03OwnerSetupSlotsPitchPricingTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerSetsSlotsCreatesPitchAndSetsPrice() {
        loginOwner();
        ensureFacilityExists("Arena Halısaha");
        setSlotsDayAndSave();

        ensurePitchExists();
        upsertPrice60();

        assertTrue(byId("pricingBox").getText().contains("250"));
    }
}
