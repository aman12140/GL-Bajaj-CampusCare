import axios from 'axios';

const BASE = import.meta.env.VITE_API_BASE_URL || '';
const TOKEN_KEY = 'cc_token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const api = axios.create({ baseURL: BASE });

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// An expired/invalid token (401) sends the user back to the login page.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const hadToken = !!tokenStore.get();
    if (err.response?.status === 401 && hadToken && !err.config?.url?.includes('/api/auth/login')) {
      tokenStore.clear();
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login?expired=1';
    }
    return Promise.reject(err);
  }
);

/** Turns any axios error into a friendly message for the UI. */
export function errorMessage(err) {
  if (!err.response) return 'Cannot reach the server. Please check your connection and try again.';
  const data = err.response.data;
  if (data instanceof Blob) return 'The request could not be completed.';
  return data?.message || 'Something went wrong. Please try again.';
}

/** Field-level validation errors returned by the backend ({ field: message }). */
export function fieldErrors(err) {
  return err.response?.data?.errors || {};
}

/** Uploaded images are served by the backend at /uploads/... */
export function fileUrl(path) {
  if (!path) return null;
  return path.startsWith('http') ? path : `${BASE}${path}`;
}

/** Downloads a file (CSV/PDF) using the authenticated client. */
export async function downloadFile(url, params, fallbackName) {
  const res = await api.get(url, { params, responseType: 'blob' });
  const disposition = res.headers['content-disposition'] || '';
  const match = /filename="?([^"]+)"?/.exec(disposition);
  const link = document.createElement('a');
  link.href = URL.createObjectURL(res.data);
  link.download = match ? match[1] : fallbackName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(link.href);
}

export default api;
