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


def test_owner_can_see_reservation_in_owner_reservations_page():
    """Owner gerçek kullanım: rezervasyonlar sayfasında member1 rezervasyonu listelenmeli."""
    wait_for_app()

    driver = new_driver()
    try:
        _login_owner(driver)

        # Rezervasyon ledger sayfasına git
        driver.get(base_url() + "/ui/owner-reservations.html")

        fac_sel = wait_visible(driver, By.ID, "facilitySel", timeout=20)
        _select_first_non_empty(fac_sel)

        pitch_sel = wait_visible(driver, By.ID, "pitchSel", timeout=20)
        _select_first_non_empty(pitch_sel)

        # Bugün + yenile
        driver.find_element(By.ID, "btnToday").click()
        driver.find_element(By.ID, "btnRefresh").click()

        # listBox dolsun
        WebDriverWait(driver, 25).until(lambda d: "member1" in d.page_source.lower() or len(d.find_elements(By.CSS_SELECTOR, "#listBox .card")) > 0)

        assert "member1" in driver.page_source.lower(), "member1 rezervasyonu listede görünmedi"
    finally:
        driver.quit()
