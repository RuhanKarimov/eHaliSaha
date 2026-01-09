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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BaseE2ETestE2E {

    protected WebDriver driver;
    protected WebDriverWait wait;

    protected String baseUrl() {
        String env = System.getenv("BASE_URL");
        if (env != null && !env.isBlank()) return env;

        String sel = seleniumUrl();
        // Selenium’a localhost üzerinden gidiyorsan: chrome container’da çalışır,
        // host’taki app’e erişim için host.docker.internal gerekir.
        if (sel.contains("localhost") || sel.contains("127.0.0.1")) {
            return "http://host.docker.internal:8080";
        }

        // Compose içi tipik senaryo: app servisi
        return "http://app:8080";
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

        // SSL/HTTPS zorlamalarını ez
        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");

        // Chrome 143+ için daha geniş disable listesi
        options.addArguments("--disable-features="
                + "HttpsOnlyMode,"
                + "UpgradeInsecureRequests,"
                + "HttpsFirstMode,"
                + "HttpsFirstModeV2,"
                + "HttpsUpgrades,"
                + "AutomaticHttpsUpgrades");

        // ekstra stabilite
        options.addArguments("--disable-gpu");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");

        System.out.println("CHROME_ARGS=" + System.getenv("CHROME_ARGS"));
        System.out.println("BASE_URL=" + baseUrl());
        System.out.println("SELENIUM_URL=" + seleniumUrl());

        // ✅ Jenkins ile verdiğin CHROME_ARGS env'ini uygula (sen ; ile ayırıyorsun)
        String raw = System.getenv("CHROME_ARGS");
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(";")) {
                String arg = part.trim();
                if (!arg.isEmpty()) options.addArguments(arg);
            }
        }

        driver = new RemoteWebDriver(new URL(seleniumUrl()), options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }



    @AfterEach
    void tearDown() {
        try {
            if (driver != null) {
                // screenshot
                try {
                    File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    File out = new File("e2e-reports/last.png");
                    out.getParentFile().mkdirs();
                    if (!src.renameTo(out)) {
                        // renameTo bazı ortamlarda false dönebilir, kopyalama fallback
                        Files.copy(src.toPath(), out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ignored) {}

                // page source
                try {
                    String html = driver.getPageSource();
                    Path p = Path.of("e2e-reports/last.html");
                    Files.createDirectories(p.getParent());
                    Files.writeString(p, html, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try { if (driver != null) driver.quit(); } catch (Exception ignored) {}
    }

    protected void go(String path) {
        String url = baseUrl() + (path.startsWith("/") ? path : ("/" + path));
        System.out.println("NAVIGATE=" + url);
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
        By selLoc = By.id("ownerFacilitySel");

        // Owner panel elementleri DOM'a gelsin
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facName")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facAddr")));
        wait.until(ExpectedConditions.presenceOfElementLocated(selLoc));

        // 1) Zaten var mı?  (select.getText yerine option'ları gez)
        try {
            WebElement selEl = driver.findElement(selLoc);
            Select sel = new Select(selEl);
            for (WebElement opt : sel.getOptions()) {
                String t = opt.getText();
                if (t != null && t.contains(name)) {
                    selectByContainsText(selLoc, name);
                    return;
                }
            }
        } catch (Exception ignored) {}

        // 2) Yoksa oluştur
        type(By.id("facName"), name);
        type(By.id("facAddr"), addr);

        // ✅ HTML'de birebir var: onclick="UI.createFacility()"
        By btn = By.cssSelector("button[onclick*='UI.createFacility']");

        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(btn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", button);

        try {
            button.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        }

        // 3) Önce ownerOut'ta başarı/hata sinyali gelsin (API çağrısı sonuçlanmış mı?)
        wait.until(d -> {
            try {
                String out = d.findElement(By.id("ownerOut")).getText();
                if (out == null) return false;
                // başarı veya hata mesajlarından biri gelince devam
                return out.contains("oluşturuldu") || out.contains("hatası") || out.contains("error") || out.contains("Error");
            } catch (Exception e) {
                return false;
            }
        });

        // Eğer hata yazdıysa, test daha anlamlı şekilde patlasın
        try {
            String out = driver.findElement(By.id("ownerOut")).getText();
            if (out != null && (out.contains("hatası") || out.toLowerCase().contains("error"))) {
                fail("Facility create failed. ownerOut=" + out);
            }
        } catch (Exception ignored) {}

        // 4) Dropdown’da option olarak görünmesini bekle (en doğru sinyal bu)
        wait.until(d -> {
            try {
                WebElement selEl = d.findElement(selLoc);
                Select s = new Select(selEl);
                for (WebElement opt : s.getOptions()) {
                    String t = opt.getText();
                    if (t != null && t.contains(name)) return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });

        // 5) Seç
        selectByContainsText(selLoc, name);
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
