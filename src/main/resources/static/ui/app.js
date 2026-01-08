/* /ui/app.js  (fixed, single AUTH system)
   - Removes duplicate AUTH+API wrapper that overwrote EH/API/toast
   - Keeps your member/owner logic intact
   - Adds proper error.status so your "if (e.status===403)" checks work
*/

"use strict";

/* =========================
   AUTH + API (Single Source of Truth)
   - localStorage key: "eh_basic"  (sayfalardaki fallback ile uyumlu)
   - 401/403 hata objesine .status ekler (UI'daki e.status kontrolleri çalışsın)
========================= */

window.EH = window.EH || {};
const EH = window.EH;

// Bazı browserlarda unicode btoa sıkıntısı için (istersen düz btoa da kalabilir)
function base64EncodeUtf8(str) {
    const bytes = new TextEncoder().encode(str);
    let bin = "";
    for (const b of bytes) bin += String.fromCharCode(b);
    return btoa(bin);
}

EH.toast = EH.toast || ((msg, type="info") => {
    const fn = type === "err" ? "error" : "log";
    console[fn](msg);
});

EH.prettyErr = EH.prettyErr || (async (res) => {
    const txt = await res.text();
    try {
        const j = JSON.parse(txt);
        if (j?.message) return j.message;
        if (j?.error) return j.error;
    } catch {}
    return txt || `${res.status} ${res.statusText}`;
});

EH.httpError = EH.httpError || (async (res) => {
    const msg = await EH.prettyErr(res);
    const err = new Error(msg);
    err.status = res.status;        // <<< kritik: UI tarafında e.status çalışır
    err.url = res.url;
    return err;
});


EH.API = {
    base: "",
    tokenKey: "eh_basic",

    setBasicAuth(username, password) {
        const t = btoa(`${username}:${password}`);
        localStorage.setItem(this.tokenKey, t);
        return t;
    },

    clearAuth() {
        localStorage.removeItem(this.tokenKey);
        // geçmişte yanlış kullanıldıysa süpür:
        localStorage.removeItem("eh_basic_token");
    },

    getAuthHeader() {
        const t = localStorage.getItem(this.tokenKey);
        return t ? { Authorization: `Basic ${t}` } : {};
    },

    async req(method, url, body) {
        const headers = { ...this.getAuthHeader() };
        const opts = { method, headers };

        if (body !== undefined) {
            opts.headers["Content-Type"] = "application/json";
            opts.body = JSON.stringify(body);
        }

        const res = await fetch(this.base + url, opts);

        if (!res.ok) {
            const txt = await res.text();
            const err = new Error(txt || `${res.status} ${res.statusText}`);
            err.status = res.status;            // kritik: UI’da 401/403 ayrımı
            throw err;
        }

        if (res.status === 204) return null;

        const ct = res.headers.get("content-type") || "";
        if (ct.includes("application/json")) return res.json();
        return res.text();
    },

    get(url) { return this.req("GET", url); },
    post(url, body) { return this.req("POST", url, body); },
    put(url, body) { return this.req("PUT", url, body); },
    del(url) { return this.req("DELETE", url); }
};

EH.getMe = EH.getMe || (async () => EH.API.get("/api/me"));

/* Rol guard:
   - Yetkisizse (401/403) token temizle ve choose-role'a at
   - Rol uymuyorsa token SİLME, doğru panele yönlendir
*/
EH.guard = EH.guard || (async (allowedRoles, redirectUrl = "/ui/choose-role.html") => {
    try {
        const me = await EH.getMe();
        if (!me?.role) throw new Error("Unauthorized");

        if (!allowedRoles.includes(me.role)) {
            const target =
                me.role === "OWNER" ? "/ui/owner.html" :
                    me.role === "MEMBER" ? "/ui/member.html" :
                        redirectUrl;

            EH.toast(`Rol uyuşmuyor: ${me.role} → yönlendiriyorum`, "info");
            location.href = target;
            return false;
        }
        return true;
    } catch (e) {
        // gerçekten auth yok/bozuk
        EH.API.clearAuth();
        location.href = redirectUrl;
        return false;
    }
});

EH.logout = EH.logout || (() => {
    EH.API.clearAuth();
    location.href = "/ui/login.html";
});


/* -----------------------------
   Tiny toast (stacked)
----------------------------- */
(function ensureToast() {
    if (document.getElementById("toastRoot")) return;
    const root = document.createElement("div");
    root.id = "toastRoot";
    root.className = "toast";
    document.body.appendChild(root);

    EH.toast = (msg, type = "ok") => {
        const el = document.createElement("div");
        el.className = type === "ok" ? "ok" : type === "info" ? "info" : "err";
        el.textContent = msg;
        root.appendChild(el);
        setTimeout(() => el.remove(), 3200);
    };
})();

/* -----------------------------
   DOM helpers
----------------------------- */
EH.$ = (id) => document.getElementById(id);
EH.exists = (id) => !!document.getElementById(id);

EH.safeText = (el, txt) => { if (el) el.textContent = txt ?? ""; };
EH.safeHTML = (el, html) => { if (el) el.innerHTML = html ?? ""; };

EH.ensureNumber = (v, msg) => {
    const n = Number(v);
    if (!Number.isFinite(n)) throw new Error(msg);
    return n;
};

EH.sleep = (ms) => new Promise(r => setTimeout(r, ms));

/* -----------------------------
   Error parsing
----------------------------- */
EH.prettyErr = async (res) => {
    const txt = await res.text();
    try {
        const j = JSON.parse(txt);
        if (j && (j.message || j.error)) return j.message || j.error;
        if (j && j.errors && Array.isArray(j.errors) && j.errors[0]) return j.errors[0];
    } catch {}
    return txt || `${res.status} ${res.statusText}`;
};

EH.isAuthProblem = (err) => {
    const m = String(err?.message || err || "");
    return m.includes("401") || m.includes("Unauthorized") || m.includes("403") || m.includes("Forbidden") || m.includes("Access is denied");
};

// UTF-8 safe base64 (btoa unicode’da bazen patlar)
EH.b64 = (str) => {
    const bytes = new TextEncoder().encode(str);
    let bin = "";
    for (const b of bytes) bin += String.fromCharCode(b);
    return btoa(bin);
};

/* -----------------------------
   API (Basic Auth in localStorage)  ✅ SINGLE SOURCE OF TRUTH
----------------------------- */
EH.API = {
    base: "",
    tokenKey: "eh_basic",

    setBasicAuth(username, password) {
        // eski: btoa(`${u}:${p}`) -> unicode’da sorun olabilir
        const t = EH.b64(`${username}:${password}`);
        localStorage.setItem(this.tokenKey, t);
        return t;
    },

    clearAuth() {
        localStorage.removeItem(this.tokenKey);
    },

    getAuthHeader() {
        const t = localStorage.getItem(this.tokenKey);
        return t ? { Authorization: `Basic ${t}` } : {};
    },

    async req(method, url, body) {
        const headers = {
            ...this.getAuthHeader()
        };

        const opts = { method, headers };

        if (body !== undefined) {
            opts.headers["Content-Type"] = "application/json";
            opts.body = JSON.stringify(body);
        }

        const res = await fetch(this.base + url, opts);

        if (!res.ok) {
            const msg = await EH.prettyErr(res);
            const err = new Error(msg);
            err.status = res.status;           // ✅ senin kodların bunu kullanıyor
            err.statusText = res.statusText;
            throw err;
        }

        if (res.status === 204) return null;

        const ct = res.headers.get("content-type") || "";
        if (ct.includes("application/json")) return res.json();
        return res.text();
    },

    get(url) { return this.req("GET", url); },
    post(url, body) { return this.req("POST", url, body); },
    put(url, body) { return this.req("PUT", url, body); },
    del(url) { return this.req("DELETE", url); }
};

/* -----------------------------
   Auth / me / guard
----------------------------- */
EH.getMe = async () => EH.API.get("/api/me");

EH.guard = async (allowedRoles, redirectUrl = "/ui/choose-role.html") => {
    try {
        const me = await EH.getMe();
        if (!me || !me.role) throw new Error("Unauthorized");

        // Rol uyuşmazlığında TOKEN SİLME!
        if (!allowedRoles.includes(me.role)) {
            EH.toast(`Bu sayfa için rolün uygun değil: ${me.role}`, "err");

            const target =
                me.role === "OWNER" ? "/ui/owner.html" :
                    me.role === "MEMBER" ? "/ui/member.html" :
                        redirectUrl;

            location.href = target;
            return false;
        }
        return true;

    } catch (e) {
        // login yok/bozuk token
        EH.API.clearAuth();
        location.href = redirectUrl;
        return false;
    }
};

/* -----------------------------
   Public catalog
----------------------------- */
EH.loadFacilities = async (selectId) => {
    const facilities = await EH.API.get("/api/public/facilities");
    const sel = EH.$(selectId);
    if (sel) sel.innerHTML = facilities.map(f => `<option value="${f.id}">${f.name}</option>`).join("");
    return facilities;
};

EH.loadPitchesPublic = async (facilityId, selectId) => {
    const pitches = await EH.API.get(`/api/public/facilities/${facilityId}/pitches`);
    const sel = EH.$(selectId);
    if (sel) sel.innerHTML = pitches.map(p => `<option value="${p.id}">${p.name}</option>`).join("");
    return pitches;
};

EH.loadDurations = async (selectId) => {
    const durs = await EH.API.get("/api/public/durations");
    const sel = EH.$(selectId);
    if (sel) {
        sel.innerHTML = durs.map(d => `<option value="${d.minutes}">${d.label}</option>`).join("");
        sel.value = sel.value || "60";
    }
    return durs;
};

EH.loadSlots = async (facilityId) => EH.API.get(`/api/public/facilities/${facilityId}/slots`);

/* -----------------------------
   Time helpers
----------------------------- */
EH.fmtMinute = (min) => {
    const h = String(Math.floor(min / 60)).padStart(2, "0");
    const m = String(min % 60).padStart(2, "0");
    return `${h}:${m}`;
};

EH.toISOWithOffset = (dt) => {
    const pad = (n) => String(n).padStart(2, "0");
    const Y = dt.getFullYear();
    const M = pad(dt.getMonth() + 1);
    const D = pad(dt.getDate());
    const hh = pad(dt.getHours());
    const mm = pad(dt.getMinutes());
    const ss = pad(dt.getSeconds());

    const offMin = -dt.getTimezoneOffset();
    const sign = offMin >= 0 ? "+" : "-";
    const abs = Math.abs(offMin);
    const oh = pad(Math.floor(abs / 60));
    const om = pad(abs % 60);

    return `${Y}-${M}-${D}T${hh}:${mm}:${ss}${sign}${oh}:${om}`;
};

EH.buildStartISO = (dateStr, startMinute) => {
    const [Y, Mo, D] = dateStr.split("-").map(Number);
    const h = Math.floor(startMinute / 60);
    const m = startMinute % 60;
    const dt = new Date(Y, Mo - 1, D, h, m, 0, 0);
    return EH.toISOWithOffset(dt);
};

/* -----------------------------
   Player row UI
----------------------------- */
EH.addPlayerRow = (boxId, fullName = "", jerseyNo = "") => {
    const box = EH.$(boxId);
    if (!box) return;

    const row = document.createElement("div");
    row.className = "grid grid-cols-12 gap-2";
    row.innerHTML = `
    <input class="col-span-8 input" placeholder="Oyuncu adı soyadı" value="${String(fullName ?? "")}">
    <input class="col-span-3 input" placeholder="No" value="${String(jerseyNo ?? "")}">
    <button class="col-span-1 btn" type="button" title="Sil">✕</button>
  `;
    row.querySelector("button").onclick = () => row.remove();
    box.appendChild(row);
};

/* -----------------------------
   MEMBER
----------------------------- */
EH.member = {
    async refreshCatalog() {
        await EH.loadFacilities("facilitySel");
        const fid = EH.ensureNumber(EH.$("facilitySel")?.value, "Facility seç");
        await EH.loadPitchesPublic(fid, "pitchSel");
        await this.refreshSlots();
    },

    async refreshSlots() {
        const fid = EH.ensureNumber(EH.$("facilitySel")?.value, "Facility seç");
        const slots = await EH.loadSlots(fid);

        const sel = EH.$("slotSel");
        if (!sel) return;

        sel.innerHTML = (slots || [])
            .filter(s => s.active !== false)
            .map(s => {
                const label = s.label || `${EH.fmtMinute(s.startMinute)}-${EH.fmtMinute(s.startMinute + s.durationMinutes)}`;
                const base = Number(s.durationMinutes || 60);
                return `<option value="${s.startMinute}" data-basedur="${base}">${label}</option>`;
            }).join("");

        const baseDur = Number(sel.selectedOptions[0]?.dataset?.basedur || 60);
        this.buildDurationMultiplier(baseDur);
    },

    buildDurationMultiplier(baseDur) {
        const mulSel = EH.$("mulSel");
        if (!mulSel) return;

        const maxK = 8;
        mulSel.innerHTML = Array.from({ length: maxK }, (_, i) => i + 1)
            .map(k => `<option value="${k}">${k}x (${k * baseDur} dk)</option>`)
            .join("");
        mulSel.value = mulSel.value || "1";
    },

    readPlayers() {
        const box = EH.$("playersBox");
        const rows = Array.from(box?.children || []);
        const players = rows.map(r => {
            const inputs = r.querySelectorAll("input");
            const fullName = (inputs[0]?.value || "").trim();
            const jerseyRaw = (inputs[1]?.value || "").trim();
            return { fullName, jerseyNo: jerseyRaw ? Number(jerseyRaw) : null };
        }).filter(p => p.fullName.length > 0);

        if (players.length < 1) throw new Error("En az 1 oyuncu girmelisin");
        return players;
    },

    async membershipRequest() {
        const out = EH.$("memberOut");
        EH.safeText(out, "Üyelik isteği gönderiliyor...");

        try {
            const facilityId = EH.ensureNumber(EH.$("facilitySel")?.value, "Facility seç");
            const r = await EH.API.post("/api/member/membership-requests", { facilityId });
            EH.safeText(out, `OK ✅ RequestId=${r.id} Status=${r.status}`);
            EH.toast("Üyelik isteği gönderildi ✅", "ok");
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            EH.toast(e.message, "err");
            if (e.status === 403) EH.toast("Bu işlem için MEMBER olman lazım (rol/hesap kontrol et).", "info");
        }
    },

    async createReservation() {
        const out = EH.$("memberOut");
        EH.safeText(out, "Rezervasyon oluşturuluyor...");

        try {
            const pitchId = EH.ensureNumber(EH.$("pitchSel")?.value, "Pitch seç");
            const dateStr = EH.$("dateSel")?.value;
            if (!dateStr) throw new Error("Tarih seçmelisin");

            const slotSel = EH.$("slotSel");
            const startMinute = EH.ensureNumber(slotSel?.value, "Başlangıç slotu seç");
            const baseDur = Number(slotSel.selectedOptions[0]?.dataset?.basedur || 60);

            const multiplier = EH.ensureNumber(EH.$("mulSel")?.value, "Süre seç");
            const durationMinutes = baseDur * multiplier;

            const paymentMethod = EH.$("paySel")?.value || "CASH";
            const shuttle = (EH.$("shuttleSel")?.value || "NO") === "YES";

            const startTime = EH.buildStartISO(dateStr, startMinute);
            const players = this.readPlayers();

            const body = { pitchId, startTime, durationMinutes, paymentMethod, players, shuttle };
            const r = await EH.API.post("/api/member/reservations", body);

            EH.safeText(out, `OK ✅ ReservationId=${r.id} Status=${r.status} Price=${r.totalPrice}`);
            EH.toast("Rezervasyon oluşturuldu ✅", "ok");
            await this.loadMyVideos();
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            EH.toast(e.message, "err");
            if (e.status === 403) EH.toast("Access denied: MEMBER onayın var mı? / Rol doğru mu?", "info");
        }
    },

    async loadMyVideos() {
        const box = EH.$("videosBox");
        if (!box) return;

        EH.safeHTML(box, `<div class="opacity-70">Yükleniyor...</div>`);
        try {
            const vids = await EH.API.get("/api/member/videos");
            if (!vids || vids.length === 0) {
                EH.safeHTML(box, `<div class="opacity-70">Henüz video yok. Maç bitince otomatik yayınlanır.</div>`);
                return;
            }

            EH.safeHTML(box, vids.map(v => `
        <a class="block p-4 rounded-2xl glass hover:bg-white/10 transition"
           href="${v.storageUrl}" target="_blank" rel="noreferrer">
          <div class="text-sm opacity-70">Reservation #${v.reservationId}</div>
          <div class="font-semibold">Video: ${v.status}</div>
          <div class="text-xs opacity-70 break-all">${v.storageUrl}</div>
        </a>
      `).join(""));
        } catch (e) {
            EH.safeHTML(box, `<div class="text-red-300">Hata: ${e.message}</div>`);
        }
    }
};

/* -----------------------------
   OWNER: Slot editor (UI rows)
----------------------------- */
EH.ownerSlots = {
    draft: [],

    genHourly24() {
        return Array.from({ length: 24 }, (_, h) => ({
            id: null,
            startMinute: h * 60,
            durationMinutes: 60,
            active: true
        }));
    },

    normalize(list) {
        const seen = new Set();
        return (list || [])
            .filter(x => x && Number.isFinite(Number(x.startMinute)) && Number.isFinite(Number(x.durationMinutes)))
            .map(x => ({
                id: x.id ?? null,
                startMinute: Math.max(0, Math.min(1439, Number(x.startMinute))),
                durationMinutes: Math.max(1, Math.min(24 * 60, Number(x.durationMinutes))),
                active: x.active !== false
            }))
            .filter(x => {
                if (seen.has(x.startMinute)) return false;
                seen.add(x.startMinute);
                return true;
            })
            .sort((a, b) => a.startMinute - b.startMinute);
    },

    ensureEditorScaffold() {
        const box = EH.$("slotBox");
        if (!box) return;

        if (box.dataset.scaffolded === "1") return;
        box.dataset.scaffolded = "1";

        const toolbar = document.createElement("div");
        toolbar.className = "flex flex-wrap items-center gap-2 mb-3";
        toolbar.innerHTML = `
      <button type="button" class="btn glass" id="btnSlotAdd">+ Slot ekle</button>
      <button type="button" class="btn glass" id="btnSlot24">24 Saat (Saatlik) Oluştur</button>
      <button type="button" class="btn glass" id="btnSlotClear">Temizle</button>
      <span class="text-xs opacity-70 ml-auto">Slotları burada düzenle. JSON yazmak yok 🙂</span>
    `;
        box.parentElement?.insertBefore(toolbar, box);

        toolbar.querySelector("#btnSlotAdd").onclick = () => {
            const last = this.draft[this.draft.length - 1];
            const next = last ? Math.min(23 * 60, last.startMinute + 60) : 17 * 60;
            this.draft = this.normalize([...this.draft, { id: null, startMinute: next, durationMinutes: 60, active: true }]);
            this.render();
        };

        toolbar.querySelector("#btnSlot24").onclick = () => {
            this.draft = this.genHourly24();
            this.render();
            EH.toast("24 saat slot taslağı hazır ✅", "ok");
        };

        toolbar.querySelector("#btnSlotClear").onclick = () => {
            this.draft = [];
            this.render();
        };

        const ta = EH.$("slotJson");
        if (ta) {
            ta.style.display = "none";
            const wrap = document.createElement("details");
            wrap.className = "mt-3";
            wrap.innerHTML = `<summary class="cursor-pointer text-sm opacity-75">Gelişmiş: JSON (istersen bak)</summary>`;
            ta.parentElement?.insertBefore(wrap, ta);
            wrap.appendChild(ta);
            ta.style.display = "block";
        }
    },

    render() {
        const box = EH.$("slotBox");
        if (!box) return;

        const list = this.normalize(this.draft);
        this.draft = list;

        if (!list.length) {
            EH.safeHTML(box, `<div class="opacity-70 text-sm">Slot yok. “24 Saat Oluştur” ile hızlıca doldurabilirsin.</div>`);
            this.syncTextarea();
            return;
        }

        EH.safeHTML(box, list.map((s, idx) => {
            const start = EH.fmtMinute(s.startMinute);
            const end = EH.fmtMinute((s.startMinute + s.durationMinutes) % (24 * 60));
            return `
        <div class="p-3 rounded-2xl bg-white/5 border border-white/10">
          <div class="flex items-center gap-2 flex-wrap">
            <div class="text-sm font-semibold min-w-[110px]">${start} - ${end}</div>

            <label class="text-xs opacity-70">Başlangıç</label>
            <input class="input w-[120px]" data-k="start" data-i="${idx}" value="${s.startMinute}" type="number" min="0" max="1439">

            <label class="text-xs opacity-70">Süre</label>
            <select class="input w-[140px]" data-k="dur" data-i="${idx}">
              ${[30,45,60,75,90,105,120].map(m => `<option value="${m}" ${m===s.durationMinutes?"selected":""}>${m} dk</option>`).join("")}
            </select>

            <label class="inline-flex items-center gap-2 text-xs opacity-80 ml-2">
              <input type="checkbox" data-k="act" data-i="${idx}" ${s.active ? "checked" : ""}>
              Aktif
            </label>

            <button class="btn glass ml-auto" type="button" data-k="del" data-i="${idx}">Sil</button>
          </div>
          <div class="text-xs opacity-60 mt-2">startMinute=${s.startMinute} • duration=${s.durationMinutes}</div>
        </div>
      `;
        }).join(""));

        box.querySelectorAll("[data-k]").forEach(el => {
            const k = el.dataset.k;
            const i = Number(el.dataset.i);

            if (k === "start") {
                el.onchange = () => {
                    const v = Number(el.value);
                    this.draft[i].startMinute = Number.isFinite(v) ? v : this.draft[i].startMinute;
                    this.draft = this.normalize(this.draft);
                    this.render();
                };
            }

            if (k === "dur") {
                el.onchange = () => {
                    const v = Number(el.value);
                    this.draft[i].durationMinutes = Number.isFinite(v) ? v : this.draft[i].durationMinutes;
                    this.draft = this.normalize(this.draft);
                    this.render();
                };
            }

            if (k === "act") {
                el.onchange = () => {
                    this.draft[i].active = !!el.checked;
                    this.draft = this.normalize(this.draft);
                    this.syncTextarea();
                };
            }

            if (k === "del") {
                el.onclick = () => {
                    this.draft.splice(i, 1);
                    this.draft = this.normalize(this.draft);
                    this.render();
                };
            }
        });

        this.syncTextarea();
    },

    syncTextarea() {
        const ta = EH.$("slotJson");
        if (!ta) return;
        const minimal = this.draft.map(s => ({
            startMinute: s.startMinute,
            durationMinutes: s.durationMinutes,
            active: s.active
        }));
        ta.value = JSON.stringify(minimal, null, 2);
    },

    importFromTextareaIfAny() {
        const ta = EH.$("slotJson");
        if (!ta) return false;
        const raw = (ta.value || "").trim();
        if (!raw) return false;
        try {
            const arr = JSON.parse(raw);
            if (!Array.isArray(arr)) return false;
            this.draft = this.normalize(arr);
            this.render();
            return true;
        } catch {
            return false;
        }
    }
};

/* -----------------------------
   OWNER actions
----------------------------- */
EH.owner = {
    async loadFacilities() {
        const out = EH.$("ownerOut");
        try {
            const facs = await EH.API.get("/api/owner/facilities");
            const sel = EH.$("ownerFacilitySel");
            if (sel) sel.innerHTML = facs.map(f => `<option value="${f.id}">${f.name}</option>`).join("");
            if (facs.length === 0) EH.safeText(out, "Henüz facility yok. Yukarıdan oluştur.");
            return facs;
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            throw e;
        }
    },

    async createFacility() {
        const out = EH.$("ownerOut");
        EH.safeText(out, "Facility oluşturuluyor...");
        try {
            const name = (EH.$("facName")?.value || "").trim();
            const address = (EH.$("facAddr")?.value || "").trim();
            const r = await EH.API.post("/api/owner/facilities", { name, address });

            EH.safeText(out, `OK ✅ FacilityId=${r.id}`);
            EH.toast("Facility oluşturuldu ✅", "ok");

            await this.loadFacilities();
            await this.loadSlots();
            await this.loadPitches();
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            EH.toast(e.message, "err");
            if (e.status === 403) EH.toast("Access denied: OWNER hesabı ile giriş yaptın mı?", "info");
        }
    },

    async loadPitches() {
        const out = EH.$("ownerOut");
        const facilityId = Number(EH.$("ownerFacilitySel")?.value);
        if (!facilityId) return [];
        try {
            const pitches = await EH.API.get(`/api/owner/facilities/${facilityId}/pitches`);
            const sel = EH.$("ownerPitchSel");
            if (sel) sel.innerHTML = pitches.map(p => `<option value="${p.id}">${p.name}</option>`).join("");
            return pitches;
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            throw e;
        }
    },

    async createPitch() {
        const out = EH.$("ownerOut");
        EH.safeText(out, "Pitch oluşturuluyor...");
        try {
            const facilityId = EH.ensureNumber(EH.$("ownerFacilitySel")?.value, "Facility seç");
            const name = (EH.$("pitchName")?.value || "").trim();
            if (!name) throw new Error("Pitch adı boş olamaz");

            const r = await EH.API.post(`/api/owner/facilities/${facilityId}/pitches`, { name });
            EH.safeText(out, `OK ✅ PitchId=${r.id}`);
            EH.toast("Pitch oluşturuldu ✅", "ok");

            await this.loadPitches();
            await this.loadPricing();
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            EH.toast(e.message, "err");
        }
    },

    async loadDurationsForPricing() {
        const durs = await EH.API.get("/api/public/durations");
        const sel = EH.$("priceDurationSel");
        if (sel) {
            sel.innerHTML = durs.map(d => `<option value="${d.minutes}">${d.label}</option>`).join("");
            sel.value = sel.value || "60";
        }
        return durs;
    },

    async upsertPricing() {
        const out = EH.$("ownerOut");
        EH.safeText(out, "Fiyat kaydediliyor...");
        try {
            const pitchId = EH.ensureNumber(EH.$("ownerPitchSel")?.value, "Pitch seç");
            const durationMinutes = EH.ensureNumber(EH.$("priceDurationSel")?.value, "Süre seç");
            const priceRaw = (EH.$("priceValue")?.value || "").trim().replace(",", ".");
            const price = EH.ensureNumber(priceRaw, "Fiyat gir");

            const r = await EH.API.post("/api/owner/pricing", { pitchId, durationMinutes, price });
            EH.safeText(out, `OK ✅ PricingRuleId=${r.id}`);
            EH.toast("Fiyat kaydedildi ✅", "ok");
            await this.loadPricing();
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            EH.toast(e.message, "err");
            if (e.status === 403) EH.toast("Access denied: OWNER rolü yok gibi. /api/me kontrol et.", "info");
        }
    },

    async loadPricing() {
        const box = EH.$("pricingBox");
        if (!box) return;

        EH.safeHTML(box, `<div class="opacity-70 text-sm">Yükleniyor...</div>`);
        try {
            const pitchId = Number(EH.$("ownerPitchSel")?.value);
            if (!pitchId) { EH.safeHTML(box, `<div class="opacity-70 text-sm">Pitch seç.</div>`); return; }

            const list = await EH.API.get(`/api/owner/pricing?pitchId=${pitchId}`);
            if (!list || list.length === 0) {
                EH.safeHTML(box, `<div class="opacity-70 text-sm">Fiyat girilmemiş.</div>`);
                return;
            }

            EH.safeHTML(box, list.map(x => `
        <div class="p-3 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-between">
          <div class="text-sm opacity-80">durationOptionId=${x.durationOptionId}</div>
          <div class="font-semibold">${x.price} ${x.currency || ""}</div>
        </div>
      `).join(""));
        } catch (e) {
            EH.safeHTML(box, `<div class="text-red-300 text-sm">Hata: ${e.message}</div>`);
        }
    },

    async loadRequests() {
        const box = EH.$("reqBox");
        if (!box) return;

        EH.safeHTML(box, `<div class="opacity-70 text-sm">Yükleniyor...</div>`);
        try {
            const reqs = await EH.API.get(`/api/owner/membership-requests`);
            const facilityId = Number(EH.$("ownerFacilitySel")?.value || 0);
            const filtered = facilityId ? reqs.filter(r => r.facilityId === facilityId) : reqs;

            if (!filtered || filtered.length === 0) {
                EH.safeHTML(box, `<div class="opacity-70 text-sm">Bekleyen istek yok.</div>`);
                return;
            }

            EH.safeHTML(box, filtered.map(r => `
        <div class="p-4 rounded-2xl glass">
          <div class="text-sm opacity-70">Request #${r.id}</div>
          <div class="font-semibold">userId: ${r.userId}</div>
          <div class="text-sm opacity-80">facilityId: ${r.facilityId}</div>
          <div class="text-sm opacity-80">status: ${r.status}</div>
          <div class="mt-3 flex gap-2">
            <button class="flex-1 py-2 rounded-xl btn-primary" type="button"
                    onclick="EH.ownerApprove(${r.id})">Onayla</button>
            <button class="flex-1 py-2 rounded-xl btn" type="button"
                    onclick="EH.ownerReject(${r.id})">Reddet</button>
          </div>
        </div>
      `).join(""));
        } catch (e) {
            EH.safeHTML(box, `<div class="text-red-300 text-sm">Hata: ${e.message}</div>`);
        }
    },

    async loadSlots() {
        const out = EH.$("ownerOut");
        const facilityId = Number(EH.$("ownerFacilitySel")?.value);
        const box = EH.$("slotBox");
        if (!box) return;

        EH.ownerSlots.ensureEditorScaffold();

        EH.safeHTML(box, `<div class="opacity-70 text-sm">Yükleniyor...</div>`);
        if (!facilityId) { EH.safeHTML(box, `<div class="opacity-70 text-sm">Facility seç.</div>`); return; }

        try {
            const slots = await EH.loadSlots(facilityId);

            if (!slots || slots.length === 0) {
                EH.ownerSlots.draft = [];
                EH.ownerSlots.render();
                EH.safeText(out, "Slot yok. İstersen 24 Saat oluşturup kaydedebilirsin.");
                return;
            }

            EH.ownerSlots.draft = EH.ownerSlots.normalize(slots.map(s => ({
                id: s.id ?? null,
                startMinute: s.startMinute,
                durationMinutes: s.durationMinutes,
                active: s.active !== false
            })));

            EH.ownerSlots.render();
        } catch (e) {
            EH.safeHTML(box, `<div class="text-red-300 text-sm">Hata: ${e.message}</div>`);
        }
    },

    async saveSlotsReplace() {
        const out = EH.$("ownerOut");
        EH.safeText(out, "Slotlar kaydediliyor...");

        try {
            const facilityId = EH.ensureNumber(EH.$("ownerFacilitySel")?.value, "Facility seç");

            EH.ownerSlots.importFromTextareaIfAny();

            const slots = EH.ownerSlots.normalize(EH.ownerSlots.draft).map(s => ({
                id: s.id ?? null,
                startMinute: s.startMinute,
                durationMinutes: s.durationMinutes,
                active: s.active !== false
            }));

            if (!slots.length) {
                throw new Error("Kaydetmek için en az 1 slot olmalı. (24 Saat oluştur → Kaydet)");
            }

            await EH.API.put(`/api/owner/facilities/${facilityId}/slots`, slots);

            EH.safeText(out, "OK ✅ Slotlar güncellendi");
            EH.toast("Slotlar güncellendi ✅", "ok");
            await this.loadSlots();
        } catch (e) {
            EH.safeText(out, "Hata: " + e.message);
            EH.toast(e.message, "err");
            if (e.status === 403) EH.toast("Access denied: OWNER hesabı ile giriş yaptın mı?", "info");
        }
    }
};

/* -----------------------------
   Owner approve/reject shortcuts
----------------------------- */
EH.ownerApprove = async (id) => {
    const out = EH.$("ownerOut");
    EH.safeText(out, "Onaylanıyor...");
    try {
        await EH.API.post(`/api/owner/membership-requests/${id}/approve`, {});
        EH.safeText(out, `OK ✅ Approved requestId=${id}`);
        EH.toast("İstek onaylandı ✅", "ok");
        await EH.owner.loadRequests();
    } catch (e) {
        EH.safeText(out, "Hata: " + e.message);
        EH.toast(e.message, "err");
    }
};

EH.ownerReject = async (id) => {
    const out = EH.$("ownerOut");
    EH.safeText(out, "Reddediliyor...");
    try {
        await EH.API.post(`/api/owner/membership-requests/${id}/reject`, {});
        EH.safeText(out, `OK ✅ Rejected requestId=${id}`);
        EH.toast("İstek reddedildi ✅", "ok");
        await EH.owner.loadRequests();
    } catch (e) {
        EH.safeText(out, "Hata: " + e.message);
        EH.toast(e.message, "err");
    }
};
