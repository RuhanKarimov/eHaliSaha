package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

public class Scenario00MemberRegisterTestE2E extends BaseE2ETestE2E {

    @Test
    void memberRegistersSuccessfully() {
        // register sayfası (member)
        go("/ui/register.html?role=MEMBER");

        // benzersiz kullanıcı (aynı test tekrar çalışabilsin diye)
        String uname = "member_" + System.currentTimeMillis();

        type(By.id("username"), uname);
        type(By.id("password"), "member123");
        type(By.id("password2"), "member123");

        click(By.id("btnRegister"));

        // başarı mesajı veya login yönlendirmesi
        wait.until(d ->
                d.getCurrentUrl().contains("/ui/login.html")
                        || safeText(By.id("out")).toLowerCase().contains("başar")
        );
    }
}
