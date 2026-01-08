// /ui/eh.api.js
"use strict";

(function () {
    // EH tek kez oluşsun
    window.EH = window.EH || {};

    // Zaten yüklüyse tekrar tanımlama (Identifier 'EH' already declared hatasını engeller)
    if (EH.API) return;

    const KEY_MAIN = "eh_basic";
    const KEY_LEGACY = "eh_basic_token";

    function getStoredToken() {
        // önce main, yoksa legacy
        return localStorage.getItem(KEY_MAIN) || localStorage.getItem(KEY_LEGACY);
    }

    function setStoredToken(t) {
        // ikisine de yaz: eski kodlar hangisini okursa okusun çalışsın
        localStorage.setItem(KEY_MAIN, t);
        localStorage.setItem(KEY_LEGACY, t);
    }

    function clearStoredToken() {
        localStorage.removeItem(KEY_MAIN);
        localStorage.removeItem(KEY_LEGACY);
    }

    EH.API = {
        base: "",

        tokenKey: KEY_MAIN, // bilgi amaçlı

        setBasicAuth(username, password) {
            // Basic: base64("u:p")
            const t = btoa(`${username}:${password}`);
            setStoredToken(t);
            return t;
        },

        getToken() {
            return getStoredToken();
        },

        clearAuth() {
            clearStoredToken();
        },

        getAuthHeader() {
            const t = getStoredToken();
            return t ? { Authorization: `Basic ${t}` } : {};
        },

        async req(method, url, body) {
            // ✅ asıl hatanın temiz hali: ...spread
            const headers = { ...this.getAuthHeader() };

            const opts = { method, headers };

            if (body !== undefined) {
                opts.headers["Content-Type"] = "application/json";
                opts.body = JSON.stringify(body);
            }

            const res = await fetch(this.base + url, opts);

            // Hata mesajını güzel çıkar
            if (!res.ok) {
                let msg = "";
                try {
                    const ct = res.headers.get("content-type") || "";
                    msg = ct.includes("application/json") ? JSON.stringify(await res.json()) : await res.text();
                } catch {
                    msg = "";
                }

                const err = new Error(msg || `${res.status} ${res.statusText}`);
                err.status = res.status;
                err.url = res.url;
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
        del(url) { return this.req("DELETE", url); },
    };
})();
