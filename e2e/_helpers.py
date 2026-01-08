import os
import time
from urllib.parse import urljoin

import requests
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


def env(name: str, default: str | None = None) -> str:
    v = os.getenv(name, default)
    if v is None or str(v).strip() == "":
        raise RuntimeError(f"Missing env var: {name}")
    return v


def base_url() -> str:
    return env("BASE_URL", "http://app:8080")


def selenium_url() -> str:
    # selenium/standalone-chrome default endpoint
    return env("SELENIUM_URL", "http://selenium:4444/wd/hub")


def wait_for_app(timeout_s: int = 90) -> None:
    """Wait until Spring Boot actuator health is UP."""
    base = base_url().rstrip("/") + "/"
    health = urljoin(base, "actuator/health")

    t0 = time.time()
    last = None
    while time.time() - t0 < timeout_s:
        try:
            r = requests.get(health, timeout=3)
            last = f"{r.status_code} {r.text[:200]}"
            if r.status_code == 200 and '"status"' in r.text and "UP" in r.text:
                return
        except Exception as e:
            last = repr(e)

        time.sleep(2)

    raise AssertionError(f"App did not become healthy within {timeout_s}s. Last: {last}")


def new_driver(timeout_s: int = 60) -> webdriver.Remote:
    """Create remote Chrome driver (waits Selenium Grid to be ready)."""
    options = webdriver.ChromeOptions()
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")

    t0 = time.time()
    last = None
    while time.time() - t0 < timeout_s:
        try:
            return webdriver.Remote(command_executor=selenium_url(), options=options)
        except Exception as e:
            last = repr(e)
            time.sleep(2)

    raise AssertionError(f"Selenium did not become ready within {timeout_s}s. Last: {last}")


def wait_url_contains(driver, frag: str, timeout: int = 15) -> None:
    WebDriverWait(driver, timeout).until(lambda d: frag in d.current_url)


def wait_visible(driver, by, value, timeout: int = 15):
    return WebDriverWait(driver, timeout).until(EC.visibility_of_element_located((by, value)))


def click_by_text(driver, tag: str, text: str, timeout: int = 15):
    xpath = f"//{tag}[contains(normalize-space(.), '{text}')]"
    el = wait_visible(driver, By.XPATH, xpath, timeout=timeout)
    el.click()
    return el
