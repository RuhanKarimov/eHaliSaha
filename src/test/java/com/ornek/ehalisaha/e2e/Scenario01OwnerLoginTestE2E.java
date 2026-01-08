package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario01OwnerLoginTestE2E extends BaseE2ETest {

    @Test
    void ownerCanLoginAndSeeOwnerPanel() {
        loginOwner();
        assertTrue(driver.getCurrentUrl().contains("/ui/owner.html"));
        // me pill should show owner1
        assertTrue(byId("mePill").getText().contains("owner1"));
    }
}
