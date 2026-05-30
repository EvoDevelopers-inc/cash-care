const initModal = document.getElementById("init-modal");
const initModalCard = initModal?.querySelector(".modal");
const initForm = document.getElementById("init-form");
const initFormContent = document.getElementById("init-form-content");
const aiLoadingPanel = document.getElementById("ai-loading-panel");
const aiLoadingStep = document.getElementById("ai-loading-step");
const aiResultsPanel = document.getElementById("ai-results-panel");
const initBtn = document.getElementById("init-btn");
const initSkipBtn = document.getElementById("init-skip-btn");
const categoriesContainer = document.getElementById("categories-container");
const totalExpensesEl = document.getElementById("total-expenses");
const balanceEl = document.getElementById("balance-preview");
const statementInput = document.getElementById("statement-input");
const statementFileName = document.getElementById("statement-file-name");
const statementUploadStatus = document.getElementById("statement-upload-status");

const INIT_MODAL_DEFAULTS = {
    title: "Настрой бюджет на месяц",
    subtitle: "Укажи доход и план по категориям расходов"
};

let selectedStatementFile = null;
let initStep = "form";
let initSaved = false;

async function openInitModal() {
    if (!initModal) {
        return;
    }

    resetInitModalState();
    initModal.classList.add("open");
    initModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    hideAlert("init-alert");

    try {
        const setupData = await CashCareApi.getInitSetup();
        renderInitCategories(setupData.categories);
        updateInitPreview();
    } catch (error) {
        showAlert("init-alert", formatError(error), "error");
    }
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
    initStep = "form";
    initSaved = false;
    resetStatementUpload();
    resetInitModalHeader();

    if (initFormContent) {
        initFormContent.classList.remove("hidden");
    }
    if (aiLoadingPanel) {
        aiLoadingPanel.classList.add("hidden");
    }
    if (aiResultsPanel) {
        aiResultsPanel.classList.add("hidden");
        aiResultsPanel.innerHTML = "";
    }
    if (initModalCard) {
        initModalCard.classList.remove("modal-wide");
    }
    if (initBtn) {
        initBtn.textContent = "Начать пользоваться";
        initBtn.disabled = false;
    }
    if (initSkipBtn) {
        initSkipBtn.classList.add("hidden");
    }
}

function resetInitModalHeader() {
    const modalTitle = document.getElementById("init-modal-title");
    const modalSubtitle = document.querySelector(".modal-subtitle");

    if (modalTitle) {
        modalTitle.textContent = INIT_MODAL_DEFAULTS.title;
    }
    if (modalSubtitle) {
        modalSubtitle.textContent = INIT_MODAL_DEFAULTS.subtitle;
    }
}

function resetStatementUpload() {
    selectedStatementFile = null;
    if (statementInput) {
        statementInput.value = "";
    }
    if (statementFileName) {
        statementFileName.textContent = "Файл не выбран";
    }
    if (statementUploadStatus) {
        statementUploadStatus.textContent = "";
        statementUploadStatus.className = "upload-status";
    }
}

function renderInitCategories(categories) {
    categoriesContainer.innerHTML = categories.map((category) => `
        <div class="category-row" data-category-id="${category.id}">
            <div class="category-row-head">
                <label for="category-${category.id}">${category.nameCategory}</label>
                ${category.required ? '<span class="category-tag">обязательная</span>' : ""}
            </div>
            <input
                id="category-${category.id}"
                type="number"
                min="0"
                step="1000"
                placeholder="0"
                data-category-id="${category.id}"
                class="category-input"
                required
            >
        </div>
    `).join("");
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

    totalExpensesEl.textContent = formatMoney(totalExpenses);
    balanceEl.textContent = formatMoney(salary - totalExpenses);
    balanceEl.className = salary - totalExpenses >= 0 ? "preview-value positive" : "preview-value negative";
}

function isPdfFile(file) {
    if (!file) {
        return false;
    }

    const name = file.name?.toLowerCase() ?? "";
    return file.type === "application/pdf" || name.endsWith(".pdf");
}

function handleStatementFileChange(event) {
    const file = event.target.files?.[0];
    selectedStatementFile = file || null;

    if (!file) {
        statementFileName.textContent = "Файл не выбран";
        statementUploadStatus.textContent = "";
        statementUploadStatus.className = "upload-status";
        return;
    }

    if (!isPdfFile(file)) {
        selectedStatementFile = null;
        statementInput.value = "";
        statementFileName.textContent = "Файл не выбран";
        statementUploadStatus.textContent = "Нужен PDF-файл выписки";
        statementUploadStatus.className = "upload-status error";
        return;
    }

    statementFileName.textContent = file.name;
    statementUploadStatus.textContent = "После сохранения бюджета AI проанализирует выписку";
    statementUploadStatus.className = "upload-status pending";
}

function handleStatementDrop(event) {
    event.preventDefault();
    event.currentTarget.classList.remove("dragover");

    const file = event.dataTransfer?.files?.[0];
    if (!file) {
        return;
    }

    if (statementInput) {
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        statementInput.files = dataTransfer.files;
        statementInput.dispatchEvent(new Event("change"));
    }
}

function showAiLoadingStep(stepText) {
    if (initFormContent) {
        initFormContent.classList.add("hidden");
    }
    if (aiResultsPanel) {
        aiResultsPanel.classList.add("hidden");
    }
    if (aiLoadingPanel) {
        aiLoadingPanel.classList.remove("hidden");
    }
    if (aiLoadingStep) {
        aiLoadingStep.textContent = stepText;
    }
    if (initSkipBtn) {
        initSkipBtn.classList.remove("hidden");
    }

    const modalTitle = document.getElementById("init-modal-title");
    const modalSubtitle = document.querySelector(".modal-subtitle");
    if (modalTitle) {
        modalTitle.textContent = "AI разбирает выписку";
    }
    if (modalSubtitle) {
        modalSubtitle.textContent = "Обычно это занимает 15–40 секунд";
    }

    initStep = "loading";
    scrollInitModalToTop();
}

function hideAiLoadingStep() {
    if (aiLoadingPanel) {
        aiLoadingPanel.classList.add("hidden");
    }
    if (initSkipBtn) {
        initSkipBtn.classList.add("hidden");
    }
}

function showAiResultsStep(analysis) {
    hideAiLoadingStep();

    if (aiResultsPanel) {
        aiResultsPanel.classList.remove("hidden");
        renderAiAnalysis(aiResultsPanel, analysis);
    }
    if (initModalCard) {
        initModalCard.classList.add("modal-wide");
    }

    const modalTitle = document.getElementById("init-modal-title");
    const modalSubtitle = document.querySelector(".modal-subtitle");
    if (modalTitle) {
        modalTitle.textContent = "AI разобрал твою выписку";
    }
    if (modalSubtitle) {
        modalSubtitle.textContent = "Категории добавлены автоматически — поправить их можно позже в кабинете";
    }

    initStep = "ai-results";
    setLoading(initBtn, false, "Перейти в кабинет");
    scrollInitModalToTop();
}

function showAiErrorStep(error) {
    hideAiLoadingStep();

    if (initFormContent) {
        initFormContent.classList.remove("hidden");
    }

    resetInitModalHeader();
    showAlert("init-alert", formatError(error), "error");

    if (statementUploadStatus) {
        statementUploadStatus.textContent = "Не удалось проанализировать выписку. Можно выбрать другой PDF и повторить.";
        statementUploadStatus.className = "upload-status error";
    }

    initStep = "ai-error";
    setLoading(initBtn, false, selectedStatementFile ? "Повторить AI-анализ" : "Перейти в кабинет");
}

function scrollInitModalToTop() {
    if (initModalCard) {
        initModalCard.scrollTop = 0;
    }
}

function goToDashboard() {
    closeInitModal();
    window.location.href = "/dashboard";
}

async function runAiAnalysis() {
    if (!selectedStatementFile) {
        goToDashboard();
        return;
    }

    if (!isPdfFile(selectedStatementFile)) {
        showAlert("init-alert", "Выбери PDF-файл выписки", "error");
        return;
    }

    hideAlert("init-alert");
    showAiLoadingStep("Читаем PDF и отправляем транзакции в AI...");
    setLoading(initBtn, true, "AI анализирует...");

    try {
        const result = await CashCareApi.uploadStatement(selectedStatementFile);
        saveAiAnalysis(result.analysis);

        if (statementUploadStatus) {
            statementUploadStatus.textContent = "AI-анализ готов";
            statementUploadStatus.className = "upload-status success";
        }

        showAiResultsStep(result.analysis);
    } catch (error) {
        showAiErrorStep(error);
    }
}

async function handleInitSubmit(event) {
    event.preventDefault();
    hideAlert("init-alert");

    if (initStep === "ai-results" || (initStep === "ai-error" && !selectedStatementFile)) {
        goToDashboard();
        return;
    }

    if (initStep === "ai-error" && selectedStatementFile) {
        await runAiAnalysis();
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

        goToDashboard();
    } catch (error) {
        showAlert("init-alert", formatError(error), "error");
    } finally {
        if (initStep === "form") {
            setLoading(initBtn, false, "Начать пользоваться");
        }
    }
}

function bindInitModal() {
    if (!initForm) {
        return;
    }

    initForm.addEventListener("submit", handleInitSubmit);
    initForm.addEventListener("input", updateInitPreview);

    if (statementInput) {
        statementInput.addEventListener("change", handleStatementFileChange);
    }

    if (initSkipBtn) {
        initSkipBtn.addEventListener("click", () => {
            if (initSaved) {
                goToDashboard();
            }
        });
    }

    document.querySelectorAll(".file-drop").forEach((dropZone) => {
        dropZone.addEventListener("dragover", (event) => {
            event.preventDefault();
            dropZone.classList.add("dragover");
        });
        dropZone.addEventListener("dragleave", () => dropZone.classList.remove("dragover"));
        dropZone.addEventListener("drop", handleStatementDrop);
    });
}

document.addEventListener("DOMContentLoaded", bindInitModal);
