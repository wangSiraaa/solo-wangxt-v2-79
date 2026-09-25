# 视频发行授权可用性判定系统

回答“某节目在某地区、某渠道、某时刻能否上架”的全栈演示系统。**仅使用虚构版权数据，不涉及真实播放。**

- **React**（`frontend/`）：授权日历 + 判定结果展示
- **Spring Boot**（`backend/`）：可用性判定 REST API
- **PostgreSQL**：合同、语言版本、排他约定

## 核心规则（统一 UTC）

- 所有时刻按 **UTC** 比较与展示。
- 授权/排他窗口均为 **`[valid_from, valid_to)`**：起点时刻**包含**、终点时刻**不包含**。
- 上架条件：存在覆盖计划时刻的合同（节目 × 语言版本 × 地区 × 渠道），**且**该时刻没有排他约定阻止本渠道。
- **排他冲突**：排他约定生效窗口内，独家渠道以外的渠道即使持有合同也判定不可上架。
- **缺少语言版本**：该地区该渠道有其他语言版本的合同、但无所请求语言版本的合同，判定不可上架并给出专门原因。
- 授权日历按**每日 00:00 UTC** 采样判定。

## 阻止原因码

| 代码 | 含义 |
| --- | --- |
| `NO_LICENSE` | 该组合下没有任何授权合同 |
| `MISSING_LANGUAGE_VERSION` | 缺少所请求语言版本的授权 |
| `NOT_YET_LICENSED` | 计划时刻早于全部授权窗口 |
| `LICENSE_EXPIRED` | 计划时刻晚于全部授权窗口 |
| `LICENSE_GAP` | 计划时刻落在两段授权窗口之间的空档 |
| `EXCLUSIVITY_CONFLICT` | 排他约定生效，另一渠道持有独家 |

## API

- `GET /api/catalog` — 节目、语言版本、地区/渠道代码
- `GET /api/rights?programId=&region=` — 合同与排他约定
- `POST /api/availability/check` — 判定，请求体 `{programId, languageVersionId, region, channel, plannedAt}`（ISO-8601 UTC）；响应中 `planned`（计划时刻）与 `current`（当前时刻）**分开返回**
- `GET /api/availability/calendar?programId=&languageVersionId=&region=&month=yyyy-MM` — 按渠道 × 日的日历状态

## 运行

```bash
# 1. PostgreSQL（localhost:5432，库 licensing / 用户 licensing）
#    本仓库演示环境使用用户态 PostgreSQL 15，见“环境说明”一节

# 2. 后端（首次启动自动建表并写入虚构种子数据）
cd backend && ./mvnw spring-boot:run        # 或 mvn spring-boot:run

# 3. 前端（Vite 代理 /api → localhost:8080）
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## 测试

```bash
cd backend && mvn test
```

`AvailabilityServiceTest` 覆盖：窗口起点包含 / 终点不包含、授权空档、同地区不同渠道（排他窗口内独家方可用、他方被阻止；排他起止边界）、缺少语言版本、尚未生效、已过期、计划状态与当前状态分离。
`AvailabilityApiTest` 验证 API 同时返回计划/当前两份判定及中文原因文案。

## 虚构演示数据（全部 UTC）

节目 **星河边界**（CN 地区）：

| 内容 | 窗口 |
| --- | --- |
| OTT-A · 英语原声 | 2026-01-01 ~ 2027-01-01 |
| OTT-A · 中文配音 | 2026-01-01 ~ 2026-07-01 **空档** 2026-09-01 ~ 2027-01-01 |
| OTT-B · 英语原声 / 中文字幕 | 2026-03-01 ~ 2026-09-01 |
| 排他：CN 由 OTT-A 独家 | 2026-04-01 ~ 2026-07-01 |

节目 **午夜面包房**：TV-LINEAR 韩语原声 2026-02-01 ~ 2026-05-01（已过期）；OTT-A 中文字幕 2026-10-01 ~ 2027-03-01（尚未生效）。

典型验证场景：

1. **同地区不同渠道**：2026-05-01 查 CN/OTT-A 英语原声 → 可上架；同一时刻 CN/OTT-B → 排他冲突阻止。
2. **授权空档**：2026-07-15 查 CN/OTT-A 中文配音 → 空档阻止，解释列出相邻两段窗口。
3. **缺少语言版本**：CN/OTT-A 中文字幕 → 缺少语言版本授权（OTT-B 有、OTT-A 无）。
4. **边界**：2026-07-01T00:00:00Z 中文配音不可用（终点不含）；2026-09-01T00:00:00Z 可用（起点含）。

## 环境说明（本演示机）

无 root 权限的 aarch64 环境，工具均为用户态安装：

- JDK 21（Temurin aarch64）与 Maven 3.9 解压于 `tools/`
- PostgreSQL 15.18 由 Debian arm64 软件包解压于 `tools/pg-debs/`，数据目录 `pgdata/`
- 启动脚本：`./start-all.sh`（数据库 + 后端 + 前端）
