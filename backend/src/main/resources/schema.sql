-- 授权域模型（全部为虚构数据）
-- 时间统一使用 timestamptz，比较一律按 UTC。
-- 窗口语义：[valid_from, valid_to) —— 起始包含、结束不包含。

DROP TABLE IF EXISTS exclusivity;
DROP TABLE IF EXISTS contract;
DROP TABLE IF EXISTS language_version;
DROP TABLE IF EXISTS program;

CREATE TABLE program (
    id          BIGSERIAL PRIMARY KEY,
    code        TEXT NOT NULL UNIQUE,
    title       TEXT NOT NULL,
    genre       TEXT NOT NULL
);

CREATE TABLE language_version (
    id          BIGSERIAL PRIMARY KEY,
    program_id  BIGINT NOT NULL REFERENCES program (id),
    code        TEXT NOT NULL,           -- 例如 ORIG-EN / DUB-ZH / SUB-ZH
    label       TEXT NOT NULL,           -- 展示名称
    UNIQUE (program_id, code)
);

CREATE TABLE contract (
    id                  BIGSERIAL PRIMARY KEY,
    contract_ref        TEXT NOT NULL UNIQUE,   -- 虚构合同编号
    program_id          BIGINT NOT NULL REFERENCES program (id),
    language_version_id BIGINT NOT NULL REFERENCES language_version (id),
    region_code         TEXT NOT NULL,          -- 例如 CN / JP / SEA
    channel_code        TEXT NOT NULL,          -- 例如 OTT-A / OTT-B / TV-LINEAR
    valid_from          TIMESTAMPTZ NOT NULL,
    valid_to            TIMESTAMPTZ NOT NULL,
    CHECK (valid_from < valid_to)
);

CREATE INDEX idx_contract_lookup ON contract (program_id, language_version_id, region_code, channel_code);
CREATE INDEX idx_contract_window ON contract (valid_from, valid_to);

-- 排他约定：某节目在某地区、某时间窗口内由 channel_code 独家持有，
-- 窗口内其他渠道即使有合同也阻止上架。
CREATE TABLE exclusivity (
    id           BIGSERIAL PRIMARY KEY,
    program_id   BIGINT NOT NULL REFERENCES program (id),
    region_code  TEXT NOT NULL,
    channel_code TEXT NOT NULL,
    valid_from   TIMESTAMPTZ NOT NULL,
    valid_to     TIMESTAMPTZ NOT NULL,
    note         TEXT NOT NULL,
    CHECK (valid_from < valid_to)
);

CREATE INDEX idx_exclusivity_lookup ON exclusivity (program_id, region_code);
