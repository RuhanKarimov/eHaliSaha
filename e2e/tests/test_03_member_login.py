from selenium.webdriver.common.by import By

from _helpers import base_url, new_driver, wait_for_app, wait_url_contains, wait_visible


def test_member_can_login_and_reach_member_panel():
    wait_for_app()

    driver = new_driver()
    try:
        driver.get(base_url() + "/ui/login.html?role=MEMBER")

        u = wait_visible(driver, By.ID, "u")
        p = wait_visible(driver, By.ID, "p")

        u.clear(); u.send_keys("member1")
        p.clear(); p.send_keys("member123")

        driver.find_element(By.ID, "btn").click()
        wait_url_contains(driver, "/ui/member.html", timeout=20)

        out = wait_visible(driver, By.ID, "memberOut", timeout=20)
        assert "Hazır" in out.text
    finally:
        driver.quit()
