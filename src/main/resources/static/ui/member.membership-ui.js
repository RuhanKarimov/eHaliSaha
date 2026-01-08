// /ui/member.membership-ui.js
// Amaç: Member panelinde üyelik durumunu (aktif/beklemede/yok) UI'a yansıtmak.
// - Backend: GET /api/member/membership-status?facilityId=...
// - Mevcut member.js'in rezervasyon mantığına dokunmaz.
"use strict";

(function () {
    if (!window.EH || !EH.API) {
        console.warn("EH.API bulunamadı (member.membership-ui.js)");
        return;
    }

    const el = (id) => document.getElementById(id);

    function setBtnStyle(btn, mode) {
        if (!btn) return;

        btn.style.borderColor = "";
        btn.style.background = "";
        btn.style.color = "";

        if (mode === "ok") {
            btn.style.borderColor = "rgba(52,211,153,.40)";
            btn.style.background = "rgba(16,185,129,.12)";
        } else if (mode === "wait") {
            btn.style.borderColor = "rgba(251,191,36,.35)";
            btn.style.background = "rgba(245,158,11,.10)";
        } else if (mode === "bad") {
            btn.style.borderColor = "rgba(248,113,113,.35)";
            btn.style.background = "rgba(239,68,68,.08)";
        }
    }

    async function syncMembershipUI() {
        const facilitySel = el("facilitySel");
        const btnMembership = el("btnMembership");
        const btnReserve = el("btnReserve");

        const fidStr = facilitySel?.value || "";
        const fid = fidStr ? Number(fidStr) : 0;

        if (!fid) {
            if (btnMembership) {
                btnMembership.disabled = true;
                btnMembership.textContent = "Üyelik isteği";
                setBtnStyle(btnMembership, "mid");
            }
            if (btnReserve) {
                btnReserve.dataset.lockedByMembership = "1";
                btnReserve.disabled = true;
                btnReserve.title = "Önce halısaha seç";
            }
            return;
        }

        try {
            const st = await EH.API.get(`/api/member/membership-status?facilityId=${encodeURIComponent(String(fid))}`);
            const isActive = !!st?.member || String(st?.membershipStatus || "").toUpperCase() === "ACTIVE";
            const req = String(st?.requestStatus || "").toUpperCase();

            if (btnMembership) {
                if (isActive) {
                    btnMembership.disabled = true;
                    btnMembership.textContent = "Üyelik aktif ✅";
                    btnMembership.title = "Bu tesiste üyeliğin aktif";
                    setBtnStyle(btnMembership, "ok");
                } else if (req === "PENDING") {
                    btnMembership.disabled = true;
                    btnMembership.textContent = "İstek beklemede ⏳";
                    btnMembership.title = "Owner onayı bekleniyor";
                    setBtnStyle(btnMembership, "wait");
                } else if (req === "REJECTED") {
                    btnMembership.disabled = false;
                    btnMembership.textContent = "Reddedildi, tekrar iste";
                    btnMembership.title = "Tekrar üyelik isteği gönderebilirsin";
                    setBtnStyle(btnMembership, "bad");
                } else {
                    btnMembership.disabled = false;
                    btnMembership.textContent = "Üyelik isteği";
                    btnMembership.title = "Rezervasyon için önce üyelik iste";
                    setBtnStyle(btnMembership, "mid");
                }
            }

            if (btnReserve) {
                if (!isActive) {
                    btnReserve.dataset.lockedByMembership = "1";
                    btnReserve.disabled = true;
                    btnReserve.title = req === "PENDING" ? "Üyelik onayı bekleniyor" : "Rezervasyon için üyelik gerekli";
                } else {
                    if (btnReserve.dataset.lockedByMembership === "1") {
                        btnReserve.disabled = false;
                        delete btnReserve.dataset.lockedByMembership;
                    }
                    btnReserve.title = "";
                }
            }

            const legend = el("slotLegend");
            if (legend) {
                if (isActive) legend.textContent = "Seç: boş slot";
                else if (req === "PENDING") legend.textContent = "Üyelik onayı bekleniyor";
                else legend.textContent = "Rezervasyon için üyelik gerekli";
            }

        } catch (e) {
            console.warn("membership-status okunamadı", e);
        }
    }

    function hook() {
        syncMembershipUI();

        const facilitySel = el("facilitySel");
        facilitySel?.addEventListener("change", () => syncMembershipUI());

        const btnMembership = el("btnMembership");
        btnMembership?.addEventListener(
            "click",
            () => setTimeout(() => syncMembershipUI(), 600),
            true
        );
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", hook);
    } else {
        hook();
    }

    EH.syncMembershipUI = syncMembershipUI;
})();
