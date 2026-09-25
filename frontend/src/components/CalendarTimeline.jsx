import React from 'react';

// 日历展示固定 2026 全年（UTC）。左闭右开，结束时刻正好落在下一区间起点。
const SCALE_START = Date.parse('2026-01-01T00:00:00Z');
const SCALE_END = Date.parse('2027-01-01T00:00:00Z');
const SCALE_MS = SCALE_END - SCALE_START;

const MONTHS = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];

function pct(t) {
  const ms = Date.parse(t);
  return Math.min(100, Math.max(0, ((ms - SCALE_START) / SCALE_MS) * 100));
}

function fmt(t) {
  if (!t) return '开放结尾';
  return t.replace('T', ' ').replace('Z', ' UTC');
}

function Bar({ c, active }) {
  const left = pct(c.startAt);
  const width = Math.max(0.3, pct(c.endAt ?? '2027-01-01T00:00:00Z') - left);
  return (
    <div
      className={`bar ${c.exclusive ? 'bar-exclusive' : ''} ${active ? 'bar-active' : ''}`}
      style={{ left: `${left}%`, width: `${width}%` }}
      title={`${c.contractNo}｜${c.licensee}\n[${fmt(c.startAt)}, ${fmt(c.endAt)}) 左闭右开\n语言: ${c.languageCodes.join(', ')}\n${c.note}`}
    >
      <span className="bar-label">{c.contractNo}</span>
    </div>
  );
}

export default function CalendarTimeline({ contracts, exclusivities, plannedAt, evaluatedNow }) {
  // 仅展示节目自身的合同（日历按 节目 -> 地区+渠道 分组）
  const groups = React.useMemo(() => {
    const map = new Map();
    for (const c of contracts) {
      const key = `${c.regionCode} / ${c.channelCode}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(c);
    }
    return [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  }, [contracts]);

  const exclByKey = React.useMemo(() => {
    const map = new Map();
    for (const e of exclusivities) {
      const key = `${e.regionCode} / ${e.channelCode}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(e);
    }
    return map;
  }, [exclusivities]);

  const plannedPct = plannedAt ? pct(plannedAt) : null;
  const nowPct = evaluatedNow ? pct(evaluatedNow) : null;

  return (
    <section className="card">
      <h2>授权日历（UTC，区间左闭右开 [start, end)）</h2>
      <p className="hint">
        横条为合同授权窗口；深色描边为排他合同，下方虚线为排他约定区间。
        🔴 标记为计划上架时刻，🟢 NOW 为当前时刻。数据均为虚构版权数据。
      </p>

      <div className="scale">
        {MONTHS.map((m, i) => (
          <div key={m} className="scale-month" style={{ left: `${(i / 12) * 100}%`, width: `${100 / 12}%` }}>
            {m}
          </div>
        ))}
      </div>

      {groups.map(([key, list]) => (
        <div key={key} className="row">
          <div className="row-label">{key}</div>
          <div className="track">
            {list.map((c) => {
              const isActive = plannedAt &&
                c.startAt <= plannedAt &&
                (c.endAt == null || plannedAt < c.endAt);
              return <Bar key={c.contractNo} c={c} active={isActive} />;
            })}
            {(exclByKey.get(key) || []).map((e) => (
              <div
                key={`excl-${e.contractNo}`}
                className="excl-marker"
                style={{ left: `${pct(e.startAt)}%`, width: `${Math.max(0.3, pct(e.endAt ?? '2027-01-01T00:00:00Z') - pct(e.startAt))}%` }}
                title={`排他约定｜${e.holder}｜${e.contractNo}\n[${fmt(e.startAt)}, ${fmt(e.endAt)})\n${e.note}`}
              />
            ))}
            {nowPct != null && nowPct > 0 && nowPct < 100 && (
              <div className="marker marker-now" style={{ left: `${nowPct}%` }}>🟢 NOW</div>
            )}
            {plannedPct != null && plannedPct >= 0 && plannedPct <= 100 && (
              <div className="marker marker-plan" style={{ left: `${plannedPct}%` }}>🔴 计划</div>
            )}
          </div>
        </div>
      ))}

      <div className="legend">
        <span><i className="swatch swatch-normal" /> 普通授权</span>
        <span><i className="swatch swatch-exclusive" /> 排他合同</span>
        <span><i className="swatch swatch-active" /> 计划时刻命中</span>
      </div>
    </section>
  );
}
