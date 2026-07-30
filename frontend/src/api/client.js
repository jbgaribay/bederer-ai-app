// Defaults to a relative "/api" path, which works behind the Vite dev proxy
// (vite.config.js) or the nginx reverse proxy in docker-compose. For a static
// S3/CloudFront deployment of the frontend there's no server-side proxy, so
// set VITE_API_BASE_URL to the backend's full URL at build time instead
// (see README's AWS deployment notes).
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, options);

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

function authHeader({ username, password } = {}) {
  if (!username || !password) return {};
  return { Authorization: `Basic ${btoa(`${username}:${password}`)}` };
}

export const api = {
  getHistory: () => request("/swings"),

  analyzeSwing: (file, shotType, auth) => {
    const formData = new FormData();
    formData.append("video", file);
    formData.append("shotType", shotType);

    return request("/swings", {
      method: "POST",
      body: formData,
      headers: authHeader(auth),
    });
  },

  deleteSwing: (id, auth) =>
    request(`/swings/${id}`, {
      method: "DELETE",
      headers: authHeader(auth),
    }),
};
