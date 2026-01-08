package com.ornek.ehalisaha;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("E2E UI test is executed in Jenkins Stage 6 via docker-compose + selenium")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UiSeleniumE2E {

    @LocalServerPort
    int port;

    @Container
    static BrowserWebDriverContainer<?> chrome =
            new BrowserWebDriverContainer<>()
                    .withCapabilities(new ChromeOptions());

    @Test
    void uiHealthCheckShouldShowUp() {
        // ✅ import yok, tam isimle çağır
        org.testcontainers.Testcontainers.exposeHostPorts(port);

        WebDriver driver = chrome.getWebDriver(); // RemoteWebDriver kurmana gerek yok
        try {
            String url = "http://host.testcontainers.internal:" + port + "/ui/index.html";
            driver.get(url);

            // Burada index.html'de btn yoksa login.html'e gitmelisin
            // driver.get("http://host.testcontainers.internal:" + port + "/ui/login.html");

            driver.findElement(By.id("btn")).click();
            String out = driver.findElement(By.id("out")).getText();

            assertTrue(out.contains("UP"));
        } finally {
            driver.quit();
        }
    }
}
