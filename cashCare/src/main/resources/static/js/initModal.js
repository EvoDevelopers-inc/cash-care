const initModal = document.getElementById("init-modal");
const initForm = document.getElementById("init-form");
const initBtn = document.getElementById("init-btn");
const initBackBtn = document.getElementById("init-back-btn");
const initSkipBtn = document.getElementById("init-skip-btn");
const initStepCounter = document.getElementById("init-step-counter");
const initTitle = document.getElementById("init-modal-title");
const initSubtitle = document.getElementById("init-modal-subtitle");

const stepPanels = {
    1: document.getElementById("init-step-1"),
    2: document.getElementById("init-step-2"),
    3: document.getElementById("init-step-3")
};
const stepDots = document.querySelectorAll(".step-dot");
const aiLoadingPanel = document.getElementById("ai-loading-panel");
const aiLoadingStep = document.getElementById("ai-loading-step");
const aiResultsPanel = document.getElementById("ai-results-panel");

const categoriesContainer = document.getElementById("categories-container");
const totalExpensesEl = document.getElementById("total-expenses");
const balanceEl = document.getElementById("balance-preview");
const statementInput = document.getElementById("statement-input");
const statementFileName = document.getElementById("statement-file-name");
const statementUploadStatus = document.getElementById("statement-upload-status");

const STEP_META = {
    1: { title: "Доход", subtitle: "Сколько денег у тебя есть в этом месяце" },
    2: { title: "Расходы", subtitle: "План расходов по категориям — необязательно заполнять всё" },
    3: { title: "AI-анализ выписки", subtitle: "Опционально — но это сразу даст красивый профиль" }
};

let selectedStatementFile = null;
let initStep = 1;
let initSaved = false;
let reuploadOnlyMode = false;

async function openInitModal() {
    if (!initModal) {
        return;
    }

    reuploadOnlyMode = false;
    resetInitModalState();
    initModal.classList.add("open");
    initModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    hideAlert("init-alert");

    try {
        const setupData = await CashCareApi.getInitSetup();
        renderInitCategories(setupData.categories || []);
        updateInitPreview();
    } catch (error) {
        showAlert("init-alert", formatError(error), "error");
    }
}

async function openReuploadModal() {
    if (!initModal) return;

    reuploadOnlyMode = true;
    selectedStatementFile = null;

    if (statementInput) statementInput.value = "";
    if (statementFileName) statementFileName.textContent = "Файл не выбран";
    if (statementUploadStatus) {
        statementUploadStatus.textContent = "";
        statementUploadStatus.className = "text-xs text-slate-500";
    }
    if (aiLoadingPanel) aiLoadingPanel.classList.add("hidden");
    if (aiResultsPanel) {
        aiResultsPanel.classList.add("hidden");
        aiResultsPanel.innerHTML = "";
    }
    const card = document.getElementById("init-modal-card");
    if (card) card.classList.remove("max-w-[760px]");

    Object.entries(stepPanels).forEach(([key, panel]) => {
        if (!panel) return;
        panel.classList.toggle("hidden", key !== "3");
    });
    initStep = 3;

    initModal.classList.add("open");
    initModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    hideAlert("init-alert");

    if (initStepCounter) initStepCounter.textContent = "Обновление AI";
    stepDots.forEach((d) => {
        d.classList.remove("active", "done");
        if (Number(d.dataset.step) === 3) d.classList.add("active");
        else d.classList.add("done");
    });
    if (initTitle) initTitle.textContent = "Загрузить новую выписку";
    if (initSubtitle) initSubtitle.textContent = "AI пересчитает категории и профиль на основе свежей выписки";

    updateNavButtons();
}

function closeInitModal() {
    if (!initModal) {
        return;
    }
    initModal.classList.remove("open");
    initModal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
}

function resetInitModalState() {
    initStep = 1;
    initSaved = false;
    selectedStatementFile = null;
    if (statementInput) statementInput.value = "";
    if (statementFileName) statementFileName.textContent = "Файл не выбран";
    if (statementUploadStatus) {
        statementUploadStatus.textContent = "";
        statementUploadStatus.className = "text-xs text-slate-500";
    }
    if (aiLoadingPanel) aiLoadingPanel.classList.add("hidden");
    if (aiResultsPanel) {
        aiResultsPanel.classList.add("hidden");
        aiResultsPanel.innerHTML = "";
    }
    const card = document.getElementById("init-modal-card");
    if (card) card.classList.remove("max-w-[760px]");
    showStep(1);
}

function showStep(step) {
    initStep = step;

    Object.entries(stepPanels).forEach(([key, panel]) => {
        if (!panel) return;
        panel.classList.toggle("hidden", String(key) !== String(step));
    });

    if (aiLoadingPanel) aiLoadingPanel.classList.add("hidden");
    if (aiResultsPanel) aiResultsPanel.classList.add("hidden");

    stepDots.forEach((dot) => {
        const dotStep = Number(dot.dataset.step);
        dot.classList.remove("active", "done");
        if (dotStep < step) dot.classList.add("done");
        else if (dotStep === step) dot.classList.add("active");
    });

    const meta = STEP_META[step];
    if (meta) {
        if (initTitle) initTitle.textContent = meta.title;
        if (initSubtitle) initSubtitle.textContent = meta.subtitle;
    }
    if (initStepCounter) {
        initStepCounter.textContent = `Шаг ${step} из 3`;
    }

    updateNavButtons();
}

function updateNavButtons() {
    if (!initBackBtn || !initBtn || !initSkipBtn) return;

    initBackBtn.classList.remove("invisible");
    if (initStep === 1 || reuploadOnlyMode) initBackBtn.classList.add("invisible");

    initSkipBtn.classList.add("hidden");

    if (initStep === 1 || initStep === 2) {
        setLoading(initBtn, false, "Далее →");
    } else if (initStep === 3) {
        let label;
        if (reuploadOnlyMode) {
            label = selectedStatementFile ? "Проанализировать" : "Закрыть";
        } else {
            label = selectedStatementFile ? "Сохранить и проанализировать" : "Сохранить и закрыть";
        }
        setLoading(initBtn, false, label);
    }
}

function showAiLoadingStepUi(text) {
    Object.values(stepPanels).forEach((panel) => panel?.classList.add("hidden"));
    if (aiResultsPanel) aiResultsPanel.classList.add("hidden");
    if (aiLoadingPanel) aiLoadingPanel.classList.remove("hidden");
    if (aiLoadingStep && text) aiLoadingStep.textContent = text;

    if (initTitle) initTitle.textContent = "AI работает";
    if (initSubtitle) initSubtitle.textContent = "Подожди немного — обычно 5–25 секунд";

    initSkipBtn.classList.remove("hidden");
    initSkipBtn.textContent = "Пропустить AI →";
    initBackBtn.classList.add("invisible");
    setLoading(initBtn, true, "AI анализирует...");
}

function showAiResultsUi(analysis) {
    Object.values(stepPanels).forEach((panel) => panel?.classList.add("hidden"));
    if (aiLoadingPanel) aiLoadingPanel.classList.add("hidden");
    if (aiResultsPanel) {
        aiResultsPanel.classList.remove("hidden");
        renderAiAnalysis(aiResultsPanel, analysis);
    }

    const card = document.getElementById("init-modal-card");
    if (card) card.classList.add("max-w-[760px]");

    if (initTitle) initTitle.textContent = "AI разобрал твою выписку";
    if (initSubtitle) initSubtitle.textContent = "Категории добавлены — подправить можно в кабинете";
    if (initStepCounter) initStepCounter.textContent = "Готово";

    stepDots.forEach((d) => d.classList.add("done"));

    initBackBtn.classList.add("invisible");
    initSkipBtn.classList.add("hidden");
    setLoading(initBtn, false, "Перейти в кабинет →");
    initStep = "ai-results";
}

function renderInitCategories(categories) {
    if (!categoriesContainer) return;

    categoriesContainer.innerHTML = (categories || []).map((category) => `
        <div class="init-cat-row">
            <div>
                <div class="init-cat-name">${category.nameCategory}
                    ${category.required ? '<span class="init-cat-required">обяз.</span>' : ''}
                </div>
            </div>
            <input
                type="number"
                min="0"
                step="1000"
                placeholder="0"
                data-category-id="${category.id}"
                class="init-input category-input"
            >
        </div>
    `).join("");

    if (!categories || categories.length === 0) {
        categoriesContainer.innerHTML = '<p class="text-xs text-slate-500">Нет категорий — пропусти этот шаг.</p>';
    }
}

function collectInitPayload() {
    const salary = Number(document.getElementById("salary-input").value);
    const others = document.getElementById("others-input").value.trim();
    const categories = Array.from(document.querySelectorAll(".category-input")).map((input) => ({
        categoryId: Number(input.dataset.categoryId),
        plannedAmount: Number(input.value || 0)
    }));

    return {
        salary,
        others: others || null,
        categories
    };
}

function updateInitPreview() {
    const salary = Number(document.getElementById("salary-input").value || 0);
    const totalExpenses = Array.from(document.querySelectorAll(".category-input"))
        .reduce((sum, input) => sum + Number(input.value || 0), 0);

    if (totalExpensesEl) totalExpensesEl.textContent = formatMoney(totalExpenses);
    if (balanceEl) {
        balanceEl.textContent = formatMoney(salary - totalExpenses);
        balanceEl.classList.toggle("text-rose-600", salary - totalExpenses < 0);
        balanceEl.classList.toggle("text-emerald-900", salary - totalExpenses >= 0);
    }
}

function isPdfFile(file) {
    if (!file) return false;
    const name = file.name?.toLowerCase() ?? "";
    return file.type === "application/pdf" || name.endsWith(".pdf");
}

function handleStatementFileChange(event) {
    const file = event.target.files?.[0];
    selectedStatementFile = file || null;

    if (!file) {
        statementFileName.textContent = "Файл не выбран";
        statementUploadStatus.textContent = "";
        statementUploadStatus.className = "text-xs text-slate-500";
    } else if (!isPdfFile(file)) {
        selectedStatementFile = null;
        statementInput.value = "";
        statementFileName.textContent = "Файл не выбран";
        statementUploadStatus.textContent = "Нужен PDF-файл выписки";
        statementUploadStatus.className = "text-xs text-rose-600 font-semibold";
    } else {
        statementFileName.textContent = file.name;
        statementUploadStatus.textContent = "Готово к анализу";
        statementUploadStatus.className = "text-xs text-emerald-700 font-semibold";
    }

    if (initStep === 3) updateNavButtons();
}

function handleStatementDrop(event) {
    event.preventDefault();
    event.currentTarget.classList.remove("dragover");

    const file = event.dataTransfer?.files?.[0];
    if (!file || !statementInput) return;

    const dt = new DataTransfer();
    dt.items.add(file);
    statementInput.files = dt.files;
    statementInput.dispatchEvent(new Event("change"));
}

async function runAiAnalysis() {
    if (!selectedStatementFile) {
        finishAndGoToDashboard();
        return;
    }
    if (!isPdfFile(selectedStatementFile)) {
        showAlert("init-alert", "Выбери PDF-файл выписки", "error");
        return;
    }

    hideAlert("init-alert");
    showAiLoadingStepUi("Читаем PDF и шлём транзакции в нейросеть...");

    const progressTimer = setTimeout(() => {
        if (aiLoadingStep) aiLoadingStep.textContent = "Нейросеть категоризирует транзакции...";
    }, 8000);
    const progressTimer2 = setTimeout(() => {
        if (aiLoadingStep) aiLoadingStep.textContent = "Почти готово, формируем профиль...";
    }, 25000);

    try {
        const result = await CashCareApi.uploadStatement(selectedStatementFile);
        clearTimeout(progressTimer);
        clearTimeout(progressTimer2);

        saveAiAnalysis(result.analysis);

        if (reuploadOnlyMode) {
            closeInitModal();
            if (typeof reloadOverview === "function") {
                await reloadOverview();
            } else {
                window.location.reload();
            }
            return;
        }

        showAiResultsUi(result.analysis);
    } catch (error) {
        clearTimeout(progressTimer);
        clearTimeout(progressTimer2);

        if (reuploadOnlyMode) {
            Object.entries(stepPanels).forEach(([key, panel]) => {
                if (!panel) return;
                panel.classList.toggle("hidden", key !== "3");
            });
            if (aiLoadingPanel) aiLoadingPanel.classList.add("hidden");
            initStep = 3;
            updateNavButtons();
        } else {
            showStep(3);
            setLoading(initBtn, false, selectedStatementFile ? "Повторить AI-анализ" : "Сохранить и закрыть");
        }
        showAlert("init-alert", formatError(error), "error");
        if (statementUploadStatus) {
            statementUploadStatus.textContent = "Ошибка анализа — попробуй ещё раз или закрой модалку";
            statementUploadStatus.className = "text-xs text-rose-600 font-semibold";
        }
    }
}

function finishAndGoToDashboard() {
    closeInitModal();
    if (window.location.pathname !== "/dashboard") {
        window.location.href = "/dashboard";
    } else {
        window.location.reload();
    }
}

async function handleInitSubmit(event) {
    event.preventDefault();
    hideAlert("init-alert");

    if (initStep === "ai-results") {
        finishAndGoToDashboard();
        return;
    }

    if (initStep === 1) {
        const salary = Number(document.getElementById("salary-input").value);
        if (!salary || salary <= 0) {
            showAlert("init-alert", "Укажи зарплату/доход", "error");
            return;
        }
        showStep(2);
        return;
    }

    if (initStep === 2) {
        showStep(3);
        return;
    }

    if (initStep === 3) {
        if (reuploadOnlyMode) {
            if (!selectedStatementFile) {
                closeInitModal();
                return;
            }
            try {
                await runAiAnalysis();
            } catch (error) {
                showAlert("init-alert", formatError(error), "error");
                updateNavButtons();
            }
            return;
        }

        setLoading(initBtn, true, "Сохраняем бюджет...");
        try {
            if (!initSaved) {
                await CashCareApi.submitInit(collectInitPayload());
                initSaved = true;
            }

            if (selectedStatementFile) {
                await runAiAnalysis();
                return;
            }

            finishAndGoToDashboard();
        } catch (error) {
            showAlert("init-alert", formatError(error), "error");
            updateNavButtons();
        }
    }
}

function handleBack() {
    if (initStep === 2) showStep(1);
    else if (initStep === 3) showStep(2);
}

function handleSkip() {
    if (initSaved) {
        finishAndGoToDashboard();
    }
}

function bindInitModal() {
    if (!initForm) return;

    initForm.addEventListener("submit", handleInitSubmit);
    initForm.addEventListener("input", updateInitPreview);

    if (initBackBtn) initBackBtn.addEventListener("click", handleBack);
    if (initSkipBtn) initSkipBtn.addEventListener("click", handleSkip);

    if (statementInput) {
        statementInput.addEventListener("change", handleStatementFileChange);
    }

    document.querySelectorAll(".file-drop-zone").forEach((zone) => {
        zone.addEventListener("dragover", (e) => {
            e.preventDefault();
            zone.classList.add("dragover");
        });
        zone.addEventListener("dragleave", () => zone.classList.remove("dragover"));
        zone.addEventListener("drop", handleStatementDrop);
    });
}

document.addEventListener("DOMContentLoaded", bindInitModal);
