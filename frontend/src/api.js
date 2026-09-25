async function request(path, options) {
  const res = await fetch(path, options)
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${res.statusText}: ${text}`)
  }
  return res.json()
}

export const api = {
  catalog: () => request('/api/catalog'),
  rights: (programId, region) =>
    request(`/api/rights?programId=${programId}&region=${encodeURIComponent(region)}`),
  calendar: (programId, languageVersionId, region, month) =>
    request(
      `/api/availability/calendar?programId=${programId}&languageVersionId=${languageVersionId}` +
        `&region=${encodeURIComponent(region)}&month=${month}`,
    ),
  check: (body) =>
    request('/api/availability/check', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
}
