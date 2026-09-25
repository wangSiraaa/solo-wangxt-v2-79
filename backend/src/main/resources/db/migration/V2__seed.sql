-- 虚构版权数据：节目《星海追猎者》(Starhunt Saga)，所有公司/合同号均为虚构。
-- 时间字面量按 UTC 写入（数据库 timezone=UTC，应用亦强制 UTC）。
-- 统一规则：[start_at, end_at) —— 含开始时刻，不含结束时刻；end_at 为 NULL 表示开放结尾。

INSERT INTO program (id, code, title)
VALUES (1, 'SHOW-STARHUNT', '星海追猎者（虚构）');

-- ============ 中国香港 CN-HK ============
-- WEB：Aurora Stream，全年，含繁中/英文
INSERT INTO contract (id, program_id, contract_no, licensee, region_code, channel_code,
                      start_at, end_at, exclusive, note)
VALUES (101, 1, 'C-AURORA-WEB-HK', 'Aurora Stream Ltd（虚构）', 'CN-HK', 'WEB',
        TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2027-01-01 00:00:00', FALSE,
        '香港 WEB 年度授权');
INSERT INTO contract_language (contract_id, language_code) VALUES
    (101, 'zh-Hant'), (101, 'en');

-- MOBILE：同地区不同渠道，仅授繁中（用于缺少语言版本案例：申请简中应被阻止）
INSERT INTO contract (id, program_id, contract_no, licensee, region_code, channel_code,
                      start_at, end_at, exclusive, note)
VALUES (102, 1, 'C-AURORA-MOBILE-HK', 'Aurora Stream Ltd（虚构）', 'CN-HK', 'MOBILE',
        TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2027-01-01 00:00:00', FALSE,
        '香港 MOBILE 年度授权，仅繁体中文版');
INSERT INTO contract_language (contract_id, language_code) VALUES
    (102, 'zh-Hant');

-- OTT：同一被授权方两份合同之间存在授权空档
--   合同一：2026-01-01T00:00Z（含） ~ 2026-06-30T16:00Z（不含）
--   空档：  2026-06-30T16:00Z  ~ 2026-07-15T16:00Z
--   合同二：2026-07-15T16:00Z（含） ~ 2027-01-01T00:00Z（不含）
INSERT INTO contract (id, program_id, contract_no, licensee, region_code, channel_code,
                      start_at, end_at, exclusive, note)
VALUES (103, 1, 'C-BOREALIS-OTT-HK-P1', 'Borealis Media（虚构）', 'CN-HK', 'OTT',
        TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-06-30 16:00:00', FALSE,
        '香港 OTT 授权（上半年，结束时刻不含）'),
       (104, 1, 'C-BOREALIS-OTT-HK-P2', 'Borealis Media（虚构）', 'CN-HK', 'OTT',
        TIMESTAMP '2026-07-15 16:00:00', TIMESTAMP '2027-01-01 00:00:00', FALSE,
        '香港 OTT 授权（下半年，开始时刻含）');
INSERT INTO contract_language (contract_id, language_code) VALUES
    (103, 'zh-Hant'), (103, 'en'),
    (104, 'zh-Hant'), (104, 'en');

-- ============ 新加坡 SG ============
-- OTT：Meridian 独家窗口
INSERT INTO contract (id, program_id, contract_no, licensee, region_code, channel_code,
                      start_at, end_at, exclusive, note)
VALUES (201, 1, 'C-MERIDIAN-SG-OTT-EXCL', 'Meridian Exclusive Pte（虚构）', 'SG', 'OTT',
        TIMESTAMP '2026-03-01 00:00:00', TIMESTAMP '2026-10-01 00:00:00', TRUE,
        '新加坡 OTT 独家授权窗口');
INSERT INTO contract_language (contract_id, language_code) VALUES
    (201, 'en'), (201, 'zh-Hans');
INSERT INTO exclusivity_agreement (contract_id, region_code, channel_code, start_at, end_at, note)
VALUES (201, 'SG', 'OTT',
        TIMESTAMP '2026-03-01 00:00:00', TIMESTAMP '2026-10-01 00:00:00',
        '新加坡 OTT 渠道独家约定（虚构）');

-- OTT：另一家 Nova 的非独家合同，窗口落在独家期内 → 排他冲突
INSERT INTO contract (id, program_id, contract_no, licensee, region_code, channel_code,
                      start_at, end_at, exclusive, note)
VALUES (202, 1, 'C-NOVA-SG-OTT', 'Nova Channels（虚构）', 'SG', 'OTT',
        TIMESTAMP '2026-06-01 00:00:00', TIMESTAMP '2026-09-01 00:00:00', FALSE,
        '与独家窗口重叠，构成排他冲突');
INSERT INTO contract_language (contract_id, language_code) VALUES
    (202, 'en');

-- WEB：同地区不同渠道，不受 OTT 独家影响
INSERT INTO contract (id, program_id, contract_no, licensee, region_code, channel_code,
                      start_at, end_at, exclusive, note)
VALUES (203, 1, 'C-ACME-SG-WEB', 'Acme Web SG（虚构）', 'SG', 'WEB',
        TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2027-01-01 00:00:00', FALSE,
        '新加坡 WEB 授权，不受 OTT 独家约定影响');
INSERT INTO contract_language (contract_id, language_code) VALUES
    (203, 'en'), (203, 'zh-Hans');

-- 重置序列，避免显式插入后再自增产生冲突
SELECT setval(pg_get_serial_sequence('program', 'id'),  (SELECT MAX(id) FROM program));
SELECT setval(pg_get_serial_sequence('contract', 'id'), (SELECT MAX(id) FROM contract));
