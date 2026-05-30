const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const loginBtn = document.getElementById("login-btn");
const registerBtn = document.getElementById("register-btn");

document.addEventListener("DOMContentLoaded", () => {
    handleAuthenticatedEntry();
    initTabs();

    loginForm.addEventListener("submit", handleLogin);
    registerForm.addEventListener("submit", handleRegister);
});

async function handleAuthenticatedEntry() {
    if (!CashCareApi.isAuthenticated()) {
        return;
    }

    try {
        await CashCareApi.getProfile();
        window.location.href = "/dashboard";
    } catch (_) {
        CashCareApi.logout();
    }
}

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

    loginForm.classList.toggle("hidden", tabName !== "login");
    registerForm.classList.toggle("hidden", tabName !== "register");
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
        window.location.href = "/dashboard";
    } catch (error) {
        showAlert("login-alert", formatError(error), "error");
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
        await CashCareApi.login(payload.email, payload.password);
        window.location.href = "/dashboard";
    } catch (error) {
        showAlert("register-alert", formatError(error), "error");
        setLoading(registerBtn, false, "Создать аккаунт");
    }
}
