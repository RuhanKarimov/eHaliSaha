package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class Scenario02OwnerCreateFacilityTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerCreatesFacility() {
        // loginOwner() zaten assertUiLoaded() çağırıyor (Base class'ındaki versiyonda).
        loginOwner();

        String facilityName = "Arena Halisaha";
        ensureFacilityExists(facilityName);

        By selLoc = By.id("ownerFacilitySel");

        // seçili option gerçekten güncellendi mi? (race'i öldürür)
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> {
                    try {
                        Select s = new Select(d.findElement(selLoc));
                        String selected = s.getFirstSelectedOption().getText();
                        return foldLite(selected).contains("arena");
                    } catch (Exception e) {
                        return false;
                    }
                });

        Select s = new Select(byId("ownerFacilitySel"));
        WebElement opt = s.getFirstSelectedOption();
        String selected = opt == null ? "" : opt.getText().trim();

        System.out.println("Selected facility option = [" + selected + "]");

        // hem sağlam hem kısa assert
        assertTrue(foldLite(selected).contains("arena"),
                "Expected selected option to contain 'Arena' but was: " + selected);
    }

    // Test içinde minik normalize: Base'deki fold'a bağımlı kalma, hızlı ve stabil olsun.
    private static String foldLite(String s) {
        if (s == null) return "";
        String x = s.trim().toLowerCase(Locale.ROOT);
        x = x.replace('ı', 'i').replace('İ', 'i');
        x = x.replace('\u00A0', ' '); // nbsp
        x = x.replaceAll("\\s+", " ");
        return x;
    }
}
