package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseE2ETestE2E {

    protected WebDriver driver;
    protected WebDriverWait wait;

    // ---------- env / urls ----------

    protected String cfg(String... keys) {
        for (String k : keys) {
            if (k == null || k.isBlank()) continue;
            String v = System.getProperty(k);
            if (v != null && !v.isBlank()) return v.trim();
            v = System.getenv(k);
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    protected String baseUrl() {
        String v = cfg(
                "BASE_URL", "E2E_BASE_URL",
                "e2e.baseUrl", "e2e.base_url"
        );
        if (v != null) return v;

        // Varsayılan: docker network içinden app servisine git (en stabil yol)
        String mode = cfg("E2E_MODE", "e2e.mode");
        if (mode != null && mode.equalsIgnoreCase("host")) {
            String hostPort = cfg("APP_HOST_PORT", "e2e.appHostPort");
            if (hostPort == null) hostPort = "18080";
            return "http://host.docker.internal:" + hostPort;
        }
        return "http://host.docker.internal:18080";
    }

    protected String seleniumUrl() {
        String v = cfg(
                "SELENIUM_URL", "E2E_SELENIUM_URL",
                "e2e.seleniumUrl", "e2e.selenium_url"
        );
        if (v != null) return v;

        // Varsayılan: Jenkins/host tarafı (compose'da 14444:4444)
        return "http://localhost:14444/wd/hub";
    }

// ---------- setup / teardown ----------

    @BeforeEach
    void setUp() throws Exception {
        ChromeOptions options = new ChromeOptions();

        // headless
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1280,900");

        // stability for docker/grid
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");

        // ignore SSL / https-only
        options.setAcceptInsecureCerts(true);
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("--disable-features=" +
                "HttpsOnlyMode," +
                "UpgradeInsecureRequests," +
                "HttpsFirstMode," +
                "HttpsFirstModeV2," +
                "HttpsUpgrades," +
                "AutomaticHttpsUpgrades");

        // apply CHROME_ARGS from Jenkins (split by ;)
        String raw = cfg("CHROME_ARGS", "e2e.chromeArgs");
        System.out.println("CHROME_ARGS=" + raw);
        System.out.println("BASE_URL=" + baseUrl());
        System.out.println("SELENIUM_URL=" + seleniumUrl());

        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(";")) {
                String arg = part.trim();
                if (!arg.isEmpty()) options.addArguments(arg);
            }
        }

        driver = new RemoteWebDriver(new URL(seleniumUrl()), options);

        wait = new WebDriverWait(driver, Duration.ofSeconds(60));
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
                    Files.copy(src.toPath(), out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}

                // page source
                try {
                    String html = driver.getPageSource();
                    Path p = dir.resolve(testName + ".html");
                    Files.writeString(p, html, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}

                // small debug
                try {
                    String url = safeText(() -> driver.getCurrentUrl());
                    String ownerOut = safeText(By.id("ownerOut"));
                    Path p = dir.resolve(testName + ".txt");
                    Files.writeString(p,
                            "url=" + url + "\n" +
                                    "BASE_URL=" + baseUrl() + "\n" +
                                    "SELENIUM_URL=" + seleniumUrl() + "\n" +
                                    "ownerOut=" + ownerOut + "\n",
                            StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try { if (driver != null) driver.quit(); } catch (Exception ignored) {}
    }

    // ---------- helpers ----------

    protected void go(String path) {
        String url = baseUrl() + (path.startsWith("/") ? path : ("/" + path));
        System.out.println("NAVIGATE=" + url);
        driver.get(url);
        waitForDocumentReady();
    }

    protected void waitForDocumentReady() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        } catch (Exception ignored) {}
    }

    protected WebElement byId(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
    }

    protected void type(By locator, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(text);
    }

    protected void click(By locator) {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
        el.click();
    }



// --------- out assertions (geri eklendi) ---------

    protected void assertOutContains(String outId, String expected) {
        By outLoc = By.id(outId);

        // out elementi var mı?
        wait.until(ExpectedConditions.presenceOfElementLocated(outLoc));

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(60));
        w.pollingEvery(Duration.ofMillis(250));

        try {
            w.until(d -> {
                String t = safeText(d, outLoc);
                // beklenen metin gelince başarı
                if (t != null && t.contains(expected)) return true;
                // hata mesajı geldiyse beklemeyi bitir (sonra fail edeceğiz)
                return isErrorLike(t);
            });
        } catch (TimeoutException te) {
            // aşağıda net fail mesajı vereceğiz
        }

        String finalText = safeText(outLoc);
        if (finalText == null) finalText = "";

        if (!finalText.contains(expected)) {
            fail("Beklenen çıktı bulunamadı. outId=" + outId +
                    " expected='" + expected + "'" +
                    " actual='" + finalText + "'");
        }
    }


    protected void selectByContainsText(By selectLocator, String containsText) {
        WebElement sel = wait.until(ExpectedConditions.visibilityOfElementLocated(selectLocator));
        Select s = new Select(sel);
        for (WebElement opt : s.getOptions()) {
            String t = opt.getText();
            if (t != null && t.contains(containsText)) {
                s.selectByVisibleText(t);
                return;
            }
        }
        fail("Option not found containing: " + containsText);
    }

    protected String safeText(By by) {
        try {
            String t = driver.findElement(by).getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    protected String safeText(WebDriver d, By by) {
        try {
            String t = d.findElement(by).getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    protected String safeText(SupplierThrows<String> fn) {
        try {
            String v = fn.get();
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    @FunctionalInterface
    protected interface SupplierThrows<T> {
        T get() throws Exception;
    }

    protected boolean isErrorLike(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();

        return t.contains("hata")
                || t.contains("error")
                || t.contains("exception")
                || t.contains("failed")
                || t.contains("invalid")
                || t.contains("başarısız")
                || t.contains("yetkisiz")
                || t.contains("unauthorized")
                || t.contains("forbidden")
                || t.contains("conflict")
                || t.contains("409")
                || t.contains("zaten")
                || t.contains("mevcut")
                || t.contains("exists")
                || t.contains("already");
    }


    protected boolean isOkLike(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        return t.contains("ok")
                || t.contains("başar")
                || t.contains("oluştur")
                || t.contains("created")
                || t.contains("saved")
                || t.contains("facilityid=")
                || t.contains("id=");
    }


    protected boolean hasOptionContaining(By selectBy, String contains) {
        return hasOptionContaining(driver, selectBy, contains);
    }

    protected boolean hasOptionContaining(WebDriver d, By selectBy, String contains) {
        try {
            Select sel = new Select(d.findElement(selectBy));
            String want = fold(contains);

            for (WebElement opt : sel.getOptions()) {
                String t = opt.getText();

                // debug istiyorsan:
                System.out.println("hasOptionContainingFold: " + fold(t));
                System.out.println("containsRaw: " + contains);
                System.out.println("containsCP : " + codepoints(contains));
                System.out.println("optRaw     : " + t);
                System.out.println("optCP      : " + codepoints(t));


                // contains yerine startsWith daha mantıklı (option genelde "Name + ayırıcı + Address")
                if (fold(t).contains(want)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String fold(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFKC);

        // NBSP ve türev boşlukları normal boşluğa çevir
        x = x.replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2007', ' ');

        // Jenkins konsolunda � gördüğün şey bazen gerçek metinde U+FFFD olabilir
        // Biz bunu "i" gibi davranacak şekilde ele alıyoruz (özellikle Halısaha gibi yerlerde)
        x = x.replace('\uFFFD', 'i');

        // Türkçe i/ı problemleri (test verilerini stabil yapar)
        x = x.replace('ı', 'i').replace('İ', 'I').replace('i', 'i');

        // Fazla boşlukları tek boşluk yap
        x = x.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return x;
    }

    private static String codepoints(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(String.format("U+%04X ", (int) s.charAt(i)));
        }
        return sb.toString().trim();
    }


    protected String dumpOptions(By selectBy) {
        try {
            Select s = new Select(driver.findElement(selectBy));
            StringBuilder sb = new StringBuilder();
            for (WebElement opt : s.getOptions()) sb.append("[").append(opt.getText()).append("] ");
            return sb.toString().trim();
        } catch (Exception e) {
            return "(options okunamadı)";
        }
    }

    protected void tryRefreshAll() {
        // owner.html: onclick="UI.refreshAll()"
        By btnRefresh = By.cssSelector("button[onclick*='UI.refreshAll']");
        if (!driver.findElements(btnRefresh).isEmpty()) {
            try { click(btnRefresh); } catch (Exception ignored) {}
        }
    }

    // ---------- login ----------

    protected void loginOwner() {
        login("OWNER", "owner1", "owner123", "/ui/owner.html");
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
        click(By.id("btn"));

        wait.until(d -> d.getCurrentUrl().contains(expectedPath));
        waitForDocumentReady();
    }

    // ---------- Owner flows ----------

    protected void ensureFacilityExists(String name, String addr) {
        By selLoc = By.id("ownerFacilitySel");
        By outLoc = By.id("ownerOut");

        // owner panel DOM hazır mı?
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facName")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facAddr")));
        wait.until(ExpectedConditions.presenceOfElementLocated(selLoc));
        wait.until(ExpectedConditions.presenceOfElementLocated(outLoc));

        // sayfa ilk açılışında listeler geç gelebiliyor
        tryRefreshAll();

        // 1) zaten var mı?
        if (hasOptionContaining(selLoc, name)) {
            selectByContainsText(selLoc, name);
            return;
        }

        // 2) create
        type(By.id("facName"), name);
        type(By.id("facAddr"), addr);

        // out'u CLICK'ten önce temizle (race olmasın)
        String outBeforeTMP = safeText(outLoc);
        boolean clear = false;
        try {
            WebElement outEl = driver.findElement(outLoc);
            ((JavascriptExecutor) driver).executeScript("arguments[0].innerText='';", outEl);
            clear = true;
        } catch (Exception ignored) {}
        final String outBefore = clear? "" : outBeforeTMP;
        By btnCreate = By.id("btnCreateFacility");
        if (driver.findElements(btnCreate).isEmpty()) {
            btnCreate = By.cssSelector("button[onclick*='UI.createFacility']");
        }
        click(btnCreate);

        // 3) API sinyali: option geldi VEYA ownerOut doldu (ok/err)
        WebDriverWait apiWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        apiWait.pollingEvery(Duration.ofMillis(250));
        apiWait.until(d -> {
            if (hasOptionContaining(d, selLoc, name)) return true;
            String out = safeText(d, outLoc);
            out = (out == null) ? "" : out.trim();
            return !out.isBlank() && !out.equals(outBefore);
        });

        String outNow = safeText(outLoc);

        // 4) list her zaman anında güncellenmeyebiliyor -> 1 kez refreshAll
        tryRefreshAll();

        // 5) asıl başarı: dropdown option
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        longWait.pollingEvery(Duration.ofMillis(300));
        longWait.until(d -> hasOptionContaining(d, selLoc, name) || isErrorLike(safeText(d, outLoc)));

        if (!hasOptionContaining(selLoc, name)) {
            fail("Facility dropdown'a düşmedi. ownerOut=" + safeText(outLoc) + " options=" + dumpOptions(selLoc));
        }

        // 6) select
        selectByContainsText(selLoc, name);

        // 7) Eğer UI hata gösteriyorsa, net patlatalım
        if (isErrorLike(outNow)) {
            fail("Facility oluşturma hata verdi. ownerOut=" + outNow);
        }
    }

    protected void setSlotsDayAndSave() {
        By dayBtn = By.xpath("//button[contains(normalize-space(.), 'Gündüz')]");
        By saveBtn = By.xpath("//button[contains(normalize-space(.), 'Slotları Kaydet')]");
        By outLoc = By.id("ownerOut");

        click(dayBtn);
        click(saveBtn);

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> {
            String out = safeText(d, outLoc);
            return (out != null && out.toLowerCase().contains("slot")) || isErrorLike(out);
        });

        String out = safeText(outLoc);
        if (isErrorLike(out)) fail("Slot kaydetme hata verdi. ownerOut=" + out);
    }

    protected void ensurePitchExists(String pitchName) {
        By selLoc = By.id("ownerPitchSel");
        By outLoc = By.id("ownerOut");

        if (hasOptionContaining(selLoc, pitchName)) {
            selectByContainsText(selLoc, pitchName);
            return;
        }

        type(By.id("pitchName"), pitchName);

        By btn = By.cssSelector("button[onclick*='UI.createPitch']");
        if (driver.findElements(btn).isEmpty()) {
            btn = By.xpath("//button[normalize-space(.)='Ekle']");
        }
        click(btn);

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> hasOptionContaining(d, selLoc, pitchName) || isErrorLike(safeText(d, outLoc)));

        if (!hasOptionContaining(selLoc, pitchName)) {
            fail("Pitch dropdown'a düşmedi. ownerOut=" + safeText(outLoc) + " options=" + dumpOptions(selLoc));
        }

        selectByContainsText(selLoc, pitchName);
    }

    protected void upsertPrice60(int price) {
        By outLoc = By.id("ownerOut");

        Select dur = new Select(byId("priceDurationSel"));
        boolean has60 = dur.getOptions().stream().anyMatch(o -> {
            String t = o.getText();
            return t != null && t.contains("60");
        });
        if (!has60) fail("60 minutes duration option not found");

        for (WebElement opt : dur.getOptions()) {
            String t = opt.getText();
            if (t != null && t.contains("60")) {
                dur.selectByVisibleText(t);
                break;
            }
        }

        By priceBox = driver.findElements(By.id("priceValue")).isEmpty()
                ? By.id("priceInput")
                : By.id("priceValue");
        type(priceBox, String.valueOf(price));

        By saveBtn = By.cssSelector("button[onclick*='UI.savePrice']");
        if (driver.findElements(saveBtn).isEmpty()) {
            saveBtn = By.xpath("//button[contains(normalize-space(.), 'Kaydet')]");
        }
        click(saveBtn);

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> {
            String box = safeText(d, By.id("pricingBox"));
            if (box.contains(String.valueOf(price))) return true;
            return isErrorLike(safeText(d, outLoc));
        });

        String box = safeText(By.id("pricingBox"));
        if (!box.contains(String.valueOf(price))) {
            fail("Price kaydedilemedi. ownerOut=" + safeText(outLoc) + " pricingBox=" + box);
        }
    }

    // ---------- Member flows ----------

    protected void memberSelectFacilityAndPitch(String facilityName, String pitchName) {
        selectByContainsText(By.id("facilitySel"), facilityName);

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> {
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
            boolean disabled;
            try { disabled = !b.isEnabled(); } catch (Exception e) { disabled = true; }
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
