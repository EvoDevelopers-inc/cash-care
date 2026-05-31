const spontState = {
    items: [],
    pendingType: null,
    monthId: null,
    monthLabel: null
};

function bindSpontaneousModule() {
    const btnIncome = document.getElementById("spont-btn-income");
    const btnExpense = document.getElementById("spont-btn-expense");
    const form = document.getElementById("spont-form");
    const cancelBtn = document.getElementById("spont-cancel-btn");

    if (btnIncome) btnIncome.addEventListener("click", () => openSpontForm("INCOME"));
    if (btnExpense) btnExpense.addEventListener("click", () => openSpontForm("EXPENSE"));
    if (cancelBtn) cancelBtn.addEventListener("click", closeSpontForm);
    if (form) form.addEventListener("submit", handleSpontSubmit);
}

function formatSpontMonthLabel(currentMonth) {
    if (!currentMonth || !currentMonth.year || !currentMonth.month) {
        return "за текущий месяц";
    }
    const idx = Number(currentMonth.month) - 1;
    const RU_PREP = ["январе", "феврале", "марте", "апреле", "мае", "июне", "июле", "августе", "сентябре", "октябре", "ноябре", "декабре"];
    const prep = RU_PREP[idx] || "месяце";
    const cap = prep.charAt(0).toUpperCase() + prep.slice(1);
    return `за ${cap} ${currentMonth.year}`;
}

function openSpontForm(type) {
    if (!spontState.monthId) {
        const hint = spontState.monthLabel || "текущий месяц";
        showProfileAlert(`Сначала настрой бюджет на ${hint}`);
        return;
    }
    spontState.pendingType = type;
    const form = document.getElementById("spont-form");
    const label = document.getElementById("spont-form-label");
    const amount = document.getElementById("spont-amount");
    const note = document.getElementById("spont-note");
    if (!form) return;

    if (label) {
        label.textContent = type === "INCOME" ? "Спонтанный доход" : "Спонтанный расход";
        label.classList.toggle("text-emerald-700", type === "INCOME");
        label.classList.toggle("text-rose-700", type === "EXPENSE");
    }
    if (amount) amount.value = "";
    if (note) note.value = "";
    form.classList.remove("hidden");
    if (amount) amount.focus();
    scheduleSyncBudgetHealthHeights();
}

function closeSpontForm() {
    spontState.pendingType = null;
    const form = document.getElementById("spont-form");
    if (form) form.classList.add("hidden");
    scheduleSyncBudgetHealthHeights();
}

async function handleSpontSubmit(e) {
    e.preventDefault();
    if (!spontState.monthId || !spontState.pendingType) return;

    const amountEl = document.getElementById("spont-amount");
    const noteEl = document.getElementById("spont-note");
    const submitBtn = document.getElementById("spont-submit-btn");
    const amount = Math.round(Number(amountEl?.value || 0));
    if (!amount || amount <= 0) {
        showProfileAlert("Укажи сумму больше нуля");
        return;
    }

    if (submitBtn) submitBtn.disabled = true;
    try {
        await CashCareApi.createSpontaneous({
            monthlyFinancesId: spontState.monthId,
            type: spontState.pendingType,
            amount,
            note: noteEl?.value?.trim() || null
        });
        closeSpontForm();
        if (typeof reloadOverview === "function") {
            await reloadOverview();
        }
    } catch (err) {
        showProfileAlert(err?.message || "Не удалось сохранить операцию");
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

function resetBudgetHealthHeights() {
    const right = document.querySelector(".health-spont-column");
    const listBody = document.querySelector(".spontaneous-list-body");
    if (right) {
        right.style.height = "";
        right.style.minHeight = "";
    }
    if (listBody) {
        listBody.style.height = "";
        listBody.style.minHeight = "";
    }
}

function syncBudgetHealthHeights() {
    const left = document.querySelector(".budget-month-card");
    const right = document.querySelector(".health-spont-column");
    const listBody = document.querySelector(".spontaneous-list-body");
    if (!left || !right) return;

    if (window.innerWidth < 1024) {
        resetBudgetHealthHeights();
        return;
    }

    const leftH = left.offsetHeight;
    if (leftH <= 0) return;

    right.style.height = `${leftH}px`;
    right.style.minHeight = `${leftH}px`;

    if (!listBody) return;

    const rightRect = right.getBoundingClientRect();
    const listRect = listBody.getBoundingClientRect();
    const sectionPadBottom = 20;
    const listH = rightRect.bottom - listRect.top - sectionPadBottom;

    if (listH > 0) {
        const px = `${Math.floor(listH)}px`;
        listBody.style.height = px;
        listBody.style.minHeight = px;
    }
}

function scheduleSyncBudgetHealthHeights() {
    requestAnimationFrame(() => {
        syncBudgetHealthHeights();
        requestAnimationFrame(syncBudgetHealthHeights);
    });
}

function bindBudgetHealthResizeObserver() {
    const left = document.querySelector(".budget-month-card");
    if (!left || !window.ResizeObserver) return;
    const obs = new ResizeObserver(() => scheduleSyncBudgetHealthHeights());
    obs.observe(left);
}

function applySpontaneousFromOverview(overview) {
    spontState.monthId = overview?.currentMonth?.id ?? null;
    spontState.monthLabel = formatSpontMonthLabel(overview?.currentMonth);
    spontState.items = overview?.spontaneousTransactions || [];
    renderSpontaneousPanel(overview?.balance, overview?.currentMonth);
}

function renderSpontaneousPanel(balance, currentMonth) {
    const list = document.getElementById("spont-list");
    const empty = document.getElementById("spont-empty");
    const sumIncome = document.getElementById("spont-sum-income");
    const sumExpense = document.getElementById("spont-sum-expense");
    const monthLabelEl = document.getElementById("spont-month-label");
    const btnIncome = document.getElementById("spont-btn-income");
    const btnExpense = document.getElementById("spont-btn-expense");

    const monthText = formatSpontMonthLabel(currentMonth);
    if (monthLabelEl) monthLabelEl.textContent = monthText;

    const hasMonth = Boolean(spontState.monthId);
    if (btnIncome) btnIncome.disabled = !hasMonth;
    if (btnExpense) btnExpense.disabled = !hasMonth;

    const inc = Number(balance?.spontaneousIncome || 0);
    const exp = Number(balance?.spontaneousExpense || 0);

    if (sumIncome) sumIncome.textContent = `+${formatMoney(inc)}`;
    if (sumExpense) sumExpense.textContent = `−${formatMoney(exp)}`;

    const items = spontState.items || [];
    if (!list || !empty) return;

    if (!hasMonth) {
        empty.classList.remove("hidden");
        empty.textContent = `Нет бюджета ${monthText} — сначала настрой план месяца`;
        list.classList.add("hidden");
        list.innerHTML = "";
        closeSpontForm();
        scheduleSyncBudgetHealthHeights();
        return;
    }

    if (!items.length) {
        empty.classList.remove("hidden");
        empty.textContent = `${monthText.charAt(0).toUpperCase() + monthText.slice(1)} пока пусто — добавь кофе, чаевые или бонус`;
        list.classList.add("hidden");
        list.innerHTML = "";
        scheduleSyncBudgetHealthHeights();
        return;
    }

    empty.classList.add("hidden");
    list.classList.remove("hidden");
    list.innerHTML = items.map((t) => spontRowHtml(t)).join("");
    bindSpontaneousRowActions();
    refreshLucideSpontaneous();
    scheduleSyncBudgetHealthHeights();
}

function spontRowHtml(t) {
    const isIncome = t.type === "INCOME";
    const sign = isIncome ? "+" : "−";
    const cls = isIncome ? "spontaneous-row-income" : "spontaneous-row-expense";
    const icon = isIncome ? "arrow-down-left" : "arrow-up-right";
    const title = escapeHtml(t.note || t.typeLabel || (isIncome ? "Доход" : "Расход"));
    const when = escapeHtml(t.createdAtLabel || "");
    return `
        <li class="spontaneous-row ${cls}" data-id="${t.id}">
            <div class="spontaneous-row-main">
                <i data-lucide="${icon}" class="lc lc-xs"></i>
                <div class="min-w-0 flex-1">
                    <div class="truncate text-xs font-bold text-slate-800">${title}</div>
                    <div class="text-[10px] text-slate-500">${when}</div>
                </div>
                <div class="spontaneous-row-amount">${sign}${formatMoney(t.amount)}</div>
            </div>
            <button type="button" class="spontaneous-row-del" data-id="${t.id}" title="Удалить" aria-label="Удалить">
                <i data-lucide="x" class="lc lc-xs"></i>
            </button>
        </li>
    `;
}

function bindSpontaneousRowActions() {
    document.querySelectorAll(".spontaneous-row-del").forEach((btn) => {
        btn.addEventListener("click", async () => {
            const id = Number(btn.dataset.id);
            if (!id) return;
            try {
                await CashCareApi.deleteSpontaneous(id);
                if (typeof reloadOverview === "function") {
                    await reloadOverview();
                }
            } catch (err) {
                showProfileAlert(err?.message || "Не удалось удалить");
            }
        });
    });
}

function refreshLucideSpontaneous() {
    if (window.lucide && typeof window.lucide.createIcons === "function") {
        window.lucide.createIcons();
    }
}

document.addEventListener("DOMContentLoaded", () => {
    bindSpontaneousModule();
    bindBudgetHealthResizeObserver();
    scheduleSyncBudgetHealthHeights();
    window.addEventListener("resize", scheduleSyncBudgetHealthHeights);
});
