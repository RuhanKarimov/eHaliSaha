from selenium.webdriver.common.by import By

from _helpers import base_url, new_driver, wait_for_app, wait_url_contains, wait_visible


def test_owner_can_login_and_reach_owner_panel():
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

        # owner panelde temel bir bölüm görünmeli
        owner_out = wait_visible(driver, By.ID, "ownerOut", timeout=20)
        assert "Hazır" in owner_out.text or "Bildirim" in driver.page_source
    finally:
        driver.quit()
