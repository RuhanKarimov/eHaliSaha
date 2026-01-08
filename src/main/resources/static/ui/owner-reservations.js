// /ui/owner-reservations.js
"use strict";

(function () {
    if (!window.EH || !EH.API) {
        console.error("EH.API yok");
        return;
    }

    const TZ = EH.TZ || "Europe/Istanbul";
    const el = (id) => document.getElementById(id);

    const SEEN_FAC_PREFIX = "eh_owner_seen_res_max_id_fac_";
    const SEEN_GLOBAL_KEY = "eh_owner_seen_res_max_id";

    const SCAN_NEW_COUNT = (fid) => `eh_owner_last_scan_new_count_fac_${fid}`;
    const SCAN_EARLIEST_NEW_DATE = (fid) => `eh_owner_last_scan_earliest_new_date_fac_${fid}`;
    const SCAN_MAX_ID = (fid) => `eh_owner_last_scan_max_id_fac_${fid}`;
    const SCAN_NEW_BY_DATE = (fid) => `eh_owner_last_scan_new_by_date_fac_${fid}`;

    const DAYS_PER_PAGE = 7;

    const state = {
        facilities: [],
        pitches: [],
        list: [],
        dayCache: {},
        dayMeta: {},
        seenThreshold: 0,
        pageStart: null,
        selectedDate: null,
        newByDate: {},
        newCountTotal: 0,
    };

    function out(msg, obj) {
        const box = el("resOut");
        if (!box) return;
        if (obj === undefined || obj === null) box.textContent = String(msg ?? "");
        else box.textContent = `${msg}\n${JSON.stringify(obj, null, 2)}`;
    }

    function todayStrIstanbul() {
        const parts = new Intl.DateTimeFormat("en-CA", {
            timeZone: TZ,
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
        }).formatToParts(new Date());
        return `${parts.find((p) => p.type === "year").value}-${parts.find((p) => p.type === "month").value}-${parts.find((p) => p.type === "day").value}`;
    }

    function addDaysISO(dateStr, days) {
        const [y, m, d] = dateStr.split("-").map(Number);
        const dt = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
        dt.setUTCDate(dt.getUTCDate() + days);
        const yy = dt.getUTCFullYear();
        const mm = String(dt.getUTCMonth() + 1).padStart(2, "0");
        const dd = String(dt.getUTCDate()).padStart(2, "0");
        return `${yy}-${mm}-${dd}`;
    }

    function startOfWeekISO(dateStr) {
        const [y, m, d] = dateStr.split("-").map(Number);
        const dt = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
        const day = dt.getUTCDay();
        const diffToMon = (day === 0) ? -6 : (1 - day);
        dt.setUTCDate(dt.getUTCDate() + diffToMon);
        const yy = dt.getUTCFullYear();
        const mm = String(dt.getUTCMonth() + 1).padStart(2, "0");
        const dd = String(dt.getUTCDate()).padStart(2, "0");
        return `${yy}-${mm}-${dd}`;
    }

    function fmtDayLabel(dateStr) {
        const [y, m, d] = dateStr.split("-").map(Number);
        const dt = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
        return new Intl.DateTimeFormat("tr-TR", {
            timeZone: TZ,
            weekday: "short",
            day: "2-digit",
            month: "short",
        }).format(dt);
    }

    function fmtRangeLabel(fromStr) {
        const toStr = addDaysISO(fromStr, DAYS_PER_PAGE - 1);
        const [y1, m1, d1] = fromStr.split("-").map(Number);
        const [y2, m2, d2] = toStr.split("-").map(Number);
        const dt1 = new Date(Date.UTC(y1, m1 - 1, d1, 12, 0, 0));
        const dt2 = new Date(Date.UTC(y2, m2 - 1, d2, 12, 0, 0));
        const f1 = new Intl.DateTimeFormat("tr-TR", { timeZone: TZ, day: "2-digit", month: "short" }).format(dt1);
        const f2 = new Intl.DateTimeFormat("tr-TR", { timeZone: TZ, day: "2-digit", month: "short" }).format(dt2);
        return `${f1} → ${f2}`;
    }

    function getSeenMaxIdForFacility(fid) {
        try {
            const facVal = Number(localStorage.getItem(SEEN_FAC_PREFIX + fid) || "0");
            if (facVal) return facVal;
            const globalVal = Number(localStorage.getItem(SEEN_GLOBAL_KEY) || "0");
            return globalVal || 0;
        } catch {
            return 0;
        }
    }

    function setSeenMaxIdOnlyIncrease(fid, maxId) {
        const next = Number(maxId || 0);
        if (!next) return;
        try {
            const cur = getSeenMaxIdForFacility(fid);
            if (next <= cur) return;
            localStorage.setItem(SEEN_FAC_PREFIX + fid, String(next));
            const g = Number(localStorage.getItem(SEEN_GLOBAL_KEY) || "0");
            if (next > g) localStorage.setItem(SEEN_GLOBAL_KEY, String(next));
        } catch {}
    }

    function readNotifyScanHints(fid) {
        try {
            const n = Number(localStorage.getItem(SCAN_NEW_COUNT(fid)) || "0");
            state.newCountTotal = n || 0;
            const byDate = localStorage.getItem(SCAN_NEW_BY_DATE(fid));
            state.newByDate = byDate ? (JSON.parse(byDate) || {}) : {};
            return localStorage.getItem(SCAN_EARLIEST_NEW_DATE(fid)) || "";
        } catch {
            state.newCountTotal = 0;
            state.newByDate = {};
            return "";
        }
    }

    function getNotifyMaxId(fid) {
        try {
            return Number(localStorage.getItem(SCAN_MAX_ID(fid)) || "0");
        } catch {
            return 0;
        }
    }

    async function loadFacilities() {
        state.facilities = await EH.API.get("/api/owner/facilities");
        const sel = el("facilitySel");
        sel.innerHTML = "";

        if (!state.facilities.length) {
            sel.innerHTML = `<option value="">(Önce owner panelden tesis oluştur)</option>`;
            out("Hiç tesis yok. Owner Panel → Facility oluştur.");
            return false;
        }

        sel.innerHTML = state.facilities.map((f) => `<option value="${f.id}">${f.name}</option>`).join("");
        sel.value = String(state.facilities[0].id);
        return true;
    }

    async function loadPitches() {
        const fidStr = el("facilitySel").value;
        const fid = Number(fidStr || 0);
        if (!fid) {
            state.pitches = [];
            fillPitchSel();
            return;
        }

        state.pitches = await EH.API.get(`/api/public/facilities/${fid}/pitches`);
        fillPitchSel();
    }

    function fillPitchSel() {
        const sel = el("pitchSel");
        sel.innerHTML = `<option value="">(Hepsi)</option>`;
        for (const p of state.pitches || []) {
            const opt = document.createElement("option");
            opt.value = p.id;
            opt.textContent = p.name;
            sel.appendChild(opt);
        }
    }

    async function fetchLedger(dateStr, force = false) {
        const facilityId = Number(el("facilitySel").value || 0);
        if (!facilityId) return [];

        const pitchIdStr = el("pitchSel").value || "";
        const qs = new URLSearchParams({ facilityId: String(facilityId), date: dateStr });
        if (pitchIdStr) qs.set("pitchId", pitchIdStr);

        if (!force && state.dayCache[dateStr]) return state.dayCache[dateStr];

        const list = await EH.API.get(`/api/owner/reservation-ledger?${qs.toString()}`);
        const arr = Array.isArray(list) ? list : [];
        state.dayCache[dateStr] = arr;
        return arr;
    }

    function computeNewCountForList(list) {
        return (list || []).reduce((acc, r) => (Number(r && r.id ? r.id : 0) > state.seenThreshold ? acc + 1 : acc), 0);
    }

    async function loadDayPage(force = false) {
        const start = state.pageStart || startOfWeekISO(el("dateSel").value || todayStrIstanbul());
        state.pageStart = start;

        el("rangeLabel").textContent = fmtRangeLabel(start);

        const dates = [];
        for (let i = 0; i < DAYS_PER_PAGE; i++) dates.push(addDaysISO(start, i));

        await Promise.all(
            dates.map(async (ds) => {
                const list = await fetchLedger(ds, force);
                state.dayMeta[ds] = { total: list.length, newCount: computeNewCountForList(list) };
            })
        );

        renderDayList(dates);
    }

    function renderDayList(dates) {
        const box = el("dayList");
        box.innerHTML = "";

        for (const ds of dates) {
            const meta = state.dayMeta[ds] || { total: 0, newCount: 0 };
            const isActive = ds === state.selectedDate;

            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = `daybtn ${isActive ? "daybtn-active" : ""}`;
            btn.dataset.date = ds;

            btn.innerHTML = `
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <span class="dot ${meta.newCount > 0 ? "dot-new" : ""}"></span>
              <div class="font-semibold truncate">${fmtDayLabel(ds)}</div>
            </div>
            <div class="text-xs opacity-70 mt-1">${meta.total} rezervasyon</div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            ${meta.newCount > 0 ? `<span class="newcount" title="Yeni">${meta.newCount}</span>` : ""}
          </div>
        </div>
      `;

            btn.addEventListener("click", () => selectDate(ds, { force: false }));
            box.appendChild(btn);
        }
    }

    function renderReservations(dateStr, list) {
        const box = el("listBox");
        const empty = el("emptyHint");
        box.innerHTML = "";

        const meta = state.dayMeta[dateStr] || { total: list.length, newCount: computeNewCountForList(list) };

        el("countPill").textContent = String(meta.total || 0);
        el("newPill").textContent = `${meta.newCount || 0} yeni`;

        el("activeDayTitle").textContent = fmtDayLabel(dateStr);
        el("activeDaySub").textContent = `Toplam: ${meta.total} • Yeni: ${meta.newCount}`;

        if (!list || !list.length) {
            empty.classList.remove("hidden");
            return;
        }
        empty.classList.add("hidden");

        for (const r of list) {
            const rid = Number(r && r.id ? r.id : 0);
            const isNew = rid > state.seenThreshold;

            const start = EH.fmtIstanbulTime ? EH.fmtIstanbulTime(r.startTime) : (r.startTime || "");
            const end = EH.fmtIstanbulTime ? EH.fmtIstanbulTime(r.endTime) : (r.endTime || "");
            const pitchLabel = r.pitchName ?? (r.pitchId ? `pitch#${r.pitchId}` : "pitch");
            const memberLabel = r.memberUsername ?? (r.membershipId ? `membership#${r.membershipId}` : "-");

            const playersHtml = (r.players || []).map((p, idx) => {
                const paid = !!p.paid;
                const cls = paid ? "player-pill player-paid" : "player-pill";
                const jersey = p.jerseyNo ? ` <span class="opacity-70">#${p.jerseyNo}</span>` : "";
                const pid = p.id ?? "";
                return `
          <button type="button" class="${cls}"
                  data-rid="${r.id}" data-idx="${idx}" data-pid="${pid}">
            ${p.fullName}${jersey}
          </button>`;
            }).join(" ");

            const card = document.createElement("div");
            card.className = `card p-5 ${isNew ? "card-new" : ""}`;
            card.innerHTML = `
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="text-lg font-bold truncate">${pitchLabel}</div>
            <div class="text-sm opacity-70">${start} - ${end}</div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            ${isNew ? `<span class="badge" style="border-color:rgba(110,231,183,.35);background:rgba(16,185,129,.14)">Yeni</span>` : ""}
            <div class="text-xs opacity-70">${memberLabel}</div>
          </div>
        </div>

        <div class="mt-4">
          <div class="text-xs opacity-70 mb-2">Oyuncular</div>
          <div class="flex flex-wrap gap-2">
            ${playersHtml || `<span class="text-sm opacity-70">Oyuncu yok</span>`}
          </div>
        </div>
      `;
            box.appendChild(card);
        }

        box.querySelectorAll("button[data-rid][data-idx]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const rid = btn.dataset.rid;
                const idx = Number(btn.dataset.idx);
                const playerId = btn.dataset.pid ? Number(btn.dataset.pid) : null;

                const rr = (state.list || []).find((x) => String(x.id) === String(rid));
                if (!rr || !rr.players || !rr.players[idx]) return;

                const cur = !!rr.players[idx].paid;
                const next = !cur;

                btn.disabled = true;
                try {
                    if (playerId) {
                        await EH.API.req("PATCH", `/api/owner/reservation-ledger/${rid}/players/${playerId}`, { paid: next });
                    } else {
                        await EH.API.req("PATCH", `/api/owner/reservation-ledger/${rid}/players/${idx}`, { paid: next });
                    }
                    rr.players[idx].paid = next;

                    const ds = state.selectedDate;
                    if (ds && state.dayCache[ds]) {
                        const cached = state.dayCache[ds].find((x) => String(x.id) === String(rid));
                        if (cached && cached.players && cached.players[idx]) cached.players[idx].paid = next;
                    }
                    renderReservations(state.selectedDate, state.list);
                } catch (e) {
                    alert("Ödeme işaretleme hatası: " + (e.message || e));
                } finally {
                    btn.disabled = false;
                }
            });
        });
    }

    async function selectDate(dateStr, { force }) {
        state.selectedDate = dateStr;
        el("dateSel").value = dateStr;

        const weekStart = startOfWeekISO(dateStr);
        if (state.pageStart !== weekStart) {
            state.pageStart = weekStart;
            await loadDayPage(false);
        } else {
            const dates = [];
            for (let i = 0; i < DAYS_PER_PAGE; i++) dates.push(addDaysISO(state.pageStart, i));
            renderDayList(dates);
        }

        const list = await fetchLedger(dateStr, !!force);
        state.list = list;
        state.dayMeta[dateStr] = { total: list.length, newCount: computeNewCountForList(list) };
        renderReservations(dateStr, list);
    }

    function resetCaches() {
        state.dayCache = {};
        state.dayMeta = {};
    }

    function markSeenOnceForThisVisit() {
        const fid = Number(el("facilitySel").value || 0);
        if (!fid) return;

        let candidate = getNotifyMaxId(fid) || 0;

        for (const ds of Object.keys(state.dayCache || {})) {
            for (const r of state.dayCache[ds] || []) {
                const rid = Number(r && r.id ? r.id : 0);
                if (rid > candidate) candidate = rid;
            }
        }

        setSeenMaxIdOnlyIncrease(fid, candidate);
    }

    async function boot() {
        const ok = await EH.guard(["OWNER"], "/ui/choose-role.html");
        if (!ok) return;

        const d = el("dateSel");
        if (d && !d.value) d.value = todayStrIstanbul();

        const hasFacilities = await loadFacilities();
        if (!hasFacilities) return;

        await loadPitches();

        const fid = Number(el("facilitySel").value || 0);
        state.seenThreshold = getSeenMaxIdForFacility(fid);

        const earliest = readNotifyScanHints(fid);
        const initialDate = (earliest && /^\d{4}-\d{2}-\d{2}$/.test(earliest)) ? earliest : (el("dateSel").value || todayStrIstanbul());
        el("dateSel").value = initialDate;

        state.pageStart = startOfWeekISO(initialDate);

        await loadDayPage(false);

        const pageDates = [];
        for (let i = 0; i < DAYS_PER_PAGE; i++) pageDates.push(addDaysISO(state.pageStart, i));

        let pick = initialDate;
        const firstNewInPage = pageDates.find((ds) => (state.dayMeta[ds] && state.dayMeta[ds].newCount > 0));
        if (firstNewInPage) pick = firstNewInPage;

        await selectDate(pick, { force: false });

        markSeenOnceForThisVisit();

        el("facilitySel").addEventListener("change", async () => {
            resetCaches();
            await loadPitches();

            const fid2 = Number(el("facilitySel").value || 0);
            state.seenThreshold = getSeenMaxIdForFacility(fid2);
            const earliest2 = readNotifyScanHints(fid2);
            const init2 = (earliest2 && /^\d{4}-\d{2}-\d{2}$/.test(earliest2)) ? earliest2 : todayStrIstanbul();

            el("dateSel").value = init2;
            state.pageStart = startOfWeekISO(init2);

            await loadDayPage(false);

            const pageDates2 = [];
            for (let i = 0; i < DAYS_PER_PAGE; i++) pageDates2.push(addDaysISO(state.pageStart, i));
            const firstNew2 = pageDates2.find((ds) => (state.dayMeta[ds] && state.dayMeta[ds].newCount > 0));
            const pick2 = firstNew2 || init2;

            await selectDate(pick2, { force: false });
            markSeenOnceForThisVisit();
        });

        el("pitchSel").addEventListener("change", async () => {
            resetCaches();
            await loadDayPage(false);
            await selectDate(el("dateSel").value || todayStrIstanbul(), { force: false });
            markSeenOnceForThisVisit();
        });

        el("dateSel").addEventListener("change", async () => {
            const ds = el("dateSel").value || todayStrIstanbul();
            state.pageStart = startOfWeekISO(ds);
            await loadDayPage(false);
            await selectDate(ds, { force: false });
        });

        el("btnRefresh").addEventListener("click", async () => {
            const ds = state.selectedDate || (el("dateSel").value || todayStrIstanbul());
            await loadDayPage(true);
            await selectDate(ds, { force: true });
            out("Yenilendi ✅");
        });

        el("btnToday").addEventListener("click", async () => {
            const t = todayStrIstanbul();
            el("dateSel").value = t;
            state.pageStart = startOfWeekISO(t);
            await loadDayPage(false);
            await selectDate(t, { force: false });
        });

        el("btnPrevPage").addEventListener("click", async () => {
            state.pageStart = addDaysISO(state.pageStart, -DAYS_PER_PAGE);
            await loadDayPage(false);
            const ds = state.selectedDate;
            const weekStart = startOfWeekISO(ds);
            if (weekStart !== state.pageStart) await selectDate(state.pageStart, { force: false });
            else renderDayList(Array.from({ length: DAYS_PER_PAGE }, (_, i) => addDaysISO(state.pageStart, i)));
        });

        el("btnNextPage").addEventListener("click", async () => {
            state.pageStart = addDaysISO(state.pageStart, DAYS_PER_PAGE);
            await loadDayPage(false);
            const ds = state.selectedDate;
            const weekStart = startOfWeekISO(ds);
            if (weekStart !== state.pageStart) await selectDate(state.pageStart, { force: false });
            else renderDayList(Array.from({ length: DAYS_PER_PAGE }, (_, i) => addDaysISO(state.pageStart, i)));
        });

        out("Hazır ✅ (Yeni rezervasyon varsa otomatik en erken güne atlar)");
    }

    boot();
})();
