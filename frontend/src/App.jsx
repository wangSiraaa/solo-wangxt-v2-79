import React, { useEffect, useMemo, useState } from 'react';
import CalendarTimeline from './components/CalendarTimeline.jsx';
import ResultPanel from './components/ResultPanel.jsx';
import {
  checkAvailability, fetchContracts, fetchExclusivities, fetchPrograms,
} from './api.js';

const REGIONS = [
  { code: 'CN-HK', label: '中国香港 CN-HK' },
  { code: 'SG', label: '新加坡 SG' },
  { code: 'JP', label: '日本 JP（无合同对照）' },
];
const CHANNELS = [
  { code: 'WEB', label: 'WEB 网页端' },
  { code: 'MOBILE', label: 'MOBILE 移动端' },
  { code: 'OTT', label: 'OTT 大屏端' },
];
const LANGUAGES = [
  { code: 'zh-Hans', label: '简体中文 zh-Hans' },
  { code: 'zh-Hant', label: '繁体中文 zh-Hant' },
  { code: 'en', label: '英文 en' },
];

// 边界验证用例
const CASES = [
  { name: '①同渠道开始边界(含) WEB 2026-01-01 00:00', regionCode: 'CN-HK', channelCode: 'WEB', languageCode: 'zh-Hant', plannedLocal: '2026-01-01T00:00' },
  { name: '①同渠道结束边界(不含) WEB 2027-01-01 00:00', regionCode: 'CN-HK', channelCode: 'WEB', languageCode: 'zh-Hant', plannedLocal: '2027-01-01T00:00' },
  { name: '②空档起点(不含) OTT 06-30 16:00', regionCode: 'CN-HK', channelCode: 'OTT', languageCode: 'zh-Hant', plannedLocal: '2026-06-30T16:00' },
  { name: '②空档前1秒 OTT 06-30 15:59:59', regionCode: 'CN-HK', channelCode: 'OTT', languageCode: 'zh-Hant', plannedLocal: '2026-06-30T15:59:59' },
  { name: '②空档正中 OTT 07-08 00:00', regionCode: 'CN-HK', channelCode: 'OTT', languageCode: 'en', plannedLocal: '2026-07-08T00:00' },
  { name: '②空档终点(含) OTT 07-15 16:00', regionCode: 'CN-HK', channelCode: 'OTT', languageCode: 'zh-Hant', plannedLocal: '2026-07-15T16:00' },
  { name: '③缺语言版本 MOBILE 申请简中', regionCode: 'CN-HK', channelCode: 'MOBILE', languageCode: 'zh-Hans', plannedLocal: '2026-05-01T08:00' },
  { name: '④排他冲突 SG OTT 重叠期', regionCode: 'SG', channelCode: 'OTT', languageCode: 'en', plannedLocal: '2026-07-01T00:00' },
  { name: '④同地区不同渠道 SG WEB 不受影响', regionCode: 'SG', channelCode: 'WEB', languageCode: 'en', plannedLocal: '2026-07-01T00:00' },
  { name: '⑤无合同地区 JP', regionCode: 'JP', channelCode: 'MOBILE', languageCode: 'zh-Hans', plannedLocal: '2026-05-01T00:00' },
];

/** datetime-local 的值“按 UTC 解释”，补上 Z（输入框本身不带时区） */
function toUtcInstant(localInputValue) {
  return localInputValue ? `${localInputValue}Z` : null;
}

export default function App() {
  const [programs, setPrograms] = useState([]);
  const [programCode, setProgramCode] = useState('SHOW-STARHUNT');
  const [contracts, setContracts] = useState([]);
  const [exclusivities, setExclusivities] = useState([]);

  const [regionCode, setRegionCode] = useState('CN-HK');
  const [channelCode, setChannelCode] = useState('WEB');
  const [languageCode, setLanguageCode] = useState('zh-Hant');
  const [plannedLocal, setPlannedLocal] = useState('2026-06-15T12:00');

  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchPrograms().then(setPrograms).catch((e) => setError(e.message));
    fetchExclusivities().then(setExclusivities).catch(() => {});
  }, []);

  useEffect(() => {
    if (programCode) {
      fetchContracts(programCode).then(setContracts).catch((e) => setError(e.message));
    }
  }, [programCode]);

  const plannedInstant = useMemo(() => toUtcInstant(plannedLocal), [plannedLocal]);

  async function submit(overrides = {}) {
    const payload = {
      programCode,
      regionCode: overrides.regionCode || regionCode,
      channelCode: overrides.channelCode || channelCode,
      languageCode: overrides.languageCode || languageCode,
      plannedAt: overrides.plannedAt || plannedInstant,
    };
    if (overrides.regionCode) setRegionCode(overrides.regionCode);
    if (overrides.channelCode) setChannelCode(overrides.channelCode);
    if (overrides.languageCode) setLanguageCode(overrides.languageCode);
    if (overrides.plannedLocal) setPlannedLocal(overrides.plannedLocal);

    setLoading(true);
    setError('');
    try {
      setResult(await checkAvailability(payload));
    } catch (e) {
      setResult(null);
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <header>
        <h1>🎬 视频发行授权可用性判定台</h1>
        <p className="subtitle">
          授权按 地区 × 渠道 × 语言版本 × UTC 时间窗 判定；排他冲突或缺少语言版授权一律阻止上架。
          <br />本系统仅使用<b>虚构版权数据</b>，不处理任何真实播放数据。
        </p>
      </header>

      <section className="card">
        <h2>运营上架计划输入</h2>
        <div className="form-grid">
          <label>
            节目
            <select value={programCode} onChange={(e) => setProgramCode(e.target.value)}>
              {programs.map((p) => (
                <option key={p.code} value={p.code}>{p.title}（{p.code}）</option>
              ))}
            </select>
          </label>
          <label>
            地区
            <select value={regionCode} onChange={(e) => setRegionCode(e.target.value)}>
              {REGIONS.map((r) => <option key={r.code} value={r.code}>{r.label}</option>)}
            </select>
          </label>
          <label>
            渠道
            <select value={channelCode} onChange={(e) => setChannelCode(e.target.value)}>
              {CHANNELS.map((c) => <option key={c.code} value={c.code}>{c.label}</option>)}
            </select>
          </label>
          <label>
            语言版本
            <select value={languageCode} onChange={(e) => setLanguageCode(e.target.value)}>
              {LANGUAGES.map((l) => <option key={l.code} value={l.code}>{l.label}</option>)}
            </select>
          </label>
          <label>
            计划上架时刻（按 UTC 解释）
            <input
              type="datetime-local"
              step="1"
              value={plannedLocal}
              onChange={(e) => setPlannedLocal(e.target.value)}
            />
            <small>输入框不含时区，统一按 UTC 处理 → <span className="mono">{plannedInstant}</span></small>
          </label>
        </div>
        <div className="actions">
          <button className="primary" disabled={loading} onClick={() => submit()}>
            {loading ? '判定中…' : '判定能否上架'}
          </button>
        </div>
        {error && <div className="error-box">⚠️ {error}</div>}

        <div className="cases">
          <h4>边界验证用例（一键填充并判定）</h4>
          <div className="case-buttons">
            {CASES.map((c) => (
              <button key={c.name} className="case-btn" onClick={() => submit(c)}>
                {c.name}
              </button>
            ))}
          </div>
        </div>
      </section>

      <ResultPanel result={result} />

      <CalendarTimeline
        contracts={contracts}
        exclusivities={exclusivities}
        plannedAt={result ? result.plannedStatus.evaluatedAt : null}
        evaluatedNow={result ? result.evaluatedNow : null}
      />

      <footer>
        时间统一 UTC，窗口规则 <span className="mono">[start_at, end_at)</span>：含开始、不含结束；end 为空表示开放结尾。
      </footer>
    </div>
  );
}
