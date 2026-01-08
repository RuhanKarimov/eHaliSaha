package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

public class Scenario10MembershipAlreadyActiveShouldFailTestE2E extends BaseE2ETestE2E {

    @Test
    void memberCannotSendMembershipRequestWhenAlreadyActive() {
        loginMember();
        memberSelectFacilityAndPitch("Arena Halısaha", "Saha-1");

        click(By.id("btnMembership"));
        // second click should fail (already active or already requested)
        click(By.id("btnMembership"));

        assertOutContains("memberOut", "Üyelik isteği hatası");
    }
}
