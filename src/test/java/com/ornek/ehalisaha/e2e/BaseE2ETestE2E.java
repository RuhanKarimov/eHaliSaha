package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.*;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

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
        if (sel.contains("localhost") || sel.contains("127.0.0.1")) {
            return "http://host.docker.internal:8080";
        }
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

        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");

        options.addArguments("--disable-features="
                + "HttpsOnlyMode,"
                + "UpgradeInsecureRequests,"
                + "HttpsFirstMode,"
                + "HttpsFirstModeV2,"
                + "HttpsUpgrades,"
                + "AutomaticHttpsUpgrades");

        options.addArguments("--disable-gpu");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");

        System.out.println("CHROME_ARGS=" + System.getenv("CHROME_ARGS"));
        System.out.println("BASE_URL=" + baseUrl());
        System.out.println("SELENIUM_URL=" + seleniumUrl());

        String raw = System.getenv("CHROME_ARGS");
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(";")) {
                String arg = part.trim();
                if (!arg.isEmpty()) options.addArguments(arg);
            }
        }

        driver = new RemoteWebDriver(new URL(seleniumUrl()), options);

        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.pollingEvery(Duration.ofMillis(250));
    }

    @AfterEach
    void tearDown(TestInfo info) {
        String testName = info.getTestClass().map(Class::getSimpleName).orElse("Test")
                + "_" + info.getTestMethod().map(m -> m.getName()).orElse("method");

        testName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");

        try {
            if (driver != null) {
                Path dir = Path.of("e2e-reports");
                Files.createDirectories(dir);

                // screenshot
                try {
                    File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    Path out = dir.resolve(testName + ".png");
                    try {
                        Files.copy(src.toPath(), out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}

                // page source
                try {
                    String html = driver.getPageSource();
                    Path p = dir.resolve(testName + ".html");
                    Files.writeString(p, html, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}

                // small debug
                try {
                    String url = driver.getCurrentUrl();
                    String out = safeText(By.id("ownerOut"));
                    Path p = dir.resolve(testName + ".txt");
                    Files.writeString(p,
                            "url=" + url + "\nownerOut=" + out + "\n",
                            StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try { if (driver != null) driver.quit(); } catch (Exception ignored) {}
    }

    // ---------------- helpers ----------------

    protected void go(String path) {
        String url = baseUrl() + (path.startsWith("/") ? path : ("/" + path));
        System.out.println("NAVIGATE=" + url);
        driver.get(url);
        waitForDocumentReady();
    }

    protected void waitForDocumentReady() {
        try {
            WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));
            w.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        } catch (Exception ignored) {}
    }

    protected WebElement byId(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void clickSmart(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        } catch (Exception ignored) {}

        try {
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (Exception e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            } catch (Exception ex) {
                throw e;
            }
        }
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

    protected String safeText(By by) {
        try {
            String t = driver.findElement(by).getText();
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    protected String safeText(WebDriver d, By by) {
        try {
            String t = d.findElement(by).getText();
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    protected boolean isErrorLike(String s) {
        if (s == null) return false;
        String t = s.toLowerCase();
        return t.contains("hata") || t.contains("error") || t.contains("exception")
                || t.contains("failed") || t.contains("invalid");
    }

    protected boolean hasOptionContaining(By selectBy, String contains) {
        return hasOptionContaining(driver, selectBy, contains);
    }

    protected boolean hasOptionContaining(WebDriver d, By selectBy, String contains) {
        try {
            Select sel = new Select(d.findElement(selectBy));
            for (WebElement opt : sel.getOptions()) {
                String t = opt.getText();
                if (t != null && t.contains(contains)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- login ----------------

    protected void loginOwner() {
        login("OWNER", "owner1", "owner123", "/ui/owner.html");
        // owner sayfasının gerçekten geldiğine dair bir sinyal
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ownerFacilitySel")));
    }

    protected void loginMember() {
        login("MEMBER", "member1", "member123", "/ui/member.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facilitySel")));
    }

    protected void login(String role, String username, String password, String expectedPath) {
        go("/ui/login.html?role=" + role);

        type(By.id("u"), username);
        type(By.id("p"), password);
        clickSmart(By.id("btn"));

        wait.until(d -> d.getCurrentUrl().contains(expectedPath));
    }

    protected void assertOutContains(String outId, String expected) {
        WebElement out = byId(outId);
        wait.until(d -> out.getText() != null && out.getText().contains(expected));
        assertTrue(out.getText().contains(expected));
    }

    // ---------------- Owner flows ----------------

    protected void ensureFacilityExists(String name, String addr) {
        By selLoc = By.id("ownerFacilitySel");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facName")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facAddr")));
        wait.until(ExpectedConditions.presenceOfElementLocated(selLoc));

        // 1) Zaten var mı?
        if (hasOptionContaining(selLoc, name)) {
            selectByContainsText(selLoc, name);
            return;
        }

        // (opsiyonel) eski out temizle
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var el=document.getElementById('ownerOut'); if(el) el.textContent='';"
            );
        } catch (Exception ignored) {}

        // 2) Yoksa oluştur
        type(By.id("facName"), name);
        type(By.id("facAddr"), addr);

        By btn = By.cssSelector("button[onclick*='UI.createFacility']");
        clickSmart(btn);

        // ✅ Başarı sinyali: dropdown option gelmesi
        // ownerOut sadece hata tespiti için kullanılır (başarı mesajı overwrite olabilir)
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        longWait.pollingEvery(Duration.ofMillis(300));

        longWait.until(d -> {
            if (hasOptionContaining(d, selLoc, name)) return true;
            String out = safeText(d, By.id("ownerOut"));
            return isErrorLike(out);
        });

        if (!hasOptionContaining(selLoc, name)) {
            String out = safeText(By.id("ownerOut"));
            String opts = "";
            try {
                Select sel = new Select(driver.findElement(selLoc));
                StringBuilder sb = new StringBuilder();
                for (WebElement opt : sel.getOptions()) sb.append("[").append(opt.getText()).append("] ");
                opts = sb.toString().trim();
            } catch (Exception ignored) {}
            fail("Facility oluşturulamadı veya dropdown'a düşmedi. ownerOut=" + out + " options=" + opts);
        }

        selectByContainsText(selLoc, name);
    }

    protected void ensurePitchExists(String pitchName) {
        By selLoc = By.id("ownerPitchSel");

        // pitch sel varsa ve içeriyorsa direkt seç
        if (hasOptionContaining(selLoc, pitchName)) {
            selectByContainsText(selLoc, pitchName);
            return;
        }

        type(By.id("pitchName"), pitchName);

        // "Ekle" text'i yerine direkt createPitch butonu
        By btn = By.cssSelector("button[onclick*='UI.createPitch']");
        if (driver.findElements(btn).isEmpty()) {
            // fallback (sayfada onclick yoksa)
            btn = By.xpath("//button[normalize-space(.)='Ekle']");
        }
        clickSmart(btn);

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        longWait.pollingEvery(Duration.ofMillis(300));
        longWait.until(d -> hasOptionContaining(d, selLoc, pitchName) || isErrorLike(safeText(d, By.id("ownerOut"))));

        if (!hasOptionContaining(selLoc, pitchName)) {
            fail("Pitch oluşturulamadı veya dropdown'a düşmedi. ownerOut=" + safeText(By.id("ownerOut")));
        }

        selectByContainsText(selLoc, pitchName);
    }

    protected void upsertPrice60(int price) {
        // duration select should include 60
        Select dur = new Select(byId("priceDurationSel"));
        boolean ok = dur.getOptions().stream().anyMatch(o -> o.getText() != null && o.getText().contains("60"));
        if (!ok) fail("60 minutes duration option not found");

        for (WebElement opt : dur.getOptions()) {
            if (opt.getText() != null && opt.getText().contains("60")) {
                dur.selectByVisibleText(opt.getText());
                break;
            }
        }

        // input id farklı olabiliyor: priceValue / priceInput
        By priceBox = driver.findElements(By.id("priceValue")).isEmpty() ? By.id("priceInput") : By.id("priceValue");
        type(priceBox, String.valueOf(price));

        By saveBtn = By.cssSelector("button[onclick*='UI.savePrice']");
        if (driver.findElements(saveBtn).isEmpty()) {
            saveBtn = By.xpath("//button[contains(normalize-space(.), 'Kaydet')]");
        }
        clickSmart(saveBtn);

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        longWait.pollingEvery(Duration.ofMillis(300));
        longWait.until(d -> {
            try {
                String box = d.findElement(By.id("pricingBox")).getText();
                if (box != null && box.contains(String.valueOf(price))) return true;
            } catch (Exception ignored) {}
            return isErrorLike(safeText(d, By.id("ownerOut")));
        });

        if (!safeText(By.id("pricingBox")).contains(String.valueOf(price))) {
            fail("Price kaydedilemedi. ownerOut=" + safeText(By.id("ownerOut"))
                    + " pricingBox=" + safeText(By.id("pricingBox")));
        }
    }

    protected void setSlotsDayAndSave() {
        By dayBtn = By.xpath("//button[contains(normalize-space(.), 'Gündüz')]");
        By saveBtn = By.xpath("//button[contains(normalize-space(.), 'Slotları Kaydet')]");

        clickSmart(dayBtn);
        clickSmart(saveBtn);

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        longWait.pollingEvery(Duration.ofMillis(300));

        longWait.until(d -> {
            String out = safeText(d, By.id("ownerOut"));
            return (out != null && out.toLowerCase().contains("slot")) || isErrorLike(out);
        });

        String out = safeText(By.id("ownerOut"));
        if (isErrorLike(out)) {
            fail("Slot kaydetme hata verdi. ownerOut=" + out);
        }
    }

    // ---------------- Member flows ----------------

    protected void memberSelectFacilityAndPitch(String facilityName, String pitchName) {
        selectByContainsText(By.id("facilitySel"), facilityName);

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(45));
        longWait.pollingEvery(Duration.ofMillis(300));

        longWait.until(d -> {
            try {
                Select p = new Select(d.findElement(By.id("pitchSel")));
                for (WebElement opt : p.getOptions()) {
                    String t = opt.getText();
                    if (t != null && t.contains(pitchName)) return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });

        selectByContainsText(By.id("pitchSel"), pitchName);
    }

    protected String pickFirstFreeSlotLabel() {
        WebElement grid = byId("slotGrid");
        List<WebElement> btns = grid.findElements(By.cssSelector("button"));

        if (btns.isEmpty()) fail("slotGrid içinde button yok");

        for (WebElement b : btns) {
            boolean disabled = false;
            try { disabled = !b.isEnabled(); } catch (Exception ignored) {}
            if (disabled) continue;

            String cls = Optional.ofNullable(b.getAttribute("class")).orElse("").toLowerCase();
            if (cls.contains("slot-full") || cls.contains("full") || cls.contains("disabled")) continue;

            String label = Optional.ofNullable(b.getText()).orElse("").trim();
            if (label.isEmpty()) label = cls;

            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", b);
            } catch (Exception ignored) {}

            try { b.click(); }
            catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", b);
            }
            return label;
        }

        fail("Boş slot bulunamadı (butonların hepsi dolu/disabled gibi görünüyor)");
        return "";
    }
}
