import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from './api.js'

/** datetime-local 值（按 UTC 解释）→ ISO 字符串 */
function toUtcIso(v) {
  if (!v) return null
  return v.length === 16 ? `${v}:00Z` : `${v}Z`
}

/** ISO → 紧凑 UTC 展示 */
function fmtUtc(iso) {
  if (!iso) return '—'
  return iso.replace('T', ' ').replace(/(\.\d+)?Z$/, ' UTC')
}

const REASON_LABEL = {
  NO_LICENSE: '无授权合同',
  MISSING_LANGUAGE_VERSION: '缺少语言版本授权',
  NOT_YET_LICENSED: '授权尚未生效',
  LICENSE_EXPIRED: '授权已过期',
  LICENSE_GAP: '授权空档',
  EXCLUSIVITY_CONFLICT: '排他冲突',
}

function StatusBadge({ status }) {
  return (
    <span className={`badge ${status === 'AVAILABLE' ? 'ok' : 'blocked'}`}>
      {status === 'AVAILABLE' ? '可上架' : '不可上架'}
    </span>
  )
}

function DecisionCard({ title, decision }) {
  if (!decision) return null
  return (
    <div className={`card decision ${decision.status === 'AVAILABLE' ? 'ok' : 'blocked'}`}>
      <div className="decision-head">
        <h3>{title}</h3>
        <StatusBadge status={decision.status} />
      </div>
      <div className="muted">判定时刻：{fmtUtc(decision.evaluatedAt)}</div>

      {decision.matchedContract && (
        <div className="hit">
          命中合同 <b>{decision.matchedContract.contractRef}</b>（
          {fmtUtc(decision.matchedContract.validFrom)} ~ {fmtUtc(decision.matchedContract.validTo)}）
        </div>
      )}

      {decision.reasons?.length > 0 && (
        <ul className="reasons">
          {decision.reasons.map((r) => (
            <li key={r.code}>
              <b>{REASON_LABEL[r.code] ?? r.code}</b>：{r.message}
            </li>
          ))}
        </ul>
      )}

      {decision.relatedContracts?.length > 0 && (
        <details>
          <summary>同组合合同窗口（{decision.relatedContracts.length}）</summary>
          <ul>
            {decision.relatedContracts.map((c) => (
              <li key={c.id}>
                {c.contractRef}：[{fmtUtc(c.validFrom)} ~ {fmtUtc(c.validTo)}）
              </li>
            ))}
          </ul>
        </details>
      )}

      {decision.activeExclusivities?.length > 0 && (
        <details>
          <summary>该时刻生效的排他约定（{decision.activeExclusivities.length}）</summary>
          <ul>
            {decision.activeExclusivities.map((e) => (
              <li key={e.id}>
                {e.channelCode} 独家 [{fmtUtc(e.validFrom)} ~ {fmtUtc(e.validTo)}）：{e.note}
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  )
}

const CELL_CLASS = {
  AVAILABLE: 'cell ok',
  BLOCKED: 'cell blocked',
}
const CELL_REASON_CLASS = {
  EXCLUSIVITY_CONFLICT: 'cell blocked excl',
  LICENSE_GAP: 'cell blocked gap',
}

function Calendar({ data, onPickDay }) {
  if (!data) return null
  if (data.channels.length === 0) {
    return <div className="muted">该地区没有合同或排他数据。</div>
  }
  const daysInMonth = data.channels[0]?.days.length ?? 0
  return (
    <div className="calendar-wrap">
      <table className="calendar">
        <thead>
          <tr>
            <th className="chan-col">渠道 \ 日</th>
            {Array.from({ length: daysInMonth }, (_, i) => (
              <th key={i}>{i + 1}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.channels.map((ch) => (
            <tr key={ch.channel}>
              <td className="chan-col">{ch.channel}</td>
              {ch.days.map((d) => {
                const cls =
                  d.status === 'AVAILABLE'
                    ? CELL_CLASS.AVAILABLE
                    : CELL_REASON_CLASS[d.reason] ?? CELL_CLASS.BLOCKED
                const tip = `${d.date} 00:00 UTC · ${d.status === 'AVAILABLE' ? '可上架' : '不可上架'}${
                  d.reasonText ? ` · ${d.reasonText}` : ''
                }`
                return (
                  <td
                    key={d.date}
                    className={cls}
                    title={tip}
                    onClick={() => onPickDay(d.date)}
                  />
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="legend">
        <span className="sw ok" /> 可上架
        <span className="sw gap" /> 授权空档
        <span className="sw excl" /> 排他冲突
        <span className="sw blocked" /> 其他原因不可上架
        <span className="muted">（每日按 00:00 UTC 采样判定；点击日期格可填入计划时间）</span>
      </div>
    </div>
  )
}

function RightsTables({ rights, lvLabel }) {
  if (!rights) return null
  return (
    <div className="rights">
      <div className="card">
        <h3>授权合同</h3>
        <table>
          <thead>
            <tr>
              <th>合同号</th><th>语言版本</th><th>地区</th><th>渠道</th><th>生效（UTC）</th><th>失效（UTC，不含）</th>
            </tr>
          </thead>
          <tbody>
            {rights.contracts.map((c) => (
              <tr key={c.id}>
                <td>{c.contractRef}</td>
                <td>{lvLabel(c.languageVersionId)}</td>
                <td>{c.regionCode}</td>
                <td>{c.channelCode}</td>
                <td>{fmtUtc(c.validFrom)}</td>
                <td>{fmtUtc(c.validTo)}</td>
              </tr>
            ))}
            {rights.contracts.length === 0 && (
              <tr><td colSpan={6} className="muted">无合同</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="card">
        <h3>排他约定</h3>
        <table>
          <thead>
            <tr>
              <th>地区</th><th>独家渠道</th><th>生效（UTC）</th><th>失效（UTC，不含）</th><th>说明</th>
            </tr>
          </thead>
          <tbody>
            {rights.exclusivities.map((e) => (
              <tr key={e.id}>
                <td>{e.regionCode}</td>
                <td>{e.channelCode}</td>
                <td>{fmtUtc(e.validFrom)}</td>
                <td>{fmtUtc(e.validTo)}</td>
                <td>{e.note}</td>
              </tr>
            ))}
            {rights.exclusivities.length === 0 && (
              <tr><td colSpan={5} className="muted">无排他约定</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default function App() {
  const [catalog, setCatalog] = useState(null)
  const [programId, setProgramId] = useState(null)
  const [languageVersionId, setLanguageVersionId] = useState(null)
  const [region, setRegion] = useState('CN')
  const [channel, setChannel] = useState('OTT-A')
  const [plannedLocal, setPlannedLocal] = useState('2026-07-15T00:00')
  const [month, setMonth] = useState('2026-07')
  const [result, setResult] = useState(null)
  const [calendar, setCalendar] = useState(null)
  const [rights, setRights] = useState(null)
  const [error, setError] = useState(null)

  // 默认语言版本：优先中文配音（演示授权空档），否则取第一个
  const defaultLv = (p) =>
    p.languageVersions.find((l) => l.code === 'DUB-ZH')?.id ?? p.languageVersions[0]?.id ?? null

  useEffect(() => {
    api.catalog().then((c) => {
      setCatalog(c)
      const p = c.programs[0]
      setProgramId(p.id)
      setLanguageVersionId(defaultLv(p))
    }).catch((e) => setError(String(e)))
  }, [])

  const program = useMemo(
    () => catalog?.programs.find((p) => p.id === programId),
    [catalog, programId],
  )

  const runCheck = useCallback(
    (pid, lvid, reg, chan, plannedIso) => {
      if (!pid || !lvid || !reg || !chan || !plannedIso) return
      api.check({
        programId: pid,
        languageVersionId: lvid,
        region: reg,
        channel: chan,
        plannedAt: plannedIso,
      }).then(setResult).catch((e) => setError(String(e)))
    },
    [],
  )

  // 选择变化：刷新判定、日历与权利清单
  useEffect(() => {
    if (!programId || !languageVersionId || !region) return
    const plannedIso = toUtcIso(plannedLocal)
    runCheck(programId, languageVersionId, region, channel, plannedIso)
    api.calendar(programId, languageVersionId, region, month)
      .then(setCalendar).catch((e) => setError(String(e)))
    api.rights(programId, region).then(setRights).catch((e) => setError(String(e)))
  }, [programId, languageVersionId, region, channel, plannedLocal, month, runCheck])

  const lvLabel = useCallback(
    (id) => program?.languageVersions.find((l) => l.id === id)?.code ?? `#${id}`,
    [program],
  )

  const shiftMonth = (delta) => {
    const [y, m] = month.split('-').map(Number)
    const d = new Date(Date.UTC(y, m - 1 + delta, 1))
    setMonth(`${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, '0')}`)
  }

  const pickDay = (date) => setPlannedLocal(`${date}T00:00`)

  if (error) return <div className="page"><div className="card error">请求失败：{error}</div></div>
  if (!catalog) return <div className="page"><div className="muted">加载中…</div></div>

  return (
    <div className="page">
      <header>
        <h1>视频发行授权可用性判定</h1>
        <p className="muted">
          虚构版权数据演示 · 全部时间按 UTC · 授权窗口为 [生效， 失效)：起点包含、终点不包含
        </p>
      </header>

      <div className="card selectors">
        <label>
          节目
          <select
            value={programId ?? ''}
            onChange={(e) => {
              const p = catalog.programs.find((x) => x.id === Number(e.target.value))
              setProgramId(p.id)
              setLanguageVersionId(defaultLv(p))
            }}
          >
            {catalog.programs.map((p) => (
              <option key={p.id} value={p.id}>{p.title}（{p.code} · {p.genre}）</option>
            ))}
          </select>
        </label>
        <label>
          语言版本
          <select
            value={languageVersionId ?? ''}
            onChange={(e) => setLanguageVersionId(Number(e.target.value))}
          >
            {program?.languageVersions.map((l) => (
              <option key={l.id} value={l.id}>{l.label}（{l.code}）</option>
            ))}
          </select>
        </label>
        <label>
          地区
          <select value={region} onChange={(e) => setRegion(e.target.value)}>
            {catalog.regions.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
        </label>
        <label>
          渠道
          <select value={channel} onChange={(e) => setChannel(e.target.value)}>
            {catalog.channels.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
        <label>
          计划上架时间（按 UTC 解释）
          <input
            type="datetime-local"
            value={plannedLocal}
            onChange={(e) => setPlannedLocal(e.target.value)}
          />
        </label>
      </div>

      <div className="decisions">
        <DecisionCard title={`计划状态 · ${channel}`} decision={result?.planned} />
        <DecisionCard title={`当前状态 · ${channel}`} decision={result?.current} />
      </div>

      <div className="card">
        <div className="cal-head">
          <h3>授权日历 · {program?.title} · {region} · {lvLabel(languageVersionId)}</h3>
          <div>
            <button onClick={() => shiftMonth(-1)}>← 上月</button>
            <b className="month-label">{month}</b>
            <button onClick={() => shiftMonth(1)}>下月 →</button>
          </div>
        </div>
        <Calendar data={calendar} onPickDay={pickDay} />
      </div>

      <RightsTables rights={rights} lvLabel={lvLabel} />
    </div>
  )
}
