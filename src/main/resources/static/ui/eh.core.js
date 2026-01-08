(() => {
    "use strict";

    window.EH = window.EH || {};
    const EH = window.EH;

    // Tiny toast (stacked)
    EH.toast = EH.toast || ((msg, type = "info") => {
        let root = document.getElementById("toastRoot");
        if (!root) {
            root = document.createElement("div");
            root.id = "toastRoot";
            root.className = "toast";
            document.body.appendChild(root);
        }

        const el = document.createElement("div");
        el.textContent = msg;
        el.className = type === "ok" ? "ok" : type === "err" ? "err" : "info";
        root.appendChild(el);
        setTimeout(() => el.remove(), 3200);
    });

    // DOM helpers
    EH.$ = (id) => document.getElementById(id);
    EH.safeText = (el, txt) => { if (el) el.textContent = txt ?? ""; };
    EH.safeHTML = (el, html) => { if (el) el.innerHTML = html ?? ""; };
    EH.sleep = (ms) => new Promise(r => setTimeout(r, ms));

    // UTF-8 safe base64
    EH.b64 = (str) => {
        const bytes = new TextEncoder().encode(str);
        let bin = "";
        for (const b of bytes) bin += String.fromCharCode(b);
        return btoa(bin);
    };

    // error parsing
    EH.prettyErr = EH.prettyErr || (async (res) => {
        const txt = await res.text();
        try {
            const j = JSON.parse(txt);
            if (j?.message) return j.message;
            if (j?.error) return j.error;
        } catch {}
        return txt || `${res.status} ${res.statusText}`;
    });
})();
