const GENDER_LABELS = {
    MALE: "Мужской",
    FEMALE: "Женский"
};

function formatMoney(value) {
    return new Intl.NumberFormat("ru-RU").format(Math.round(Number(value) || 0)) + " ₽";
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

function redirectIfGuest() {
    if (!CashCareApi.isAuthenticated()) {
        window.location.href = "/";
    }
}
