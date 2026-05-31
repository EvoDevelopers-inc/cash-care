const TOKEN_KEY = "cashcare.accessToken";
const REFRESH_KEY = "cashcare.refreshToken";

const CashCareApi = {
    getAccessToken() {
        return localStorage.getItem(TOKEN_KEY);
    },

    getRefreshToken() {
        return localStorage.getItem(REFRESH_KEY);
    },

    saveTokens(accessToken, refreshToken) {
        localStorage.setItem(TOKEN_KEY, accessToken);
        localStorage.setItem(REFRESH_KEY, refreshToken);
    },

    clearTokens() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_KEY);
    },

    isAuthenticated() {
        return Boolean(this.getAccessToken());
    },

    async request(method, url, data, auth = false) {
        const headers = {
            "Content-Type": "application/json"
        };

        if (auth) {
            const token = this.getAccessToken();
            if (token) {
                headers.Authorization = `Bearer ${token}`;
            }
        }

        const options = { method, headers };

        if (data !== undefined) {
            options.body = JSON.stringify(data);
        }

        const response = await fetch(url, options);
        let payload = null;

        try {
            payload = await response.json();
        } catch (_) {
            payload = null;
        }

        if (!response.ok) {
            const message = payload?.message || `Request failed (${response.status})`;
            const details = payload?.details ?? null;
            const error = new Error(message);
            error.status = response.status;
            error.details = details;
            throw error;
        }

        return payload;
    },

    async register(data) {
        return this.request("POST", "/api/auth/register", data);
    },

    async login(login, password) {
        const response = await this.request("POST", "/api/auth/login", { login, password });
        this.saveTokens(response.accessToken, response.refreshToken);
        return response;
    },

    async refresh() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            throw new Error("Refresh token not found");
        }

        const response = await this.request("POST", "/api/auth/refresh", { refreshToken });
        this.saveTokens(response.accessToken, response.refreshToken);
        return response;
    },

    async getProfile() {
        return this.request("GET", "/api/auth/me", undefined, true);
    },

    async getAnalyticsOverview() {
        return this.request("GET", "/api/analytics/overview", undefined, true);
    },

    async runBudgetAi() {
        return this.request("POST", "/api/analytics/ai-budget", {}, true);
    },

    async createCategory(monthlyFinancesId, nameCategory, required = false) {
        return this.request("POST", "/api/finances/categories", {
            monthlyFinancesId,
            nameCategory,
            required
        }, true);
    },

    async updateCategory(id, payload) {
        return this.request("PUT", `/api/finances/categories/${id}`, payload, true);
    },

    async deleteCategory(id) {
        return this.request("DELETE", `/api/finances/categories/${id}`, undefined, true);
    },

    async updateMonthlyFinances(id, payload) {
        return this.request("PUT", `/api/finances/monthly/${id}`, payload, true);
    },

    async getInitSetup() {
        return this.request("GET", "/api/finances/monthly/init/setup", undefined, true);
    },

    async getSurvey() {
        return this.request("GET", "/api/user/survey", undefined, true);
    },

    async saveSurvey(payload) {
        return this.request("POST", "/api/user/survey", payload, true);
    },

    async getCredits() {
        return this.request("GET", "/api/credits", undefined, true);
    },

    async createCredit(payload) {
        return this.request("POST", "/api/credits", payload, true);
    },

    async updateCredit(id, payload) {
        return this.request("PUT", `/api/credits/${id}`, payload, true);
    },

    async deleteCredit(id) {
        return this.request("DELETE", `/api/credits/${id}`, undefined, true);
    },

    async bulkSaveCredits(items) {
        return this.request("POST", "/api/credits/bulk", items || [], true);
    },

    async getGoals() {
        return this.request("GET", "/api/goals", undefined, true);
    },

    async createGoal(payload) {
        return this.request("POST", "/api/goals", payload, true);
    },

    async updateGoal(id, payload) {
        return this.request("PUT", `/api/goals/${id}`, payload, true);
    },

    async deleteGoal(id) {
        return this.request("DELETE", `/api/goals/${id}`, undefined, true);
    },

    async contributeGoal(id, payload) {
        return this.request("POST", `/api/goals/${id}/contribute`, payload, true);
    },

    async getSpontaneous(monthlyFinancesId) {
        return this.request(
            "GET",
            `/api/finances/spontaneous?monthlyFinancesId=${monthlyFinancesId}`,
            undefined,
            true
        );
    },

    async createSpontaneous(payload) {
        return this.request("POST", "/api/finances/spontaneous", payload, true);
    },

    async deleteSpontaneous(id) {
        return this.request("DELETE", `/api/finances/spontaneous/${id}`, undefined, true);
    },

    async submitInit(data) {
        return this.request("POST", "/api/finances/monthly/init", data, true);
    },

    async uploadStatement(file) {
        const formData = new FormData();
        formData.append("file", file);

        const headers = {};
        const token = this.getAccessToken();
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 90000);

        let response;
        try {
            response = await fetch("/api/statements/upload", {
                method: "POST",
                headers,
                body: formData,
                signal: controller.signal
            });
        } catch (e) {
            clearTimeout(timeoutId);
            if (e.name === "AbortError") {
                const error = new Error("AI слишком долго думал — попробуй ещё раз или загрузи короче выписку");
                error.status = 504;
                throw error;
            }
            throw e;
        }
        clearTimeout(timeoutId);

        let payload = null;
        try {
            payload = await response.json();
        } catch (_) {
            payload = null;
        }

        if (!response.ok) {
            const message = payload?.message || `Upload failed (${response.status})`;
            const details = payload?.details ?? null;
            const error = new Error(message);
            error.status = response.status;
            error.details = details;
            throw error;
        }

        return payload;
    },

    logout() {
        this.clearTokens();
    }
};
