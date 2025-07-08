import api from "../api/apiClient";

export function getCsrfToken(): string | null {
  const match = document.cookie.match(/(?:^|; )csrfToken=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

export async function postWithCsrf(url: string, data?: any) {
  const csrfToken = getCsrfToken();
  return api.post(url, data, {
    headers: csrfToken ? { "X-CSRF-Token": csrfToken } : {},
  });
}
