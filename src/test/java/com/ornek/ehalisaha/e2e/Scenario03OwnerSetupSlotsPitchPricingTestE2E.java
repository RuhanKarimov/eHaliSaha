package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario03OwnerSetupSlotsPitchPricingTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerSetsSlotsCreatesPitchAndSetsPrice() {
        loginOwner();
        String facilityName = "Arena-" + System.currentTimeMillis();
        ensureFacilityExists(facilityName, "Merkez / Malatya");
        setSlotsDayAndSave();

        ensurePitchExists("Saha-1");
        upsertPrice60(250);

        assertTrue(byId("pricingBox").getText().contains("250"));
    }
}
