package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario02OwnerCreateFacilityTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerCreatesFacility() {
        loginOwner();

        String facilityName = "Arena Halısaha";
        ensureFacilityExists(facilityName, "Merkez / Malatya");
        System.out.println("Facility Name: " + byId("ownerFacilitySel").getText());
        assertTrue(byId("ownerFacilitySel").getText().contains("Hazır"));
    }
}
