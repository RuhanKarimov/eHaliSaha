// /ui/owner.js
"use strict";

/* ---------------------------
   Minimal fallback: EH.API yoksa bile çalışsın
---------------------------- */
(function bootstrapEH() {
    window.EH = window.EH || {};
    const EH = window.EH;

    EH.API = EH.API || {
        _auth: null,
        setBasicAuth(u, p) { this._auth = btoa(`${u}:${p}`); localStorage.setItem("eh_basic", this._auth); },
        clearAuth() { this._auth = null; localStorage.removeItem("eh_basic"); },
        _hdr() {
            const a = this._auth || localStorage.getItem("eh_basic");
            const h = { "Content-Type": "application/json" };
            if (a) h["Authorization"] = `Basic ${a}`;
            return h;
        },
        async get(url) {
            const r = await fetch(url, { headers: this._hdr() });
            if (!r.ok) throw await safeErr(r);
            return r.json();
        },
        async post(url, body) {
            const r = await fetch(url, { method: "POST", headers: this._hdr(), body: JSON.stringify(body ?? {}) });
            if (!r.ok) throw await safeErr(r);
            return r.json();
        },
        async put(url, body) {
            const r = await fetch(url, { method: "PUT", headers: this._hdr(), body: JSON.stringify(body ?? {}) });
            if (!r.ok) throw await safeErr(r);
            return r.json();
        }
    };

    EH.guard = EH.guard || (async (allowedRoles, redirectTo) => {
        try {
            const me = await EH.API.get("/api/me");
            if (!allowedRoles.includes(me.role)) {
                EH.API.clearAuth();
                location.href = redirectTo || "/ui/choose-role.html";
                return false;
            }
            return true;
        } catch {
            EH.API.clearAuth();
            location.href = redirectTo || "/ui/choose-role.html";
            return false;
        }
    });

    async function safeErr(r) {
        let j = null; try { j = await r.json(); } catch {}
        const msg = j?.message || j?.error || `${r.status} ${r.statusText}`;
        const e = new Error(typeof msg === "string" ? msg : JSON.stringify(msg));
        e.status = r.status;
        e.body = j;
        return e;
    }
})();

/* ---------------------------
   UI Logic (mantık aynı, sadece UI güzelleştirme)
---------------------------- */
window.UI = (function () {
    const EH = window.EH;
    const el = (id) => document.getElementById(id);

    const state = {
        facilities: [],
        pitches: [],
        durations: [],
        pricing: [],
        slots: [] // {startMinute,durationMinutes,active}
    };

    // ---- UI notice helpers (ownerOut artık "bildirim" gibi) ----
    function parseErr(e) {
        const raw = e?.message || "";
        try {
            const j = JSON.parse(raw);
            if (j?.fields && typeof j.fields === "object") {
                const k = Object.keys(j.fields)[0];
                return (j.message ? j.message + " - " : "") + `${k}: ${j.fields[k]}`;
            }
            return j?.message || raw;
        } catch {
            // bazıları already JSON body: e.body
            if (e?.body?.fields && typeof e.body.fields === "object") {
                const k = Object.keys(e.body.fields)[0];
                return (e.body.message ? e.body.message + " - " : "") + `${k}: ${e.body.fields[k]}`;
            }
            return raw || String(e);
        }
    }

    function notice(msg, tone = "info", obj = null) {
        const out = el("ownerOut");
        if (!out) return;

        out.textContent = msg || "";

        // detay debug konsola
        if (obj) console.log("[owner-ui]", msg, obj);

        const card = out.closest(".notice") || out.parentElement;
        if (!card) return;

        // reset
        card.style.borderColor = "rgba(255,255,255,.10)";
        card.style.boxShadow = "";

        if (tone === "ok") {
            card.style.borderColor = "rgba(52,211,153,.35)";
            card.style.boxShadow = "0 10px 28px rgba(16,185,129,.10)";
        } else if (tone === "warn") {
            card.style.borderColor = "rgba(251,191,36,.28)";
            card.style.boxShadow = "0 10px 28px rgba(251,191,36,.08)";
        } else if (tone === "err") {
            card.style.borderColor = "rgba(248,113,113,.30)";
            card.style.boxShadow = "0 10px 28px rgba(248,113,113,.08)";
        }
    }

    // ---- formatting ----
    function fmt(min) {
        const h = String(Math.floor(min / 60)).padStart(2, "0");
        const m = String(min % 60).padStart(2, "0");
        return `${h}:${m}`;
    }

    function slotLabel(s) {
        return `${fmt(s.startMinute)}-${fmt(s.startMinute + s.durationMinutes)}`;
    }

    function selectedFacilityId() {
        const sel = el("ownerFacilitySel");
        const v = sel ? sel.value : "";
        return v ? Number(v) : null;
    }

    function selectedPitchId() {
        const sel = el("ownerPitchSel");
        const v = sel ? sel.value : "";
        return v ? Number(v) : null;
    }

    function defaultSlots24h() {
        const arr = [];
        for (let h = 0; h < 24; h++) arr.push({ startMinute: h * 60, durationMinutes: 60, active: true });
        return arr;
    }

    // ---- renderers (UI only) ----
    function renderFacilities() {
        const sel = el("ownerFacilitySel");
        if (!sel) return;

        const prev = sel.value;
        sel.innerHTML = "";

        if (!state.facilities.length) {
            sel.innerHTML = `<option value="">(Facility yok)</option>`;
            return;
        }

        for (const f of state.facilities) {
            const opt = document.createElement("option");
            opt.value = f.id;
            opt.textContent = `${f.name}${f.address ? " • " + f.address : ""}`;
            sel.appendChild(opt);
        }

        // seçim korunmaya çalışılsın (mantığı bozmaz)
        if (prev && state.facilities.some(x => String(x.id) === String(prev))) {
            sel.value = prev;
        }
    }

    function renderPitches() {
        const sel = el("ownerPitchSel");
        if (!sel) return;

        const prev = sel.value;
        sel.innerHTML = "";

        if (!state.pitches.length) {
            sel.innerHTML = `<option value="">(Pitch yok)</option>`;
            return;
        }

        for (const p of state.pitches) {
            const opt = document.createElement("option");
            opt.value = p.id;
            opt.textContent = p.name;
            sel.appendChild(opt);
        }

        if (prev && state.pitches.some(x => String(x.id) === String(prev))) {
            sel.value = prev;
        }
    }

    function renderDurations() {
        const sel = el("priceDurationSel");
        if (!sel) return;

        const prev = sel.value;
        sel.innerHTML = "";

        for (const d of state.durations) {
            const opt = document.createElement("option");
            opt.value = d.minutes;
            opt.textContent = d.label || `${d.minutes} dk`;
            sel.appendChild(opt);
        }

        if (prev) sel.value = prev;
    }

    function renderPricing() {
        const box = el("pricingBox");
        if (!box) return;

        box.innerHTML = "";

        if (!state.pricing.length) {
            box.innerHTML = `
        <div class="p-4 rounded-2xl bg-black/20 border border-white/10 text-sm text-white/70">
          Henüz fiyat yok. Yukarıdan süre seçip fiyat girerek ekleyebilirsin.
        </div>`;
            return;
        }

        for (const pr of state.pricing) {
            const minutes = pr.durationMinutes ?? pr.minutes ?? "?";
            const row = document.createElement("div");
            row.className = "p-4 rounded-2xl bg-black/20 border border-white/10 flex items-center justify-between gap-3";

            row.innerHTML = `
        <div>
          <div class="font-extrabold tracking-tight">${minutes} dk</div>
          <div class="text-xs text-white/60 mt-1">Bu pitch için geçerli</div>
        </div>
        <div class="text-2xl font-extrabold tracking-tight">${pr.price} <span class="text-sm font-semibold text-white/70">TRY</span></div>
      `;

            box.appendChild(row);
        }
    }

    function renderSlots() {
        const grid = el("slotGrid");
        if (!grid) return;

        grid.innerHTML = "";

        const activeCount = state.slots.filter(s => s.active).length;
        const pill = el("slotCountPill");
        if (pill) pill.textContent = `${activeCount} aktif`;

        for (const s of state.slots) {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = `chip ${s.active ? "chip-on" : ""}`;
            btn.setAttribute("aria-pressed", String(!!s.active));
            btn.setAttribute("title", s.active ? "Açık (tıkla kapat)" : "Kapalı (tıkla aç)");

            // mantık aynı: tıkla toggle
            btn.onclick = () => { s.active = !s.active; renderSlots(); };

            const time = slotLabel(s);
            const tagCls = s.active ? "chipTag chipTag-on" : "chipTag chipTag-off";
            const tagTxt = s.active ? "Açık" : "Kapalı";
            const meta = s.active ? "Member rezervasyona açık" : "Member tarafında pasif";

            btn.innerHTML = `
        <div class="chipTop">
          <div class="chipTime">${time}</div>
          <span class="${tagCls}">${tagTxt}</span>
        </div>
        <div class="chipMeta">${meta}</div>
      `;

            grid.appendChild(btn);
        }
    }

    // Requests render: aynı mantık, daha güzel kartlar
    function renderRequests(box, reqs) {
        box.innerHTML = "";

        if (!reqs || !reqs.length) {
            box.innerHTML = `
        <div class="reqCard">
          <div class="font-semibold">Bekleyen istek yok</div>
          <div class="text-sm mt-1 text-white/60">Yeni istek gelince burada görünecek.</div>
        </div>`;
            return;
        }

        for (const r of reqs) {
            const card = document.createElement("div");
            card.className = "reqCard";

            const st = (r.status || "PENDING").toUpperCase();
            const badge = st === "PENDING"
                ? `<span class="badge badge-green">PENDING</span>`
                : `<span class="badge">${st}</span>`;

            card.innerHTML = `
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="font-extrabold tracking-tight">Üyelik isteği #${r.id}</div>
            <div class="text-sm text-white/60 mt-1">
              facilityId=${r.facilityId} • userId=${r.userId}
            </div>
            <div class="mt-2">${badge}</div>
          </div>
          <div class="flex gap-2 shrink-0">
            <button class="btn btn-primary" data-a="approve">Onayla</button>
            <button class="btn btn-ghost" data-a="reject">Reddet</button>
          </div>
        </div>
      `;

            card.querySelector('[data-a="approve"]').onclick = () => approveRequest(r.id);
            card.querySelector('[data-a="reject"]').onclick = () => rejectRequest(r.id);

            box.appendChild(card);
        }
    }

    // ---- loaders (mantık aynı) ----
    async function loadMe() {
        try {
            const me = await EH.API.get("/api/me");
            const pill = el("mePill");
            if (!pill) return;
            pill.classList.remove("hidden");
            pill.textContent = `${me.username} • ${me.role}`;
        } catch {}
    }

    async function loadFacilities() {
        state.facilities = await EH.API.get("/api/owner/facilities");
        renderFacilities();
    }

    async function loadPitches() {
        const facilityId = selectedFacilityId();
        if (!facilityId) { state.pitches = []; renderPitches(); return; }
        state.pitches = await EH.API.get(`/api/owner/facilities/${facilityId}/pitches`);
        renderPitches();
    }

    async function loadDurations() {
        state.durations = await EH.API.get("/api/public/durations");
        renderDurations();
    }

    async function loadSlots() {
        const facilityId = selectedFacilityId();
        if (!facilityId) {
            state.slots = defaultSlots24h();
            renderSlots();
            return;
        }

        try {
            const slots = await EH.API.get(`/api/owner/facilities/${facilityId}/slots`);
            state.slots = (slots && slots.length)
                ? slots.map(s => ({
                    startMinute: Number(s.startMinute),
                    durationMinutes: Number(s.durationMinutes ?? 60),
                    active: Boolean(s.active)
                }))
                : defaultSlots24h();
        } catch {
            try {
                const slots2 = await EH.API.get(`/api/public/facilities/${facilityId}/slots`);
                state.slots = (slots2 && slots2.length)
                    ? slots2.map(s => ({
                        startMinute: Number(s.startMinute),
                        durationMinutes: Number(s.durationMinutes ?? 60),
                        active: Boolean(s.active ?? true)
                    }))
                    : defaultSlots24h();
            } catch {
                state.slots = defaultSlots24h();
            }
        }

        state.slots.sort((a, b) => a.startMinute - b.startMinute);
        renderSlots();
    }

    async function saveSlots() {
        const facilityId = selectedFacilityId();
        if (!facilityId) { notice("Önce facility seç.", "warn"); return; }

        const payload = state.slots.map(s => ({
            startMinute: s.startMinute,
            durationMinutes: s.durationMinutes ?? 60,
            active: !!s.active
        }));

        try {
            const saved = await EH.API.put(`/api/owner/facilities/${facilityId}/slots`, payload);
            notice("Slotlar kaydedildi ✅ (Kapalı slotlar da saklandı)", "ok", saved);
            await loadSlots();
        } catch (e) {
            notice("Slot kaydetme hatası ❌\n" + parseErr(e), "err", e?.body);
        }
    }

    function slotPreset(kind) {
        if (!state.slots.length) state.slots = defaultSlots24h();

        if (kind === "all") state.slots.forEach(s => s.active = true);
        else if (kind === "clear") state.slots.forEach(s => s.active = false);
        else if (kind === "prime") state.slots.forEach(s => s.active = (s.startMinute >= 17 * 60));
        else if (kind === "day") state.slots.forEach(s => s.active = (s.startMinute >= 8 * 60 && s.startMinute < 23 * 60));

        renderSlots();
        notice("Slot ön ayarı uygulandı ✅", "ok");
    }

    async function createFacility() {
        const name = (el("facName")?.value || "").trim();
        const address = (el("facAddr")?.value || "").trim();
        if (!name) { notice("Facility adı boş olamaz.", "warn"); return; }

        try {
            const created = await EH.API.post("/api/owner/facilities", { name, address });
            notice("Facility oluşturuldu ✅", "ok", created);

            await loadFacilities();
            if (created?.id) el("ownerFacilitySel").value = String(created.id);

            await onFacilityChanged();
        } catch (e) {
            notice("Facility oluşturma hatası ❌\n" + parseErr(e), "err", e?.body);
        }
    }

    async function createPitch() {
        const facilityId = selectedFacilityId();
        if (!facilityId) { notice("Önce facility seç.", "warn"); return; }

        const name = (el("pitchName")?.value || "").trim();
        if (!name) { notice("Pitch adı boş olamaz.", "warn"); return; }

        try {
            const created = await EH.API.post(`/api/owner/facilities/${facilityId}/pitches`, { facilityId, name });
            notice("Pitch oluşturuldu ✅", "ok", created);
            if (el("pitchName")) el("pitchName").value = "";

            await loadPitches();
            if (created?.id) el("ownerPitchSel").value = String(created.id);
            await loadPricing();
        } catch (e) {
            notice("Pitch oluşturma hatası ❌\n" + parseErr(e), "err", e?.body);
        }
    }

    async function upsertPricing() {
        const pitchId = selectedPitchId();
        if (!pitchId) { notice("Önce pitch seç.", "warn"); return; }

        const durationMinutes = Number(el("priceDurationSel")?.value || 0);
        const price = Number(String(el("priceValue")?.value || "").replace(",", "."));

        if (!durationMinutes) { notice("Süre seç.", "warn"); return; }
        if (!price || price <= 0) { notice("Fiyat geçersiz.", "warn"); return; }

        try {
            const saved = await EH.API.post("/api/owner/pricing", { pitchId, durationMinutes, price });
            notice("Fiyat kaydedildi ✅", "ok", saved);
            await loadPricing();
        } catch (e) {
            notice("Fiyat kaydetme hatası ❌\n" + parseErr(e), "err", e?.body);
        }
    }

    async function loadPricing() {
        const pitchId = selectedPitchId();
        if (!pitchId) { state.pricing = []; renderPricing(); return; }

        try {
            const list = await EH.API.get(`/api/owner/pricing?pitchId=${pitchId}`);
            state.pricing = (list || []).map(x => ({
                pitchId: x.pitchId,
                durationMinutes: x.durationMinutes,
                minutes: x.minutes,
                price: x.price
            }));
            renderPricing();
        } catch (e) {
            try {
                const list2 = await EH.API.get(`/api/public/pricing?pitchId=${pitchId}`);
                state.pricing = (list2 || []).map(x => ({
                    pitchId: x.pitchId,
                    durationMinutes: x.durationMinutes,
                    minutes: x.minutes,
                    price: x.price
                }));
                renderPricing();
            } catch {
                state.pricing = [];
                renderPricing();
            }
        }
    }

    async function loadRequests() {
        const facilityId = selectedFacilityId();
        const box = el("reqBox");
        if (!box) return;

        box.innerHTML = `
      <div class="reqCard">
        <div class="font-semibold">Yükleniyor...</div>
        <div class="text-sm mt-1 text-white/60">İstekler getiriliyor.</div>
      </div>`;

        try {
            const list = await EH.API.get(`/api/owner/membership-requests`);
            renderRequests(box, list);
        } catch (e) {
            try {
                if (!facilityId) throw new Error("facility seçili değil");
                const list2 = await EH.API.get(`/api/owner/membership-requests?facilityId=${facilityId}`);
                renderRequests(box, list2);
            } catch (e2) {
                box.innerHTML = `
          <div class="reqCard">
            <div class="font-semibold">Hata ❌</div>
            <div class="text-sm mt-1 text-white/70">${(e2 && e2.message) ? e2.message : "Bilinmeyen hata"}</div>
          </div>`;
            }
        }
    }

    async function approveRequest(id) {
        try {
            await EH.API.post(`/api/owner/membership-requests/${id}/approve`, {});
            await loadRequests();
            notice(`İstek onaylandı ✅ (#${id})`, "ok");
        } catch (e) {
            notice(`Onay hatası ❌\n${parseErr(e)}`, "err", e?.body);
        }
    }

    async function rejectRequest(id) {
        try {
            await EH.API.post(`/api/owner/membership-requests/${id}/reject`, {});
            await loadRequests();
            notice(`İstek reddedildi ✅ (#${id})`, "ok");
        } catch (e) {
            notice(`Red hatası ❌\n${parseErr(e)}`, "err", e?.body);
        }
    }

    async function onFacilityChanged() {
        await loadSlots();
        await loadPitches();
        await loadRequests();
        await loadPricing();
        // küçük UX: facility değişince bilgi
        notice("Facility değişti. Kurulum alanları güncellendi ✅", "ok");
    }

    async function refreshAll() {
        await loadFacilities();
        await loadDurations();
        await onFacilityChanged();
        notice("Yenilendi ✅", "ok");
    }

    return {
        loadMe,
        loadFacilities,
        loadPitches,
        loadDurations,
        loadSlots,
        loadPricing,
        loadRequests,
        createFacility,
        createPitch,
        upsertPricing,
        saveSlots,
        slotPreset,
        onFacilityChanged,
        refreshAll
    };
})();

/* ---------------------------
   BOOT
---------------------------- */
(async () => {
    const EH = window.EH;
    const ok = await EH.guard(["OWNER"], "/ui/choose-role.html");
    if (!ok) return;

    await window.UI.loadMe();
    await window.UI.loadFacilities();
    await window.UI.loadDurations();
    await window.UI.onFacilityChanged();
})();
