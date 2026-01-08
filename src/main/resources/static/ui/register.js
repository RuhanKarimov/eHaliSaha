"use strict";

(function () {
    const $ = (id) => document.getElementById(id);

    const u = $("username");
    const p = $("password");
    const r = $("role");
    const b = $("btnRegister");
    const msg = $("msg");

    // login.css anim sistemi
    requestAnimationFrame(() => document.body.classList.add("loaded"));

    // spotlight hover: mouse hangi noktadaysa orası parlasın
    function bindSpotlight(el) {
        if (!el) return;
        const onMove = (ev) => {
            const rect = el.getBoundingClientRect();
            const x = ((ev.clientX - rect.left) / rect.width) * 100;
            const y = ((ev.clientY - rect.top) / rect.height) * 100;
            el.style.setProperty("--mx", x.toFixed(2) + "%");
            el.style.setProperty("--my", y.toFixed(2) + "%");
        };
        el.addEventListener("pointermove", onMove);
        el.addEventListener("pointerenter", onMove);
    }
    bindSpotlight(b);

    function setMsg(text, ok = false) {
        msg.textContent = text || "";
        msg.style.opacity = text ? "1" : "0";
        msg.style.color = ok ? "#a7f3d0" : "#fecaca";
    }

    function setBtnLoading(loading) {
        b.disabled = !!loading;
        if (loading) {
            b.innerHTML = 'Kaydediliyor <span class="arrow" aria-hidden="true">…</span>';
        } else {
            b.innerHTML = 'Kayıt Ol <span class="arrow" aria-hidden="true">→</span>';
        }
    }

    b.addEventListener("click", async () => {
        setMsg("");

        const username = (u.value || "").trim();
        const password = (p.value || "").trim();
        const role = r.value;

        if (username.length < 3) return setMsg("Kullanıcı adı en az 3 karakter olmalı.");
        if (password.length < 6) return setMsg("Şifre en az 6 karakter olmalı.");

        setBtnLoading(true);

        try {
            const url = role === "OWNER" ? "/api/public/register-owner" : "/api/public/register";
            await EH.API.post(url, { username, password });

            setMsg("Kayıt başarılı. Şimdi giriş yapabilirsin ✅", true);
            setTimeout(() => (location.href = "/ui/login.html"), 700);
        } catch (e) {
            setMsg(e?.message || "Kayıt başarısız.");
        } finally {
            setBtnLoading(false);
        }
    });
})();
