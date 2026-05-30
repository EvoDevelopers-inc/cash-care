const AI_ANALYSIS_KEY = "cashcare.aiAnalysis.v2";

function aiField(obj, camelKey, snakeKey) {
    if (!obj) {
        return undefined;
    }
    return obj[camelKey] ?? obj[snakeKey];
}

function normalizeAiAnalysis(analysis) {
    if (!analysis) {
        return null;
    }

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
        categories: (analysis.categories ?? []).map((category) => ({
            categoryName: aiField(category, "categoryName", "category_name") ?? "Без названия",
            amount: category.amount ?? 0,
            percentage: category.percentage ?? 0
        })),
        subscriptions: (analysis.detected_subscriptions ?? analysis.detectedSubscriptions ?? []).map((item) => ({
            serviceName: aiField(item, "serviceName", "service_name") ?? "Сервис",
            estimatedMonthlyAmount: item.estimatedMonthlyAmount ?? item.estimated_monthly_amount ?? 0
        })),
        suggestedNew: (analysis.suggested_new_categories ?? analysis.suggestedNewCategories ?? []).map((item) => ({
            categoryName: aiField(item, "categoryName", "category_name") ?? "Без названия",
            amount: item.amount ?? 0,
            percentage: item.percentage ?? 0,
            reason: item.reason ?? ""
        })),
        insights: analysis.insights ?? []
    };
}

function saveAiAnalysis(analysis) {
    const normalized = normalizeAiAnalysis(analysis);
    if (!normalized) {
        return;
    }
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

function renderAiAnalysis(container, analysis, options = {}) {
    if (!container || !analysis) {
        return;
    }

    const meta = normalizeAiAnalysis(analysis);
    if (!meta) {
        return;
    }

    const compact = options.compact === true;
    const categoriesHtml = meta.categories.map((category) => `
        <div class="ai-category-row">
            <div class="ai-category-head">
                <span>${category.categoryName}</span>
                <strong>${formatMoney(category.amount)}</strong>
            </div>
            <div class="ai-progress">
                <div class="ai-progress-bar" style="width: ${Math.min(category.percentage, 100)}%"></div>
            </div>
            <div class="ai-category-meta">${Math.round(category.percentage)}% от расходов</div>
        </div>
    `).join("");

    const suggestedHtml = meta.suggestedNew.length
        ? meta.suggestedNew.map((item) => `
            <div class="ai-suggested-row">
                <div class="ai-category-head">
                    <span>${item.categoryName} <span class="category-tag">новая</span></span>
                    <strong>${formatMoney(item.amount)}</strong>
                </div>
                <p class="ai-suggested-reason">${item.reason || "AI выделил отдельную категорию по паттерну трат."}</p>
            </div>
        `).join("")
        : "";

    const subscriptionsHtml = meta.subscriptions.length
        ? meta.subscriptions.map((item) => `
            <div class="ai-chip">${item.serviceName}: ${formatMoney(item.estimatedMonthlyAmount)}/мес</div>
        `).join("")
        : `<div class="ai-empty">Подписки не найдены</div>`;

    const insightsHtml = meta.insights.length
        ? meta.insights.map((insight) => `<li>${insight}</li>`).join("")
        : `<li>AI не вернул рекомендации</li>`;

    const detailsHtml = compact
        ? `
            <div class="ai-compact-meta">
                <span>${meta.categories.length} категорий</span>
                <span>${meta.subscriptions.length} подписок</span>
                <span>${meta.suggestedNew.length} новых</span>
            </div>
        `
        : `
            ${suggestedHtml ? `
            <div class="init-section">
                <h3 class="section-title">Новые категории от AI</h3>
                <div class="ai-suggested-list">${suggestedHtml}</div>
            </div>` : ""}

            <div class="init-section">
                <h3 class="section-title">Категории расходов</h3>
                <div class="ai-category-list">${categoriesHtml || '<div class="ai-empty">Категории не найдены</div>'}</div>
            </div>

            <div class="init-section">
                <h3 class="section-title">Подписки</h3>
                <div class="ai-chip-list">${subscriptionsHtml}</div>
            </div>

            <div class="init-section">
                <h3 class="section-title">Советы AI</h3>
                <ul class="ai-insights">${insightsHtml}</ul>
            </div>
        `;

    container.innerHTML = `
        <div class="ai-hero">
            <div class="badge badge-auth">AI-профиль</div>
            <h3 class="ai-personality">${meta.personality}</h3>
            <p class="ai-reason">${meta.reasoning}</p>
            <p class="ai-period">Период выписки: ${meta.periodStart} — ${meta.periodEnd} · ${meta.currency}</p>
        </div>

        <div class="ai-stats-grid">
            <div class="stat-card">
                <div class="stat-label">Доход по выписке</div>
                <div class="stat-value positive-value">${formatMoney(meta.income)}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Расход по выписке</div>
                <div class="stat-value negative-value">${formatMoney(meta.expense)}</div>
            </div>
        </div>

        ${detailsHtml}
    `;
}

function renderDashboardAiSummary() {
    const card = document.getElementById("ai-summary-card");
    const container = document.getElementById("ai-summary-content");
    const dismissBtn = document.getElementById("ai-summary-dismiss");
    const analysis = getStoredAiAnalysis();

    if (!card || !container || !analysis) {
        return;
    }

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
