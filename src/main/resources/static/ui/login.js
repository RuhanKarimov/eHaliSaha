// /ui/login.js
"use strict";

(function () {
    if (!window.EH || !EH.API) {
        console.error("EH veya EH.API bulunamadı. Script sırasını kontrol et: eh.core -> eh.api -> eh.auth -> login");
        return;
    }

    // ✅ legacy token migration (eh_basic_token -> eh_basic)
    try {
        const legacy = localStorage.getItem("eh_basic_token");
        const current = localStorage.getItem("eh_basic");
        if (legacy && !current) {
            localStorage.setItem("eh_basic", legacy);
            localStorage.removeItem("eh_basic_token");
        }
    } catch (e) {
        console.warn("localStorage migration warning", e);
    }

    // ----- role read -----
    const qs = new URLSearchParams(location.search);
    const wantedRole = (qs.get("role") || "").toUpperCase();

    // Guard: role seçilmeden gelinirse geri
    if (wantedRole !== "MEMBER" && wantedRole !== "OWNER") {
        location.href = "/ui/choose-role.html";
        return;
    }

    // DOM refs (ID'ler aynı kalmalı)
    const titleEl = document.getElementById("title");
    const descEl = document.getElementById("desc");
    const pillEl = document.getElementById("pill");
    const roleAccent = document.getElementById("roleAccent");
    const roleDesc = document.getElementById("roleDesc");

    const uEl = document.getElementById("u");
    const pEl = document.getElementById("p");
    const btnEl = document.getElementById("btn");
    const btnDemoEl = document.getElementById("btnDemo");
    const outEl = document.getElementById("out");
    const statusPill = document.getElementById("statusPill");
    const togglePwd = document.getElementById("togglePwd");

    // /api/me helper
    if (!EH.getMe) EH.getMe = async () => await EH.API.get("/api/me");
    if (!EH.toast) EH.toast = (m, t) => console.log(t || "info", m);

    // ---------------- UI helpers ----------------
    function esc(s) {
        return String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function parseErrMessage(e) {
        // e.message bazen JSON string oluyor
        const raw = e?.message || "";
        try {
            const j = JSON.parse(raw);
            if (j?.fields && typeof j.fields === "object") {
                const k = Object.keys(j.fields)[0];
                return (j.message ? j.message + " - " : "") + `${k}: ${j.fields[k]}`;
            }
            return j?.message || raw;
        } catch {
            if (e?.body?.fields && typeof e.body.fields === "object") {
                const k = Object.keys(e.body.fields)[0];
                return (e.body.message ? e.body.message + " - " : "") + `${k}: ${e.body.fields[k]}`;
            }
            return raw || "Giriş başarısız";
        }
    }

    function setStatus(text, type) {
        if (!statusPill) return;

        // görünür yap
        statusPill.classList.remove("opacity-0");
        statusPill.style.transform = "translateY(0px)";

        // ikon
        const icon =
            type === "ok" ? "✅" :
                type === "err" ? "⛔" :
                    type === "mid" ? "⏳" :
                        type === "warn" ? "⚠️" : "ℹ️";

        statusPill.textContent = `${icon} ${text}`;

        // renkler (yeşil tema)
        if (type === "ok") {
            statusPill.style.borderColor = "rgba(52,211,153,.30)";
            statusPill.style.background = "rgba(16,185,129,.14)";
            statusPill.style.color = "rgba(167,243,208,.95)";
        } else if (type === "err") {
            statusPill.style.borderColor = "rgba(248,113,113,.30)";
            statusPill.style.background = "rgba(248,113,113,.10)";
            statusPill.style.color = "rgba(254,202,202,.95)";
        } else if (type === "warn") {
            statusPill.style.borderColor = "rgba(251,191,36,.28)";
            statusPill.style.background = "rgba(251,191,36,.10)";
            statusPill.style.color = "rgba(253,230,138,.95)";
        } else { // mid/info
            statusPill.style.borderColor = "rgba(255,255,255,.20)";
            statusPill.style.background = "rgba(255,255,255,.08)";
            statusPill.style.color = "rgba(255,255,255,.86)";
        }
    }

    function setOut(kind, title, message) {
        if (!outEl) return;

        const tone =
            kind === "ok" ? "rgba(16,185,129,.14)" :
                kind === "err" ? "rgba(248,113,113,.10)" :
                    kind === "warn" ? "rgba(251,191,36,.10)" :
                        "rgba(255,255,255,.08)";

        const border =
            kind === "ok" ? "rgba(52,211,153,.30)" :
                kind === "err" ? "rgba(248,113,113,.30)" :
                    kind === "warn" ? "rgba(251,191,36,.28)" :
                        "rgba(255,255,255,.14)";

        const icon =
            kind === "ok" ? "✅" :
                kind === "err" ? "⛔" :
                    kind === "warn" ? "⚠️" :
                        kind === "mid" ? "⏳" : "ℹ️";

        outEl.innerHTML = `
      <div style="border:1px solid ${border}; background:${tone}; border-radius:16px; padding:12px 14px;">
        <div style="font-weight:800; letter-spacing:-.01em;">${icon} ${esc(title || "")}</div>
        ${message ? `<div style="margin-top:6px; opacity:.9">${esc(message)}</div>` : ""}
      </div>
    `;
    }

    function setBusy(busy) {
        if (!btnEl || !btnDemoEl) return;

        btnEl.disabled = !!busy;
        btnDemoEl.disabled = !!busy;

        btnEl.dataset._old = btnEl.dataset._old || btnEl.textContent;
        btnDemoEl.dataset._old = btnDemoEl.dataset._old || btnDemoEl.textContent;

        if (busy) {
            btnEl.textContent = "Giriş yapılıyor…";
            btnDemoEl.textContent = "Bekle…";
        } else {
            btnEl.textContent = btnEl.dataset._old;
            btnDemoEl.textContent = btnDemoEl.dataset._old;
        }
    }

    // ---------------- copy / demo ----------------
    function setCopy() {
        // pill biraz daha “badge” gibi görünsün (HTML değişmeden)
        if (pillEl) {
            pillEl.textContent = wantedRole;
            pillEl.style.borderColor = "rgba(52,211,153,.25)";
            pillEl.style.background = "rgba(16,185,129,.14)";
            pillEl.style.color = "rgba(167,243,208,.95)";
        }

        if (wantedRole === "OWNER") {
            titleEl.textContent = "Owner Girişi";
            descEl.textContent = "Saha sahibi hesabınla giriş yap. Member hesapları owner paneline giremez.";
            roleAccent.textContent = "owner paneline";
            roleDesc.textContent = "Facility, slot, pitch ve fiyat yönetimi buradan. Üyelik isteklerini burada onaylarsın.";
        } else {
            titleEl.textContent = "Member Girişi";
            descEl.textContent = "Üye hesabınla giriş yap. Rezervasyon ve videolar buradan.";
            roleAccent.textContent = "member paneline";
            roleDesc.textContent = "Slot seç, ödeme seç. Maç bitince videon otomatik görünür.";
        }
    }

    function fillDemo() {
        if (wantedRole === "OWNER") {
            uEl.value = "owner1";
            pEl.value = "owner123";
        } else {
            uEl.value = "member1";
            pEl.value = "member123";
        }
        uEl.focus();
        setStatus("Demo dolduruldu", "ok");
        setOut("ok", "Demo hazır", "Giriş yapabilirsin.");
    }

    // ---------------- login flow (mantık aynı) ----------------
    let inFlight = false;

    async function doLogin() {
        if (inFlight) return;
        inFlight = true;

        const u = (uEl.value || "").trim();
        const p = pEl.value || "";

        if (!u || !p) {
            setOut("err", "Eksik bilgi", "Kullanıcı adı ve şifre gerekli.");
            setStatus("Eksik bilgi", "err");
            inFlight = false;
            return;
        }

        setOut("mid", "Kontrol ediliyor", "Bilgiler doğrulanıyor…");
        setStatus("Kontrol ediliyor…", "mid");
        setBusy(true);

        // Token kaydet
        EH.API.setBasicAuth(u, p);

        try {
            const me = await EH.getMe(); // {id, username, role}
            if (!me || !me.role) throw new Error("role yok");

            // Role uyuşmazsa token’ı temizle
            if (me.role !== wantedRole) {
                EH.API.clearAuth();

                setOut(
                    "err",
                    "Rol uyuşmuyor",
                    `Bu kullanıcı ${me.role}. ${wantedRole} ekranına giriş yok.`
                );
                setStatus("Rol uyuşmuyor", "err");
                EH.toast("Rol uyuşmuyor", "err");
                return;
            }

            setOut("ok", "Giriş başarılı", "Yönlendiriyorum…");
            setStatus("Başarılı", "ok");
            EH.toast("Giriş başarılı ✅", "ok");

            // ✅ hedefi me.role'a göre kesinleştir
            const target = me.role === "OWNER" ? "/ui/owner.html" : "/ui/member.html";
            location.href = target;

        } catch (e) {
            EH.API.clearAuth();
            const msg = parseErrMessage(e);

            setOut("err", "Giriş başarısız", msg);
            setStatus("Başarısız", "err");
            EH.toast("Giriş başarısız", "err");
            console.error(e);
        } finally {
            setBusy(false);
            inFlight = false;
        }
    }

    // show/hide password
    togglePwd.addEventListener("click", () => {
        const isPwd = pEl.type === "password";
        pEl.type = isPwd ? "text" : "password";
        togglePwd.textContent = isPwd ? "Gizle" : "Göster";
        pEl.focus();
    });

    // actions
    btnEl.addEventListener("click", (e) => {
        if (e && e.preventDefault) e.preventDefault();
        doLogin();
    });

    btnDemoEl.addEventListener("click", (e) => { e.preventDefault(); fillDemo(); });

    // keyboard UX
    document.addEventListener("keydown", (e) => {
        // Enter: input alanlarındayken daha mantıklı
        if (e.key === "Enter") {
            const a = document.activeElement;
            const isField = a === uEl || a === pEl || a === btnEl || a === btnDemoEl;
            if (isField) doLogin();
        }

        if (e.key === "Escape") {
            uEl.value = "";
            pEl.value = "";
            if (outEl) outEl.textContent = "";
            setStatus("Temizlendi", "mid");
            setOut("info", "Temizlendi", "Bilgileri yeniden gir.");
            uEl.focus();
        }
    });

    // init
    setCopy();
    setOut("info", "Hazır", "Bilgilerini girip giriş yapabilirsin.");
    uEl.focus();
})();
