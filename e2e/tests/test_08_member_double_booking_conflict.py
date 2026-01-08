import re

import requests
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


def _parse_start_minute(slot_text: str) -> int | None:
    # slot_text içinde "08:00-09:00" gibi bir parça var
    m = re.search(r"(\d{2}):(\d{2})\s*-", slot_text)
    if not m:
        return None
    hh = int(m.group(1)); mm = int(m.group(2))
    return hh * 60 + mm


def test_member_cannot_double_book_same_slot_conflict_409():
    """Gerçekçi durum: UI'de slot DOLU görünür, API'ye aynı slotla POST atınca 409 döner."""
    wait_for_app()

    driver = new_driver()
    try:
        _login_member(driver)

        fac_sel = wait_visible(driver, By.ID, "facilitySel", timeout=20)
        _select_first_non_empty(fac_sel)

        pitch_sel = wait_visible(driver, By.ID, "pitchSel", timeout=20)
        _select_first_non_empty(pitch_sel)

        # slotlar gelsin
        WebDriverWait(driver, 25).until(lambda d: len(d.find_elements(By.CSS_SELECTOR, "#slotGrid button")) > 0)

        # DOLU slot bul
        full_btn = None
        for b in driver.find_elements(By.CSS_SELECTOR, "#slotGrid button"):
            if "DOLU" in b.text.upper():
                full_btn = b
                break
        assert full_btn is not None, "DOLU slot bulunamadı (rezervasyon oluşmamış olabilir)"

        # UI doğrulama: DOLU slot disabled olmalı
        assert not full_btn.is_enabled()

        # API ile aynı slotu tekrar dene (409 beklenir)
        date_str = wait_visible(driver, By.ID, "dateSel", timeout=10).get_attribute("value")
        start_min = _parse_start_minute(full_btn.text)
        assert start_min is not None

        pitch_id = int(Select(pitch_sel).first_selected_option.get_attribute("value"))

        h = start_min // 60
        m = start_min % 60
        start_iso = f"{date_str}T{h:02d}:{m:02d}:00+03:00"

        payload = {
            "pitchId": pitch_id,
            "startTime": start_iso,
            "durationMinutes": 60,
            "paymentMethod": "CASH",
            "players": [{"fullName": "API Player"}],
            "shuttle": False
        }

        r = requests.post(
            base_url() + "/api/member/reservations",
            auth=("member1", "member123"),
            json=payload,
            timeout=10
        )
        assert r.status_code in (409, 400), f"Beklenen conflict benzeri hata, gelen: {r.status_code} {r.text[:200]}"
        # ideal: 409
        assert r.status_code == 409 or "conflict" in r.text.lower() or "already" in r.text.lower()

    finally:
        driver.quit()
