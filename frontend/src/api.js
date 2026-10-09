export const token = {
  get: () => localStorage.getItem('vt'),
  set: (v) => localStorage.setItem('vt', v),
  clear: () => localStorage.removeItem('vt'),
};

export async function api(path, { method = 'GET', body } = {}) {
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token.get() ? { Authorization: 'Bearer ' + token.get() } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) { token.clear(); window.dispatchEvent(new Event('logout')); throw new Error('Please log in again'); }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || 'Request failed');
  return data;
}

export const inr = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n ?? 0);
export const pct = (n) => (n >= 0 ? '+' : '') + (n ?? 0).toFixed(2) + '%';
