// /ui/member.js
"use strict";

(function () {
    // ---- idempotent load guard (double include olursa sorun çıkarmasın)
    if (window.__EH_MEMBER_JS_LOADED__) return;
    window.__EH_MEMBER_JS_LOADED__ = true;

    // ---- EH bootstrap
    window.EH = window.EH || {};
    const EH = window.EH;

    if (!EH.API) {
        console.error("EH.API yok. Script sırası: eh.core -> eh.api -> eh.auth -> member olmalı.");
        return;
    }

    const TZ = EH.TZ || "Europe/Istanbul";
    const IST_OFFSET = "+03:00"; // Türkiye kalıcı +03:00 (Instant kayması olmasın diye manual)
    const el = (id) => document.getElementById(id);

    // ---- tiny UI helpers
    function setOut(msg, type = "info") {
        const box = el("memberOut");
        if (!box) return;
        box.textContent = msg || "";
        // pre tag olduğu için class değiştirmiyoruz (UI bozulmasın), sadece metin basıyoruz.
    }

    function safeJsonFromError(e) {
        // EH.API hata mesajını JSON string olarak fırlatıyor (message içinde)
        if (!e) return null;
        if (e.body) return e.body;
        const m = e.message;
        if (typeof m === "string" && m.trim().startsWith("{")) {
            try { return JSON.parse(m); } catch { return null; }
        }
        return null;
    }

    function fmtMinute(min) {
        const h = String(Math.floor(min / 60)).padStart(2, "0");
        const m = String(min % 60).padStart(2, "0");
        return `${h}:${m}`;
    }

    function buildStartISO(dateStr, startMinute) {
        // YYYY-MM-DD + HH:mm + fixed +03:00
        const hh = String(Math.floor(startMinute / 60)).padStart(2, "0");
        const mm = String(startMinute % 60).padStart(2, "0");
        return `${dateStr}T${hh}:${mm}:00${IST_OFFSET}`;
    }

    function todayStrIstanbul() {
        const parts = new Intl.DateTimeFormat("en-CA", {
            timeZone: TZ, year: "numeric", month: "2-digit", day: "2-digit"
        }).formatToParts(new Date());
        const y = parts.find(p => p.type === "year")?.value;
        const m = parts.find(p => p.type === "month")?.value;
        const d = parts.find(p => p.type === "day")?.value;
        return `${y}-${m}-${d}`;
    }

    function fmtMoney(amount, currency = "TRY") {
        if (amount == null || Number.isNaN(Number(amount))) return "-";
        try {
            return new Intl.NumberFormat("tr-TR", { style: "currency", currency }).format(Number(amount));
        } catch {
            return `${amount} ${currency}`;
        }
    }

    // ---- state
    const state = {
        me: null,
        facilities: [],
        pitches: [],
        slots: [],        // from /availability
        selectedSlot: null,
        price: null,      // price quote dto
        priceOk: false,
        players: []       // {fullName, jerseyNo}
    };

    // ---- DOM refs
    const $facilitySel = el("facilitySel");
    const $pitchSel = el("pitchSel");
    const $dateSel = el("dateSel");
    const $paySel = el("paySel");
    const $slotGrid = el("slotGrid");
    const $slotLegend = el("slotLegend");

    const $sumLine = el("summaryLine");
    const $sumSub = el("summarySub");
    const $sumMinutes = el("sumMinutes");
    const $sumPrice = el("sumPrice");
    const $sumPay = el("sumPay");

    const $btnMembership = el("btnMembership");
    const $btnReserve = el("btnReserve");
    const $btnAddPlayer = el("btnAddPlayer");
    const $btnFillPlayers = el("btnFillPlayers");

    const $playersBox = el("playersBox");

    const $btnVideos = el("btnVideos");
    const $videosBox = el("videosBox");

    // ---- render helpers
    function selectedFacilityId() {
        const v = $facilitySel?.value || "";
        return v ? Number(v) : null;
    }

    function selectedPitchId() {
        const v = $pitchSel?.value || "";
        return v ? Number(v) : null;
    }

    function getFacilityName(fid) {
        return state.facilities.find(f => String(f.id) === String(fid))?.name || "-";
    }

    function getPitchName(pid) {
        return state.pitches.find(p => String(p.id) === String(pid))?.name || "-";
    }

    function updateSummary() {
        const date = $dateSel?.value || "";
        const fid = selectedFacilityId();
        const pid = selectedPitchId();

        const facName = fid ? getFacilityName(fid) : "-";
        const pitchName = pid ? getPitchName(pid) : "-";

        if (!$sumMinutes) return;

        $sumMinutes.textContent = "60 dk";

        if (!date || !fid || !pid || !state.selectedSlot) {
            if ($sumLine) $sumLine.textContent = "Henüz seçim yok";
            if ($sumSub) $sumSub.textContent = "";
        } else {
            const s = state.selectedSlot;
            const t1 = fmtMinute(s.startMinute);
            const t2 = fmtMinute(s.startMinute + (s.durationMinutes ?? 60));
            if ($sumLine) $sumLine.textContent = `${facName} • ${pitchName}`;
            if ($sumSub) $sumSub.textContent = `${date} • ${t1}-${t2}`;
        }

        // price ui
        if (!state.priceOk) {
            if ($sumPrice) $sumPrice.textContent = "Fiyat yok";
            if ($sumPay) $sumPay.textContent = "Owner 60 dk fiyat girmemiş";
            if ($btnReserve) $btnReserve.disabled = true;
        } else {
            const cur = state.price?.currency || "TRY";
            const shown = state.price?.totalPrice ?? state.price?.basePrice;
            if ($sumPrice) $sumPrice.textContent = fmtMoney(shown, cur);
            if ($sumPay) $sumPay.textContent = ($paySel?.value === "CARD") ? "Kart ile ödeme" : "Nakit ödeme";
            if ($btnReserve) $btnReserve.disabled = false;
        }
    }

    function renderSlots() {
        if (!$slotGrid) return;
        $slotGrid.innerHTML = "";

        if (!state.slots?.length) {
            $slotGrid.innerHTML = `<div class="text-sm opacity-70">Slot yok. (Owner slot tanımlamamış olabilir)</div>`;
            if ($slotLegend) $slotLegend.textContent = "Slot bulunamadı";
            return;
        }

        if ($slotLegend) $slotLegend.textContent = "Seç: boş slot";

        for (const s of state.slots) {
            const btn = document.createElement("button");
            btn.type = "button";

            const isSelected =
                state.selectedSlot &&
                state.selectedSlot.startMinute === s.startMinute;

            const disabled = !s.active || s.occupied;
            const clsBase = "slot flex items-center justify-between gap-2";
            const clsSelected = isSelected ? " slot-on" : "";
            const clsDisabled = disabled ? " slot-full" : "";

            btn.className = clsBase + clsSelected + clsDisabled;

            const t1 = fmtMinute(s.startMinute);
            const t2 = fmtMinute(s.startMinute + (s.durationMinutes ?? 60));

            const badge =
                !s.active ? `<span class="pill opacity-80">KAPALI</span>` :
                    s.occupied ? `<span class="pill opacity-80">DOLU</span>` :
                        `<span class="pill opacity-80">BOŞ</span>`;

            btn.innerHTML = `
        <div class="flex items-center gap-2">
          <span class="slot-dot"></span>
          <div class="text-sm font-semibold">${t1}-${t2}</div>
        </div>
        ${badge}
      `;

            btn.disabled = disabled;

            btn.addEventListener("click", () => {
                if (disabled) return;
                state.selectedSlot = s;
                renderSlots();
                updateSummary();
            });

            $slotGrid.appendChild(btn);
        }
    }

    function renderPlayers() {
        if (!$playersBox) return;
        $playersBox.innerHTML = "";

        if (!state.players.length) {
            $playersBox.innerHTML = `<div class="text-sm opacity-70">Henüz oyuncu yok. “+ Oyuncu ekle” ile ekle.</div>`;
            return;
        }

        state.players.forEach((p, idx) => {
            const row = document.createElement("div");
            row.className = "card p-3 flex flex-wrap gap-3 items-center";

            row.innerHTML = `
        <div class="flex-1 min-w-[220px]">
          <div class="text-xs opacity-70">Oyuncu Ad Soyad</div>
          <input class="input mt-1" data-k="fullName" data-idx="${idx}" placeholder="Örn: Ahmet Yılmaz" value="${p.fullName ?? ""}">
        </div>

        <div class="w-36">
          <div class="text-xs opacity-70">Forma No</div>
          <input class="input mt-1" data-k="jerseyNo" data-idx="${idx}" placeholder="0-99" value="${p.jerseyNo ?? ""}">
        </div>

        <button type="button" class="btn btn-ghost" data-del="${idx}">Sil</button>
      `;

            $playersBox.appendChild(row);
        });

        // input handlers
        $playersBox.querySelectorAll("input[data-k][data-idx]").forEach(inp => {
            inp.addEventListener("input", () => {
                const idx = Number(inp.dataset.idx);
                const k = inp.dataset.k;
                if (!state.players[idx]) return;
                if (k === "fullName") state.players[idx].fullName = inp.value;
                if (k === "jerseyNo") {
                    const v = inp.value.trim();
                    state.players[idx].jerseyNo = v === "" ? null : Number(v);
                }
            });
        });

        // delete handlers
        $playersBox.querySelectorAll("button[data-del]").forEach(btn => {
            btn.addEventListener("click", () => {
                const idx = Number(btn.dataset.del);
                state.players.splice(idx, 1);
                renderPlayers();
            });
        });
    }

    // ---- data loaders
    async function loadMe() {
        state.me = await EH.getMe();
        const pill = el("mePill");
        if (pill && state.me) {
            pill.classList.remove("hidden");
            pill.textContent = `${state.me.username} • ${state.me.role}`;
        }
    }

    async function loadFacilities() {
        state.facilities = await EH.API.get("/api/public/facilities");
        if (!$facilitySel) return;

        $facilitySel.innerHTML = state.facilities.map(f => `<option value="${f.id}">${f.name}</option>`).join("");

        if (!$facilitySel.value && state.facilities.length) {
            $facilitySel.value = String(state.facilities[0].id);
        }
    }

    async function loadPitches() {
        const fid = selectedFacilityId();
        state.pitches = [];

        if (!$pitchSel) return;
        $pitchSel.innerHTML = "";

        if (!fid) return;

        state.pitches = await EH.API.get(`/api/public/facilities/${fid}/pitches`);
        $pitchSel.innerHTML = state.pitches.map(p => `<option value="${p.id}">${p.name}</option>`).join("");

        if (!$pitchSel.value && state.pitches.length) {
            $pitchSel.value = String(state.pitches[0].id);
        }
    }

    async function loadAvailability() {
        const pid = selectedPitchId();
        const date = $dateSel?.value || todayStrIstanbul();
        state.slots = [];
        state.selectedSlot = null;

        if (!pid) {
            renderSlots();
            updateSummary();
            return;
        }

        try {
            state.slots = await EH.API.get(`/api/public/pitches/${pid}/availability?date=${encodeURIComponent(date)}`);
        } catch (e) {
            const j = safeJsonFromError(e);
            const msg = j?.message || e.message || "Availability alınamadı";
            setOut(`Slotlar yüklenemedi: ${msg}`, "err");
            state.slots = [];
        }

        renderSlots();
        updateSummary();
    }

    async function loadPriceBase() {
        const pid = selectedPitchId();
        state.price = null;
        state.priceOk = false;

        if (!pid) {
            updateSummary();
            return;
        }

        try {
            state.price = await EH.API.get(`/api/public/pitches/${pid}/price?baseMinutes=60`);
            state.priceOk = true;
        } catch (e) {
            const j = safeJsonFromError(e);
            const status = e?.status ?? j?.status;

            // ✅ KRİTİK FIX: 409 ise akışı durdurma, UI’yi kilitleme
            if (status === 409) {
                state.price = null;
                state.priceOk = false;
                // kullanıcıya nazikçe söyle
                setOut("Bu saha için 60 dk fiyat tanımlı değil. Owner panelden fiyat girilince otomatik görünecek.", "info");
            } else {
                const msg = j?.message || e.message || "Fiyat alınamadı";
                setOut(`Fiyat alınamadı: ${msg}`, "err");
            }
        }

        updateSummary();
    }

    async function loadVideos() {
        if (!$videosBox) return;
        $videosBox.innerHTML = `<div class="text-sm opacity-70">Yükleniyor...</div>`;

        try {
            const list = await EH.API.get("/api/member/videos");
            if (!list.length) {
                $videosBox.innerHTML = `<div class="text-sm opacity-70">Henüz video yok.</div>`;
                return;
            }

            $videosBox.innerHTML = list.map(v => {
                const url = v.storageUrl || v.storage || "#";
                const st = v.status || "PUBLISHED";
                return `
          <div class="card p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="font-semibold">Rez#${v.reservationId}</div>
              <span class="pill">${st}</span>
            </div>
            <a class="mt-2 inline-block underline opacity-90 hover:opacity-100" href="${url}" target="_blank" rel="noopener">
              Videoyu aç
            </a>
          </div>
        `;
            }).join("");
        } catch (e) {
            $videosBox.innerHTML = `<div class="text-sm opacity-70">Video listesi alınamadı.</div>`;
        }
    }

    // ---- actions
    async function createMembershipRequest() {
        const fid = selectedFacilityId();
        if (!fid) return setOut("Önce facility seç.", "err");

        try {
            const res = await EH.API.post("/api/member/membership-requests", { facilityId: fid });
            setOut("Üyelik isteği gönderildi ✅");
            EH.toast?.("Üyelik isteği gönderildi ✅", "ok");
            return res;
        } catch (e) {
            const j = safeJsonFromError(e);
            setOut(`Üyelik isteği hatası: ${j?.message || e.message}`, "err");
        }
    }

    async function createReservation() {
        const fid = selectedFacilityId();
        const pid = selectedPitchId();
        const date = $dateSel?.value || todayStrIstanbul();
        const pm = $paySel?.value || "CASH";
        const shuttle = (el("shuttleSel")?.value || "NO") === "YES";

        if (!fid || !pid) return setOut("Facility + pitch seçmelisin.", "err");
        if (!state.selectedSlot) return setOut("Boş bir saat seçmelisin.", "err");

        // fiyat yoksa rezervasyonu kilitle (backend de fail edecek)
        if (!state.priceOk) {
            return setOut("Bu saha için 60 dk fiyat tanımlı değil. Owner fiyat girmeden rezervasyon yapılamaz.", "err");
        }

        // players validate
        const players = (state.players || [])
            .map(p => ({
                fullName: String(p.fullName || "").trim(),
                jerseyNo: (p.jerseyNo === null || p.jerseyNo === undefined || p.jerseyNo === "") ? null : Number(p.jerseyNo)
            }))
            .filter(p => p.fullName.length > 0);

        if (players.length < 1) return setOut("En az 1 oyuncu adı gerekli.", "err");

        const payload = {
            pitchId: pid,
            startTime: buildStartISO(date, state.selectedSlot.startMinute),
            durationMinutes: 60,          // member sabit 60
            paymentMethod: pm,
            players,
            shuttle
        };

        try {
            const created = await EH.API.post("/api/member/reservations", payload);
            setOut("Rezervasyon alındı ✅");
            EH.toast?.("Rezervasyon alındı ✅", "ok");
            // refresh occupancy
            await Promise.allSettled([loadAvailability(), loadVideos()]);
            return created;
        } catch (e) {
            const j = safeJsonFromError(e);
            if (j?.fields) {
                // validasyon detayını kısa bas
                const firstKey = Object.keys(j.fields)[0];
                setOut(`Rezervasyon hatası: ${firstKey} → ${j.fields[firstKey]}`, "err");
            } else {
                setOut(`Rezervasyon hatası: ${j?.message || e.message}`, "err");
            }
        }
    }

    function addPlayerRow(prefill = null) {
        state.players.push({
            fullName: prefill?.fullName ?? "",
            jerseyNo: prefill?.jerseyNo ?? null
        });
        renderPlayers();
    }

    // ---- master refresh (409 olsa bile slotlar kesin yüklenecek)
    async function refreshAll() {
        // date set
        if ($dateSel && !$dateSel.value) $dateSel.value = todayStrIstanbul();

        // facilities/pitches chain
        await loadFacilities();
        await loadPitches();

        // ✅ price 409 olsa bile availability render olacak
        await Promise.allSettled([
            loadPriceBase(),
            loadAvailability()
        ]);

        updateSummary();
    }

    // ---- boot
    async function boot() {
        const ok = await EH.guard(["MEMBER"], "/ui/choose-role.html");
        if (!ok) return;

        await loadMe();

        // init default date
        if ($dateSel && !$dateSel.value) $dateSel.value = todayStrIstanbul();

        // attach listeners
        $facilitySel?.addEventListener("change", async () => {
            await loadPitches();
            await Promise.allSettled([loadPriceBase(), loadAvailability()]);
        });

        $pitchSel?.addEventListener("change", async () => {
            await Promise.allSettled([loadPriceBase(), loadAvailability()]);
        });

        $dateSel?.addEventListener("change", async () => {
            await loadAvailability();
        });

        $paySel?.addEventListener("change", updateSummary);

        $btnMembership?.addEventListener("click", createMembershipRequest);
        $btnReserve?.addEventListener("click", createReservation);

        $btnAddPlayer?.addEventListener("click", () => addPlayerRow());
        $btnFillPlayers?.addEventListener("click", () => {
            if (!state.players.length) {
                addPlayerRow({ fullName: "Ahmet Yılmaz", jerseyNo: 7 });
                addPlayerRow({ fullName: "Mehmet Kaya", jerseyNo: 10 });
            }
        });

        $btnVideos?.addEventListener("click", loadVideos);

        // initial
        renderPlayers();
        await refreshAll();
        await loadVideos();
    }

    boot();
})();
