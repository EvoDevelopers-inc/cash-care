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
    salaryDirty: false
};

document.addEventListener("DOMContentLoaded", async () => {
    redirectIfGuest();

    if (logoutBtn) {
        logoutBtn.addEventListener("click", handleLogout);
    }

    bindNav();
    bindFakeButtons();
    bindBudgetEditor();
    bindReupload();

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

    renderOverview(overview);
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

    renderBalance(overview.balance, ai);
    renderMoodCard(overview.balance);
    renderDistribution(overview.balance);
    renderTopCategories(ai, overview.plannedCategories);
    renderAllCategories(ai, overview.plannedCategories);
    renderHealth(ai, overview.rating);
    renderTips(ai);
    renderBudgetEditor();
    renderAiRefresh(overview.aiRefresh);
}

function renderAiRefresh(refresh) {
    const btn = document.getElementById("reupload-pdf-btn");
    if (!btn) return;

    const canRefresh = refresh ? !!refresh.canRefresh : true;
    btn.disabled = !canRefresh;
    btn.classList.toggle("is-locked", !canRefresh);

    if (canRefresh) {
        btn.textContent = "↻ Обновить AI";
        btn.removeAttribute("title");
    } else {
        const reason = refresh?.reason || "Доступно один раз в месяц";
        btn.textContent = "⏳ AI ждёт следующий месяц";
        btn.title = reason;
    }
}

function renderDistribution(balance) {
    const required = Number(balance?.requiredExpense || 0);
    const optional = Number(balance?.optionalExpense || 0);
    const savings = Math.max(Number(balance?.savingsAmount || 0), 0);
    const free = Math.max(Number(balance?.freePocket || 0), 0);
    const total = required + optional + savings + free || 1;

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

    const income = ai && ai.income ? ai.income : Number(balance?.salary || 0);
    const expense = ai && ai.expense ? ai.expense : Number(balance?.plannedExpense || 0);
    const free = income - expense;

    setText("balance-amount", formatMoney(free));
    setText("balance-free", formatMoney(Math.max(free, 0)));
    setText("balance-income", formatMoney(income));
    setText("balance-expense", formatMoney(expense));
    setText("chip-income", formatMoney(income));
    setText("chip-expense", `−${formatMoney(expense)}`);
}

function renderTopCategories(ai, planned) {
    const list = document.getElementById("top-cat-list");
    const nameEl = document.getElementById("top-cat-name");
    if (!list || !nameEl) return;

    if (ai && ai.categories.length) {
        const sorted = [...ai.categories].filter((c) => c.amount > 0).sort((a, b) => b.amount - a.amount);
        if (!sorted.length) {
            nameEl.textContent = "—";
            list.innerHTML = '<p class="text-xs text-slate-500">AI не нашёл существенных трат</p>';
            return;
        }
        nameEl.textContent = sorted[0].categoryName;
        list.innerHTML = sorted.slice(0, 4).map((c) => `
            <div class="flex items-center justify-between rounded-xl bg-white/55 px-3 py-2.5">
                <div>
                    <div class="text-sm font-semibold">${escapeHtml(c.categoryName)}</div>
                    <div class="text-[11px] text-slate-500">за этот месяц</div>
                </div>
                <div class="text-sm font-extrabold text-rose-600">−${formatMoney(c.amount)}</div>
            </div>
        `).join("");
        return;
    }

    if (planned && planned.length) {
        const sorted = [...planned]
            .filter((c) => Number(c.plannedAmount) > 0)
            .sort((a, b) => Number(b.plannedAmount) - Number(a.plannedAmount));
        if (!sorted.length) {
            nameEl.textContent = "—";
            list.innerHTML = '<p class="text-xs text-slate-500">Заполни планы по категориям</p>';
            return;
        }
        nameEl.textContent = sorted[0].name;
        list.innerHTML = sorted.slice(0, 4).map((c) => `
            <div class="flex items-center justify-between rounded-xl bg-white/55 px-3 py-2.5">
                <div>
                    <div class="text-sm font-semibold">${escapeHtml(c.name)}</div>
                    <div class="text-[11px] text-slate-500">плановая сумма</div>
                </div>
                <div class="text-sm font-extrabold text-slate-700">${formatMoney(c.plannedAmount)}</div>
            </div>
        `).join("");
        return;
    }

    nameEl.textContent = "Нет данных";
    list.innerHTML = '<p class="text-xs text-slate-500">Заполни бюджет и загрузи PDF — здесь появятся топ траты.</p>';
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
    setText("health-subs", ai && ai.subscriptions.length ? `${ai.subscriptions.length} шт` : "—");

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
}

function renderTips(ai) {
    const list = document.getElementById("tips-list");
    const meta = document.getElementById("tips-meta");
    if (!list) return;

    if (ai && ai.insights && ai.insights.length) {
        list.innerHTML = ai.insights.slice(0, 3).map((t, i) => `<li>${i + 1}) ${escapeHtml(t)}</li>`).join("");
        if (meta) meta.textContent = "из выписки";
    } else {
        list.innerHTML = '<li class="text-xs text-slate-500">Загрузи PDF-выписку — AI выделит, где можно сэкономить.</li>';
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
    setText("top-cat-name", "Нет данных");
    const tcl = document.getElementById("top-cat-list");
    if (tcl) tcl.innerHTML = '<p class="text-xs text-slate-500">Заполни бюджет и загрузи PDF — здесь появятся топ траты.</p>';
    const acl = document.getElementById("all-cats-list");
    if (acl) acl.innerHTML = '<p class="text-xs text-slate-500">Запусти AI-анализ выписки — категории появятся автоматически.</p>';
}

/* ─────────────────────────  БЮДЖЕТ-ЭДИТОР  ───────────────────────── */

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

async function saveSalary() {
    const salaryInput = document.getElementById("budget-salary-input");
    if (!salaryInput || !dashState.monthId) return;
    if (!dashState.salaryDirty) return;

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

/* ─────────────────────────  RE-UPLOAD PDF  ───────────────────────── */

function bindReupload() {
    const btn = document.getElementById("reupload-pdf-btn");
    if (!btn) return;
    btn.addEventListener("click", () => {
        if (btn.disabled) return;
        runBudgetAi();
    });
}

async function runBudgetAi() {
    const overlay = document.getElementById("ai-overlay");
    const stepEl = document.getElementById("ai-overlay-step");
    if (overlay) overlay.classList.remove("hidden");
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
        if (overlay) overlay.classList.add("hidden");
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
