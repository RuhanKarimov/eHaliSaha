"use strict";

(function(){
    window.EH = window.EH || {};
    const EH = window.EH;   // bu block içinde, global değil
    EH.addPlayerRow = EH.addPlayerRow || ((boxId, fullName = "", jerseyNo = "") => {
        const box = EH.$(boxId);
        if (!box) return;

        const row = document.createElement("div");
        row.className = "grid grid-cols-12 gap-2";
        row.innerHTML = `
    <input class="col-span-8 input" placeholder="Oyuncu adı soyadı" value="${String(fullName ?? "")}">
    <input class="col-span-3 input" placeholder="No" value="${String(jerseyNo ?? "")}">
    <button class="col-span-1 btn" type="button" title="Sil">✕</button>
  `;
        row.querySelector("button").onclick = () => row.remove();
        box.appendChild(row);
    });

})();

