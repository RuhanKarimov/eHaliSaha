package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

public class Scenario05MemberSendsMembershipRequestTestE2E extends BaseE2ETestE2E {

    @Test
    void memberSendsMembershipRequest() {
        loginMember();
        memberSelectFacilityAndPitch("Arena Halısaha", "Saha-1");
        click(By.id("btnMembership"));
        assertOutContains("memberOut", "Üyelik isteği");
    }
}
