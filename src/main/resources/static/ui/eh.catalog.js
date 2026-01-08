"use strict";
(function(){
    window.EH = window.EH || {};
    const EH = window.EH;   // bu block içinde, global değil

    EH.loadFacilities = EH.loadFacilities || (async (selectId) => {
        const facilities = await EH.API.get("/api/public/facilities");
        const sel = EH.$(selectId);
        if (sel) sel.innerHTML = facilities.map(f => `<option value="${f.id}">${f.name}</option>`).join("");
        return facilities;
    });

    EH.loadPitchesPublic = EH.loadPitchesPublic || (async (facilityId, selectId) => {
        const pitches = await EH.API.get(`/api/public/facilities/${facilityId}/pitches`);
        const sel = EH.$(selectId);
        if (sel) sel.innerHTML = pitches.map(p => `<option value="${p.id}">${p.name}</option>`).join("");
        return pitches;
    });

    EH.loadDurations = EH.loadDurations || (async (selectId) => {
        const durs = await EH.API.get("/api/public/durations");
        const sel = EH.$(selectId);
        if (sel) {
            sel.innerHTML = durs.map(d => `<option value="${d.minutes}">${d.label}</option>`).join("");
            sel.value = sel.value || "60";
        }
        return durs;
    });

    EH.loadSlots = EH.loadSlots || (async (facilityId) =>
            EH.API.get(`/api/public/facilities/${facilityId}/slots`)
    );

})();
