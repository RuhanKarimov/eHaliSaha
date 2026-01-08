from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from _helpers import base_url, new_driver, wait_for_app, wait_visible


def _login_member(driver):
    driver.get(base_url() + "/ui/login.html?role=MEMBER")
    u = wait_visible(driver, By.ID, "u")
    p = wait_visible(driver, By.ID, "p")
    u.clear(); u.send_keys("member1")
    p.clear(); p.send_keys("member123")
    driver.find_element(By.ID, "btn").click()
    wait_visible(driver, By.ID, "memberOut", timeout=20)


def _select_first_non_empty(select_el):
    sel = Select(select_el)
    for opt in sel.options:
        v = (opt.get_attribute("value") or "").strip()
        if v and v.lower() not in ("null", "undefined"):
            sel.select_by_value(v)
            return v
    sel.select_by_index(0)
    return sel.first_selected_option.get_attribute("value")


def test_member_can_send_membership_request():
    wait_for_app()

    driver = new_driver()
    try:
        _login_member(driver)

        fac_sel = wait_visible(driver, By.ID, "facilitySel", timeout=20)
        _select_first_non_empty(fac_sel)

        pitch_sel = wait_visible(driver, By.ID, "pitchSel", timeout=20)
        _select_first_non_empty(pitch_sel)

        driver.find_element(By.ID, "btnMembership").click()

        out = wait_visible(driver, By.ID, "memberOut", timeout=20)
        assert "Üyelik" in out.text and ("gönderildi" in out.text.lower() or "istek" in out.text.lower() or "✅" in out.text)
    finally:
        driver.quit()
