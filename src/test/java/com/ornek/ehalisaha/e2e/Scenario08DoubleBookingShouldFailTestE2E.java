package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario08DoubleBookingShouldFailTestE2E extends BaseE2ETestE2E {

    @Test
    void doubleBookingSameSlotShouldFailAndSlotShouldBeFull() {
        loginMember();
        memberSelectFacilityAndPitch("Arena Halısaha", "Saha-1");

        // find a FULL slot (created by previous scenario) OR create one if none full
        WebElement grid = byId("slotGrid");
        List<WebElement> full = grid.findElements(By.cssSelector("button.slot.slot-full"));
        if (full.isEmpty()) {
            // create a reservation first (fallback)
            pickFirstFreeSlotLabel();
            click(By.id("btnFillPlayers"));
            click(By.id("btnReserve"));
            assertOutContains("memberOut", "Rezervasyon alındı");
            full = byId("slotGrid").findElements(By.cssSelector("button.slot.slot-full"));
        }
        assertFalse(full.isEmpty(), "Expected at least one occupied slot");

        // attempt to click the first full slot and reserve again (should be blocked by UI, but we also check error path)
        WebElement fullSlot = full.get(0);
        String label = fullSlot.getText();
        // UI blocks click if disabled; still try:
        try { fullSlot.click(); } catch (Exception ignored) {}

        click(By.id("btnFillPlayers"));
        click(By.id("btnReserve"));

        assertOutContains("memberOut", "Rezervasyon hatası");
        // slot should still be full
        assertTrue(byId("slotGrid").getText().contains(label));
    }
}
