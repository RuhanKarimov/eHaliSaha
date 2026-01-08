from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.webdriver.support.ui import WebDriverWait

from _helpers import base_url, new_driver, wait_for_app, wait_visible


def _login_owner(driver):
    driver.get(base_url() + "/ui/login.html?role=OWNER")
    u = wait_visible(driver, By.ID, "u")
    p = wait_visible(driver, By.ID, "p")
    u.clear(); u.send_keys("owner1")
    p.clear(); p.send_keys("owner123")
    driver.find_element(By.ID, "btn").click()
    wait_visible(driver, By.ID, "ownerOut", timeout=20)


def _select_first_non_empty(select_el):
    sel = Select(select_el)
    for opt in sel.options:
        v = (opt.get_attribute("value") or "").strip()
        if v and v.lower() not in ("null", "undefined"):
            sel.select_by_value(v)
            return v
    sel.select_by_index(0)
    return sel.first_selected_option.get_attribute("value")


def test_owner_can_approve_membership_request():
    wait_for_app()

    driver = new_driver()
    try:
        _login_owner(driver)

        # Facility seç
        fac_sel = wait_visible(driver, By.ID, "ownerFacilitySel", timeout=20)
        _select_first_non_empty(fac_sel)

        # Requests kutusunda approve butonu gelene kadar bekle
        WebDriverWait(driver, 20).until(lambda d: len(d.find_elements(By.CSS_SELECTOR, "#reqBox [data-a='approve']")) > 0)
        approve_btn = driver.find_elements(By.CSS_SELECTOR, "#reqBox [data-a='approve']")[0]
        approve_btn.click()

        out = wait_visible(driver, By.ID, "ownerOut", timeout=20)
        assert "onaylandı" in out.text.lower() or "✅" in out.text
    finally:
        driver.quit()
