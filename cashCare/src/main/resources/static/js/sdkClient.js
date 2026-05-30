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

    async getInitSetup() {
        return this.request("GET", "/api/finances/monthly/init/setup", undefined, true);
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

        const response = await fetch("/api/statements/upload", {
            method: "POST",
            headers,
            body: formData
        });

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
