# 视频发行授权可用性判定（全栈 Demo）

运营输入节目、地区、渠道、语言版本与计划上架时刻（UTC），系统给出**可上架 / 不可上架**结论、
命中的合同与具体阻止原因，并展示授权日历。**全部使用虚构版权数据，不处理任何真实播放。**

## 技术栈

| 层 | 技术 | 说明 |
| --- | --- | --- |
| 前端 | React 18 + Vite | 授权日历时间轴、判定表单、计划/当前状态分开显示 |
| 后端 | Spring Boot 3.3 (Java 17) | 可用性判定 REST API；判定核心为纯函数引擎 |
| 数据库 | PostgreSQL 16（真实实例，非 H2） | 合同、语言版本、排他约定；Flyway 建表与种子数据 |

## 判定规则

1. 授权四要素：**地区 × 渠道 × 语言版本 × 时间窗**，全部匹配才可上架。
2. 时间统一 **UTC**；窗口左闭右开 **`[start_at, end_at)`**——含开始时刻、不含结束时刻；
   `end_at = NULL` 表示开放结尾。
   - 判定时刻 `t == start_at` → 有效；`t == end_at` → 无效（已到期）。
3. 阻止原因：
   - `NO_VALID_CONTRACT`：该时刻无生效合同（附带前后合同，构成空档时给出 `gap` 区间解释）。
   - `MISSING_LANGUAGE_VERSION`：有生效合同但均未授予所申请语言版本（生效合同仍返回展示）。
   - `EXCLUSIVITY_CONFLICT`：同地区同渠道存在**不同被授权方**的有效排他约定。
   - `EXCLUSIVITY_OVERLAP`：多个不同权利方的排他窗口互相重叠。
4. 排他只在**同地区 + 同渠道**内生效；同地区不同渠道互不影响。
5. **计划状态**（按运营输入时间）与**当前状态**（服务器当前 UTC 时刻）独立计算、分开显示。

## 数据模型（Flyway）

- `program`：虚构节目（《星海追猎者》SHOW-STARHUNT）
- `contract`：合同窗口（region/channel/start_at/end_at/exclusive）
- `contract_language`：每份合同授予的语言版本（zh-Hans / zh-Hant / en）
- `exclusivity_agreement`：排他约定（指向独家合同，携带地区/渠道/窗口）

虚构种子数据刻意覆盖四类场景：

| 场景 | 数据 |
| --- | --- |
| 同地区不同渠道 | CN-HK：WEB / MOBILE / OTT 独立合同；SG：OTT 独家不影响 WEB |
| 授权空档 | CN-HK OTT：P1 止于 `2026-06-30T16:00Z`，P2 始于 `2026-07-15T16:00Z` |
| 缺语言版本 | CN-HK MOBILE 仅授 zh-Hant，申请 zh-Hans 被阻止 |
| 排他冲突 | SG OTT：Meridian 独家窗口 3/1–10/1，Nova 非独家合同 6/1–9/1 落入其中 |

## 运行方式

```bash
# 1) PostgreSQL（embedded-postgres，真实 PG16 二进制，UTC）
cd pg && npm i && node start-pg.mjs            # 监听 127.0.0.1:5432, db=licensing

# 2) Spring Boot（自动执行 Flyway 迁移与种子数据）
cd backend
mvn spring-boot:run                             # http://localhost:8080

# 3) React
cd frontend && npm i && npm run dev             # http://127.0.0.1:5173
```

## API

- `POST /api/availability`
  ```json
  {
    "programCode": "SHOW-STARHUNT",
    "regionCode": "CN-HK",
    "channelCode": "OTT",
    "languageCode": "zh-Hant",
    "plannedAt": "2026-06-30T16:00:00Z"
  }
  ```
  返回 `plannedStatus`（计划状态）与 `currentStatus`（当前状态），各自含
  `available / blockers / matchedContracts / coveringContracts / gap / timeRule`。
- `GET /api/programs`、`GET /api/contracts?programCode=...`、`GET /api/exclusivities`：日历数据。

## 边界验证结果（已通过 JUnit 15 例 + 真实接口逐条验证）

| 用例（UTC） | 期望 | 实际 |
| --- | --- | --- |
| WEB `2026-01-01T00:00`（=开始，含） | 可用 | ✅ |
| WEB `2026-12-31T23:59:59` | 可用 | ✅ |
| WEB `2027-01-01T00:00`（=结束，不含） | 阻止 NO_VALID_CONTRACT | ✅ |
| OTT `2026-06-30T15:59:59`（空档前 1 秒） | 可用，命中 P1 | ✅ |
| OTT `2026-06-30T16:00`（空档起点） | 阻止，gap=[16:00, 07-15T16:00) | ✅ |
| OTT `2026-07-08T00:00`（空档正中） | 阻止，给出前后合同 | ✅ |
| OTT `2026-07-15T16:00`（=P2 开始，含） | 可用，命中 P2 | ✅ |
| MOBILE 申请 zh-Hans（只有 zh-Hant） | 阻止 MISSING_LANGUAGE_VERSION，仍展示生效合同 | ✅ |
| SG OTT `2026-07-01`（独家+非独家重叠） | 阻止 EXCLUSIVITY_CONFLICT，点出双方合同 | ✅ |
| SG OTT `2026-05-01`（仅独家方） | 可用 | ✅ |
| SG WEB `2026-07-01`（同地区不同渠道） | 可用，不受 OTT 独家影响 | ✅ |
| JP MOBILE（无合同） | 阻止，提示无授权记录 | ✅ |

前端提供以上用例的一键填充按钮，判定结论、阻止原因、命中合同、空档区间与日历标记联动。
