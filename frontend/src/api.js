const BASE = 'http://localhost:8080/api';

async function getJson(path) {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(`GET ${path} 失败: HTTP ${res.status}`);
  return res.json();
}

export async function fetchPrograms() {
  return getJson('/programs');
}

export async function fetchContracts(programCode) {
  return getJson(`/contracts?programCode=${encodeURIComponent(programCode)}`);
}

export async function fetchExclusivities() {
  return getJson('/exclusivities');
}

export async function checkAvailability(payload) {
  const res = await fetch(`${BASE}/availability`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body = await res.json();
  if (!res.ok) {
    throw new Error(body.message || `判定请求失败: HTTP ${res.status}`);
  }
  return body;
}
