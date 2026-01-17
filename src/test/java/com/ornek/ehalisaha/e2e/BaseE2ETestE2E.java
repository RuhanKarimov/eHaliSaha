package com.ornek.ehalisaha.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
        String v = cfg("BASE_URL", "E2E_BASE_URL", "e2e.baseUrl", "e2e.base_url");
        if (v != null) return v;

        String mode = cfg("E2E_MODE", "e2e.mode");
        if (mode != null && mode.equalsIgnoreCase("host")) {
            String hostPort = cfg("APP_HOST_PORT", "e2e.appHostPort");
            if (hostPort == null) hostPort = "18080";
            return "http://host.docker.internal:" + hostPort;
        }
        return "http://host.docker.internal:18080";
    }

    protected String seleniumUrl() {
        String v = cfg("SELENIUM_URL", "E2E_SELENIUM_URL", "e2e.seleniumUrl", "e2e.selenium_url");
        if (v != null) return v;

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

        URL gridUrl = URI.create(seleniumUrl()).toURL();
        driver = new RemoteWebDriver(gridUrl, options);

        wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        wait.pollingEvery(Duration.ofMillis(250));
    }

    @AfterEach
    void tearDown(TestInfo info) {
        String testName = info.getTestClass().map(Class::getSimpleName).orElse("Test")
                + "_" + info.getTestMethod().map(Method::getName).orElse("method");
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
                    Files.writeString(p, html == null ? "" : html, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}

                // debug txt
                try {
                    String url = safeText(() -> driver.getCurrentUrl());
                    String ownerOut = safeText(By.id("ownerOut"));
                    Object jsErr = safeJsErr();
                    Object lastNet = safeLastNet();

                    Path p = dir.resolve(testName + ".txt");
                    Files.writeString(p,
                            "url=" + url + "\n" +
                                    "BASE_URL=" + baseUrl() + "\n" +
                                    "SELENIUM_URL=" + seleniumUrl() + "\n" +
                                    "ownerOut=" + ownerOut + "\n" +
                                    "lastNet=" + String.valueOf(lastNet) + "\n" +
                                    "jsErr=" + String.valueOf(jsErr) + "\n",
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
        assertNotNull(el);

        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});", el
            );
        } catch (Exception ignored) {}

        // daha sağlam temizle
        try { el.clear(); } catch (Exception ignored) {
            try { ((JavascriptExecutor) driver).executeScript("arguments[0].value='';", el); } catch (Exception ignored2) {}
        }

        try { el.sendKeys(text); } catch (Exception ignored) {}

        // yazı gerçekten input.value içine girdi mi?
        String v = "";
        try { v = Optional.ofNullable(el.getAttribute("value")).orElse(""); } catch (Exception ignored) {}

        if (!v.contains(text)) {
            // JS fallback + input/change event
            ((JavascriptExecutor) driver).executeScript(
                    "const el=arguments[0], val=arguments[1];" +
                            "el.value=val;" +
                            "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                            "el.dispatchEvent(new Event('change',{bubbles:true}));",
                    el, text
            );
        }

        String finalVal = "";
        try { finalVal = Optional.ofNullable(el.getAttribute("value")).orElse(""); } catch (Exception ignored) {}
        System.out.println("TYPE: " + locator + " value='" + finalVal + "'");
    }


    /**
     * Stabil click:
     * - visible + enabled
     * - scroll into view
     * - native click
     * - Actions fallback
     * - JS click fallback
     */
    protected void click(By locator) {
        installClickSpyOnce();

        RuntimeException last = null;

        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                // her attempt'te elementi tekrar bul (stale / re-render olabiliyor)
                WebElement el = new WebDriverWait(driver, Duration.ofSeconds(20))
                        .pollingEvery(Duration.ofMillis(200))
                        .until(ExpectedConditions.presenceOfElementLocated(locator));

                // görünür + enabled bekle
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .pollingEvery(Duration.ofMillis(200))
                        .until(d -> {
                            try {
                                return el.isDisplayed() && el.isEnabled();
                            } catch (Exception e) {
                                return false;
                            }
                        });

                // merkeze getir
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({block:'center', inline:'center'});", el);
                } catch (Exception ignored) {}

                // üstünde overlay var mı? (center noktasında gerçekten bu element mi üstte)
                boolean topOk = Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                        "const el = arguments[0];" +
                                "const r = el.getBoundingClientRect();" +
                                "const x = r.left + r.width/2;" +
                                "const y = r.top + r.height/2;" +
                                "const top = document.elementFromPoint(x, y);" +
                                "return top === el || (el.contains(top));",
                        el
                ));
                if (!topOk) {
                    // küçük bir scroll düzeltmesi bazen overlay/header çakışmasını çözer
                    try { ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -80);"); } catch (Exception ignored) {}
                    // tekrar dene
                    throw new RuntimeException("Element center point covered (overlay/header?)");
                }

                String expectedId = "";
                try { expectedId = Optional.ofNullable(el.getAttribute("id")).orElse(""); } catch (Exception ignored) {}

                long clickBefore = clickSpyCount();

                // 1) Önce Actions (grid/headless'ta en “gerçek” click budur)
                try {
                    new Actions(driver)
                            .moveToElement(el)
                            .pause(Duration.ofMillis(40))
                            .click()
                            .perform();
                } catch (Exception e1) {
                    // 2) sonra native click
                    el.click();
                }

                // Click event gerçekten geldi mi + doğru elemana mı geldi?
                String finalExpectedId = expectedId;
                boolean ok = new WebDriverWait(driver, Duration.ofSeconds(2))
                        .pollingEvery(Duration.ofMillis(100))
                        .until(d -> {
                            if (clickSpyCount() <= clickBefore) return false;

                            String id = "";
                            try {
                                Object o = ((JavascriptExecutor) d).executeScript(
                                        "return (window.__e2eLastClick && window.__e2eLastClick.id) || '';");
                                id = o == null ? "" : o.toString();
                            } catch (Exception ignored) {}

                            // id varsa birebir match iste (btnCreateFacility gibi)
                            if (finalExpectedId != null && !finalExpectedId.isBlank()) {
                                return finalExpectedId.equals(id);
                            }
                            // id yoksa sadece click geldi mi yeter
                            return true;
                        });

                if (!ok) throw new RuntimeException("Click event did not land on expected element");

                System.out.println("CLICK: " + locator);
                return;

            } catch (RuntimeException ex) {
                last = ex;
                try { Thread.sleep(150L * attempt); } catch (InterruptedException ignored) {}
            }
        }

        // en sonda debug bas
        String topInfo = "";
        try {
            topInfo = String.valueOf(((JavascriptExecutor) driver).executeScript(
                    "try{" +
                            " const el=arguments[0];" +
                            " const r=el.getBoundingClientRect();" +
                            " const x=r.left + r.width/2;" +
                            " const y=r.top + r.height/2;" +
                            " const top=document.elementFromPoint(x,y);" +
                            " if(!top) return 'top=null';" +
                            " return 'top=' + top.tagName + '#' + (top.id||'') + ' class=' + (top.className||'');" +
                            "}catch(e){return 'topInfoEx:' + e;}",
                    driver.findElement(locator)
            ));
        } catch (Exception ignored) {}

        fail("CLICK failed after retries: " + locator +
                "\nlastClick=" + String.valueOf(lastClickSpy()) +
                "\n" + topInfo);
    }


    protected void installClickSpyOnce() {
        ((JavascriptExecutor) driver).executeScript(
                "if (window.__e2eClickSpyInstalled) return;" +
                        "window.__e2eClickSpyInstalled=true;" +
                        "window.__e2eClickCount=0;" +
                        "window.__e2eLastClick=null;" +
                        "document.addEventListener('click', function(ev){" +
                        "  try{" +
                        "    window.__e2eClickCount++;" +
                        "    var t = ev && ev.target ? ev.target : null;" +
                        "    var a = null;" +
                        "    if (t && t.closest) a = t.closest('button,a,input,[role=button]');" +
                        "    var k = a || t;" +
                        "    window.__e2eLastClick={" +
                        "      ts:Date.now()," +
                        "      id:(k && k.id) ? String(k.id) : ''," +
                        "      tag:(k && k.tagName) ? String(k.tagName) : ''," +
                        "      trusted: !!(ev && ev.isTrusted)" +
                        "    };" +
                        "  }catch(e){}" +
                        "}, true);"
        );
    }


    protected long clickSpyCount() {
        Object n = ((JavascriptExecutor) driver).executeScript("return window.__e2eClickCount || 0;");
        return (n instanceof Number) ? ((Number)n).longValue() : 0L;
    }

    protected Object lastClickSpy() {
        return ((JavascriptExecutor) driver).executeScript("return window.__e2eLastClick;");
    }


    // --------- safeText / error-like ----------

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
        String t = s.trim().toLowerCase(Locale.ROOT);

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

    // --------- UI loaded check ----------

    protected void assertUiLoaded() {
        String s = (String) ((JavascriptExecutor) driver).executeScript(
                "return (function(){" +
                        "  const hasUI = (typeof UI !== 'undefined') || (window && window.UI);" +
                        "  const ui = (typeof UI !== 'undefined') ? UI : (window ? window.UI : undefined);" +
                        "  const hasCreate = !!(ui && typeof ui.createFacility === 'function');" +
                        "  const hasEH = (typeof EH !== 'undefined') || (window && window.EH);" +
                        "  const eh = (typeof EH !== 'undefined') ? EH : (window ? window.EH : undefined);" +
                        "  const hasAPI = !!(eh && eh.API);" +
                        "  return 'UI=' + hasUI + ', create=' + hasCreate + ', EH=' + hasEH + ', API=' + hasAPI;" +
                        "})();"
        );

        System.out.println("JS_STATE=" + s);

        if (!s.contains("UI=true") || !s.contains("create=true")) {
            fail("UI scriptleri yüklenmemiş/bozulmuş görünüyor. " + s);
        }
    }


    // ---------- OUT assertions (geri uyumlu) ----------

    /**
     * Geri uyumlu: Eski testler sadece assertOutContains("...") çağırıyor.
     * Bu metod; sırayla memberOut -> ownerOut -> genel out id'leri içinde arar.
     */
    protected void assertOutContains(String expected) {
        assertOutContainsAny(expected, "memberOut", "ownerOut", "out", "msg", "result");
    }

    /** Sadece memberOut kontrolü istiyorsan */
    protected void assertMemberOutContains(String expected) {
        assertOutContainsAny(expected, "memberOut");
    }

    /** Sadece ownerOut kontrolü istiyorsan */
    protected void assertOwnerOutContains(String expected) {
        assertOutContainsAny(expected, "ownerOut");
    }

    private void assertOutContainsAny(String expected, String... outIds) {
        if (expected == null) expected = "";
        final String exp = expected.trim();
        if (exp.isEmpty()) fail("assertOutContains: expected boş olamaz");

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(250));

        // Out element(ler)i sayfada var mı? En az biri görünsün/present olsun
        boolean anyPresent = false;
        for (String id : outIds) {
            if (id == null || id.isBlank()) continue;
            if (!driver.findElements(By.id(id)).isEmpty()) { anyPresent = true; break; }
        }
        if (!anyPresent) {
            fail("Sayfada out alanı bulunamadı. aranan id'ler=" + String.join(",", outIds));
        }

        // Bekle: beklenen metin gelsin veya error-like çıksın
        try {
            w.until(d -> {
                for (String id : outIds) {
                    if (id == null || id.isBlank()) continue;
                    String t = safeText(d, By.id(id));
                    if (t != null && t.contains(exp)) return true;
                    if (isErrorLike(t)) return true; // erken çıkıp altta fail mesajı basacağız
                }
                return false;
            });
        } catch (TimeoutException ignored) {}

        // Final kontrol + debug
        StringBuilder dbg = new StringBuilder();
        boolean ok = false;
        for (String id : outIds) {
            if (id == null || id.isBlank()) continue;
            String t = safeText(By.id(id));
            dbg.append(id).append("='").append(t).append("' ");
            if (t != null && t.contains(exp)) ok = true;
        }

        if (!ok) {
            fail("Beklenen çıktı bulunamadı. expected='" + exp + "' " + dbg);
        }
    }



    // ---------- option helpers ----------

    protected void selectByContainsText(By selectLocator, String containsText) {
        WebElement sel = wait.until(ExpectedConditions.visibilityOfElementLocated(selectLocator));
        assertNotNull(sel);

        Select s = new Select(sel);
        for (WebElement opt : s.getOptions()) {
            String t = opt.getText();
            if (t != null && t.contains(containsText)) {
                s.selectByVisibleText(t);
                return;
            }
        }
        fail("Option not found containing: " + containsText + " options=" + dumpOptions(selectLocator));
    }

    protected boolean hasOptionContaining(By selectBy, String contains) {
        return hasOptionContaining(driver, selectBy, contains);
    }

    protected boolean hasOptionContaining(WebDriver d, By selectBy, String contains) {
        try {
            WebElement el = d.findElement(selectBy);
            Select sel = new Select(el);
            String want = fold(contains);

            for (WebElement opt : sel.getOptions()) {
                String t = opt.getText();

                // debug istersen açık kalsın
                System.out.println("hasOptionContainingFold: " + fold(t));
                System.out.println("containsRaw: " + contains);
                System.out.println("containsCP : " + codepoints(contains));
                System.out.println("optRaw     : " + t);
                System.out.println("optCP      : " + codepoints(t));

                if (fold(t).contains(want)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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

    private static String fold(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFKC);

        x = x.replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2007', ' ');

        x = x.replace('\uFFFD', 'i');

        x = x.replace('ı', 'i').replace('İ', 'I');

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

    // ---------- Refresh ----------

    protected void tryRefreshAll() {
        // 1) Önce UI.refreshAll()'ı bekleyerek çalıştır (en stabil)
        try {
            String r = refreshAllAndWait();
            if ("OK".equals(r)) return;
        } catch (Exception ignored) {}

        // 2) Fallback: eski buton click yolu
        By btnRefresh = By.cssSelector("button[onclick*='UI.refreshAll']");
        if (!driver.findElements(btnRefresh).isEmpty()) {
            try { click(btnRefresh); } catch (Exception ignored) {}
        }
    }

    protected String refreshAllAndWait() {
        Object res = ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[arguments.length-1];" +
                        "try {" +
                        "  const ui = (typeof UI !== 'undefined') ? UI : (window ? window.UI : null);" +
                        "  if (!ui || typeof ui.refreshAll !== 'function') return done('NO_UI_REFRESH');" +
                        "  Promise.resolve(ui.refreshAll())" +
                        "    .then(()=>done('OK'))" +
                        "    .catch(e=>done('ERR:' + (e && e.message ? e.message : String(e))));" +
                        "} catch(e) { done('EX:' + String(e)); }"
        );

        String s = String.valueOf(res);
        System.out.println("UI.refreshAll() => " + s);
        return s;
    }



    // ---------- login ----------

    protected void loginOwner() {
        login("OWNER", "owner1", "owner123", "/ui/owner.html");
        assertUiLoaded();
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

        wait.until(d -> Objects.requireNonNull(d.getCurrentUrl()).contains(expectedPath));
        waitForDocumentReady();
    }

    // ---------- Net Spy (fetch + XHR) + JS error capture ----------

    protected void installNetSpyOnce() {
        ((JavascriptExecutor) driver).executeScript(
                "if (window.__e2eNetSpyInstalled) return;" +
                        "window.__e2eNetSpyInstalled = true;" +

                        // net
                        "window.__e2eNetCount = 0;" +
                        "window.__e2eLastNet = null;" +

                        // js error
                        "window.__e2eLastJsError = null;" +

                        // click spy
                        "window.__e2eClickCount = 0;" +
                        "window.__e2eLastClick = null;" +

                        // UI.createFacility spy
                        "window.__e2eCreateFacilityCalls = 0;" +

                        "window.addEventListener('error', function(e){" +
                        "  try{ window.__e2eLastJsError = {ts:Date.now(), msg:String(e.message||e.error||e), src:String(e.filename||''), line:e.lineno||0, col:e.colno||0}; }catch(_){}" +
                        "});" +
                        "window.addEventListener('unhandledrejection', function(e){" +
                        "  try{ window.__e2eLastJsError = {ts:Date.now(), msg:'unhandledrejection: ' + String(e.reason && (e.reason.message||e.reason) || e.reason)}; }catch(_){}" +
                        "});" +

                        // global click listener (capture)
                        "document.addEventListener('click', function(ev){" +
                        "  try{" +
                        "    window.__e2eClickCount++;" +
                        "    const t = ev && ev.target ? ev.target : null;" +
                        "    const id = t && t.id ? String(t.id) : '';" +
                        "    const tag = t && t.tagName ? String(t.tagName) : '';" +
                        "    window.__e2eLastClick = {ts:Date.now(), id:id, tag:tag, trusted: !!(ev && ev.isTrusted)};" +
                        "  }catch(_){}" +
                        "}, true);" +

                        // wrap UI.createFacility once it exists
                        "try{" +
                        "  const ui = (typeof UI !== 'undefined') ? UI : (window ? window.UI : null);" +
                        "  if (ui && typeof ui.createFacility === 'function' && !ui.__e2eWrappedCreateFacility) {" +
                        "    const orig = ui.createFacility;" +
                        "    ui.__e2eWrappedCreateFacility = true;" +
                        "    ui.createFacility = function(){" +
                        "      try{ window.__e2eCreateFacilityCalls = (window.__e2eCreateFacilityCalls||0) + 1; }catch(_){}" +
                        "      return orig.apply(this, arguments);" +
                        "    };" +
                        "  }" +
                        "}catch(_){}" +

                        // fetch hook (Request obj ihtimali dahil)
                        "if (window.fetch) {" +
                        "  const _fetch = window.fetch;" +
                        "  window.fetch = function() {" +
                        "    try {" +
                        "      window.__e2eNetCount++;" +
                        "      const u = arguments[0];" +
                        "      const opt = arguments[1] || {};" +
                        "      let urlStr = '';" +
                        "      if (typeof u === 'string') urlStr = u;" +
                        "      else if (u && typeof u.url === 'string') urlStr = u.url;" +
                        "      else urlStr = String(u);" +
                        "      window.__e2eLastNet = {ts:Date.now(), kind:'fetch', url:String(urlStr), method:String(opt.method||'GET')};" +
                        "    } catch(e) {}" +
                        "    return _fetch.apply(this, arguments);" +
                        "  };" +
                        "}" +

                        // XHR hook
                        "(function(){" +
                        "  const XHR = window.XMLHttpRequest;" +
                        "  if (!XHR) return;" +
                        "  const _open = XHR.prototype.open;" +
                        "  const _send = XHR.prototype.send;" +
                        "  XHR.prototype.open = function(method, url) {" +
                        "    try { this.__e2eMethod = method; this.__e2eUrl = url; } catch(e) {}" +
                        "    return _open.apply(this, arguments);" +
                        "  };" +
                        "  XHR.prototype.send = function() {" +
                        "    try {" +
                        "      window.__e2eNetCount++;" +
                        "      window.__e2eLastNet = {ts:Date.now(), kind:'xhr', url:String(this.__e2eUrl||''), method:String(this.__e2eMethod||'GET')};" +
                        "    } catch(e) {}" +
                        "    return _send.apply(this, arguments);" +
                        "  };" +
                        "})();"
        );
    }

    protected long netSpyCount() {
        try {
            Object n = ((JavascriptExecutor) driver).executeScript("return window.__e2eNetCount || 0;");
            return (n instanceof Number) ? ((Number) n).longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    protected String lastNetUrl() {
        try {
            Object u = ((JavascriptExecutor) driver).executeScript("return window.__e2eLastNet && window.__e2eLastNet.url;");
            return u == null ? "" : u.toString();
        } catch (Exception e) {
            return "";
        }
    }

    protected Object safeLastNet() {
        try {
            return ((JavascriptExecutor) driver).executeScript("return window.__e2eLastNet;");
        } catch (Exception e) {
            return null;
        }
    }

    protected Object safeJsErr() {
        try {
            return ((JavascriptExecutor) driver).executeScript("return window.__e2eLastJsError;");
        } catch (Exception e) {
            return null;
        }
    }

    protected void clickAndAssertNet(By locator, String mustContainUrlPart) {
        installNetSpyOnce();
        installClickSpyOnce();

        long netBefore = netSpyCount();
        long clickBefore = clickSpyCount();
        String outBefore = safeText(By.id("ownerOut"));

        click(locator);

        // 1) Click event gerçekten üretildi mi?
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(200))
                .until(d -> clickSpyCount() > clickBefore);

        System.out.println("CLICK_SPY=" + lastClickSpy());

        // 2) Sonrasında net/out/jsErr’den biri gelsin
        try {
            new WebDriverWait(driver, Duration.ofSeconds(60))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> {
                        if (netSpyCount() > netBefore) return true;

                        String outNow = safeText(d, By.id("ownerOut"));
                        if (outNow != null && !outNow.trim().isEmpty() && !outNow.equals(outBefore)) return true;

                        return safeJsErr() != null;
                    });
        } catch (TimeoutException te) {
            fail("clickAndAssertNet timeout!\n" +
                    "expected~" + mustContainUrlPart + "\n" +
                    "lastNet=" + safeLastNet() + "\n" +
                    "jsErr=" + safeJsErr() + "\n" +
                    "ownerOut=" + safeText(By.id("ownerOut")) + "\n" +
                    "lastClick=" + lastClickSpy());
        }

        // Net olduysa URL kontrolünü yine yap
        if (mustContainUrlPart != null && !mustContainUrlPart.isBlank()) {
            String url = lastNetUrl();
            if (url != null && !url.isBlank() && !url.contains(mustContainUrlPart)) {
                fail("Net oldu ama beklenen endpoint değil. expected~" + mustContainUrlPart + " actual=" + url);
            }
        }

        System.out.println("LAST_NET_URL=" + lastNetUrl());
    }

    protected void jsClick(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});", el
            );
        } catch (Exception ignored) {}
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        System.out.println("JS_CLICK: " + locator);
    }

    protected long uiCreateFacilityCalls() {
        try {
            Object n = ((JavascriptExecutor) driver).executeScript("return window.__e2eCreateFacilityCalls || 0;");
            return (n instanceof Number) ? ((Number) n).longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private boolean waitAnySignal(long netBefore, long clickBefore, long createBefore, By outLoc, String outBefore, Duration timeout) {
        WebDriverWait w = new WebDriverWait(driver, timeout);
        w.pollingEvery(Duration.ofMillis(200));
        try {
            return w.until(d -> {
                if (netSpyCount() > netBefore) return true;
                if (clickSpyCount() > clickBefore) return true;
                if (uiCreateFacilityCalls() > createBefore) return true;

                String out = safeText(d, outLoc);
                if (out != null) {
                    String t = out.trim();
                    if (!t.isEmpty() && !t.equals(outBefore)) return true;
                }
                return safeJsErr() != null;
            });
        } catch (TimeoutException te) {
            return false;
        }
    }


    protected String safeAttr(By by, String attr) {
        try {
            WebElement el = driver.findElement(by);
            String v = el.getAttribute(attr);
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }


    // ---------- Debug fallback (normal akışta kullanma) ----------

    protected void callUiCreateFacilityDirect() {
        Object res = ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[arguments.length-1];" +
                        "try {" +
                        "  const ui = (typeof UI !== 'undefined') ? UI : (window ? window.UI : null);" +
                        "  if (!ui || !ui.createFacility) return done('NO_UI_CREATE');" +
                        "  Promise.resolve(ui.createFacility())" +
                        "    .then(()=>done('OK'))" +
                        "    .catch(e=>done('ERR:' + (e && e.message ? e.message : String(e))));" +
                        "} catch(e) { done('EX:' + String(e)); }"
        );
        System.out.println("UI.createFacility() => " + res);
    }

    // ---------- Owner flows ----------

    protected void ensureFacilityExists(String name) {
        By selLoc = By.id("ownerFacilitySel");
        By outLoc = By.id("ownerOut");
        By btnCreate = By.id("btnCreateFacility");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facName")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("facAddr")));
        wait.until(ExpectedConditions.presenceOfElementLocated(selLoc));
        wait.until(ExpectedConditions.presenceOfElementLocated(outLoc));
        wait.until(ExpectedConditions.presenceOfElementLocated(btnCreate));

        tryRefreshAll();

        if (hasOptionContaining(selLoc, name)) {
            selectByContainsText(selLoc, name);
            return;
        }

        type(By.id("facName"), name);
        type(By.id("facAddr"), "Merkez / Malatya");

        String outBefore = safeText(outLoc);
        try {
            WebElement outEl = driver.findElement(outLoc);
            ((JavascriptExecutor) driver).executeScript("arguments[0].textContent='';", outEl);
            outBefore = "";
        } catch (Exception ignored) {}

        // TEK TETİKLEME: sadece tıkla ve net çağrı geldi mi doğrula
// --- robust trigger: click -> jsClick -> direct UI call ---
        installNetSpyOnce(); // spy’lar + net hooklar

        long netBefore = netSpyCount();
        long clickBefore = clickSpyCount();
        long createBefore = uiCreateFacilityCalls();
        String outBefore2 = safeText(outLoc);

        System.out.println("DEBUG btnCreate onclick.attr=" +
                safeText(() -> driver.findElement(btnCreate).getAttribute("onclick")) +
                " enabled=" + safeText(() -> String.valueOf(driver.findElement(btnCreate).isEnabled())));

        click(btnCreate);

// 1) normal click sonrası bir sinyal var mı?
        boolean ok = waitAnySignal(netBefore, clickBefore, createBefore, outLoc, outBefore2, Duration.ofSeconds(4));

        if (!ok) {
            System.out.println("WARN: normal click tetiklemedi. lastClick=" + String.valueOf(lastClickSpy())
                    + " lastNet=" + String.valueOf(safeLastNet())
                    + " jsErr=" + String.valueOf(safeJsErr()));

            // 2) JS click dene
            jsClick(btnCreate);
            ok = waitAnySignal(netBefore, clickBefore, createBefore, outLoc, outBefore2, Duration.ofSeconds(4));
        }

        if (!ok) {
            System.out.println("WARN: jsClick de tetiklemedi. UI.createFacility() direkt çağrılıyor...");
            // 3) En garanti: direkt fonksiyon
            callUiCreateFacilityDirect();
        }

        WebDriverWait apiWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        apiWait.pollingEvery(Duration.ofMillis(250));

        try {
            String finalOutBefore = outBefore;
            apiWait.until(d -> {
                if (hasOptionContaining(d, selLoc, name)) return true;

                String out = safeText(d, outLoc);
                if (out != null) out = out.trim();
                if (out != null && !out.isBlank() && !out.equals(finalOutBefore)) return true;

                Object jsErr = safeJsErr();
                return jsErr != null;
            });
        } catch (TimeoutException te) {
            fail("Facility create sonrası UI ilerlemedi.\n" +
                    "ownerOut=" + safeText(outLoc) + "\n" +
                    "lastNetUrl=" + lastNetUrl() + "\n" +
                    "jsErr=" + safeJsErr() + "\n" +
                    "options=" + dumpOptions(selLoc));
        }

        // Bazen liste geç güncellenir
        tryRefreshAll();

        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        longWait.pollingEvery(Duration.ofMillis(300));
        longWait.until(d -> hasOptionContaining(d, selLoc, name) || isErrorLike(safeText(d, outLoc)) || safeJsErr() != null);

        if (!hasOptionContaining(selLoc, name)) {
            fail("Facility dropdown'a düşmedi.\n" +
                    "ownerOut=" + safeText(outLoc) + "\n" +
                    "lastNetUrl=" + lastNetUrl() + "\n" +
                    "jsErr=" + safeJsErr() + "\n" +
                    "options=" + dumpOptions(selLoc));
        }

        selectByContainsText(selLoc, name);

        String outNow = safeText(outLoc);
        if (isErrorLike(outNow)) {
            fail("Facility oluşturma hata verdi.\n" +
                    "ownerOut=" + outNow + "\n" +
                    "lastNetUrl=" + lastNetUrl() + "\n" +
                    "jsErr=" + safeJsErr());
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
            return (out != null && out.toLowerCase(Locale.ROOT).contains("slot")) || isErrorLike(out) || safeJsErr() != null;
        });

        String out = safeText(outLoc);
        if (isErrorLike(out)) fail("Slot kaydetme hata verdi. ownerOut=" + out + " jsErr=" + safeJsErr());
    }

    protected void ensurePitchExists() {
        By selLoc = By.id("ownerPitchSel");
        By outLoc = By.id("ownerOut");

        if (hasOptionContaining(selLoc, "Saha-1")) {
            selectByContainsText(selLoc, "Saha-1");
            return;
        }

        type(By.id("pitchName"), "Saha-1");

        By btn = By.cssSelector("button[onclick*='UI.createPitch']");
        if (driver.findElements(btn).isEmpty()) {
            btn = By.xpath("//button[normalize-space(.)='Ekle']");
        }

        click(btn);

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> hasOptionContaining(d, selLoc, "Saha-1") || isErrorLike(safeText(d, outLoc)) || safeJsErr() != null);

        if (!hasOptionContaining(selLoc, "Saha-1")) {
            fail("Pitch dropdown'a düşmedi. ownerOut=" + safeText(outLoc)
                    + " jsErr=" + safeJsErr()
                    + " options=" + dumpOptions(selLoc));
        }

        selectByContainsText(selLoc, "Saha-1");
    }

    protected void upsertPrice60() {
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
        type(priceBox, String.valueOf(250));

        By saveBtn = By.cssSelector("button[onclick*='UI.savePrice']");
        if (driver.findElements(saveBtn).isEmpty()) {
            saveBtn = By.xpath("//button[contains(normalize-space(.), 'Kaydet')]");
        }
        click(saveBtn);

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> {
            String box = safeText(d, By.id("pricingBox"));
            if (box.contains(String.valueOf(250))) return true;
            return isErrorLike(safeText(d, outLoc)) || safeJsErr() != null;
        });

        String box = safeText(By.id("pricingBox"));
        if (!box.contains(String.valueOf(250))) {
            fail("Price kaydedilemedi. ownerOut=" + safeText(outLoc) + " pricingBox=" + box + " jsErr=" + safeJsErr());
        }
    }

    // ---------- Member flows ----------

    protected void memberSelectFacilityAndPitch() {
        selectByContainsText(By.id("facilitySel"), "Arena Halısaha");

        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(45));
        w.pollingEvery(Duration.ofMillis(300));
        w.until(d -> {
            try {
                Select p = new Select(d.findElement(By.id("pitchSel")));
                for (WebElement opt : p.getOptions()) {
                    String t = opt.getText();
                    if (t != null && t.contains("Saha-1")) return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });

        selectByContainsText(By.id("pitchSel"), "Saha-1");
    }

    protected String pickFirstFreeSlotLabel() {
        WebElement grid = byId("slotGrid");
        List<WebElement> btns = grid.findElements(By.cssSelector("button"));

        if (btns.isEmpty()) fail("slotGrid içinde button yok");

        for (WebElement b : btns) {
            boolean disabled;
            try { disabled = !b.isEnabled(); } catch (Exception e) { disabled = true; }
            if (disabled) continue;

            String cls = Optional.ofNullable(b.getAttribute("class")).orElse("").toLowerCase(Locale.ROOT);
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
