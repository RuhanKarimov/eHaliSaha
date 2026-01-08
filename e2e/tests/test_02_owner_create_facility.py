import time

from selenium.webdriver.common.by import By

from _helpers import base_url, new_driver, wait_for_app, wait_url_contains, wait_visible, click_by_text


def test_owner_can_create_facility():
    wait_for_app()

    driver = new_driver()
    try:
        driver.get(base_url() + "/ui/login.html?role=OWNER")

        u = wait_visible(driver, By.ID, "u")
        p = wait_visible(driver, By.ID, "p")

        u.clear(); u.send_keys("owner1")
        p.clear(); p.send_keys("owner123")
        driver.find_element(By.ID, "btn").click()

        wait_url_contains(driver, "/ui/owner.html", timeout=20)

        # yeni facility oluştur
        fac_name = wait_visible(driver, By.ID, "facName", timeout=20)
        fac_addr = wait_visible(driver, By.ID, "facAddr", timeout=20)

        uniq = int(time.time())
        fac_name.clear(); fac_name.send_keys(f"CI Facility {uniq}")
        fac_addr.clear(); fac_addr.send_keys("CI Address")

        click_by_text(driver, "button", "Facility oluştur", timeout=20)

        out = wait_visible(driver, By.ID, "ownerOut", timeout=20)
        assert "Facility" in out.text
        assert "oluştur" in out.text.lower() or "✅" in out.text

    finally:
        driver.quit()
