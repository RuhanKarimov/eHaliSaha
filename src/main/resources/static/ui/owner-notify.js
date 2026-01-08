// /ui/owner-notify.js
"use strict";

/**
 * Owner sayfasındaki "Rezervasyonlar" butonunun sağ üstüne
 * Instagram bildirim mantığı gibi "yeni rezervasyon sayısı" basar.
 *
 * Notlar:
 * - Mantık: localStorage'taki "seen max id" ile /reservation-ledger dönen id'leri kıyaslar.
 * - Bu script "seen" değerini ASLA arttırmaz (sadece okur). Böylece bildirim "kaybolmaz".
 * - Son tarama özetini localStorage'a yazar: en erken yeni tarih, yeni sayısı, taramada görülen max id, gün bazlı yeni sayıları.
 */
(function () {
    if (!window.EH || !EH.API) return;

    const TZ = EH.TZ || "Europe/Istanbul";
    const el = (id) => document.getElementById(id);

    // Owner HTML'de var: <span id="resNotifBadge" ...>
    const badgeEl = () => el("resNotifBadge");

    const SEEN_FAC_PREFIX = "eh_owner_seen_res_max_id_fac_";
    const SEEN_GLOBAL_KEY = "eh_owner_seen_res_max_id";

    // 👇 owner-reservations sayfası bunları okuyacak (sıçrama + gün listesi için)
    const SCAN_NEW_COUNT = (fid) => `eh_owner_last_scan_new_count_fac_${fid}`;
    const SCAN_EARLIEST_NEW_DATE = (fid) => `eh_owner_last_scan_earliest_new_date_fac_${fid}`;
    const SCAN_MAX_ID = (fid) => `eh_owner_last_scan_max_id_fac_${fid}`;
    const SCAN_NEW_BY_DATE = (fid) => `eh_owner_last_scan_new_by_date_fac_${fid}`;
    const SCAN_AT = (fid) => `eh_owner_last_scan_at_fac_${fid}`;

    // Performans: çok büyütme. Çoğu demo/gerçek kullanım için 14 gün yeterli.
    // İstersen 21 yapabilirsin (daha çok istek atar).
    const DAYS_TO_SCAN = 14;
    const POLL_MS = 25000;

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

    function setBadge(n) {
        const b = badgeEl();
        if (!b) return;
        const num = Number(n || 0);

        if (num <= 0) {
            b.classList.add("hidden");
            b.textContent = "";
            return;
        }

        b.textContent = String(num);
        b.classList.remove("hidden");
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
        // DST'e takılmamak için UTC öğlen
        const [y, m, d] = dateStr.split("-").map(Number);
        const dt = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
        dt.setUTCDate(dt.getUTCDate() + days);
        const yy = dt.getUTCFullYear();
        const mm = String(dt.getUTCMonth() + 1).padStart(2, "0");
        const dd = String(dt.getUTCDate()).padStart(2, "0");
        return `${yy}-${mm}-${dd}`;
    }

    async function getFacilityIdFromOwnerPage() {
        // owner.html'de select id="ownerFacilitySel"
        const sel = el("ownerFacilitySel");
        const v = sel ? sel.value : "";
        const fid = Number(v || 0);
        return fid || 0;
    }

    async function fetchLedgerDay(facilityId, dateStr) {
        const qs = new URLSearchParams({ facilityId: String(facilityId), date: dateStr });
        return await EH.API.get(`/api/owner/reservation-ledger?${qs.toString()}`);
    }

    function storeScanSummary(fid, { newCount, earliestNewDate, maxId, newByDate }) {
        try {
            localStorage.setItem(SCAN_NEW_COUNT(fid), String(newCount || 0));
            if (earliestNewDate) localStorage.setItem(SCAN_EARLIEST_NEW_DATE(fid), earliestNewDate);
            else localStorage.removeItem(SCAN_EARLIEST_NEW_DATE(fid));

            localStorage.setItem(SCAN_MAX_ID(fid), String(maxId || 0));
            localStorage.setItem(SCAN_NEW_BY_DATE(fid), JSON.stringify(newByDate || {}));
            localStorage.setItem(SCAN_AT(fid), String(Date.now()));
        } catch {
            // ignore
        }
    }

    async function refreshBadge() {
        const fid = await getFacilityIdFromOwnerPage();
        if (!fid) {
            setBadge(0);
            return;
        }

        const seen = getSeenMaxIdForFacility(fid);
        const base = todayStrIstanbul();

        let newCount = 0;
        let earliestNewDate = null;
        let maxId = seen;
        const newByDate = {};

        for (let i = 0; i < DAYS_TO_SCAN; i++) {
            const dateStr = addDaysISO(base, i);

            let list = [];
            try {
                list = await fetchLedgerDay(fid, dateStr);
                if (!Array.isArray(list)) list = [];
            } catch (e) {
                continue;
            }

            for (const r of list) {
                const rid = Number(r && r.id ? r.id : 0);
                if (rid > maxId) maxId = rid;

                if (rid > seen) {
                    newCount++;
                    if (!earliestNewDate || dateStr < earliestNewDate) earliestNewDate = dateStr;
                    newByDate[dateStr] = (newByDate[dateStr] || 0) + 1;
                }
            }
        }

        storeScanSummary(fid, { newCount, earliestNewDate, maxId, newByDate });
        setBadge(newCount);
    }

    async function boot() {
        await refreshBadge();
        setInterval(refreshBadge, POLL_MS);
    }

    boot();
})();
