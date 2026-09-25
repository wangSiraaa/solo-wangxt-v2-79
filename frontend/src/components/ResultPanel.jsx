import React from 'react';

function fmtInstant(t) {
  if (!t) return '-';
  return t.replace('T', ' ').replace(/Z$/, ' UTC');
}

function ContractTable({ contracts, title, tone }) {
  if (!contracts || contracts.length === 0) return null;
  return (
    <div className={`hit-table ${tone || ''}`}>
      <h4>{title}（{contracts.length}）</h4>
      <table>
        <thead>
          <tr>
            <th>合同号</th><th>被授权方</th><th>窗口 [start, end) UTC</th><th>语言版本</th><th>排他</th>
          </tr>
        </thead>
        <tbody>
          {contracts.map((c) => (
            <tr key={c.id || c.contractNo}>
              <td className="mono">{c.contractNo}</td>
              <td>{c.licensee}</td>
              <td className="mono small">[{fmtInstant(c.startAt)}, {c.endAt ? fmtInstant(c.endAt) : '开放结尾'})</td>
              <td>{(c.languageCodes || []).join('、')}</td>
              <td>{c.exclusive ? '是' : '否'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DecisionCard({ d, badge }) {
  if (!d) return null;
  return (
    <div className={`decision ${d.available ? 'ok' : 'blocked'}`}>
      <div className="decision-head">
        <span className={`badge ${d.available ? 'badge-ok' : 'badge-blocked'}`}>
          {d.available ? '✅ 可上架' : '⛔ 不可上架'}
        </span>
        <span className="decision-when">{badge}：{fmtInstant(d.evaluatedAt)}</span>
      </div>
      <div className="decision-meta">
        地区 <b>{d.regionCode}</b> · 渠道 <b>{d.channelCode}</b> · 语言版本 <b>{d.languageCode}</b>
      </div>

      {d.blockers.length > 0 && (
        <ul className="blockers">
          {d.blockers.map((b, i) => (
            <li key={i}>
              <span className="blocker-code">{b.code}</span>
              <span>{b.message}</span>
            </li>
          ))}
        </ul>
      )}

      {d.gap && (
        <div className="gap-box">
          授权空档区间（UTC）：<b>[{fmtInstant(d.gap.gapStart)}, {fmtInstant(d.gap.gapEnd)})</b><br />
          上一份：{d.gap.previousContractNo}｜下一份：{d.gap.nextContractNo}
        </div>
      )}

      <ContractTable contracts={d.matchedContracts} title="命中合同（含所申请语言版本）" tone="tone-match" />
      <ContractTable contracts={
        d.coveringContracts.filter(cc => !d.matchedContracts.some(mc => (mc.id || mc.contractNo) === (cc.id || cc.contractNo)))
      } title="同时刻生效但不满足语言版本的合同" tone="tone-cover" />
    </div>
  );
}

export default function ResultPanel({ result }) {
  if (!result) return null;
  return (
    <section className="card">
      <h2>判定结果</h2>
      <p className="hint">
        时间规则：{result.plannedStatus.timeRule}。计划状态由运营输入时间决定，当前状态由服务器 UTC 当前时刻决定，二者独立显示。
      </p>
      <div className="decision-grid">
        <div>
          <h3>📋 计划上架状态</h3>
          <DecisionCard d={result.plannedStatus} badge="计划时刻" />
        </div>
        <div>
          <h3>🕒 当前可用状态（独立）</h3>
          <DecisionCard d={result.currentStatus} badge="当前时刻" />
        </div>
      </div>
    </section>
  );
}
