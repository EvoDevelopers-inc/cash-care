const logoutBtn = document.getElementById("logout-btn");

document.addEventListener("DOMContentLoaded", async () => {
    redirectIfGuest();

    try {
        const profile = await CashCareApi.getProfile();
        if (!profile.init) {
            await openInitModal();
        }
    } catch (_) {
        CashCareApi.logout();
        window.location.href = "/";
        return;
    }

    logoutBtn.addEventListener("click", handleLogout);
    await loadProfile();
    renderDashboardAiSummary();
});

async function loadProfile() {
    hideAlert("profile-alert");

    try {
        const profile = await CashCareApi.getProfile();
        fillProfile(profile);
    } catch (error) {
        if (error.status === 401) {
            try {
                await CashCareApi.refresh();
                fillProfile(await CashCareApi.getProfile());
                return;
            } catch (_) {
                CashCareApi.logout();
                window.location.href = "/";
                return;
            }
        }

        showAlert("profile-alert", formatError(error), "error");
    }
}

function handleLogout() {
    CashCareApi.logout();
    window.location.href = "/";
}

function fillProfile(profile) {
    document.getElementById("profile-first-name").textContent = profile.firstName ?? "—";
    document.getElementById("profile-last-name").textContent = profile.lastName ?? "—";
    document.getElementById("profile-username").textContent = profile.username ?? "—";
    document.getElementById("profile-email").textContent = profile.email ?? "—";
    document.getElementById("profile-age").textContent = profile.age ?? "—";
    document.getElementById("profile-gender").textContent = GENDER_LABELS[profile.gender] ?? profile.gender ?? "—";
}
