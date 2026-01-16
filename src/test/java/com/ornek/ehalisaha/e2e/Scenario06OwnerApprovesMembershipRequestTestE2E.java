package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario06OwnerApprovesMembershipRequestTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerApprovesPendingRequest() {
        loginOwner();
        ensureFacilityExists("Arena Halısaha");
        // Requests box should show a card and "Onayla" button
        click(By.xpath("//div[@id='reqBox']//button[contains(normalize-space(.),'Onayla')]"));

        // After approve, should show no pending requests
        assertTrue(byId("reqBox").getText().contains("Bekleyen istek yok") ||
                   byId("ownerOut").getText().contains("İstek"));
    }
}
