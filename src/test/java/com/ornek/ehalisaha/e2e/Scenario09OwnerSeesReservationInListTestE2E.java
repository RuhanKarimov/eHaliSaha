package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario09OwnerSeesReservationInListTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerShouldSeeMemberReservationOnReservationsPage() {
        loginOwner();
        go("/ui/owner-reservations.html");

        // select facility and pitch
        selectByContainsText(By.id("facilitySel"), "Arena Halısaha");
        wait.until(d -> d.findElement(By.id("pitchSel")).getText().contains("Saha-1"));
        selectByContainsText(By.id("pitchSel"), "Saha-1");

        // refresh
        click(By.id("btnRefresh"));

        // list should include member1
        wait.until(d -> d.findElement(By.id("listBox")).getText().contains("member1"));
        assertTrue(byId("listBox").getText().contains("member1"));
    }
}
