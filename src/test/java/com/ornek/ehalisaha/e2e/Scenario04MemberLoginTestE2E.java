package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario04MemberLoginTestE2E extends BaseE2ETestE2E {

    @Test
    void memberCanLoginAndSeeMemberPanel() {
        loginMember();
        assertTrue(driver.getCurrentUrl().contains("/ui/member.html"));
        assertTrue(byId("mePill").getText().contains("member1"));
    }
}
