package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.*;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseE2ETestE2E {

    protected WebDriver driver;
    protected WebDriverWait wait;

    protected String baseUrl() {
        return System.getenv().getOrDefault("BASE_URL", "http://app:8080");
    }

    protected String seleniumUrl() {
        return System.getenv().getOrDefault("SELENIUM_URL", "http://selenium:4444/wd/hub");
    }

    @BeforeEach
    void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1280,900");

        driver = new RemoteWebDriver(new URL(seleniumUrl()), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void tearDown() {
        try { if (driver != null) driver.quit(); } catch (Exception ignored) {}
    }

    protected void go(String path) {
        String url = baseUrl() + (path.startsWith("/") ? path : ("/" + path));
        driver.get(url);
    }

    protected WebElement byId(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(text);
    }

    protected void selectByContainsText(By selectLocator, String containsText) {
        WebElement sel = wait.until(ExpectedConditions.visibilityOfElementLocated(selectLocator));
        Select s = new Select(sel);
        for (WebElement opt : s.getOptions()) {
            if (opt.getText() != null && opt.getText().contains(containsText)) {
                s.selectByVisibleText(opt.getText());
                return;
            }
        }
        fail("Option not found containing: " + containsText);
    }

    protected void loginOwner() {
        login("OWNER", "owner1", "owner123", "/ui/owner.html");
    }

    protected void loginMember() {
        login("MEMBER", "member1", "member123", "/ui/member.html");
    }

    protected void login(String role, String username, String password, String expectedPath) {
        go("/ui/login.html?role=" + role);

        type(By.id("u"), username);
        type(By.id("p"), password);
        click(By.id("btn"));

        wait.until(d -> d.getCurrentUrl().contains(expectedPath));
    }

    protected void assertOutContains(String outId, String expected) {
        WebElement out = byId(outId);
        wait.until(d -> out.getText() != null && out.getText().contains(expected));
        assertTrue(out.getText().contains(expected));
    }

    protected void ensureFacilityExists(String name, String addr) {
        // assumes owner page open
        type(By.id("facName"), name);
        type(By.id("facAddr"), addr);

        // click "Facility oluştur"
        click(By.xpath("//button[contains(normalize-space(.), 'Facility oluştur')]"));

        // facility dropdown should contain it (either newly created or already exists)
        wait.until(d -> {
            try {
                WebElement sel = d.findElement(By.id("ownerFacilitySel"));
                return sel.getText() != null && sel.getText().contains(name);
            } catch (Exception e) {
                return false;
            }
        });

        selectByContainsText(By.id("ownerFacilitySel"), name);
    }

    protected void ensurePitchExists(String pitchName) {
        type(By.id("pitchName"), pitchName);
        click(By.xpath("//button[normalize-space(.)='Ekle']"));

        wait.until(d -> {
            try {
                WebElement sel = d.findElement(By.id("ownerPitchSel"));
                return sel.getText() != null && sel.getText().contains(pitchName);
            } catch (Exception e) {
                return false;
            }
        });

        selectByContainsText(By.id("ownerPitchSel"), pitchName);
    }

    protected void upsertPrice60(int price) {
        // duration select should include 60
        Select dur = new Select(byId("priceDurationSel"));
        boolean ok = dur.getOptions().stream().anyMatch(o -> o.getText().contains("60"));
        if (!ok) fail("60 minutes duration option not found");

        // pick the first option containing 60
        for (WebElement opt : dur.getOptions()) {
            if (opt.getText().contains("60")) {
                dur.selectByVisibleText(opt.getText());
                break;
            }
        }

        type(By.id("priceValue"), String.valueOf(price));
        click(By.xpath("//button[contains(normalize-space(.), 'Kaydet')]"));

        wait.until(d -> {
            try {
                return d.findElement(By.id("pricingBox")).getText().contains(String.valueOf(price));
            } catch (Exception e) {
                return false;
            }
        });
    }

    protected void setSlotsDayAndSave() {
        click(By.xpath("//button[contains(normalize-space(.), 'Gündüz (08-23)')]"));
        click(By.xpath("//button[contains(normalize-space(.), 'Slotları Kaydet')]"));
        // wait toast/out update
        wait.until(d -> d.findElement(By.id("ownerOut")).getText().contains("Slot"));
    }

    protected void memberSelectFacilityAndPitch(String facilityName, String pitchName) {
        selectByContainsText(By.id("facilitySel"), facilityName);
        wait.until(d -> d.findElement(By.id("pitchSel")).getText().contains(pitchName));
        selectByContainsText(By.id("pitchSel"), pitchName);
    }

    protected String pickFirstFreeSlotLabel() {
        WebElement grid = byId("slotGrid");
        // buttons with class 'slot' and NOT 'slot-full'
        List<WebElement> btns = grid.findElements(By.cssSelector("button.slot:not(.slot-full)"));
        if (btns.isEmpty()) fail("No free slots found");
        WebElement b = btns.get(0);
        String label = b.getText(); // includes time range
        b.click();
        return label;
    }
}
