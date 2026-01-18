package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.text.Normalizer;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Scenario02OwnerCreateFacilityTestE2E extends BaseE2ETestE2E {

    @Test
    void ownerCreatesFacility_andItAppearsInDropdown() {
        loginOwner();
        assertUiLoaded();

        String facilityName = "Arena Halisaha";

        // create + refresh + dropdown wait (senin base içindeki click-only ensureFacilityExists kullanılır)
        ensureFacilityExists(facilityName);

        // dropdown’dan seç ve doğrula
        By sel = By.id("ownerFacilitySel");
        WebElement selEl = byId("ownerFacilitySel");
        Select s = new Select(selEl);

        String selected = s.getFirstSelectedOption().getText();
        System.out.println("SELECTED_OPTION=" + selected);

        // Türkçe/ı/İ farkları yüzünden fold ile kontrol
        assertTrue(fold(selected).contains(fold(facilityName)) || fold(selected).contains("arena"),
                "Dropdown seçimi beklenen facility değil. selected='" + selected + "'");
    }

    // Scenario içinde küçük fold helper (Base’dekine dokunmadan)
    private static String fold(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFKC);
        x = x.replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2007', ' ');
        x = x.replace('ı', 'i').replace('İ', 'I');
        x = x.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return x;
    }
}
