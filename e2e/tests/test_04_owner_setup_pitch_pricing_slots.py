import time

from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

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


def test_owner_can_setup_slots_pitch_and_pricing():
    """Gerçekçi kurulum: facility seç, slot preset yap, pitch ekle, 60dk fiyat gir."""
    wait_for_app()

    driver = new_driver()
    try:
        _login_owner(driver)

        # Facility seçili olmalı
        fac_sel = wait_visible(driver, By.ID, "ownerFacilitySel", timeout=20)
        _select_first_non_empty(fac_sel)

        # Slot preset: day (08-23) + kaydet
        driver.find_element(By.XPATH, "//button[contains(@onclick, \"slotPreset('day')\")]").click()
        driver.find_element(By.XPATH, "//button[contains(@onclick, 'saveSlots')]").click()

        out = wait_visible(driver, By.ID, "ownerOut", timeout=20)
        assert "Slotlar kaydedildi" in out.text

        # Pitch ekle
        pitch_name = wait_visible(driver, By.ID, "pitchName", timeout=20)
        uniq = int(time.time())
        pitch_name.clear(); pitch_name.send_keys(f"CI Pitch {uniq}")
        driver.find_element(By.XPATH, "//button[contains(@onclick, 'createPitch')]").click()

        out = wait_visible(driver, By.ID, "ownerOut", timeout=20)
        assert "Pitch oluşturuldu" in out.text

        # Pitch dropdown dolu olmalı
        pitch_sel = wait_visible(driver, By.ID, "ownerPitchSel", timeout=20)
        _select_first_non_empty(pitch_sel)

        # 60 dk fiyat gir
        dur_sel = wait_visible(driver, By.ID, "priceDurationSel", timeout=20)
        # 60 dakikayı bul, yoksa ilkini seç
        dur = Select(dur_sel)
        if any((o.get_attribute("value") or "") == "60" for o in dur.options):
            dur.select_by_value("60")
        else:
            _select_first_non_empty(dur_sel)

        price = wait_visible(driver, By.ID, "priceValue", timeout=20)
        price.clear(); price.send_keys("250")

        driver.find_element(By.XPATH, "//button[contains(@onclick, 'upsertPricing')]").click()

        out = wait_visible(driver, By.ID, "ownerOut", timeout=20)
        assert "Fiyat kaydedildi" in out.text

        pricing_box = wait_visible(driver, By.ID, "pricingBox", timeout=20)
        assert "250" in pricing_box.text or "250" in driver.page_source
    finally:
        driver.quit()
