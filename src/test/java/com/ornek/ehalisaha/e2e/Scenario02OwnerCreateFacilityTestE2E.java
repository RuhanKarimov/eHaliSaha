package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario02OwnerCreateFacilityTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerCreatesFacility() {
        loginOwner();
        assertUiLoaded();

        String facilityName = "Arena Halisaha";
        ensureFacilityExists(facilityName);

        Select s = new Select(byId("ownerFacilitySel"));
        String selected = s.getFirstSelectedOption().getText();
        assertTrue(selected.contains("Arena"), "Selected=" + selected);
    }
}
