/* همیار: ذخیرهٔ پاسخ/آرشیو/نمره روی دستگاه + صحت‌سنجی انتهای هر صفحه */
(function () {
  if (window.__hamyarHooked) return;
  window.__hamyarHooked = true;

  function persist() {
    var payload = {
      stats: typeof stats !== "undefined" ? stats : {},
      repeatCount: typeof repeatCount !== "undefined" ? repeatCount : {},
      archive: typeof archive !== "undefined" ? archive : [],
      currentIndex: typeof currentIndex !== "undefined" ? currentIndex : 0,
      currentMode: typeof currentMode !== "undefined" ? currentMode : "all",
      currentRound: typeof currentRound !== "undefined" ? currentRound : 1,
      radios: {}
    };
    document.querySelectorAll('input[type="radio"]:checked').forEach(function (r) {
      payload.radios[r.name] = r.value;
    });
    var raw = JSON.stringify(payload);
    try { localStorage.setItem("hamyar_html_state", raw); } catch (e) {}
    if (window.Hamyar) {
      try { Hamyar.saveState(raw); } catch (e) {}
    }
  }

  function restore() {
    var raw = "";
    if (window.Hamyar) {
      try { raw = Hamyar.loadState() || ""; } catch (e) {}
    }
    if (!raw) {
      try { raw = localStorage.getItem("hamyar_html_state") || ""; } catch (e) {}
    }
    if (!raw) return;
    try {
      var p = JSON.parse(raw);
      if (p.stats && typeof stats !== "undefined") stats = p.stats;
      if (p.repeatCount && typeof repeatCount !== "undefined") repeatCount = p.repeatCount;
      if (p.archive && typeof archive !== "undefined") archive = p.archive;
      if (typeof currentIndex !== "undefined" && typeof p.currentIndex === "number") currentIndex = p.currentIndex;
      if (p.currentMode && typeof currentMode !== "undefined") currentMode = p.currentMode;
      if (typeof currentRound !== "undefined" && typeof p.currentRound === "number") currentRound = p.currentRound;
      if (p.radios) {
        Object.keys(p.radios).forEach(function (name) {
          var el = document.querySelector('input[name="' + name + '"][value="' + p.radios[name] + '"]');
          if (!el) return;
          el.checked = true;
          var opt = el.closest(".mc-option");
          if (opt) opt.classList.add("selected");
        });
      }
      if (typeof renderCard === "function") renderCard();
    } catch (e) {}
  }

  function wrap(name, after) {
    if (typeof window[name] !== "function") return;
    var orig = window[name];
    window[name] = function () {
      var cardId = null;
      try {
        if (typeof cardsOrder !== "undefined" && typeof currentIndex !== "undefined" && cardsOrder[currentIndex]) {
          cardId = String(cardsOrder[currentIndex].id);
        }
      } catch (e) {}
      var r = orig.apply(this, arguments);
      try { persist(); } catch (e) {}
      try { if (after) after(cardId); } catch (e) {}
      return r;
    };
  }

  wrap("confirmAnswer", function (cardId) {
    if (!window.Hamyar || !cardId || typeof stats === "undefined") return;
    var s = stats[cardId] || stats[parseInt(cardId, 10)];
    if (s && s.answered) Hamyar.recordItem(cardId, !!s.correct);
  });
  wrap("nextCard", null);
  wrap("showStats", function () {
    if (!window.Hamyar || typeof allCards === "undefined" || typeof stats === "undefined") return;
    var ok = 0, wrong = [], tot = allCards.length;
    allCards.forEach(function (c) {
      var s = stats[c.id];
      if (s && s.answered) {
        if (s.correct) ok++;
        else wrong.push(String(c.id));
      }
    });
    var pct = tot > 0 ? Math.round((ok * 100) / tot) : 0;
    Hamyar.recordExam(pct, tot, ok, wrong.join(","));
  });
  wrap("verifyAll", function () {
    reportList(document.querySelectorAll(".question[data-q]"));
  });
  wrap("saveAnswers", null);
  wrap("restartAll", null);
  wrap("startRepeatRound", null);

  function correctOf(q) {
    var id = q.getAttribute("data-q");
    try {
      if (window.answerKey && answerKey[id] && answerKey[id].correct != null) {
        return String(answerKey[id].correct);
      }
    } catch (e) {}
    var fb = q.querySelector(".feedback");
    if (fb && fb.getAttribute("data-correct")) return fb.getAttribute("data-correct");
    return "";
  }

  function explainOf(q, user) {
    var id = q.getAttribute("data-q");
    if (typeof buildExplanation === "function") {
      try { return buildExplanation(id, user); } catch (e) {}
    }
    return "";
  }

  function gradeList(nodeList) {
    var questions = Array.prototype.slice.call(nodeList);
    var correctCount = 0, wrongCount = 0, emptyCount = 0;
    questions.forEach(function (q) {
      var selected = q.querySelector('input[type="radio"]:checked');
      var feedback = q.querySelector(".feedback");
      var options = q.querySelectorAll(".mc-option");
      options.forEach(function (opt) { opt.classList.remove("correct-option", "wrong-option"); });
      var correctAnswer = correctOf(q);
      if (!selected) {
        if (feedback) {
          feedback.className = "feedback empty";
          feedback.innerHTML = "به این سوال پاسخ نداده‌اید." + explainOf(q, null);
        }
        emptyCount++;
        return;
      }
      var userAnswer = selected.value;
      var correctInput = correctAnswer ? q.querySelector('input[value="' + correctAnswer + '"]') : null;
      if (correctInput) {
        var wrapEl = correctInput.closest(".mc-option");
        if (wrapEl) wrapEl.classList.add("correct-option");
      }
      if (correctAnswer && userAnswer === correctAnswer) {
        if (feedback) {
          feedback.className = "feedback correct";
          feedback.innerHTML = "آفرین! پاسخ شما درست است." + explainOf(q, userAnswer);
        }
        correctCount++;
      } else {
        if (feedback) {
          feedback.className = "feedback wrong";
          feedback.innerHTML = "پاسخ شما نادرست است. گزینه‌ی صحیح با رنگ سبز مشخص شده است." + explainOf(q, userAnswer);
        }
        var bad = selected.closest(".mc-option");
        if (bad) bad.classList.add("wrong-option");
        wrongCount++;
      }
    });
    return {
      ok: correctCount,
      bad: wrongCount,
      empty: emptyCount,
      total: questions.length,
      questions: questions
    };
  }

  function fillScorePanel(res, title) {
    var scorePanel = document.getElementById("scorePanel");
    var scoreNum = document.getElementById("scoreNum");
    var scoreTotal = document.getElementById("scoreTotal");
    var progressFill = document.getElementById("progressFill");
    var scoreMsg = document.getElementById("scoreMsg");
    if (!scorePanel || !scoreNum) return;
    var h = scorePanel.querySelector("h3");
    if (h && title) h.textContent = title;
    scorePanel.classList.add("show");
    scoreNum.textContent = res.ok;
    if (scoreTotal) scoreTotal.textContent = "/ " + res.total;
    var percent = res.total > 0 ? (res.ok / res.total) * 100 : 0;
    if (progressFill) progressFill.style.width = percent + "%";
    var msg = "";
    if (res.total === 0) msg = "در این صفحه سوالی نیست.";
    else if (percent === 100) msg = "🌟 عالی! تمام پاسخ‌های این بخش درست است.";
    else if (percent >= 80) msg = "👏 خیلی خوب بود. چند مورد را دوباره ببین.";
    else if (percent >= 50) msg = "ادامه بده؛ بخشی از پاسخ‌ها نیاز به مرور دارد.";
    else msg = "این صفحه را از روی کتاب یک‌بار دیگر مرور کن.";
    if (res.empty) msg += " (" + res.empty + " بدون پاسخ)";
    if (scoreMsg) scoreMsg.textContent = msg;
    try { scorePanel.scrollIntoView({ behavior: "smooth", block: "center" }); } catch (e) {}
  }

  function reportList(nodeList) {
    if (!window.Hamyar) return;
    var qs = Array.prototype.slice.call(nodeList);
    var ok = 0, tot = 0, wrong = [];
    qs.forEach(function (q) {
      tot++;
      var id = q.getAttribute("data-q") || "";
      var sel = q.querySelector('input[type="radio"]:checked');
      var correct = correctOf(q);
      var good = !!(sel && correct && sel.value === correct);
      if (sel) {
        try { Hamyar.recordItem(id, good); } catch (e) {}
      }
      if (good) ok++;
      else if (sel) wrong.push(id);
    });
    var pct = tot > 0 ? Math.round((ok * 100) / tot) : 0;
    try { Hamyar.recordExam(pct, tot, ok, wrong.join(",")); } catch (e) {}
  }

  window.verifySection = function (btn) {
    var sec = btn && btn.closest ? btn.closest(".section") : null;
    if (!sec) return;
    var titleEl = sec.querySelector(".section-title");
    var title = titleEl ? titleEl.textContent.replace(/\s+/g, " ").trim() : "این صفحه";
    var list = sec.querySelectorAll(".question[data-q]");
    var res = gradeList(list);
    fillScorePanel(res, "📊 نتیجهٔ " + title);
    reportList(list);
    try { persist(); } catch (e) {}
  };

  function injectPageChecks() {
    var sections = document.querySelectorAll(".section");
    if (!sections.length) return;
    sections.forEach(function (sec) {
      if (sec.querySelector(".page-verify-btn")) return;
      var qs = sec.querySelectorAll(".question[data-q]");
      if (!qs.length) return;
      var titleEl = sec.querySelector(".section-title");
      var title = titleEl ? titleEl.textContent.replace(/\s+/g, " ").trim() : "این صفحه";
      var bar = document.createElement("div");
      bar.className = "button-bar page-verify-bar";
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn-success page-verify-btn";
      btn.textContent = "✅ صحت‌سنجی " + title;
      btn.addEventListener("click", function () {
        var list = sec.querySelectorAll(".question[data-q]");
        var res = gradeList(list);
        fillScorePanel(res, "📊 نتیجهٔ " + title);
        reportList(list);
        try { persist(); } catch (e) {}
        var firstFb = sec.querySelector(".feedback.wrong, .feedback.empty, .feedback.correct");
        if (firstFb) {
          try { firstFb.scrollIntoView({ behavior: "smooth", block: "center" }); } catch (e2) {}
        }
      });
      bar.appendChild(btn);
      sec.appendChild(bar);
    });
  }

  document.addEventListener("change", function () { try { persist(); } catch (e) {} });
  restore();
  injectPageChecks();
  setTimeout(injectPageChecks, 400);
})();
