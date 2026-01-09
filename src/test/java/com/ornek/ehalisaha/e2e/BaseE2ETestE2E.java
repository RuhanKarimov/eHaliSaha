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

    protected RemoteWebDriver driver;
    protected WebDriverWait wait;

    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(20);

    @BeforeEach
    void setup() throws Exception {
        String selenium = seleniumUrl();
        String base = baseUrl();

        System.out.println("CHROME_ARGS=" + chromeArgs());
        System.out.println("BASE_URL=" + base);
        System.out.println("SELENIUM_URL=" + selenium);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1280,900");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.setAcceptInsecureCerts(true);

        // extra args from env (split by ;)
        String extra = chromeArgs();
        if (extra != null && !extra.isBlank()) {
            for (String a : extra.split(";")) {
                a = a.trim();
                if (!a.isBlank()) options.addArguments(a);
            }
        }

        driver = new RemoteWebDriver(new URL(selenium), options);
        wait = new WebDriverWait(driver, DEFAULT_WAIT);
        wait.pollingEvery(Duration.ofMillis(250));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    // ---------- ENV helpers ----------

    protected String seleniumUrl() {
        String env = System.getenv("SELENIUM_URL");
        if (env != null && !env.isBlank()) return env;
        return "http://localhost:4444/wd/hub";
    }

    protected String chromeArgs() {
        String env = System.getenv("CHROME_ARGS");
        if (env != null) return env;
        return "--disable-features=HttpsOnlyMode,UpgradeInsecureRequests,HttpsFirstMode,HttpsFirstModeV2,HttpsUpgrades,AutomaticHttpsUpgrades;--ignore-certificate-errors;--allow-insecure-localhost";
    }

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

    protected String loginUrlOwner() {
        return baseUrl() + "/ui/login.html?role=OWNER";
    }

    // ---------- Small UI actions ----------

    protected WebElement byId(String id) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
    }

    protected void click(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    protected void type(By by, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        el.clear();
        el.sendKeys(text);
    }

    protected void navigate(String url) {
        System.out.println("NAVIGATE=" + url);
        driver.navigate().to(url);
    }

    protected void selectByContainsText(By selectBy, String contains) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(selectBy));
        Select sel = new Select(el);
        for (WebElement opt : sel.getOptions()) {
            if (opt.getText() != null && opt.getText().contains(contains)) {
                sel.selectByVisibleText(opt.getText());
                return;
            }
        }
        fail("Select option not found containing: " + contains);
    }

    // ---------- Login helpers ----------

    protected void loginOwner() {
        login("owner1", "owner123", "/ui/owner.html");
    }

    protected void login(String username, String password, String expectedPath) {
        navigate(baseUrl() + "/ui/login.html?role=OWNER");

        // login form
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("u")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("p")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("btn")));

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

    // ---------- Robust helpers (fix for Jenkins flakiness) ----------

    private String safeText(By by) {
        try {
            return driver.findElement(by).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private String safeText(WebDriver d, By by) {
        try {
            return d.findElement(by).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isErrorLike(String s) {
        if (s == null) return false;
        String t = s.toLowerCase();
        return t.contains("hata") || t.contains("error") || t.contains("exception") || t.contains("failed") || t.contains("invalid");
    }

    private boolean hasOptionContaining(By selectBy, String contains) {
        return hasOptionContaining(driver, selectBy, contains);
    }

    private boolean hasOptionContaining(WebDriver d, By selectBy, String contains) {
        try {
            WebElement selEl = d.findElement(selectBy);
            Select sel = new Select(selEl);
            for (WebElement opt : sel.getOptions()) {
                String t = opt.getText();
                if (t != null && t.contains(contains)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void clickSmart(By by) {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(by));
        try {
            el.click();
        } catch (ElementClickInterceptedException ice) {
            // overlay vs: JS click fallback
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    // ---------- E2E steps ----------

    protected void ensureFacilityExists(String name, String addr) {
        By selLoc = By.id("ownerFacilitySel");

        // Owner panel elementleri DOM'a gelsin
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facName")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facAddr")));
        wait.until(ExpectedConditions.presenceOfElementLocated(selLoc));

        // 1) Zaten var mı?
        if (hasOptionContaining(selLoc, name)) {
            selectByContainsText(selLoc, name);
            return;
        }

        // 2) Yoksa oluştur
        type(By.id("facName"), name);
        type(By.id("facAddr"), addr);

        // HTML: onclick="UI.createFacility()"
        By btn = By.cssSelector("button[onclick*='UI.createFacility']");
        clickSmart(btn);

        /*
          ⚠️ ÖNEMLİ:
          UI tarafında "Facility oluşturuldu ✅" mesajı çok kısa süre görünüp,
          hemen sonra "Facility değişti... ✅" gibi başka bir notice ile overwrite olabiliyor.
          Bu yüzden ownerOut'ta "oluşturuldu" aramak flaky.
          En güvenilir sinyal: dropdown option'ın gelmesi.
         */

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        longWait.pollingEvery(Duration.ofMillis(300));

        longWait.until(d -> {
            // başarı: select option geldi
            if (hasOptionContaining(d, selLoc, name)) return true;

            // hata/uyarı: ownerOut bir şeyler yazdıysa test anlamlı patlasın
            String out = safeText(d, By.id("ownerOut"));
            return isErrorLike(out);
        });

        // Hata/uyarı geldi ama option yoksa fail et
        if (!hasOptionContaining(selLoc, name)) {
            String out = safeText(By.id("ownerOut"));
            fail("Facility oluşturulamadı veya dropdown'a düşmedi. ownerOut=" + out);
        }

        // 3) Seç
        selectByContainsText(selLoc, name);
    }

    protected void ensurePitchExists(String pitchName) {
        type(By.id("pitchName"), pitchName);

        // HTML: onclick="UI.createPitch()"
        By btn = By.cssSelector("button[onclick*='UI.createPitch']");
        clickSmart(btn);

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

        type(By.id("priceInput"), String.valueOf(price));

        // HTML: onclick="UI.savePrice()"
        By btn = By.cssSelector("button[onclick*='UI.savePrice']");
        clickSmart(btn);

        // pricing box should reflect
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

    protected void maybeUploadEvidence(String name) {
        // optional: if want to store page source / screenshot, you can enable
        // currently left as no-op
    }

    // Helper: click first free slot if exists and return label
    protected String pickFirstFreeSlotAndReturnLabel() {
        List<WebElement> btns = driver.findElements(By.cssSelector("#slotGrid button"));
        if (btns.isEmpty()) fail("No slots grid buttons found");

        for (WebElement b : btns) {
            String cls = b.getAttribute("class");
            boolean disabled = Boolean.parseBoolean(String.valueOf(b.getAttribute("disabled")));
            if (disabled) continue;

            // heuristic: skip reserved/closed ones
            if (cls != null) {
                String c = cls.toLowerCase();
                if (c.contains("disabled") || c.contains("closed") || c.contains("reserved")) continue;
            }

            String label = b.getText();
            b.click();
            return label;
        }

        // if reached, no free range
        fail("No free slots found");
        return "";
    }

    // Optional debug dump (kept small, safe for CI)
    protected void dumpDebug(String prefix) {
        try {
            String url = driver.getCurrentUrl();
            String out = safeText(By.id("ownerOut"));
            String html = driver.getPageSource();

            Path dir = Path.of("e2e-reports");
            Files.createDirectories(dir);

            Files.writeString(dir.resolve(prefix + "_url.txt"), url, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve(prefix + "_ownerOut.txt"), out, StandardCharsets.UTF_8);

            // page source can be large; still useful
            Files.writeString(dir.resolve(prefix + "_page.html"), html, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    protected void takeScreenshot(String name) {
        try {
            File f = driver.getScreenshotAs(OutputType.FILE);
            Path dir = Path.of("e2e-reports");
            Files.createDirectories(dir);
            Files.copy(f.toPath(), dir.resolve(name + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }
}
