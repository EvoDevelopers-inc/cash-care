const logoutBtn = document.getElementById("logout-btn");

const MOOD_BY_CODE = {
    calm:    { label: "Спокойно",    className: "", title: "Ты держишь курс",      desc: "Бюджет в плюсе — есть запас на месяц. Можно подумать о подушке безопасности." },
    warning: { label: "Внимание",    className: "warning", title: "Бюджет на грани", desc: "Свободного остатка почти нет. Срежь одну необязательную категорию или подними доход." },
    danger:  { label: "Тревога",     className: "danger",  title: "Минус по месяцу", desc: "Расходы выше доходов. Удали или уменьши категории, без которых проживёшь." },
    neutral: { label: "Жду данных",  className: "",        title: "Жду данных",      desc: "Заполни доход и расходы — посчитаю, насколько спокойно тебе с этим бюджетом." }
};

const dashState = {
    monthId: null,
    salary: 0,
    others: null,
    plannedCategories: [],
    salaryDirty: false,
    aiRefresh: null
};

let aiBudgetRunning = false;

document.addEventListener("DOMContentLoaded", async () => {
    redirectIfGuest();

    if (window.lucide && typeof window.lucide.createIcons === "function") {
        window.lucide.createIcons();
    }

    if (logoutBtn) {
        logoutBtn.addEventListener("click", handleLogout);
    }

    bindNav();
    bindFakeButtons();
    bindBudgetEditor();
    bindReupload();
    bindEditSurvey();
    bindManageCredits();
    bindPdfOnboard();
    bindAiDetailModal();

    if (typeof preloadGoalsForDashboard === "function") {
        preloadGoalsForDashboard();
    }

    let overview;
    try {
        overview = await fetchOverviewWithRetry();
    } catch (err) {
        if (err && err.status === 401) {
            CashCareApi.logout();
            window.location.href = "/";
            return;
        }
        renderEmptyState();
        showProfileAlert(err?.message || "Не удалось загрузить аналитику");
        return;
    }

    fillProfileSidebar(overview.profile);
    applyOverview(overview);

    window.onSurveyCompleted = async () => {
        try {
            const fresh = await CashCareApi.getAnalyticsOverview();
            applyOverview(fresh);
            if (!fresh.profile.init) {
                await openInitModal();
            }
        } catch (err) {
            showProfileAlert(err?.message || "Не удалось обновить данные");
        }
    };

    if (!overview.profile.surveyCompleted) {
        await openSurveyModal();
        return;
    }

    if (!overview.profile.init) {
        await openInitModal();
    }
});

async function fetchOverviewWithRetry() {
    try {
        return await CashCareApi.getAnalyticsOverview();
    } catch (err) {
        if (err && err.status === 401) {
            try {
                await CashCareApi.refresh();
                return await CashCareApi.getAnalyticsOverview();
            } catch (_) {
                err.status = 401;
                throw err;
            }
        }
        throw err;
    }
}

async function reloadOverview() {
    try {
        const overview = await CashCareApi.getAnalyticsOverview();
        applyOverview(overview);
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось обновить аналитику");
    }
}

function applyOverview(overview) {
    dashState.monthId = overview.currentMonth?.id ?? null;
    dashState.salary = Number(overview.currentMonth?.salary ?? 0);
    dashState.others = overview.currentMonth?.others ?? null;
    dashState.plannedCategories = overview.plannedCategories || [];
    dashState.salaryDirty = false;
    dashState.aiAnalysis = overview.aiAnalysis || null;
    dashState.aiRefresh = overview.aiRefresh || null;
    dashState.freePocket = Math.max(Number(overview.balance?.freePocket || 0), 0);
    dashState.canSave = Math.max(Number(overview.balance?.canSave || 0), 0);

    if (typeof goalsState !== "undefined" && goalsState) {
        goalsState.freePocket = dashState.freePocket;
        goalsState.canSave = dashState.canSave;
        if (typeof renderGoalsTeaser === "function") renderGoalsTeaser();
    }

    renderOverview(overview);
    if (typeof applySpontaneousFromOverview === "function") {
        applySpontaneousFromOverview(overview);
    }
}

function showProfileAlert(message) {
    const el = document.getElementById("profile-alert");
    if (!el) return;
    el.textContent = message;
    el.style.display = "block";
}

function fillProfileSidebar(profile) {
    if (!profile) return;
    const nameEl = document.getElementById("profile-name");
    const emailEl = document.getElementById("profile-email");
    if (nameEl) {
        const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(" ");
        nameEl.textContent = fullName || profile.username || "—";
    }
    if (emailEl) {
        emailEl.textContent = profile.email || "—";
    }
}

function handleLogout() {
    CashCareApi.logout();
    window.location.href = "/";
}

function bindNav() {
    document.querySelectorAll(".nav-item").forEach((item) => {
        item.addEventListener("click", (e) => {
            e.preventDefault();
            document.querySelectorAll(".nav-item").forEach((n) => n.classList.remove("active"));
            item.classList.add("active");
        });
    });
}

function bindFakeButtons() {
    document.querySelectorAll("[data-fake]").forEach((btn) => {
        btn.addEventListener("click", (e) => e.preventDefault());
    });
}

function renderOverview(overview) {
    const ai = overview.aiAnalysis ? normalizeAiAnalysis(overview.aiAnalysis) : null;

    renderMonthLabels(overview.currentMonth);
    renderAiStatusBanner(overview.aiRefresh, ai);
    renderPdfOnboard(ai);
    renderBalance(overview.balance, ai);
    renderMoodCard(overview.balance);
    renderDistribution(overview.balance, ai);
    renderKlerkVacancies();
    renderAllCategories(ai, overview.plannedCategories);
    renderHealth(ai, overview.rating);
    renderTips(ai);
    renderBudgetEditor();
    renderAiRefresh(overview.aiRefresh);
    applyBudgetLock(overview.aiRefresh);
}

function renderPdfOnboard(ai) {
    const card = document.getElementById("pdf-onboard-card");
    if (!card) return;

    if (ai) {
        card.classList.add("hidden");
        return;
    }
    card.classList.remove("hidden");
    refreshLucide();
}

function bindPdfOnboard() {
    const card = document.getElementById("pdf-onboard-card");
    if (!card) return;
    const drop = document.getElementById("pdf-onboard-drop");
    const input = document.getElementById("pdf-onboard-input");
    if (!drop || !input) return;

    input.addEventListener("change", (e) => {
        const file = e.target.files && e.target.files[0];
        if (file) handlePdfOnboardUpload(file);
        input.value = "";
    });

    ["dragenter", "dragover"].forEach((evt) => {
        drop.addEventListener(evt, (e) => {
            e.preventDefault();
            drop.classList.add("is-drag");
        });
    });
    ["dragleave", "drop"].forEach((evt) => {
        drop.addEventListener(evt, (e) => {
            e.preventDefault();
            drop.classList.remove("is-drag");
        });
    });
    drop.addEventListener("drop", (e) => {
        const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
        if (file) handlePdfOnboardUpload(file);
    });
}

async function handlePdfOnboardUpload(file) {
    const drop = document.getElementById("pdf-onboard-drop");
    const progress = document.getElementById("pdf-onboard-progress");
    const progressText = document.getElementById("pdf-onboard-progress-text");
    const errorEl = document.getElementById("pdf-onboard-error");
    const label = document.getElementById("pdf-onboard-label");

    const isPdf = (file.type === "application/pdf") ||
                  (file.name && file.name.toLowerCase().endsWith(".pdf"));
    if (!isPdf) {
        showOnboardError("Нужен PDF-файл");
        return;
    }
    if (file.size > 10 * 1024 * 1024) {
        showOnboardError("Файл больше 10 МБ — слишком большой");
        return;
    }

    if (errorEl) errorEl.classList.add("hidden");
    drop?.classList.add("is-loading");
    progress?.classList.remove("hidden");
    if (label) label.textContent = file.name;

    let stage = 0;
    const stages = [
        "Парсим транзакции из выписки...",
        "Категоризируем расходы...",
        "AI определяет финансовый тип...",
        "Почти готово, формируем профиль..."
    ];
    if (progressText) progressText.textContent = stages[0];
    const ticker = setInterval(() => {
        stage = Math.min(stage + 1, stages.length - 1);
        if (progressText) progressText.textContent = stages[stage];
    }, 5000);

    try {
        await CashCareApi.uploadStatement(file);
        await reloadOverview();
    } catch (err) {
        showOnboardError(err?.message || "Не удалось загрузить выписку");
    } finally {
        clearInterval(ticker);
        drop?.classList.remove("is-loading");
        progress?.classList.add("hidden");
        if (label) label.textContent = "Выбрать PDF или перетащи сюда";
    }
}

function showOnboardError(message) {
    const errorEl = document.getElementById("pdf-onboard-error");
    if (!errorEl) return;
    errorEl.textContent = message;
    errorEl.classList.remove("hidden");
}

const RU_MONTHS_NOM = ["январь","февраль","март","апрель","май","июнь","июль","август","сентябрь","октябрь","ноябрь","декабрь"];
const RU_MONTHS_PREP = ["январе","феврале","марте","апреле","мае","июне","июле","августе","сентябре","октябре","ноябре","декабре"];

function getCurrentMonthName(form) {
    const idx = new Date().getMonth();
    const name = form === "prep" ? RU_MONTHS_PREP[idx] : RU_MONTHS_NOM[idx];
    return capitalize(name);
}

function capitalize(str) {
    return str ? str[0].toUpperCase() + str.slice(1) : str;
}

function formatMonthYearLabel(currentMonth, form) {
    if (currentMonth && currentMonth.year && currentMonth.month) {
        const idx = Number(currentMonth.month) - 1;
        const year = currentMonth.year;
        const name = form === "prep"
            ? RU_MONTHS_PREP[idx]
            : RU_MONTHS_NOM[idx];
        return { name: capitalize(name), year, prep: RU_MONTHS_PREP[idx] };
    }
    const idx = new Date().getMonth();
    const name = form === "prep" ? RU_MONTHS_PREP[idx] : RU_MONTHS_NOM[idx];
    return { name: capitalize(name), year: new Date().getFullYear(), prep: RU_MONTHS_PREP[idx] };
}

function renderMonthLabels(currentMonth) {
    const nom = formatMonthYearLabel(currentMonth, "nom");
    const balanceMonthEl = document.getElementById("balance-month-label");
    if (balanceMonthEl) balanceMonthEl.textContent = `${nom.name} ${nom.year}`;

    const subtitle = document.getElementById("dash-subtitle");
    if (subtitle) {
        subtitle.textContent = `Бюджет на ${nom.name.toLowerCase()} · план месяца и AI-анализ`;
    }
}

function renderAiStatusBanner(refresh, ai) {
    const banner = document.getElementById("ai-status-banner");
    if (!banner) return;

    const titleEl = document.getElementById("ai-status-title");
    const metaEl = document.getElementById("ai-status-meta");
    const ctaEl = document.getElementById("ai-status-cta");
    const iconEl = document.getElementById("ai-status-icon");

    banner.classList.remove("hidden", "state-locked", "state-ready", "state-refresh");

    const hasAi = !!ai;
    const locked = refresh && refresh.canRefresh === false;
    const monthPrep = getCurrentMonthName("prep");

    if (locked) {
        const nextHuman = formatLockHuman(refresh.nextAvailableAt) || "следующий месяц";
        const lastHuman = formatLockHuman(refresh.lastRunAt);
        banner.classList.add("state-locked");
        setLucide(iconEl, "bot");
        titleEl.textContent = `Ты в ${monthPrep} — план зафиксирован AI`;
        metaEl.textContent = lastHuman
            ? `AI-анализ от ${lastHuman}. Следующий — ${nextHuman}.`
            : `Следующий AI-анализ — ${nextHuman}.`;
        ctaEl.innerHTML = `<i data-lucide="lightbulb" class="lc lc-sm align-text-bottom mr-1"></i>Сейчас иди по советам нейронки ниже — они подобраны под твою анкету и план.`;
        refreshLucide();
        return;
    }

    if (hasAi) {
        banner.classList.add("state-refresh");
        setLucide(iconEl, "refresh-cw");
        titleEl.textContent = `Месяц ${monthPrep.toLowerCase()} — пора пересобрать план`;
        metaEl.textContent = "Прошлый AI-анализ устарел. Обнови план под новый месяц.";
        ctaEl.innerHTML = `Жми «<i data-lucide="refresh-cw" class="lc lc-xs align-text-bottom mx-0.5"></i>Обновить AI» сверху, чтобы получить свежие рекомендации.`;
        refreshLucide();
        return;
    }

    banner.classList.add("state-ready");
    setLucide(iconEl, "sparkles");
    titleEl.textContent = `Запусти AI на ${monthPrep.toLowerCase()}`;
    metaEl.innerHTML = `Нейронка ещё не анализировала твой план. Заполни доход и категории, потом нажми «<i data-lucide="refresh-cw" class="lc lc-xs align-text-bottom mx-0.5"></i>Обновить AI».`;
    ctaEl.textContent = "После анализа план зафиксируется на месяц, а ты получишь персональные советы.";
    refreshLucide();
}

function setLucide(target, iconName) {
    if (!target) return;
    if (target.tagName && target.tagName.toLowerCase() === "i") {
        target.setAttribute("data-lucide", iconName);
        return;
    }
    target.outerHTML = `<i id="${target.id}" data-lucide="${iconName}" class="${target.getAttribute("class") || "lc lc-lg"}"></i>`;
}

function refreshLucide() {
    if (window.lucide && typeof window.lucide.createIcons === "function") {
        window.lucide.createIcons();
    }
}

function applyBudgetLock(refresh) {
    const locked = refresh ? refresh.canRefresh === false : false;
    document.body.classList.toggle("budget-locked", locked);

    const banner = document.getElementById("budget-lock-banner");
    const text = document.getElementById("budget-lock-text");
    if (!banner || !text) return;

    if (!locked) {
        banner.classList.add("hidden");
        return;
    }

    banner.classList.remove("hidden");
    const human = formatLockHuman(refresh.nextAvailableAt);
    const monthPrep = getCurrentMonthName("prep");
    text.textContent = human
        ? `В ${monthPrep.toLowerCase()} категории и доход не правим — план зафиксирован до ${human}. Следуй советам AI ниже, они под твою ситуацию.`
        : (refresh.reason || "Категории и доход нельзя менять — попробуй позже.");
}

function formatLockHuman(iso) {
    if (!iso) return null;
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return null;
    return d.toLocaleString("ru-RU", {
        day: "numeric",
        month: "long",
        hour: "2-digit",
        minute: "2-digit"
    });
}

let aiCountdownTimer = null;

function setReuploadButton(btn, iconName, labelText) {
    btn.innerHTML = `<i data-lucide="${iconName}" class="lc lc-sm"></i><span id="reupload-pdf-btn-label">${labelText}</span>`;
    refreshLucide();
}

function renderAiRefresh(refresh) {
    const btn = document.getElementById("reupload-pdf-btn");
    if (!btn) return;

    if (aiCountdownTimer) {
        clearInterval(aiCountdownTimer);
        aiCountdownTimer = null;
    }

    const canRefresh = refresh ? !!refresh.canRefresh : true;

    if (canRefresh) {
        btn.disabled = false;
        btn.classList.remove("is-locked");
        setReuploadButton(btn, "refresh-cw", "Обновить AI");
        btn.removeAttribute("title");
        return;
    }

    const reason = refresh?.reason || "Доступно раз в месяц";
    btn.title = reason;
    btn.disabled = true;
    btn.classList.add("is-locked");

    const nextAt = refresh?.nextAvailableAt ? Date.parse(refresh.nextAvailableAt) : NaN;
    if (!Number.isFinite(nextAt)) {
        setReuploadButton(btn, "hourglass", "AI ждёт следующего запуска");
        return;
    }

    const tick = () => {
        const diff = nextAt - Date.now();
        if (diff <= 0) {
            clearInterval(aiCountdownTimer);
            aiCountdownTimer = null;
            btn.disabled = false;
            btn.classList.remove("is-locked");
            setReuploadButton(btn, "refresh-cw", "Обновить AI");
            btn.removeAttribute("title");
            return;
        }
        setReuploadButton(btn, "hourglass", `Через ${formatCountdown(diff)}`);
    };

    tick();
    aiCountdownTimer = setInterval(tick, 1000);
}

function formatCountdown(ms) {
    const totalSec = Math.max(0, Math.floor(ms / 1000));
    const days = Math.floor(totalSec / 86400);
    const hours = Math.floor((totalSec % 86400) / 3600);
    const mins = Math.floor((totalSec % 3600) / 60);
    const secs = totalSec % 60;

    if (days > 0) return `${days}д ${hours}ч ${mins}м`;
    if (hours > 0) return `${hours}ч ${mins}м ${pad2(secs)}с`;
    if (mins > 0) return `${mins}м ${pad2(secs)}с`;
    return `${secs}с`;
}

function pad2(n) {
    return n < 10 ? `0${n}` : String(n);
}

function renderDistribution(balance, ai) {
    const required = Number(balance?.requiredExpense || 0);
    const optional = Number(balance?.optionalExpense || 0);
    const savings = Math.max(Number(balance?.savingsAmount || 0), 0);
    const free = Math.max(Number(balance?.freePocket || 0), 0);
    const total = required + optional + savings + free || 1;

    const aiBadge = document.getElementById("dist-free-ai-badge");
    const aiReason = document.getElementById("dist-free-reason");
    const reasoning = ai && ai.financialProfile && ai.financialProfile.reasoning;
    if (ai && reasoning) {
        if (aiBadge) {
            aiBadge.classList.remove("hidden");
            refreshLucide();
        }
        if (aiReason) {
            aiReason.textContent = reasoning;
            aiReason.classList.remove("hidden");
        }
    } else {
        if (aiBadge) aiBadge.classList.add("hidden");
        if (aiReason) aiReason.classList.add("hidden");
    }

    const pctReq = (required / total) * 100;
    const pctOpt = (optional / total) * 100;
    const pctSave = (savings / total) * 100;
    const pctFree = Math.max(100 - pctReq - pctOpt - pctSave, 0);

    setBarWidth("dist-bar-required", pctReq);
    setBarWidth("dist-bar-optional", pctOpt);
    setBarWidth("dist-bar-save", pctSave);
    setBarWidth("dist-bar-free", pctFree);

    setText("dist-required-amount", formatMoney(required));
    setText("dist-optional-amount", formatMoney(optional));
    setText("dist-save-amount", formatMoney(savings));
    setText("dist-free-amount", formatMoney(free));

    setText("dist-required-pct", `${Math.round(pctReq)}%`);
    setText("dist-optional-pct", `${Math.round(pctOpt)}%`);
    setText("dist-save-pct", `${Math.round(pctSave)}%`);
    setText("dist-free-pct", `${Math.round(pctFree)}%`);

    toggleDistRow("dist-row-required", required);
    toggleDistRow("dist-row-optional", optional);
    toggleDistRow("dist-row-save", savings);
    toggleDistRow("dist-row-free", free);

    const statusEl = document.getElementById("dist-status");
    const status = balance?.saveStatusCode || "neutral";
    if (statusEl) {
        statusEl.textContent = balance?.saveStatusLabel || "—";
        statusEl.className = `text-xs font-bold status-${status}`;
    }

    const tipEl = document.getElementById("dist-tip");
    if (tipEl) {
        tipEl.textContent = balance?.saveTip || "Заполни доход и расходы — посчитаю, сколько можно отложить.";
        tipEl.classList.remove("tone-good", "tone-warn", "tone-bad");
        if (status === "great" || status === "good") tipEl.classList.add("tone-good");
        else if (status === "ok" || status === "tight") tipEl.classList.add("tone-warn");
        else if (status === "minus") tipEl.classList.add("tone-bad");
    }
}

function toggleDistRow(rowId, amount) {
    const row = document.getElementById(rowId);
    if (!row) return;
    row.style.display = amount > 0 ? "" : "none";
}

function setBarWidth(id, pct) {
    const el = document.getElementById(id);
    if (el) el.style.width = `${Math.max(0, Math.min(100, pct))}%`;
}

function renderMoodCard(balance) {
    const moodKey = balance?.moodCode || "neutral";
    const mood = MOOD_BY_CODE[moodKey] || MOOD_BY_CODE.neutral;
    setText("mood-title", mood.title);
    setText("mood-desc", mood.desc);
}

function renderBalance(balance, ai) {
    const moodKey = balance?.moodCode || "neutral";
    const mood = MOOD_BY_CODE[moodKey] || MOOD_BY_CODE.neutral;
    const moodEl = document.getElementById("balance-mood");
    if (moodEl) {
        moodEl.textContent = balance?.moodLabel || mood.label;
        moodEl.className = "mood-chip " + mood.className;
    }

    const spontInc = Number(balance?.spontaneousIncome || 0);
    const spontExp = Number(balance?.spontaneousExpense || 0);
    const salary = Number(balance?.salary || 0);
    const planned = Number(balance?.plannedExpense || 0);
    const income = salary + spontInc;
    const expense = planned + spontExp;
    const free = Number(balance?.balance ?? balance?.canSave ?? (income - expense));

    setText("balance-amount", formatMoney(free));
    setText("balance-free", formatMoney(Math.max(free, 0)));
    setText("balance-income", formatMoney(income));
    setText("balance-expense", formatMoney(expense));
    setText("chip-income", formatMoney(income));
    setText("chip-expense", `−${formatMoney(expense)}`);

    const spontHint = document.getElementById("balance-spont-hint");
    if (spontHint) {
        if (spontInc > 0 || spontExp > 0) {
            spontHint.classList.remove("hidden");
            spontHint.textContent = `Вне плана: +${formatMoney(spontInc)} · −${formatMoney(spontExp)}`;
        } else {
            spontHint.classList.add("hidden");
            spontHint.textContent = "";
        }
    }
}

const KLERK_VACANCIES_MOCK = [
    {
        title: "Бухгалтер на первичку",
        company: "Клерк",
        type: "employer",
        salary: "85 000 – 95 000 ₽",
        meta: "Москва · гибрид"
    },
    {
        title: "Методист онлайн-курсов",
        company: "Клерк",
        type: "employer",
        salary: "от 110 000 ₽",
        meta: "Удалённо"
    },
    {
        title: "Финансовый аналитик",
        company: "Партнёр · EdTech",
        type: "new_job",
        salary: "120 000 – 140 000 ₽",
        meta: "Полная занятость"
    },
    {
        title: "Редактор раздела «Налоги»",
        company: "Медиа-партнёр",
        type: "new_job",
        salary: "от 95 000 ₽",
        meta: "Опыт от 2 лет"
    },
    {
        title: "Консультант по зарплатному НДФЛ",
        company: "Клерк · проекты",
        type: "side_job",
        salary: "3 000 ₽/час",
        meta: "10–15 ч/мес · подработка"
    },
    {
        title: "Верстальщик лендингов курсов",
        company: "Клерк",
        type: "side_job",
        salary: "2 500 ₽/час",
        meta: "Удалённо · по проектам"
    }
];

const KLERK_VACANCY_TYPE_LABELS = {
    employer: "от работодателя",
    new_job: "новая работа",
    side_job: "подработка"
};

function renderKlerkVacancies() {
    const list = document.getElementById("klerk-vacancies-list");
    if (!list) return;

    list.innerHTML = KLERK_VACANCIES_MOCK.map((v) => {
        const badge = KLERK_VACANCY_TYPE_LABELS[v.type] || v.type;
        return `
            <article class="klerk-vacancy-item" role="listitem" data-vacancy-type="${escapeHtml(v.type)}">
                <div class="klerk-vacancy-item__top">
                    <span class="klerk-vacancy-badge klerk-vacancy-badge--${escapeHtml(v.type)}">${escapeHtml(badge)}</span>
                    <span class="klerk-vacancy-salary">${escapeHtml(v.salary)}</span>
                </div>
                <h3 class="klerk-vacancy-title">${escapeHtml(v.title)}</h3>
                <p class="klerk-vacancy-meta">${escapeHtml(v.company)} · ${escapeHtml(v.meta)}</p>
            </article>
        `;
    }).join("");
}

function renderAllCategories(ai, planned) {
    const container = document.getElementById("all-cats-list");
    if (!container) return;

    const aiByKey = new Map();
    if (ai && Array.isArray(ai.categories)) {
        for (const c of ai.categories) {
            if (c && c.amount > 0) aiByKey.set(normalizeCategoryKey(c.categoryName), c);
        }
    }

    const items = [];
    if (planned && planned.length) {
        for (const p of planned) {
            const key = normalizeCategoryKey(p.name);
            const aiMatch = aiByKey.get(key);
            if (aiMatch) aiByKey.delete(key);
            items.push({
                name: p.name,
                planned: Number(p.plannedAmount || 0),
                aiActual: aiMatch ? Number(aiMatch.amount || 0) : 0,
                required: !!p.required,
                aiOnly: false
            });
        }
    }
    for (const aiCat of aiByKey.values()) {
        items.push({
            name: aiCat.categoryName,
            planned: 0,
            aiActual: Number(aiCat.amount || 0),
            required: false,
            aiOnly: true
        });
    }

    if (!items.length) {
        container.innerHTML = '<p class="text-xs text-slate-500">Запусти AI-анализ выписки — категории появятся автоматически.</p>';
        return;
    }

    items.sort((a, b) => Math.max(b.planned, b.aiActual) - Math.max(a.planned, a.aiActual));

    const totalPlanned = items.reduce((s, i) => s + i.planned, 0) || 0;
    const totalAi = items.reduce((s, i) => s + i.aiActual, 0) || 0;

    container.innerHTML = items.map((c) => {
        const denom = Math.max(c.planned, c.aiActual);
        const maxOverall = Math.max(totalPlanned, totalAi) || 1;
        const barPct = Math.min(Math.round((denom / maxOverall) * 100), 100);

        let primaryLine;
        if (c.aiOnly) {
            primaryLine = `<span class="font-bold text-rose-600">−${formatMoney(c.aiActual)} <span class="ml-1 text-[10px] uppercase tracking-wider text-rose-500">только AI</span></span>`;
        } else if (c.planned > 0) {
            const pctPlanned = totalPlanned ? Math.round((c.planned / totalPlanned) * 100) : 0;
            primaryLine = `<span class="font-bold text-slate-700">${formatMoney(c.planned)} <span class="ml-1 text-slate-400">${pctPlanned}%</span></span>`;
        } else {
            primaryLine = `<span class="text-xs italic text-slate-400">не в плане</span>`;
        }

        let factLine = "";
        if (!c.aiOnly && c.aiActual > 0) {
            const overPlan = c.planned > 0 && c.aiActual > c.planned;
            const underPlan = c.planned > 0 && c.aiActual < c.planned;
            const cls = overPlan ? "text-rose-600" : (underPlan ? "text-emerald-600" : "text-slate-500");
            const note = overPlan
                ? "перерасход"
                : (underPlan ? "в рамках плана" : "по факту");
            factLine = `<div class="mt-1 text-[11px] font-semibold ${cls}">факт AI: −${formatMoney(c.aiActual)} · ${note}</div>`;
        }

        const reqBadge = c.required ? ' <span class="budget-required">обяз.</span>' : '';
        const barClass = c.aiOnly
            ? "from-rose-300 to-rose-400"
            : "from-slate-300 to-slate-400";

        return `
            <div>
                <div class="mb-1 flex items-center justify-between gap-2 text-xs">
                    <span class="min-w-0 truncate font-semibold text-slate-700">${escapeHtml(c.name)}${reqBadge}</span>
                    ${primaryLine}
                </div>
                <div class="h-1.5 overflow-hidden rounded-full bg-slate-200/60">
                    <div class="h-full rounded-full bg-gradient-to-r ${barClass}" style="width: ${barPct}%"></div>
                </div>
                ${factLine}
            </div>
        `;
    }).join("");
}

function normalizeCategoryKey(name) {
    return String(name || "").trim().toLowerCase().replaceAll("ё", "е");
}

function renderHealth(ai, rating) {
    const labelEl = document.getElementById("rating-label");
    const posEl = document.getElementById("rating-position");
    const tipEl = document.getElementById("rating-tip");

    if (rating && rating.totalUsers > 0) {
        if (labelEl) labelEl.textContent = rating.label || "—";
        if (posEl) posEl.textContent = `#${rating.rank} из ${rating.totalUsers}`;
        if (tipEl) tipEl.textContent = rating.tip || "—";
    } else {
        if (labelEl) labelEl.textContent = "—";
        if (posEl) posEl.textContent = "— из —";
        if (tipEl) tipEl.textContent = "Заполни доход и категории — увидишь, как ты держишься среди других.";
    }

    const openBtn = document.getElementById("open-ai-detail-btn");
    if (openBtn) {
        if (ai) {
            openBtn.classList.remove("hidden");
            refreshLucide();
        } else {
            openBtn.classList.add("hidden");
        }
    }
}

function renderTips(ai) {
    const list = document.getElementById("tips-list");
    const meta = document.getElementById("tips-meta");
    if (!list) return;

    if (ai && ai.insights && ai.insights.length) {
        list.innerHTML = ai.insights.slice(0, 3).map((t, i) => `<li>${i + 1}) ${escapeHtml(t)}</li>`).join("");
        if (meta) meta.textContent = "из всех возможных данных";
    } else {
        list.innerHTML = '<li class="text-xs text-slate-500">Здесь будет анализ на 30 дней!</li>';
        if (meta) meta.textContent = "не запускался";
    }
}

function renderEmptyState() {
    setText("balance-amount", "0 ₽");
    setText("balance-free", "0 ₽");
    setText("balance-income", "0 ₽");
    setText("balance-expense", "0 ₽");
    setText("chip-income", "0 ₽");
    setText("chip-expense", "0 ₽");
    renderKlerkVacancies();
    const acl = document.getElementById("all-cats-list");
    if (acl) acl.innerHTML = '<p class="text-xs text-slate-500">Запусти AI-анализ выписки — категории появятся автоматически.</p>';
}

function renderBudgetEditor() {
    const editor = document.getElementById("budget-editor");
    const salaryInput = document.getElementById("budget-salary-input");
    const sumIncome = document.getElementById("budget-sum-income");
    const sumExpense = document.getElementById("budget-sum-expense");
    const sumFree = document.getElementById("budget-sum-free");

    if (salaryInput && !dashState.salaryDirty) {
        salaryInput.value = dashState.salary > 0 ? dashState.salary : "";
        salaryInput.placeholder = "0";
    }

    if (editor) {
        if (!dashState.plannedCategories.length) {
            editor.innerHTML = `<p class="mt-3 text-xs text-slate-500">Категорий пока нет. Нажми «+ Добавить» — например, «Жильё 25000».</p>`;
        } else {
            editor.innerHTML = dashState.plannedCategories.map((cat) => `
                <div class="budget-row" data-id="${cat.id}">
                    <div class="flex min-w-0 items-center">
                        <input type="text" class="budget-name-input" value="${escapeAttr(cat.name)}" data-field="name" data-id="${cat.id}">
                        ${cat.required ? '<span class="budget-required" title="Обязательная к оплате">обяз.</span>' : ""}
                    </div>
                    <div class="budget-input-wrap">
                        <input type="number" class="budget-input" min="0" step="500" value="${Number(cat.plannedAmount) || ""}" placeholder="0" data-field="amount" data-id="${cat.id}">
                        <span class="budget-suffix">₽</span>
                    </div>
                    <button type="button" class="budget-delete" data-id="${cat.id}" title="Удалить">×</button>
                </div>
            `).join("");
        }
    }

    const totalExpense = dashState.plannedCategories.reduce((s, c) => s + Number(c.plannedAmount || 0), 0);
    const free = dashState.salary - totalExpense;
    if (sumIncome) sumIncome.textContent = formatMoney(dashState.salary);
    if (sumExpense) sumExpense.textContent = formatMoney(totalExpense);
    if (sumFree) {
        sumFree.textContent = formatMoney(free);
        sumFree.classList.toggle("text-rose-600", free < 0);
        sumFree.classList.toggle("text-emerald-900", free >= 0);
    }

    if (typeof scheduleSyncBudgetHealthHeights === "function") {
        scheduleSyncBudgetHealthHeights();
    }
}

function bindBudgetEditor() {
    const salaryInput = document.getElementById("budget-salary-input");
    const editor = document.getElementById("budget-editor");
    const addBtn = document.getElementById("add-category-btn");
    const newForm = document.getElementById("new-category-form");
    const newName = document.getElementById("new-cat-name");
    const newAmount = document.getElementById("new-cat-amount");
    const cancelNew = document.getElementById("cancel-new-cat");
    const saveNew = document.getElementById("save-new-cat");

    if (salaryInput) {
        salaryInput.addEventListener("input", () => {
            dashState.salaryDirty = true;
        });
        salaryInput.addEventListener("blur", saveSalary);
        salaryInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                salaryInput.blur();
            }
        });
    }

    if (editor) {
        editor.addEventListener("blur", handleCategoryFieldChange, true);
        editor.addEventListener("keydown", (e) => {
            if (e.key === "Enter" && (e.target.matches(".budget-input") || e.target.matches(".budget-name-input"))) {
                e.preventDefault();
                e.target.blur();
            }
        });
        editor.addEventListener("click", (e) => {
            const delBtn = e.target.closest(".budget-delete");
            if (delBtn) handleCategoryDelete(delBtn.dataset.id);
        });
    }

    if (addBtn && newForm) {
        addBtn.addEventListener("click", () => {
            newForm.classList.remove("hidden");
            newName.value = "";
            newAmount.value = "";
            const reqEl = document.getElementById("new-cat-required");
            if (reqEl) reqEl.checked = false;
            newName.focus();
        });
    }

    if (cancelNew && newForm) {
        cancelNew.addEventListener("click", () => newForm.classList.add("hidden"));
    }

    if (saveNew) {
        saveNew.addEventListener("click", handleCategoryCreate);
    }

    [newName, newAmount].forEach((input) => {
        if (!input) return;
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                handleCategoryCreate();
            } else if (e.key === "Escape") {
                newForm?.classList.add("hidden");
            }
        });
    });
}

function isBudgetLocked() {
    return document.body.classList.contains("budget-locked");
}

async function saveSalary() {
    const salaryInput = document.getElementById("budget-salary-input");
    if (!salaryInput || !dashState.monthId) return;
    if (!dashState.salaryDirty) return;
    if (isBudgetLocked()) {
        salaryInput.value = dashState.salary;
        dashState.salaryDirty = false;
        return;
    }

    const value = Number(salaryInput.value || 0);
    if (Number.isNaN(value) || value < 0) {
        salaryInput.value = dashState.salary;
        dashState.salaryDirty = false;
        return;
    }
    if (value === dashState.salary) {
        dashState.salaryDirty = false;
        return;
    }

    try {
        await CashCareApi.updateMonthlyFinances(dashState.monthId, { salary: value });
        dashState.salaryDirty = false;
        await reloadOverview();
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось сохранить доход");
        salaryInput.value = dashState.salary;
        dashState.salaryDirty = false;
    }
}

async function handleCategoryFieldChange(e) {
    const target = e.target;
    if (!(target instanceof HTMLElement)) return;
    if (!target.matches(".budget-input[data-field], .budget-name-input[data-field]")) return;
    if (isBudgetLocked()) return;

    const id = Number(target.dataset.id);
    const field = target.dataset.field;
    const cat = dashState.plannedCategories.find((c) => c.id === id);
    if (!cat) return;

    const row = target.closest(".budget-row");
    row?.classList.add("saving");

    try {
        if (field === "name") {
            const newName = target.value.trim();
            if (!newName || newName === cat.name) {
                target.value = cat.name;
                row?.classList.remove("saving");
                return;
            }
            await CashCareApi.updateCategory(id, { nameCategory: newName });
        } else if (field === "amount") {
            const newAmount = Number(target.value || 0);
            if (Number.isNaN(newAmount) || newAmount < 0) {
                target.value = cat.plannedAmount || 0;
                row?.classList.remove("saving");
                return;
            }
            if (Number(cat.plannedAmount || 0) === newAmount) {
                row?.classList.remove("saving");
                return;
            }
            await CashCareApi.updateCategory(id, { plannedAmount: newAmount });
        }
        await reloadOverview();
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось сохранить категорию");
        row?.classList.remove("saving");
        await reloadOverview();
    }
}

async function handleCategoryDelete(idStr) {
    if (isBudgetLocked()) return;
    const id = Number(idStr);
    if (!id) return;
    const cat = dashState.plannedCategories.find((c) => c.id === id);
    const label = cat ? `«${cat.name}»` : "категорию";
    if (!confirm(`Удалить ${label}?`)) return;

    try {
        await CashCareApi.deleteCategory(id);
        await reloadOverview();
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось удалить категорию");
    }
}

async function handleCategoryCreate() {
    if (isBudgetLocked()) return;
    const newForm = document.getElementById("new-category-form");
    const newName = document.getElementById("new-cat-name");
    const newAmount = document.getElementById("new-cat-amount");
    const newRequired = document.getElementById("new-cat-required");
    if (!dashState.monthId) return;

    const name = (newName?.value || "").trim();
    const amount = Number(newAmount?.value || 0);
    const required = Boolean(newRequired?.checked);
    if (!name) {
        newName?.focus();
        return;
    }

    try {
        const created = await CashCareApi.createCategory(dashState.monthId, name, required);
        if (amount > 0 && created?.id) {
            await CashCareApi.updateCategory(created.id, { plannedAmount: amount });
        }
        newForm?.classList.add("hidden");
        if (newRequired) newRequired.checked = false;
        await reloadOverview();
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось создать категорию");
    }
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function bindReupload() {
    const btn = document.getElementById("reupload-pdf-btn");
    if (!btn) return;
    btn.addEventListener("click", () => {
        if (btn.disabled) return;
        runBudgetAi();
    });
}

function bindEditSurvey() {
    const btn = document.getElementById("edit-survey-btn");
    if (!btn) return;
    btn.addEventListener("click", handleEditSurvey);
}

function bindManageCredits() {
    const btn = document.getElementById("manage-credits-btn");
    if (!btn) return;
    btn.addEventListener("click", () => {
        if (typeof openCreditsOnlyModal === "function") {
            openCreditsOnlyModal();
        }
    });
}

function bindAiDetailModal() {
    const openBtn = document.getElementById("open-ai-detail-btn");
    const modal = document.getElementById("ai-detail-modal");
    const closeBtn = document.getElementById("ai-detail-close-btn");

    if (openBtn) {
        openBtn.addEventListener("click", openAiDetailModal);
    }
    if (closeBtn) {
        closeBtn.addEventListener("click", closeAiDetailModal);
    }
    if (modal) {
        modal.addEventListener("click", (e) => {
            if (e.target === modal) closeAiDetailModal();
        });
    }
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal && modal.classList.contains("open")) {
            closeAiDetailModal();
        }
    });
}

function openAiDetailModal() {
    const modal = document.getElementById("ai-detail-modal");
    const content = document.getElementById("ai-detail-content");
    const ai = dashState.aiAnalysis;
    if (!modal || !content || !ai) return;

    if (typeof renderAiAnalysis === "function") {
        renderAiAnalysis(content, ai, { compact: false });
    }
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    refreshLucide();
}

function closeAiDetailModal() {
    const modal = document.getElementById("ai-detail-modal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
}

async function handleEditSurvey() {
    const btn = document.getElementById("edit-survey-btn");
    if (btn) btn.disabled = true;
    try {
        const data = await CashCareApi.getSurvey();
        await openSurveyModal({
            mandatory: false,
            prefill: {
                maritalStatus: data.maritalStatus,
                childrenCount: data.childrenCount,
                employmentType: data.employmentType,
                housingStatus: data.housingStatus,
                hasDebts: data.hasDebts,
                financialGoal: data.financialGoal,
                citySize: data.citySize,
                spendingStyle: data.spendingStyle
            }
        });
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось загрузить анкету");
    } finally {
        if (btn) btn.disabled = false;
    }
}

function showAiRunOverlay(show) {
    const overlay = document.getElementById("ai-overlay");
    if (!overlay) return;
    if (show) {
        overlay.classList.remove("hidden");
        overlay.setAttribute("aria-hidden", "false");
        document.body.classList.add("modal-open");
    } else {
        overlay.classList.add("hidden");
        overlay.setAttribute("aria-hidden", "true");
        if (!document.querySelector(".init-modal-overlay.open")) {
            document.body.classList.remove("modal-open");
        }
    }
}

function setReuploadLoading(loading) {
    const btn = document.getElementById("reupload-pdf-btn");
    if (!btn || btn.classList.contains("is-locked")) return;
    if (loading) {
        btn.disabled = true;
        btn.classList.add("is-loading");
        btn.innerHTML = '<span class="spinner" aria-hidden="true"></span><span id="reupload-pdf-btn-label">Обновляем AI...</span>';
        return;
    }
    btn.classList.remove("is-loading");
    renderAiRefresh(dashState.aiRefresh);
}

async function runBudgetAi() {
    if (aiBudgetRunning) return;
    aiBudgetRunning = true;

    const stepEl = document.getElementById("ai-overlay-step");
    showAiRunOverlay(true);
    setReuploadLoading(true);
    if (stepEl) stepEl.textContent = "Считаем категории и распределение...";

    const t1 = setTimeout(() => stepEl && (stepEl.textContent = "Подбираем советы по плану..."), 6000);
    const t2 = setTimeout(() => stepEl && (stepEl.textContent = "Почти готово, формируем профиль..."), 15000);

    try {
        const overview = await CashCareApi.runBudgetAi();
        applyOverview(overview);
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось обновить AI");
    } finally {
        clearTimeout(t1);
        clearTimeout(t2);
        aiBudgetRunning = false;
        showAiRunOverlay(false);
        setReuploadLoading(false);
    }
}

function escapeHtml(str) {
    return String(str ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;");
}

function escapeAttr(str) {
    return escapeHtml(str);
}
