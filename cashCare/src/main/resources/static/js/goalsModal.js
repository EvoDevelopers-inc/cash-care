const GOAL_CATEGORIES = [
    { code: "CAR",         label: "Машина",                 emoji: "🚗", grad: "from-slate-500 to-slate-700" },
    { code: "HOUSE",       label: "Квартира / дом",         emoji: "🏠", grad: "from-amber-400 to-orange-500" },
    { code: "RENOVATION",  label: "Ремонт",                 emoji: "🛋️", grad: "from-amber-500 to-rose-500" },
    { code: "TRIP",        label: "Путешествие",            emoji: "✈️", grad: "from-sky-400 to-cyan-500" },
    { code: "EDUCATION",   label: "Образование",            emoji: "🎓", grad: "from-indigo-400 to-violet-500" },
    { code: "GADGET",      label: "Гаджет / техника",       emoji: "📱", grad: "from-zinc-500 to-slate-700" },
    { code: "WEDDING",     label: "Свадьба",                emoji: "💍", grad: "from-pink-300 to-rose-400" },
    { code: "KIDS",        label: "Ребёнок",                emoji: "👶", grad: "from-pink-400 to-fuchsia-400" },
    { code: "BUSINESS",    label: "Бизнес / стартап",       emoji: "💼", grad: "from-emerald-500 to-teal-600" },
    { code: "EMERGENCY",   label: "Подушка безопасности",   emoji: "🛡️", grad: "from-emerald-400 to-green-500" },
    { code: "DEBT_PAYOFF", label: "Закрыть долг",           emoji: "💸", grad: "from-rose-400 to-red-500" },
    { code: "GIFT",        label: "Подарок",                emoji: "🎁", grad: "from-rose-300 to-pink-400" },
    { code: "SPORT",       label: "Спорт / фитнес",         emoji: "🏋️", grad: "from-orange-400 to-red-400" },
    { code: "HOBBY",       label: "Хобби / творчество",     emoji: "🎨", grad: "from-violet-400 to-fuchsia-500" },
    { code: "PET",         label: "Питомец",                emoji: "🐶", grad: "from-amber-300 to-orange-400" },
    { code: "HEALTH",      label: "Здоровье",               emoji: "🧘", grad: "from-teal-400 to-cyan-500" },
    { code: "INVEST",      label: "Инвестиции",             emoji: "📈", grad: "from-emerald-500 to-cyan-500" },
    { code: "PARTY",       label: "Праздник",               emoji: "🎉", grad: "from-fuchsia-400 to-pink-500" },
    { code: "MOVE",        label: "Переезд",                emoji: "📦", grad: "from-amber-400 to-yellow-500" },
    { code: "CUSTOM",      label: "Своя цель",              emoji: "⭐", grad: "from-indigo-400 to-purple-500" }
];

const goalsState = {
    items: [],
    loading: false,
    editing: null,
    contributing: null,
    freePocket: 0,
    canSave: 0
};

function refreshLucideGoals() {
    if (window.lucide && typeof window.lucide.createIcons === "function") {
        window.lucide.createIcons();
    }
}

function findCategory(code) {
    return GOAL_CATEGORIES.find((c) => c.code === code) || GOAL_CATEGORIES[GOAL_CATEGORIES.length - 1];
}

function formatGoalAmount(value) {
    return new Intl.NumberFormat("ru-RU").format(Math.round(Number(value) || 0)) + " ₽";
}

function fogStyle(pct) {
    const p = Math.max(0, Math.min(100, pct || 0));
    const blur = (24 * (1 - p / 100)).toFixed(1);
    const opacity = (0.35 + 0.65 * (p / 100)).toFixed(2);
    const scale = (0.85 + 0.15 * (p / 100)).toFixed(2);
    return `filter: blur(${blur}px); opacity: ${opacity}; transform: scale(${scale});`;
}

function goalMoodText(pct, completed) {
    if (completed || pct >= 100) return "Готово! Можно праздновать";
    if (pct >= 75) return "Уже видно, ещё чуть-чуть";
    if (pct >= 40) return "Контуры проступают";
    if (pct >= 15) return "Сквозь туман что-то виднеется";
    return "Пока в тумане — отложи первый раз";
}

async function openGoalsModal() {
    const modal = document.getElementById("goals-modal");
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");

    await loadGoalsAndRender();
}

function closeGoalsModal() {
    const modal = document.getElementById("goals-modal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    if (!document.querySelector(".init-modal-overlay.open")) {
        document.body.classList.remove("modal-open");
    }
}

async function loadGoalsAndRender() {
    if (goalsState.loading) return;
    goalsState.loading = true;
    try {
        const list = await CashCareApi.getGoals();
        goalsState.items = Array.isArray(list) ? list : [];
    } catch (err) {
        console.warn("goals load failed", err);
        goalsState.items = [];
    } finally {
        goalsState.loading = false;
    }
    renderGoalsModal();
    renderGoalsTeaser();
    updateSidebarCount();
}

function updateSidebarCount() {
    const badge = document.getElementById("sidebar-goals-count");
    if (!badge) return;
    const n = goalsState.items.length;
    if (n <= 0) {
        badge.classList.add("hidden");
    } else {
        badge.classList.remove("hidden");
        badge.textContent = String(n);
    }
}

function renderGoalsModal() {
    const grid = document.getElementById("goals-grid");
    const empty = document.getElementById("goals-empty");
    const summary = document.getElementById("goals-summary");
    if (!grid || !empty) return;

    const items = goalsState.items;

    const totalSaved = items.reduce((s, g) => s + Number(g.savedAmount || 0), 0);
    const totalRemaining = items.reduce((s, g) => s + Number(g.remainingAmount || 0), 0);
    setText("goals-stat-count", String(items.length));
    setText("goals-stat-saved", formatGoalAmount(totalSaved));
    setText("goals-stat-remaining", items.length === 0 ? "—" : formatGoalAmount(totalRemaining));

    if (items.length === 0) {
        empty.classList.remove("hidden");
        grid.innerHTML = "";
        if (summary) summary.classList.add("hidden");
        return;
    }
    empty.classList.add("hidden");
    if (summary) summary.classList.remove("hidden");

    grid.innerHTML = items.map((g) => goalCardHtml(g, { compact: false })).join("");
    bindGoalCardActions(grid);
    refreshLucideGoals();
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function goalCardHtml(g, opts = {}) {
    const compact = opts.compact === true;
    const pct = Number(g.progressPct || 0);
    const completed = Boolean(g.completed);
    const grad = g.gradient || findCategory(g.category).grad;
    const emoji = g.emoji || findCategory(g.category).emoji;
    const fog = fogStyle(pct);
    const mood = goalMoodText(pct, completed);
    const titleSafe = escapeGoalText(g.title || "Цель");
    const remaining = Number(g.remainingAmount || 0);

    const completedBadge = completed
        ? '<span class="absolute right-3 top-3 inline-flex items-center gap-1 rounded-full bg-emerald-500 px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-white shadow-md"><i data-lucide="check" class="lc lc-xs"></i> готово</span>'
        : '';

    const editBtn = compact
        ? ''
        : `<button type="button" class="goal-card-edit" data-action="edit" data-id="${g.id}" title="Редактировать">
                <i data-lucide="pencil" class="lc lc-xs pointer-events-none"></i>
            </button>`;

    let contributeBtn;
    if (completed) {
        contributeBtn = `<button type="button" class="goal-card-cta goal-card-cta-done" data-action="celebrate" data-id="${g.id}" disabled>
                <i data-lucide="party-popper" class="lc lc-sm pointer-events-none"></i>
                Цель достигнута
            </button>`;
    } else if (g.contributedThisMonth) {
        const nextLabel = escapeGoalText(g.nextContributionLabel || "след. месяц");
        contributeBtn = `<button type="button" class="goal-card-cta goal-card-cta-locked" data-id="${g.id}" disabled>
                <i data-lucide="lock" class="lc lc-sm pointer-events-none"></i>
                В этом месяце уже отложил · след. ${nextLabel}
            </button>`;
    } else {
        contributeBtn = `<button type="button" class="goal-card-cta" data-action="contribute" data-id="${g.id}">
                <i data-lucide="piggy-bank" class="lc lc-sm pointer-events-none"></i>
                Я отложил
            </button>`;
    }

    const forecast = !completed ? buildForecastHtml(g) : '';

    return `
        <div class="goal-card bg-gradient-to-br ${grad}" data-goal-id="${g.id}" style="--goal-progress: ${pct}%;">
            <div class="goal-card-fog" aria-hidden="true">
                <div class="goal-card-fog-emoji" style="${fog}">${emoji}</div>
            </div>
            ${completedBadge}
            ${editBtn}
            <div class="goal-card-body">
                <div class="goal-card-cat">${escapeGoalText(g.categoryLabel || "")}</div>
                <h4 class="goal-card-title">${titleSafe}</h4>
                <div class="goal-card-amounts">
                    <span class="goal-card-amount-saved">${formatGoalAmount(g.savedAmount)}</span>
                    <span class="goal-card-amount-target">из ${formatGoalAmount(g.targetAmount)}</span>
                </div>
                <div class="goal-card-progress">
                    <div class="goal-card-progress-fill" style="width: ${pct}%"></div>
                </div>
                <div class="goal-card-meta">
                    <span class="goal-card-pct">${pct}%</span>
                    <span class="goal-card-mood">${mood}</span>
                </div>
                ${remaining > 0 && !completed ? `<div class="goal-card-remaining">осталось ${formatGoalAmount(remaining)}</div>` : ''}
                ${forecast}
                ${contributeBtn}
            </div>
        </div>
    `;
}

function buildForecastHtml(g) {
    const remaining = Number(g.remainingAmount || 0);
    if (remaining <= 0) return '';

    const lines = [];

    const avg = Number(g.avgMonthlyContribution || 0);
    const eta = Number(g.etaMonths || 0);
    if (avg > 0 && eta > 0) {
        lines.push(`
            <div class="goal-forecast-line">
                <i data-lucide="trending-up" class="lc lc-xs pointer-events-none"></i>
                Темп ${formatGoalAmount(avg)}/мес · ETA ≈ ${formatMonthsHuman(eta)}
            </div>
        `);
    }

    const free = Number(goalsState.freePocket || 0);
    if (free > 0 && (avg <= 0 || avg < free * 0.5)) {
        const monthsByFree = Math.ceil(remaining / free);
        if (monthsByFree > 0 && monthsByFree <= 600) {
            lines.push(`
                <div class="goal-forecast-line goal-forecast-hint">
                    <i data-lucide="sparkles" class="lc lc-xs pointer-events-none"></i>
                    Если откладывать всё свободное (${formatGoalAmount(free)}) — закроешь за ${formatMonthsHuman(monthsByFree)}
                </div>
            `);
        }
    }

    const monthsToDeadline = Number(g.monthsUntilTargetDate || 0);
    const needPerMonth = Number(g.requiredMonthlyForDeadline || 0);
    if (monthsToDeadline > 0 && needPerMonth > 0) {
        const enoughPace = avg > 0 && avg >= needPerMonth;
        const cls = enoughPace ? 'goal-forecast-ok' : 'goal-forecast-warn';
        const icon = enoughPace ? 'check-circle-2' : 'alert-triangle';
        lines.push(`
            <div class="goal-forecast-line ${cls}">
                <i data-lucide="${icon}" class="lc lc-xs pointer-events-none"></i>
                До дедлайна нужно ${formatGoalAmount(needPerMonth)}/мес (осталось ${formatMonthsHuman(monthsToDeadline)})
            </div>
        `);
    }

    if (lines.length === 0) return '';
    return `<div class="goal-card-forecast">${lines.join('')}</div>`;
}

function formatMonthsHuman(months) {
    const m = Math.max(0, Math.round(Number(months) || 0));
    if (m <= 0) return "меньше месяца";
    if (m < 12) return `${m} мес`;
    const years = Math.floor(m / 12);
    const rest = m - years * 12;
    if (rest === 0) return `${years} ${pluralYear(years)}`;
    return `${years} ${pluralYear(years)} ${rest} мес`;
}

function pluralYear(n) {
    const r10 = n % 10;
    const r100 = n % 100;
    if (r10 === 1 && r100 !== 11) return "год";
    if (r10 >= 2 && r10 <= 4 && (r100 < 12 || r100 > 14)) return "года";
    return "лет";
}

function escapeGoalText(text) {
    if (text == null) return "";
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function bindGoalCardActions(host) {
    host.querySelectorAll("[data-action]").forEach((btn) => {
        btn.addEventListener("click", (e) => {
            e.stopPropagation();
            const id = Number(btn.dataset.id);
            if (!id) return;
            const action = btn.dataset.action;
            if (action === "contribute") openContributeModal(id);
            else if (action === "edit") openGoalEditModal(id);
        });
    });
}

function renderGoalsTeaser() {
    const teaser = document.getElementById("goal-teaser");
    if (!teaser) return;

    const empty = document.getElementById("goal-teaser-empty");
    const host = document.getElementById("goal-teaser-card-host");
    const hint = document.getElementById("goal-teaser-hint");
    if (!host || !empty) return;

    const items = goalsState.items;
    if (items.length === 0) {
        teaser.classList.add("hidden");
        empty.classList.add("hidden");
        host.innerHTML = "";
        if (hint) {
            hint.classList.add("hidden");
            hint.innerHTML = "";
        }
        return;
    }

    teaser.classList.remove("hidden");
    empty.classList.add("hidden");

    const candidatesActive = items.filter((g) => !g.completed && !g.contributedThisMonth);
    const candidatesActiveAny = items.filter((g) => !g.completed);
    const sortByProgress = (a, b) => Number(b.progressPct || 0) - Number(a.progressPct || 0);
    const top = (candidatesActive.length > 0
            ? [...candidatesActive].sort(sortByProgress)
            : [...candidatesActiveAny].sort(sortByProgress)
    )[0] || items[0];

    setText("goal-teaser-title", `${top.emoji} ${top.title}`);
    host.innerHTML = goalCardHtml(top, { compact: true });
    bindGoalCardActions(host);

    if (hint) {
        const free = Math.round(Number(goalsState.freePocket || 0));
        const remaining = Number(top.remainingAmount || 0);

        if (top.contributedThisMonth) {
            const next = escapeGoalText(top.nextContributionLabel || "след. месяц");
            hint.classList.remove("hidden");
            hint.innerHTML = `
                <div class="goal-teaser-hint-text goal-teaser-hint-locked">
                    <i data-lucide="lock" class="lc lc-sm"></i>
                    На «${escapeGoalText(top.title)}» в этом месяце уже отложили. Следующий взнос — <strong>${next}</strong>.
                </div>
            `;
        } else if (free > 0 && remaining > 0 && !top.completed) {
            const target = Math.min(free, remaining);
            hint.classList.remove("hidden");
            hint.innerHTML = `
                <div class="goal-teaser-hint-text">
                    <i data-lucide="sparkles" class="lc lc-sm"></i>
                    AI считает, что в этом месяце свободно <strong>${formatGoalAmount(free)}</strong> — отложи на «${escapeGoalText(top.title)}»?
                </div>
                <button type="button" id="goal-teaser-hint-cta" class="goal-teaser-hint-btn" data-amount="${target}" data-goal-id="${top.id}">
                    Отложить ${formatGoalAmount(target)}
                </button>
            `;
            const cta = hint.querySelector("#goal-teaser-hint-cta");
            if (cta) {
                cta.addEventListener("click", () => {
                    openContributeModal(top.id);
                    setTimeout(() => {
                        const input = document.getElementById("goal-contribute-amount");
                        if (input) {
                            input.value = String(target);
                            input.focus();
                        }
                    }, 100);
                });
            }
        } else {
            hint.classList.add("hidden");
            hint.innerHTML = "";
        }
    }

    refreshLucideGoals();
}

function openGoalEditModal(idOrNull) {
    const modal = document.getElementById("goal-edit-modal");
    if (!modal) return;

    const isEdit = Boolean(idOrNull);
    const goal = isEdit ? goalsState.items.find((g) => g.id === idOrNull) : null;
    goalsState.editing = goal || null;

    setText("goal-edit-badge", isEdit ? "Редактирование" : "Новая цель");
    setText("goal-edit-title", isEdit ? "Подправь мечту" : "О чём мечтаешь?");

    document.getElementById("goal-input-title").value = goal ? goal.title : "";
    document.getElementById("goal-input-target").value = goal ? Math.round(Number(goal.targetAmount || 0)) : "";
    document.getElementById("goal-input-saved").value = goal ? Math.round(Number(goal.savedAmount || 0)) : "";
    document.getElementById("goal-input-emoji").value = goal && goal.category === "CUSTOM" ? (goal.emoji || "") : "";

    renderCategoryGrid(goal ? goal.category : "CAR");
    toggleCustomEmojiRow(goal ? goal.category : "CAR");

    const deleteBtn = document.getElementById("goal-edit-delete-btn");
    if (deleteBtn) deleteBtn.classList.toggle("hidden", !isEdit);

    hideAlert("goal-edit-alert");

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    refreshLucideGoals();
}

function closeGoalEditModal() {
    const modal = document.getElementById("goal-edit-modal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    goalsState.editing = null;
    if (!document.querySelector(".init-modal-overlay.open")) {
        document.body.classList.remove("modal-open");
    }
}

function renderCategoryGrid(activeCode) {
    const grid = document.getElementById("goal-category-grid");
    if (!grid) return;
    grid.innerHTML = GOAL_CATEGORIES.map((c) => `
        <button type="button"
                class="goal-cat-pill ${c.code === activeCode ? 'active' : ''}"
                data-cat="${c.code}">
            <span class="goal-cat-pill-emoji">${c.emoji}</span>
            <span class="goal-cat-pill-label">${escapeGoalText(c.label)}</span>
        </button>
    `).join("");

    grid.querySelectorAll("[data-cat]").forEach((btn) => {
        btn.addEventListener("click", () => {
            grid.querySelectorAll("[data-cat]").forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            toggleCustomEmojiRow(btn.dataset.cat);
        });
    });
}

function toggleCustomEmojiRow(catCode) {
    const row = document.getElementById("goal-custom-emoji-row");
    if (!row) return;
    row.classList.toggle("hidden", catCode !== "CUSTOM");
}

function getSelectedCategory() {
    const active = document.querySelector("#goal-category-grid [data-cat].active");
    return active ? active.dataset.cat : "CAR";
}

async function handleGoalEditSubmit(e) {
    e.preventDefault();
    hideAlert("goal-edit-alert");

    const title = (document.getElementById("goal-input-title").value || "").trim();
    const category = getSelectedCategory();
    const targetAmount = Number(document.getElementById("goal-input-target").value || 0);
    const savedAmount = Number(document.getElementById("goal-input-saved").value || 0);
    const customEmoji = (document.getElementById("goal-input-emoji").value || "").trim();

    if (!title) {
        showAlert("goal-edit-alert", "Дай мечте имя", "error");
        return;
    }
    if (!targetAmount || targetAmount <= 0) {
        showAlert("goal-edit-alert", "Сумма цели должна быть больше нуля", "error");
        return;
    }
    if (savedAmount > targetAmount) {
        showAlert("goal-edit-alert", "«Уже отложено» не может быть больше суммы цели", "error");
        return;
    }
    if (category === "CUSTOM" && !customEmoji) {
        showAlert("goal-edit-alert", "Поставь эмодзи для своей цели", "error");
        return;
    }

    const payload = {
        title,
        category,
        targetAmount,
        savedAmount: savedAmount > 0 ? savedAmount : 0,
        customEmoji: category === "CUSTOM" ? customEmoji : null
    };

    const submitBtn = document.getElementById("goal-edit-submit-btn");
    if (submitBtn) submitBtn.disabled = true;

    try {
        if (goalsState.editing && goalsState.editing.id) {
            await CashCareApi.updateGoal(goalsState.editing.id, payload);
        } else {
            await CashCareApi.createGoal(payload);
        }
        closeGoalEditModal();
        await loadGoalsAndRender();
    } catch (err) {
        showAlert("goal-edit-alert", formatError(err) || "Не удалось сохранить", "error");
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

async function handleGoalDelete() {
    const goal = goalsState.editing;
    if (!goal || !goal.id) return;
    if (!window.confirm(`Удалить цель «${goal.title}»? Это действие нельзя отменить.`)) return;

    try {
        await CashCareApi.deleteGoal(goal.id);
        closeGoalEditModal();
        await loadGoalsAndRender();
    } catch (err) {
        showAlert("goal-edit-alert", formatError(err) || "Не удалось удалить", "error");
    }
}

function openContributeModal(goalId) {
    const goal = goalsState.items.find((g) => g.id === goalId);
    if (!goal) return;

    if (goal.contributedThisMonth) {
        const next = goal.nextContributionLabel || "следующий месяц";
        if (typeof showProfileAlert === "function") {
            showProfileAlert(`В этом месяце уже откладывали на «${goal.title}». Следующий взнос — ${next}.`);
        } else {
            alert(`В этом месяце уже откладывали на эту цель. Следующий взнос — ${next}.`);
        }
        return;
    }

    goalsState.contributing = goal;

    const modal = document.getElementById("goal-contribute-modal");
    if (!modal) return;

    setText("goal-contribute-title", `${goal.emoji} ${goal.title}`);
    setText(
        "goal-contribute-meta",
        `Отложено ${formatGoalAmount(goal.savedAmount)} из ${formatGoalAmount(goal.targetAmount)} · осталось ${formatGoalAmount(goal.remainingAmount)}`
    );

    const free = Math.round(Number(goalsState.freePocket || 0));
    const tip = document.getElementById("goal-contribute-free-tip");
    if (tip) {
        if (free > 0) {
            tip.classList.remove("hidden");
            tip.innerHTML = `<i data-lucide="sparkles" class="lc lc-xs pointer-events-none"></i> AI оценил твой свободный остаток на этот месяц как <strong>${formatGoalAmount(free)}</strong> — можешь отложить всё разом.`;
        } else {
            tip.classList.add("hidden");
            tip.innerHTML = "";
        }
    }

    document.getElementById("goal-contribute-amount").value = "";
    document.getElementById("goal-contribute-note").value = "";
    hideAlert("goal-contribute-alert");

    renderQuickAmounts(goal);

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    refreshLucideGoals();

    setTimeout(() => {
        const input = document.getElementById("goal-contribute-amount");
        if (input) input.focus();
    }, 80);
}

function closeContributeModal() {
    const modal = document.getElementById("goal-contribute-modal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    goalsState.contributing = null;
    if (!document.querySelector(".init-modal-overlay.open")) {
        document.body.classList.remove("modal-open");
    }
}

function renderQuickAmounts(goal) {
    const host = document.getElementById("goal-contribute-quick");
    if (!host) return;

    const remaining = Number(goal.remainingAmount || 0);
    const free = Math.round(Number(goalsState.freePocket || 0));

    const baseAmounts = [1000, 5000, 10000, 25000, 50000]
        .filter((v) => v > 0 && v <= remaining);

    const items = [];

    if (free > 0 && free <= remaining) {
        items.push({ value: free, label: `${formatGoalAmount(free)} · свободный остаток`, smart: true });
    }

    baseAmounts.slice(0, 4).forEach((v) => items.push({ value: v, label: formatGoalAmount(v), smart: false }));

    if (remaining > 0) {
        const isDup = items.some((it) => it.value === remaining);
        if (!isDup) {
            items.push({ value: remaining, label: `Закрыть · ${formatGoalAmount(remaining)}`, close: true });
        }
    }

    const seen = new Set();
    const dedup = [];
    for (const it of items) {
        if (seen.has(it.value)) continue;
        seen.add(it.value);
        dedup.push(it);
    }

    if (dedup.length === 0) {
        host.innerHTML = "";
        return;
    }

    host.innerHTML = dedup.map((it) => `
        <button type="button"
                class="goal-quick-pill ${it.smart ? 'goal-quick-pill-smart' : ''} ${it.close ? 'goal-quick-pill-close' : ''}"
                data-amount="${it.value}">
            ${it.smart ? '<span class="goal-pill-spark">✨</span>' : ''}${escapeGoalText(it.label)}
        </button>
    `).join("");

    host.querySelectorAll("[data-amount]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const input = document.getElementById("goal-contribute-amount");
            if (input) {
                input.value = String(btn.dataset.amount);
                input.focus();
            }
        });
    });
}

async function handleContributeSubmit(e) {
    e.preventDefault();
    hideAlert("goal-contribute-alert");

    const goal = goalsState.contributing;
    if (!goal) return;

    const amount = Number(document.getElementById("goal-contribute-amount").value || 0);
    const note = (document.getElementById("goal-contribute-note").value || "").trim();

    if (!amount || amount <= 0) {
        showAlert("goal-contribute-alert", "Введи сумму больше нуля", "error");
        return;
    }

    const submitBtn = document.getElementById("goal-contribute-submit-btn");
    if (submitBtn) submitBtn.disabled = true;

    try {
        const updated = await CashCareApi.contributeGoal(goal.id, {
            amount,
            note: note || null
        });
        closeContributeModal();

        const idx = goalsState.items.findIndex((g) => g.id === updated.id);
        if (idx >= 0) goalsState.items[idx] = updated;
        renderGoalsModal();
        renderGoalsTeaser();

        if (updated.completed) {
            launchGoalCelebration(updated);
        }
    } catch (err) {
        showAlert("goal-contribute-alert", formatError(err) || "Не удалось сохранить", "error");
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

function launchGoalCelebration(goal) {
    const overlay = document.createElement("div");
    overlay.className = "goal-celebrate-overlay";
    overlay.innerHTML = `
        <div class="goal-celebrate-card">
            <div class="goal-celebrate-emoji">${goal.emoji || "🎉"}</div>
            <div class="goal-celebrate-title">Цель достигнута!</div>
            <div class="goal-celebrate-sub">${escapeGoalText(goal.title)}</div>
        </div>
    `;
    document.body.appendChild(overlay);
    setTimeout(() => overlay.classList.add("show"), 20);
    setTimeout(() => {
        overlay.classList.remove("show");
        setTimeout(() => overlay.remove(), 400);
    }, 2200);
}

function bindGoalsModule() {
    const openBtn = document.getElementById("open-goals-btn");
    if (openBtn) openBtn.addEventListener("click", openGoalsModal);

    const teaserOpenBtn = document.getElementById("goal-teaser-open");
    if (teaserOpenBtn) teaserOpenBtn.addEventListener("click", openGoalsModal);

    const teaserAddBtn = document.getElementById("goal-teaser-add");
    if (teaserAddBtn) {
        teaserAddBtn.addEventListener("click", () => {
            openGoalsModal().then(() => openGoalEditModal(null));
        });
    }

    const closeBtn = document.getElementById("goals-close-btn");
    if (closeBtn) closeBtn.addEventListener("click", closeGoalsModal);

    const goalsModal = document.getElementById("goals-modal");
    if (goalsModal) {
        goalsModal.addEventListener("click", (e) => {
            if (e.target === goalsModal) closeGoalsModal();
        });
    }

    const addBtn = document.getElementById("goals-add-btn");
    if (addBtn) addBtn.addEventListener("click", () => openGoalEditModal(null));

    const emptyAddBtn = document.getElementById("goals-empty-add-btn");
    if (emptyAddBtn) emptyAddBtn.addEventListener("click", () => openGoalEditModal(null));

    const editClose = document.getElementById("goal-edit-close-btn");
    if (editClose) editClose.addEventListener("click", closeGoalEditModal);

    const editCancel = document.getElementById("goal-edit-cancel-btn");
    if (editCancel) editCancel.addEventListener("click", closeGoalEditModal);

    const editForm = document.getElementById("goal-edit-form");
    if (editForm) editForm.addEventListener("submit", handleGoalEditSubmit);

    const editDelete = document.getElementById("goal-edit-delete-btn");
    if (editDelete) editDelete.addEventListener("click", handleGoalDelete);

    const editModal = document.getElementById("goal-edit-modal");
    if (editModal) {
        editModal.addEventListener("click", (e) => {
            if (e.target === editModal) closeGoalEditModal();
        });
    }

    const contribClose = document.getElementById("goal-contribute-close-btn");
    if (contribClose) contribClose.addEventListener("click", closeContributeModal);

    const contribCancel = document.getElementById("goal-contribute-cancel-btn");
    if (contribCancel) contribCancel.addEventListener("click", closeContributeModal);

    const contribForm = document.getElementById("goal-contribute-form");
    if (contribForm) contribForm.addEventListener("submit", handleContributeSubmit);

    const contribModal = document.getElementById("goal-contribute-modal");
    if (contribModal) {
        contribModal.addEventListener("click", (e) => {
            if (e.target === contribModal) closeContributeModal();
        });
    }

    document.addEventListener("keydown", (e) => {
        if (e.key !== "Escape") return;
        const contribOpen = document.getElementById("goal-contribute-modal")?.classList.contains("open");
        const editOpen = document.getElementById("goal-edit-modal")?.classList.contains("open");
        const goalsOpen = document.getElementById("goals-modal")?.classList.contains("open");
        if (contribOpen) closeContributeModal();
        else if (editOpen) closeGoalEditModal();
        else if (goalsOpen) closeGoalsModal();
    });
}

async function preloadGoalsForDashboard() {
    try {
        const list = await CashCareApi.getGoals();
        goalsState.items = Array.isArray(list) ? list : [];
        renderGoalsTeaser();
        updateSidebarCount();
    } catch (err) {
        console.warn("goals preload failed", err);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    bindGoalsModule();
});
