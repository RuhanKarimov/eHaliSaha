// /ui/eh.time.js
"use strict";

/*
  Time helpers (Europe/Istanbul)
  - İstemeden iki kez include edilse bile parse-time hata üretmesin diye:
    * global scope’ta "const EH = ..." gibi deklarasyon yok
    * içerde idempotent guard var
*/
(function () {
    window.EH = window.EH || {};
    const EH = window.EH;

    // idempotent
    if (EH.__timeLoaded) return;
    EH.__timeLoaded = true;

    EH.TZ = EH.TZ || "Europe/Istanbul";

    // Türkiye 2016’dan beri sabit UTC+03 (DST yok). Bu proje için sabit offset kullanıyoruz.
    EH.IST_OFFSET = EH.IST_OFFSET || "+03:00";

    EH.fmtMinute = EH.fmtMinute || ((min) => {
        const h = Math.floor(min / 60);
        const m = min % 60;
        return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
    });

    // yyyy-mm-dd + startMinute => ISO with Istanbul offset (backend Instant parse eder)
    EH.buildStartISO = EH.buildStartISO || ((dateStr, startMinute) => {
        const h = Math.floor(startMinute / 60);
        const m = startMinute % 60;
        const hh = String(h).padStart(2, "0");
        const mm = String(m).padStart(2, "0");
        return `${dateStr}T${hh}:${mm}:00${EH.IST_OFFSET}`;
    });

    // Instant ISO (Z / +offset) => "DD.MM.YYYY HH:mm" (İstanbul TZ)
    EH.fmtIstanbulDateTime = EH.fmtIstanbulDateTime || ((iso) => {
        if (!iso) return "-";
        const d = (iso instanceof Date) ? iso : new Date(iso);
        const fmt = new Intl.DateTimeFormat("tr-TR", {
            timeZone: EH.TZ,
            year: "numeric", month: "2-digit", day: "2-digit",
            hour: "2-digit", minute: "2-digit",
            hour12: false
        });
        // bazı browser’lar "04.01.2026 21:00" bazıları "04.01.2026 21:00" (comma) verir
        return fmt.format(d).replace(",", "");
    });

    EH.fmtIstanbulTime = EH.fmtIstanbulTime || ((iso) => {
        if (!iso) return "-";
        const d = (iso instanceof Date) ? iso : new Date(iso);
        const fmt = new Intl.DateTimeFormat("tr-TR", {
            timeZone: EH.TZ,
            hour: "2-digit", minute: "2-digit",
            hour12: false
        });
        return fmt.format(d);
    });
})();
