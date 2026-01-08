from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select
from selenium.webdriver.support.ui import WebDriverWait

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


def _click_first_available_slot(driver):
    grid = wait_visible(driver, By.ID, "slotGrid", timeout=25)
    # butonlar JS ile geliyor, biraz bekle
    WebDriverWait(driver, 25).until(lambda d: len(d.find_elements(By.CSS_SELECTOR, "#slotGrid button")) > 0)
    buttons = driver.find_elements(By.CSS_SELECTOR, "#slotGrid button")
    for b in buttons:
        if b.is_enabled() and "BOŞ" in b.text.upper():
            b.click()
            return True
    # hiç boş yoksa en azından enabled olanı tıkla
    for b in buttons:
        if b.is_enabled():
            b.click()
            return True
    return False


def test_member_can_make_reservation_after_approval():
    wait_for_app()

    driver = new_driver()
    try:
        _login_member(driver)

        fac_sel = wait_visible(driver, By.ID, "facilitySel", timeout=20)
        _select_first_non_empty(fac_sel)

        pitch_sel = wait_visible(driver, By.ID, "pitchSel", timeout=20)
        _select_first_non_empty(pitch_sel)

        assert _click_first_available_slot(driver), "Seçilebilir slot bulunamadı"

        # Oyuncu ekle: hızlı doldur
        driver.find_element(By.ID, "btnFillPlayers").click()

        # Ödeme yöntemi: CARD seç (varsa)
        pay_sel = wait_visible(driver, By.ID, "paySel", timeout=20)
        pay = Select(pay_sel)
        if any((o.get_attribute("value") or "") == "CARD" for o in pay.options):
            pay.select_by_value("CARD")

        # Shuttle: YES seç (varsa)
        shut_sel = wait_visible(driver, By.ID, "shuttleSel", timeout=20)
        shut = Select(shut_sel)
        if any((o.get_attribute("value") or "") == "YES" for o in shut.options):
            shut.select_by_value("YES")

        driver.find_element(By.ID, "btnReserve").click()

        out = wait_visible(driver, By.ID, "memberOut", timeout=25)
        assert "Rezervasyon alındı" in out.text
    finally:
        driver.quit()
