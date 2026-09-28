(function () {
  "use strict";
  if (window.__hamyarPersist) return;
  window.__hamyarPersist = true;

  function faDigits(n) {
    return String(n).replace(/[0-9]/g, function (d) {
      return String.fromCharCode(0x06f0 + parseInt(d, 10));
    });
  }

  function records() {
    try {
      if (Array.isArray(window.tableRecords)) return window.tableRecords;
    } catch (e) {}
    return null;
  }

  function dump() {
    var o = {};
    try {
      for (var i = 0; i < localStorage.length; i++) {
        var k = localStorage.key(i);
        if (k) o[k] = localStorage.getItem(k);
      }
    } catch (e) {}
    try {
      var arr = records();
      if (arr) o.__tableRecords = JSON.stringify(arr);
    } catch (e) {}
    try {
      if (window.HamyarBioNotes) o.__bioNotes = JSON.stringify(window.HamyarBioNotes);
    } catch (e) {}
    return JSON.stringify(o);
  }

  function rebuildTable() {
    var arr = records();
    if (!arr) return;
    var tbody = document.getElementById("tableBody");
    var cnt = document.getElementById("recordCount");
    var fa = typeof window.toFa === "function" ? window.toFa : faDigits;
    if (cnt) cnt.textContent = fa(arr.length);
    if (!tbody) return;
    tbody.innerHTML = "";
    arr.slice().reverse().forEach(function (entry) {
      var tr = document.createElement("tr");
      var tds = "<td>" + fa(entry.id || "") + "</td>";
      if (entry.cells && entry.cells.length) {
        entry.cells.forEach(function (c) {
          tds += "<td>" + c + "</td>";
        });
      } else {
        tds += "<td>" + (entry.name || "") + "</td>";
        tds += "<td>" + (entry.input || entry.conditions || "") + "</td>";
        tds += "<td>" + (entry.output || "") + "</td>";
        tds += "<td>" + (entry.time || "") + "</td>";
      }
      tr.innerHTML = tds;
      tbody.appendChild(tr);
    });
  }

  function apply(json) {
    if (!json) return;
    try {
      var o = JSON.parse(json);
      Object.keys(o).forEach(function (k) {
        if (k === "__tableRecords") {
          try {
            window.tableRecords = JSON.parse(o[k] || "[]");
            rebuildTable();
            hookRecords();
          } catch (e) {}
          return;
        }
        if (k === "__bioNotes") {
          try { window.HamyarBioNotes = JSON.parse(o[k] || "[]"); } catch (e) {}
          return;
        }
        try { origSet.call(localStorage, k, o[k]); } catch (e) {}
      });
    } catch (e) {}
  }

  function saveNow() {
    try { if (window.HamyarTool) HamyarTool.onSave(dump()); } catch (e) {}
  }

  var origSet = Storage.prototype.setItem;
  Storage.prototype.setItem = function (k, v) {
    origSet.call(this, k, v);
    saveNow();
  };

  function hookRecords() {
    var arr = records();
    if (!arr || arr.__hamyarHooked) return;
    arr.__hamyarHooked = true;
    var p = arr.push.bind(arr);
    arr.push = function () {
      var r = p.apply(this, arguments);
      saveNow();
      return r;
    };
  }

  function wrap(name) {
    try {
      var fn = window[name];
      if (typeof fn !== "function" || fn.__hamyarWrapped) return;
      var wrapped = function () {
        var r = fn.apply(this, arguments);
        hookRecords();
        saveNow();
        return r;
      };
      wrapped.__hamyarWrapped = true;
      window[name] = wrapped;
    } catch (e) {}
  }

  var lastN = -1;
  setInterval(function () {
    hookRecords();
    wrap("recordDataPoint");
    wrap("recordAndClose");
    wrap("clearNotes");
    wrap("exportCSV");
    try {
      var arr = records();
      var n = arr ? arr.length : -1;
      if (n !== lastN) {
        lastN = n;
        saveNow();
      }
    } catch (e) {}
  }, 700);

  window.HamyarToolDump = dump;
  window.HamyarToolApply = apply;
  document.addEventListener("visibilitychange", function () {
    saveNow();
  });
  hookRecords();
})();
