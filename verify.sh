#!/usr/bin/env bash
# 端到端验证：边界时间 + 同地区不同渠道 + 授权空档 + 缺少语言版本
# 前提：后端已在 localhost:8080 运行
set -euo pipefail

BASE=http://localhost:8080/api

check() { # programId lvId region channel plannedAt 说明
  local body
  body=$(printf '{"programId":%s,"languageVersionId":%s,"region":"%s","channel":"%s","plannedAt":"%s"}' "$1" "$2" "$3" "$4" "$5")
  echo "--- $6"
  echo "    请求: $3/$4 lv=$2 planned=$5"
  curl -s -X POST "$BASE/availability/check" -H 'Content-Type: application/json' -d "$body" \
    | python3 -c '
import json,sys
r=json.load(sys.stdin)
for k in ("planned","current"):
    d=r[k]
    reasons=",".join(x["code"] for x in d["reasons"]) or "-"
    hit=d["matchedContract"]["contractRef"] if d["matchedContract"] else "-"
    msg=";".join(x["message"] for x in d["reasons"])
    st=d["status"]
    print(f"    {k:8s} {st:9s} 命中合同={hit:14s} 原因={reasons} {msg}")
'
}

# 语言版本：星河边界 ORIG-EN=1 DUB-ZH=2 SUB-ZH=3 DUB-JA=4；午夜面包房 ORIG-KO=5 SUB-ZH=6
echo "== 边界时间（窗口 [from,to)：起点含、终点不含）"
check 1 2 CN OTT-A 2026-01-01T00:00:00Z "DUB-ZH 窗口起点 t==validFrom → 应 AVAILABLE"
check 1 2 CN OTT-A 2026-07-01T00:00:00Z "DUB-ZH 第一段终点 t==validTo → 应 BLOCKED/LICENSE_GAP"
check 1 2 CN OTT-A 2026-09-01T00:00:00Z "DUB-ZH 第二段起点 → 应 AVAILABLE"

echo "== 授权空档"
check 1 2 CN OTT-A 2026-07-15T12:00:00Z "7~8月空档 → 应 BLOCKED/LICENSE_GAP"

echo "== 同地区不同渠道 + 排他"
check 1 1 CN OTT-A 2026-05-01T00:00:00Z "排他窗口内独家方 OTT-A → 应 AVAILABLE"
check 1 1 CN OTT-B 2026-05-01T00:00:00Z "排他窗口内 OTT-B 有合同 → 应 BLOCKED/EXCLUSIVITY_CONFLICT"
check 1 1 CN OTT-B 2026-04-01T00:00:00Z "排他起点 t==validFrom → 应 BLOCKED/EXCLUSIVITY_CONFLICT"
check 1 1 CN OTT-B 2026-07-01T00:00:00Z "排他终点 t==validTo → 应 AVAILABLE"

echo "== 缺少语言版本 / 无授权"
check 1 3 CN OTT-A 2026-03-01T00:00:00Z "OTT-A 无中文字幕授权 → 应 BLOCKED/MISSING_LANGUAGE_VERSION"
check 1 1 SEA OTT-A 2026-03-01T00:00:00Z "SEA 无任何合同 → 应 BLOCKED/NO_LICENSE"

echo "== 尚未生效 / 已过期"
check 2 6 CN OTT-A 2026-09-25T00:00:00Z "窗口 10-01 才开始 → 应 BLOCKED/NOT_YET_LICENSED"
check 2 5 CN TV-LINEAR 2026-09-25T00:00:00Z "窗口 05-01 已结束 → 应 BLOCKED/LICENSE_EXPIRED"

echo "== 日历（2026-07，CN，DUB-ZH：应见 OTT-A 全月空档、OTT-B 无字幕合同）"
curl -s "$BASE/availability/calendar?programId=1&languageVersionId=2&region=CN&month=2026-07" \
  | python3 -c '
import json,sys
r=json.load(sys.stdin)
for ch in r["channels"]:
    marks="".join("A" if d["status"]=="AVAILABLE" else ("G" if d["reason"]=="LICENSE_GAP" else ("X" if d["reason"]=="EXCLUSIVITY_CONFLICT" else ".")) for d in ch["days"])
    name=ch["channel"]
    print(f"    {name:10s} {marks}  (A=可用 G=空档 X=排他 .=其他不可用)")
'
echo "全部验证完成。"
