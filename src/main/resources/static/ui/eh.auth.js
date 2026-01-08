// /ui/eh.auth.js
"use strict";

(function () {
  window.EH = window.EH || {};
  if (!EH.API) {
    console.error("EH.API yok. Script sırası: eh.core -> eh.api -> eh.auth olmalı.");
    return;
  }

  EH.getMe = EH.getMe || (async () => EH.API.get("/api/me"));

  EH.guard = EH.guard || (async (allowedRoles, redirectUrl = "/ui/choose-role.html") => {
    // Token hiç yoksa direkt yönlendir (loop yapma)
    if (!EH.API.getToken()) {
      location.href = redirectUrl;
      return false;
    }

    try {
      const me = await EH.getMe();
      if (!me?.role) throw new Error("Unauthorized");

      if (!allowedRoles.includes(me.role)) {
        const target =
            me.role === "OWNER" ? "/ui/owner.html" :
                me.role === "MEMBER" ? "/ui/member.html" :
                    redirectUrl;

        // Rol uyuşmadı -> token durabilir (aynı kullanıcı farklı sayfaya girmiş olabilir)
        location.href = target;
        return false;
      }

      return true;
    } catch (e) {
      // 401 ise token bozuk/yanlış: temizle
      if (e?.status === 401) EH.API.clearAuth();
      location.href = redirectUrl;
      return false;
    }
  });

  EH.logout = EH.logout || (() => {
    EH.API.clearAuth();
    location.href = "/ui/choose-role.html";
  });
})();
