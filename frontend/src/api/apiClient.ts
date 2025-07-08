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

export default api;
