package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario02OwnerCreateFacilityTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerCreatesFacility() {
        loginOwner();
        ensureFacilityExists("Arena Halısaha", "Merkez / Malatya");
        assertTrue(byId("ownerFacilitySel").getText().contains("Arena Halısaha"));
    }
}
