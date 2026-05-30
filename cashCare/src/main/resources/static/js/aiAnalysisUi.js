const AI_ANALYSIS_KEY = "cashcare.aiAnalysis.v2";

const PERSONALITY_THEME = {
    "Контролер":   { emoji: "🎯", grad: "from-emerald-400 to-cyan-400", chip: "bg-emerald-100 text-emerald-700" },
    "Расточитель": { emoji: "💸", grad: "from-rose-400 to-orange-400",   chip: "bg-rose-100 text-rose-700"     },
    "Пофигист":    { emoji: "🌫️", grad: "from-slate-400 to-slate-500",   chip: "bg-slate-100 text-slate-700"   },
    "Достигатор":  { emoji: "🚀", grad: "from-violet-400 to-fuchsia-400", chip: "bg-violet-100 text-violet-700" }
};

function aiField(obj, camelKey, snakeKey) {
    if (!obj) return undefined;
    return obj[camelKey] ?? obj[snakeKey];
}

function normalizeAiAnalysis(analysis) {
    if (!analysis) return null;

    if (analysis.personality !== undefined && Array.isArray(analysis.categories)) {
        return analysis;
    }

    const financialProfile = analysis.financial_profile ?? analysis.financialProfile;
    const totals = analysis.totals ?? {};
    const period = analysis.period ?? {};

    return {
        personality: aiField(financialProfile, "personalityType", "personality_type") ?? "Не определён",
        reasoning: financialProfile?.reasoning ?? "AI не смог сформулировать профиль.",
        income: totals.totalIncome ?? totals.total_income ?? 0,
        expense: totals.totalExpense ?? totals.total_expense ?? 0,
        currency: totals.currency ?? "RUB",
        periodStart: aiField(period, "startDate", "start_date") ?? "—",
        periodEnd: aiField(period, "endDate", "end_date") ?? "—",
        categories: (analysis.categories ?? []).map((c) => ({
            categoryName: aiField(c, "categoryName", "category_name") ?? "Без названия",
            amount: c.amount ?? 0,
            percentage: c.percentage ?? 0
        })),
        subscriptions: (analysis.detected_subscriptions ?? analysis.detectedSubscriptions ?? []).map((it) => ({
            serviceName: aiField(it, "serviceName", "service_name") ?? "Сервис",
            estimatedMonthlyAmount: it.estimatedMonthlyAmount ?? it.estimated_monthly_amount ?? 0
        })),
        suggestedNew: (analysis.suggested_new_categories ?? analysis.suggestedNewCategories ?? []).map((it) => ({
            categoryName: aiField(it, "categoryName", "category_name") ?? "Без названия",
            amount: it.amount ?? 0,
            percentage: it.percentage ?? 0,
            reason: it.reason ?? ""
        })),
        insights: analysis.insights ?? []
    };
}

function saveAiAnalysis(analysis) {
    const normalized = normalizeAiAnalysis(analysis);
    if (!normalized) return;
    sessionStorage.setItem(AI_ANALYSIS_KEY, JSON.stringify(normalized));
}

function getStoredAiAnalysis() {
    try {
        const raw = sessionStorage.getItem(AI_ANALYSIS_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch (_) {
        return null;
    }
}

function clearStoredAiAnalysis() {
    sessionStorage.removeItem(AI_ANALYSIS_KEY);
    sessionStorage.removeItem("cashcare.aiAnalysis");
}

function buildHeroBlock(meta) {
    const theme = PERSONALITY_THEME[meta.personality] || PERSONALITY_THEME["Пофигист"];
    return `
        <div class="rounded-2xl bg-gradient-to-br ${theme.grad} p-5 text-white shadow-lg">
            <div class="mb-3 flex items-center justify-between">
                <span class="inline-flex items-center gap-1.5 rounded-full bg-white/25 px-3 py-1 text-[10px] font-bold uppercase tracking-wider backdrop-blur-sm">
                    <span class="h-1.5 w-1.5 rounded-full bg-white"></span>
                    AI-профиль
                </span>
                <span class="text-xs opacity-80">${meta.periodStart} — ${meta.periodEnd}</span>
            </div>
            <div class="flex items-center gap-3">
                <div class="text-4xl">${theme.emoji}</div>
                <div>
                    <div class="text-2xl font-extrabold leading-tight">${meta.personality}</div>
                    <div class="text-xs opacity-90">${meta.currency} · профиль трат</div>
                </div>
            </div>
            <p class="mt-3 text-sm leading-relaxed opacity-95">${meta.reasoning}</p>
        </div>
    `;
}

function buildStatsBlock(meta) {
    return `
        <div class="grid grid-cols-2 gap-3">
            <div class="rounded-2xl border border-emerald-200/60 bg-emerald-50/70 p-4">
                <div class="text-[10px] font-bold uppercase tracking-wider text-emerald-700">Доход по выписке</div>
                <div class="mt-1 text-xl font-extrabold text-emerald-900">${formatMoney(meta.income)}</div>
            </div>
            <div class="rounded-2xl border border-rose-200/60 bg-rose-50/70 p-4">
                <div class="text-[10px] font-bold uppercase tracking-wider text-rose-700">Расход по выписке</div>
                <div class="mt-1 text-xl font-extrabold text-rose-900">${formatMoney(meta.expense)}</div>
            </div>
        </div>
    `;
}

function buildCategoriesBlock(meta) {
    if (!meta.categories.length) {
        return '<p class="text-sm text-slate-500">Категории не найдены</p>';
    }

    const total = meta.expense || meta.categories.reduce((s, c) => s + c.amount, 0) || 1;
    const sorted = [...meta.categories].sort((a, b) => b.amount - a.amount);

    return `
        <div class="space-y-2.5">
            ${sorted.map((c) => {
                const pct = Math.min(Math.round((c.amount / total) * 100), 100);
                return `
                    <div class="rounded-xl border border-slate-200/70 bg-white p-3">
                        <div class="mb-2 flex items-center justify-between gap-2">
                            <span class="text-sm font-semibold text-slate-800 truncate">${c.categoryName}</span>
                            <div class="flex items-baseline gap-2 whitespace-nowrap">
                                <span class="text-sm font-extrabold text-rose-600">−${formatMoney(c.amount)}</span>
                                <span class="text-[11px] font-semibold text-slate-400">${pct}%</span>
                            </div>
                        </div>
                        <div class="h-1.5 overflow-hidden rounded-full bg-slate-100">
                            <div class="h-full rounded-full bg-gradient-to-r from-emerald-400 to-cyan-400" style="width: ${pct}%"></div>
                        </div>
                    </div>
                `;
            }).join("")}
        </div>
    `;
}

function buildSuggestedBlock(meta) {
    if (!meta.suggestedNew.length) return "";

    return `
        <div>
            <div class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">Новые категории от AI</div>
            <div class="space-y-2">
                ${meta.suggestedNew.map((item) => `
                    <div class="rounded-xl border border-cyan-200/60 bg-cyan-50/60 p-3">
                        <div class="flex items-center justify-between gap-2">
                            <div class="flex items-center gap-2">
                                <span class="text-sm font-bold text-slate-800">${item.categoryName}</span>
                                <span class="rounded-full bg-cyan-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-cyan-700">новая</span>
                            </div>
                            ${item.amount > 0 ? `<span class="text-sm font-extrabold text-rose-600">−${formatMoney(item.amount)}</span>` : ""}
                        </div>
                        ${item.reason ? `<p class="mt-1.5 text-xs leading-relaxed text-slate-600">${item.reason}</p>` : ""}
                    </div>
                `).join("")}
            </div>
        </div>
    `;
}

function buildSubscriptionsBlock(meta) {
    const total = meta.subscriptions.reduce((s, x) => s + x.estimatedMonthlyAmount, 0);
    return `
        <div>
            <div class="mb-2 flex items-baseline justify-between">
                <span class="text-xs font-bold uppercase tracking-wider text-slate-500">Подписки</span>
                ${meta.subscriptions.length ? `<span class="text-xs font-bold text-violet-700">${formatMoney(total)}/мес</span>` : ""}
            </div>
            ${meta.subscriptions.length ? `
                <div class="flex flex-wrap gap-2">
                    ${meta.subscriptions.map((s) => `
                        <div class="inline-flex items-center gap-2 rounded-full border border-violet-200/60 bg-violet-50/70 px-3 py-1.5 text-xs">
                            <span class="font-bold text-violet-800">${s.serviceName}</span>
                            <span class="font-semibold text-violet-600">${formatMoney(s.estimatedMonthlyAmount)}/мес</span>
                        </div>
                    `).join("")}
                </div>
            ` : '<p class="text-sm text-slate-500">Подписки не найдены</p>'}
        </div>
    `;
}

function buildInsightsBlock(meta) {
    if (!meta.insights.length) return "";
    return `
        <div>
            <div class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">Советы AI</div>
            <div class="space-y-2">
                ${meta.insights.map((tip, i) => `
                    <div class="flex gap-3 rounded-xl bg-amber-50/60 p-3">
                        <div class="grid h-6 w-6 flex-shrink-0 place-items-center rounded-full bg-amber-200 text-xs font-extrabold text-amber-900">${i + 1}</div>
                        <p class="text-sm leading-relaxed text-slate-700">${tip}</p>
                    </div>
                `).join("")}
            </div>
        </div>
    `;
}

function buildCompactBlock(meta) {
    return `
        <div class="mt-3 flex flex-wrap gap-2">
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">${meta.categories.length} категорий</span>
            <span class="rounded-full bg-violet-100 px-3 py-1 text-xs font-semibold text-violet-700">${meta.subscriptions.length} подписок</span>
            <span class="rounded-full bg-cyan-100 px-3 py-1 text-xs font-semibold text-cyan-700">${meta.suggestedNew.length} новых</span>
        </div>
    `;
}

function renderAiAnalysis(container, analysis, options = {}) {
    if (!container || !analysis) return;

    const meta = normalizeAiAnalysis(analysis);
    if (!meta) return;

    const compact = options.compact === true;

    const body = compact
        ? `
            ${buildHeroBlock(meta)}
            <div class="mt-3">${buildStatsBlock(meta)}</div>
            ${buildCompactBlock(meta)}
        `
        : `
            ${buildHeroBlock(meta)}
            ${buildStatsBlock(meta)}
            ${buildSuggestedBlock(meta)}
            <div>
                <div class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">Категории расходов</div>
                ${buildCategoriesBlock(meta)}
            </div>
            ${buildSubscriptionsBlock(meta)}
            ${buildInsightsBlock(meta)}
        `;

    container.innerHTML = `<div class="space-y-4 text-left">${body}</div>`;
}

function renderDashboardAiSummary() {
    const card = document.getElementById("ai-summary-card");
    const container = document.getElementById("ai-summary-content");
    const dismissBtn = document.getElementById("ai-summary-dismiss");
    const analysis = getStoredAiAnalysis();

    if (!card || !container || !analysis) return;

    renderAiAnalysis(container, analysis, { compact: true });
    card.classList.remove("hidden");

    if (dismissBtn && !dismissBtn.dataset.bound) {
        dismissBtn.dataset.bound = "true";
        dismissBtn.addEventListener("click", () => {
            clearStoredAiAnalysis();
            card.classList.add("hidden");
            container.innerHTML = "";
        });
    }
}
