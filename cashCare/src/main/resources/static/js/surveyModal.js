const SURVEY_QUESTIONS = [
    {
        key: "maritalStatus",
        title: "Ты сейчас в браке?",
        subtitle: "AI учитывает, на скольких людей рассчитан бюджет",
        layout: "list",
        options: [
            { value: "SINGLE",     icon: "user",          title: "Холост / не замужем" },
            { value: "MARRIED",    icon: "gem",           title: "В браке" },
            { value: "COHABITING", icon: "users",         title: "Живу с партнёром" },
            { value: "DIVORCED",   icon: "user-minus",    title: "Разведён(а)" }
        ]
    },
    {
        key: "childrenCount",
        title: "Сколько у тебя детей?",
        subtitle: "Дети сильно меняют расходы — AI это учтёт",
        layout: "grid",
        valueType: "number",
        options: [
            { value: 0, icon: "ban",         title: "Нет" },
            { value: 1, icon: "baby",        title: "1" },
            { value: 2, icon: "users",       title: "2" },
            { value: 3, icon: "users-round", title: "3" },
            { value: 4, icon: "users",       title: "4+" }
        ]
    },
    {
        key: "employmentType",
        title: "Кем работаешь?",
        subtitle: "От стабильности дохода зависит размер подушки",
        layout: "list",
        options: [
            { value: "EMPLOYED",      icon: "building-2",     title: "Наёмный сотрудник", hint: "стабильная зарплата" },
            { value: "SELF_EMPLOYED", icon: "monitor",        title: "Самозанятый / фрилансер", hint: "доход скачет" },
            { value: "BUSINESS",      icon: "chart-bar",      title: "Свой бизнес", hint: "доход скачет, нужны резервы" },
            { value: "STUDENT",       icon: "graduation-cap", title: "Студент" },
            { value: "UNEMPLOYED",    icon: "search",         title: "Сейчас без работы" }
        ]
    },
    {
        key: "housingStatus",
        title: "Где живёшь?",
        subtitle: "Аренда и ипотека — самые тяжёлые статьи",
        layout: "list",
        options: [
            { value: "OWN",          icon: "home",      title: "Своё жильё", hint: "без ипотеки" },
            { value: "MORTGAGE",     icon: "landmark",  title: "Своё, в ипотеке" },
            { value: "RENT",         icon: "key-round", title: "Снимаю" },
            { value: "WITH_PARENTS", icon: "users",     title: "С родителями", hint: "не плачу за жильё" }
        ]
    },
    {
        key: "hasDebts",
        title: "Есть кредиты или долги?",
        subtitle: "Не считая ипотеку (если она есть)",
        layout: "list",
        valueType: "boolean",
        options: [
            { value: true,  icon: "triangle-alert", title: "Да", hint: "кредитка, потребкредит, рассрочка..." },
            { value: false, icon: "check-check",    title: "Нет", hint: "ничего не должен" }
        ]
    },
    {
        key: "financialGoal",
        title: "Главная финансовая цель?",
        subtitle: "AI будет советовать, исходя из неё",
        layout: "list",
        options: [
            { value: "SAVE_CUSHION", icon: "life-buoy",    title: "Накопить подушку безопасности", hint: "3–6 зарплат на чёрный день" },
            { value: "PAY_DEBT",     icon: "unlock",       title: "Закрыть долги", hint: "выйти из минуса" },
            { value: "BIG_PURCHASE", icon: "car",          title: "Крупная покупка", hint: "авто, техника, отпуск" },
            { value: "BUY_PROPERTY", icon: "house-plus",   title: "Копить на жильё" },
            { value: "INVEST",       icon: "trending-up",  title: "Инвестировать" },
            { value: "JUST_LIVE",    icon: "smile",        title: "Жить комфортно без долгов" }
        ]
    },
    {
        key: "citySize",
        title: "Какой у тебя город?",
        subtitle: "Стоимость жизни в Москве и в посёлке — разная",
        layout: "list",
        options: [
            { value: "CAPITAL", icon: "building-2", title: "Москва / Санкт-Петербург" },
            { value: "MILLION", icon: "building",   title: "Город-миллионник", hint: "Казань, Новосиб, Ека..." },
            { value: "LARGE",   icon: "house-plus", title: "Крупный город", hint: "от 300 тыс. жителей" },
            { value: "SMALL",   icon: "home",       title: "Малый город / посёлок" }
        ]
    },
    {
        key: "spendingStyle",
        title: "Как ты тратишь деньги?",
        subtitle: "Честно — это влияет на размер «свободного» буфера",
        layout: "list",
        options: [
            { value: "PLANNED",   icon: "clipboard-list", title: "Планирую и держусь бюджета" },
            { value: "MIXED",     icon: "circle-help",    title: "Планирую, но иногда срываюсь" },
            { value: "IMPULSIVE", icon: "banknote",       title: "Трачу импульсивно", hint: "увидел — купил" }
        ]
    }
];

const surveyState = {
    step: 0,
    answers: {},
    mandatory: true,
    editMode: false
};

let surveyModalEl = null;
let surveyBodyEl = null;
let surveyTitleEl = null;
let surveySubtitleEl = null;
let surveyCounterEl = null;
let surveyProgressEl = null;
let surveyBackBtn = null;
let surveyNextBtn = null;
let surveyAlertEl = null;
let surveyFormEl = null;
let surveyCloseBtn = null;

function isSurveyOpen() {
    return surveyModalEl?.classList.contains("open");
}

/**
 * @param {Object} opts
 * @param {Object} [opts.prefill]   — текущие ответы пользователя (для edit-режима)
 * @param {boolean} [opts.mandatory=true] — если false, можно закрыть крестиком
 */
async function openSurveyModal(opts = {}) {
    if (!surveyModalEl) return;

    const prefill = opts.prefill || {};
    surveyState.step = 0;
    surveyState.answers = normalizePrefill(prefill);
    surveyState.mandatory = opts.mandatory !== false;
    surveyState.editMode = !surveyState.mandatory;

    surveyModalEl.classList.add("open");
    surveyModalEl.setAttribute("aria-hidden", "false");
    document.body.classList.add("modal-open");
    hideAlert("survey-alert");

    if (surveyCloseBtn) {
        surveyCloseBtn.classList.toggle("hidden", surveyState.mandatory);
    }

    renderSurveyStep();
}

function normalizePrefill(prefill) {
    const out = {};
    SURVEY_QUESTIONS.forEach((q) => {
        const v = prefill[q.key];
        if (v === undefined || v === null) return;
        out[q.key] = v;
    });
    return out;
}

function closeSurveyModal() {
    if (!surveyModalEl) return;
    surveyModalEl.classList.remove("open");
    surveyModalEl.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
}

function handleSurveyClose() {
    if (surveyState.mandatory) return;
    closeSurveyModal();
}

function renderSurveyStep() {
    const q = SURVEY_QUESTIONS[surveyState.step];
    if (!q) return;

    surveyTitleEl.textContent = q.title;
    surveySubtitleEl.textContent = q.subtitle;
    surveyCounterEl.textContent = `Вопрос ${surveyState.step + 1} из ${SURVEY_QUESTIONS.length}`;
    surveyProgressEl.style.width = `${((surveyState.step + 1) / SURVEY_QUESTIONS.length) * 100}%`;

    surveyBackBtn.classList.toggle("invisible", surveyState.step === 0);
    const isLast = surveyState.step === SURVEY_QUESTIONS.length - 1;
    surveyNextBtn.textContent = isLast
        ? (surveyState.editMode ? "Обновить анкету ✓" : "Сохранить ✓")
        : "Далее →";

    const selected = surveyState.answers[q.key];
    const containerClass = q.layout === "grid" ? "survey-children-grid" : "space-y-2";
    const optionsHtml = q.options.map((opt) => {
        const isSel = optionEquals(selected, opt.value);
        const hint = opt.hint ? `<div class="survey-label-hint">${opt.hint}</div>` : "";
        const iconSize = q.layout === "grid" ? "lc-lg" : "lc-md";
        return `
            <button type="button" class="survey-option ${isSel ? "selected" : ""}"
                    data-key="${q.key}" data-value='${JSON.stringify(opt.value)}'>
                <span class="survey-radio"></span>
                <span class="survey-emoji">
                    <i data-lucide="${opt.icon}" class="lc ${iconSize}"></i>
                </span>
                <span class="survey-label">
                    <div class="survey-label-title">${opt.title}</div>
                    ${hint}
                </span>
            </button>
        `;
    }).join("");

    surveyBodyEl.innerHTML = `<div class="${containerClass}">${optionsHtml}</div>`;

    surveyBodyEl.querySelectorAll(".survey-option").forEach((btn) => {
        btn.addEventListener("click", handleSurveyOptionClick);
    });

    if (window.lucide && typeof window.lucide.createIcons === "function") {
        window.lucide.createIcons();
    }

    updateNextBtnState();
}

function optionEquals(a, b) {
    if (a === undefined || a === null) return false;
    return JSON.stringify(a) === JSON.stringify(b);
}

function handleSurveyOptionClick(event) {
    const btn = event.currentTarget;
    const key = btn.dataset.key;
    let value;
    try {
        value = JSON.parse(btn.dataset.value);
    } catch (_) {
        value = btn.dataset.value;
    }
    surveyState.answers[key] = value;

    surveyBodyEl.querySelectorAll(".survey-option").forEach((el) => el.classList.remove("selected"));
    btn.classList.add("selected");
    updateNextBtnState();
}

function updateNextBtnState() {
    const q = SURVEY_QUESTIONS[surveyState.step];
    const has = q ? (surveyState.answers[q.key] !== undefined && surveyState.answers[q.key] !== null) : false;
    surveyNextBtn.disabled = !has;
}

function handleSurveyBack() {
    if (surveyState.step > 0) {
        surveyState.step -= 1;
        renderSurveyStep();
    }
}

async function handleSurveySubmit(event) {
    event.preventDefault();
    hideAlert("survey-alert");

    const q = SURVEY_QUESTIONS[surveyState.step];
    if (q && (surveyState.answers[q.key] === undefined || surveyState.answers[q.key] === null)) {
        showAlert("survey-alert", "Выбери вариант, чтобы продолжить", "error");
        return;
    }

    if (surveyState.step < SURVEY_QUESTIONS.length - 1) {
        surveyState.step += 1;
        renderSurveyStep();
        return;
    }

    setLoading(surveyNextBtn, true, "Сохраняем...");

    try {
        await CashCareApi.saveSurvey(surveyState.answers);
        closeSurveyModal();
        if (typeof window.onSurveyCompleted === "function") {
            await window.onSurveyCompleted();
        }
    } catch (error) {
        showAlert("survey-alert", formatError(error), "error");
        setLoading(surveyNextBtn, false, "Сохранить ✓");
    }
}

function bindSurveyModal() {
    surveyModalEl = document.getElementById("survey-modal");
    if (!surveyModalEl) return;

    surveyBodyEl = document.getElementById("survey-body");
    surveyTitleEl = document.getElementById("survey-title");
    surveySubtitleEl = document.getElementById("survey-subtitle");
    surveyCounterEl = document.getElementById("survey-step-counter");
    surveyProgressEl = document.getElementById("survey-progress-fill");
    surveyBackBtn = document.getElementById("survey-back-btn");
    surveyNextBtn = document.getElementById("survey-next-btn");
    surveyAlertEl = document.getElementById("survey-alert");
    surveyFormEl = document.getElementById("survey-form");
    surveyCloseBtn = document.getElementById("survey-close-btn");

    if (surveyFormEl) surveyFormEl.addEventListener("submit", handleSurveySubmit);
    if (surveyBackBtn) surveyBackBtn.addEventListener("click", handleSurveyBack);
    if (surveyCloseBtn) surveyCloseBtn.addEventListener("click", handleSurveyClose);
}

document.addEventListener("DOMContentLoaded", bindSurveyModal);
