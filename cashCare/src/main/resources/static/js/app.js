const GENDER_LABELS = {
    MALE: "Мужской",
    FEMALE: "Женский"
};

const authSection = document.getElementById("auth-section");
const dashboardSection = document.getElementById("dashboard-section");
const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const loginBtn = document.getElementById("login-btn");
const registerBtn = document.getElementById("register-btn");
const logoutBtn = document.getElementById("logout-btn");

document.addEventListener("DOMContentLoaded", async () => {
    initTabs();

    loginForm.addEventListener("submit", handleLogin);
    registerForm.addEventListener("submit", handleRegister);
    logoutBtn.addEventListener("click", handleLogout);

    if (CashCareApi.isAuthenticated()) {
        await showDashboard();
    } else {
        showAuth();
    }
});

function initTabs() {
    document.querySelectorAll(".tab-btn").forEach((button) => {
        button.addEventListener("click", () => {
            switchTab(button.dataset.tab);
        });
    });
}

function switchTab(tabName) {
    document.querySelectorAll(".tab-btn").forEach((button) => {
        button.classList.toggle("active", button.dataset.tab === tabName);
    });

    loginForm.classList.toggle("active", tabName === "login");
    registerForm.classList.toggle("active", tabName === "register");
    hideAlert("login-alert");
    hideAlert("register-alert");
}

async function handleLogin(event) {
    event.preventDefault();
    hideAlert("login-alert");
    setLoading(loginBtn, true, "Входим...");

    const login = document.getElementById("login-input").value.trim();
    const password = document.getElementById("login-password").value;

    try {
        await CashCareApi.login(login, password);
        await showDashboard();
    } catch (error) {
        showAlert("login-alert", formatError(error), "error");
    } finally {
        setLoading(loginBtn, false, "Войти");
    }
}

async function handleRegister(event) {
    event.preventDefault();
    hideAlert("register-alert");
    setLoading(registerBtn, true, "Создаём...");

    const payload = {
        firstName: document.getElementById("reg-first-name").value.trim(),
        lastName: document.getElementById("reg-last-name").value.trim(),
        username: document.getElementById("reg-username").value.trim(),
        email: document.getElementById("reg-email").value.trim(),
        age: Number(document.getElementById("reg-age").value),
        gender: document.getElementById("reg-gender").value,
        password: document.getElementById("reg-password").value
    };

    try {
        await CashCareApi.register(payload);
        showAlert("register-alert", "Аккаунт создан. Теперь можно войти.", "success");
        switchTab("login");
        document.getElementById("login-input").value = payload.email;
        document.getElementById("login-password").value = payload.password;
    } catch (error) {
        showAlert("register-alert", formatError(error), "error");
    } finally {
        setLoading(registerBtn, false, "Создать аккаунт");
    }
}

async function handleLogout() {
    CashCareApi.logout();
    showAuth();
    hideAlert("profile-alert");
}

async function showDashboard() {
    authSection.classList.add("hidden");
    dashboardSection.classList.add("active");

    try {
        const profile = await CashCareApi.getProfile();
        fillProfile(profile);
    } catch (error) {
        if (error.status === 401) {
            try {
                await CashCareApi.refresh();
                const profile = await CashCareApi.getProfile();
                fillProfile(profile);
                return;
            } catch (_) {
                CashCareApi.logout();
                showAuth();
                showAlert("login-alert", "Сессия истекла. Войди снова.", "error");
                return;
            }
        }

        showAlert("profile-alert", formatError(error), "error");
    }
}

function showAuth() {
    authSection.classList.remove("hidden");
    dashboardSection.classList.remove("active");
}

function fillProfile(profile) {
    document.getElementById("profile-first-name").textContent = profile.firstName ?? "—";
    document.getElementById("profile-last-name").textContent = profile.lastName ?? "—";
    document.getElementById("profile-username").textContent = profile.username ?? "—";
    document.getElementById("profile-email").textContent = profile.email ?? "—";
    document.getElementById("profile-age").textContent = profile.age ?? "—";
    document.getElementById("profile-gender").textContent = GENDER_LABELS[profile.gender] ?? profile.gender ?? "—";
}

function formatError(error) {
    if (Array.isArray(error.details) && error.details.length > 0) {
        return `${error.message}: ${error.details.join(", ")}`;
    }

    return error.message || "Что-то пошло не так";
}

function showAlert(elementId, message, type) {
    const alert = document.getElementById(elementId);
    alert.textContent = message;
    alert.className = `alert show alert-${type}`;
}

function hideAlert(elementId) {
    const alert = document.getElementById(elementId);
    alert.textContent = "";
    alert.className = "alert";
}

function setLoading(button, isLoading, label) {
    button.disabled = isLoading;
    button.innerHTML = isLoading ? `<span class="spinner"></span>${label}` : label;
}
