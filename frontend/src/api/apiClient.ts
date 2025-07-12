import axios from "axios";

// Helper to get CSRF token from cookies
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()!.split(';').shift() || null;
  return null;
}

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  withCredentials: true, // wichtig für Cookies (JWT)
});

// Add CSRF token to all unsafe requests
api.interceptors.request.use(config => {
  const csrf = getCookie("csrfToken");
  if (csrf && /post|put|delete/i.test(config.method || "")) {
    config.headers["X-CSRF-Token"] = csrf;
  }
  return config;
});

// Response interceptor: Bei 403 (CSRF) neues Token holen und Request wiederholen
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    if (
      error.response &&
      error.response.status === 403 &&
      !originalRequest._retry &&
      /csrf/i.test(error.response.data?.message || "")
    ) {
      originalRequest._retry = true;
      // Neues CSRF-Token anfordern (Endpoint ggf. anpassen)
      await api.get("/auth/csrf");
      // Token ist jetzt als Cookie gesetzt, Request wiederholen
      const csrf = getCookie("csrfToken");
      if (csrf) {
        originalRequest.headers["X-CSRF-Token"] = csrf;
      }
      return api(originalRequest);
    }
    return Promise.reject(error);
  }
);

export default api;
