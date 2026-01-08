// /ui/choose-role.js
(function () {
    // Keyboard UX
    document.addEventListener("keydown", (e) => {
        const k = (e.key || "").toLowerCase();
        if (k === "m") location.href = "/ui/login.html?role=MEMBER";
        if (k === "o") location.href = "/ui/login.html?role=OWNER";
    });

    // İstersen: Kullanıcı zaten login olmuşsa role göre panele git
    (async () => {
        try {
            if (!window.EH?.getMe) return; // app.js yoksa sessizce çık
            const me = await EH.getMe();
            if (me?.role === "OWNER") return (location.href = "/ui/owner.html");
            if (me?.role === "MEMBER") return (location.href = "/ui/member.html");
        } catch {
            // login yoksa sorun değil, bu sayfa zaten rol seçtiriyor
        }
    })();
})();
