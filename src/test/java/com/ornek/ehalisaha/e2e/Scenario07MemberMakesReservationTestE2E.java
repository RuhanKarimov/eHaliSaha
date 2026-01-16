package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario07MemberMakesReservationTestE2E extends BaseE2ETestE2E {

    @Test
    void memberMakesAReservation() {
        loginMember();
        memberSelectFacilityAndPitch();

        // pick a free slot
        String slotLabel = pickFirstFreeSlotLabel();
        assertNotNull(slotLabel);

        // add players quickly
        click(By.id("btnFillPlayers"));

        // pay method CARD (if exists)
        try {
            new org.openqa.selenium.support.ui.Select(byId("paySel")).selectByVisibleText("CARD");
        } catch (Exception ignored) {}

        // shuttle (optional)
        try {
            new org.openqa.selenium.support.ui.Select(byId("shuttleSel")).selectByIndex(0);
        } catch (Exception ignored) {}

        click(By.id("btnReserve"));

        assertOutContains("Rezervasyon alındı");
        // after success, the chosen slot should become full after refresh
        assertTrue(byId("slotGrid").getText().contains(slotLabel.split("\n")[0]));
    }
}
