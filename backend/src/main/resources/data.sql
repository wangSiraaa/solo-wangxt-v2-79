-- 虚构版权种子数据（仅用于演示，全部时间 UTC，窗口 [from, to) 起始包含、结束不包含）

INSERT INTO program (code, title, genre) VALUES
  ('SF-001', '星河边界', '科幻剧集'),
  ('DR-002', '午夜面包房', '剧情剧集');

-- 语言版本
INSERT INTO language_version (program_id, code, label) VALUES
  ((SELECT id FROM program WHERE code = 'SF-001'), 'ORIG-EN', '英语原声'),
  ((SELECT id FROM program WHERE code = 'SF-001'), 'DUB-ZH',  '中文配音'),
  ((SELECT id FROM program WHERE code = 'SF-001'), 'SUB-ZH',  '中文字幕'),
  ((SELECT id FROM program WHERE code = 'SF-001'), 'DUB-JA',  '日语配音'),
  ((SELECT id FROM program WHERE code = 'DR-002'), 'ORIG-KO', '韩语原声'),
  ((SELECT id FROM program WHERE code = 'DR-002'), 'SUB-ZH',  '中文字幕');

-- 合同
-- 星河边界 / CN / OTT-A：英语原声全年
INSERT INTO contract (contract_ref, program_id, language_version_id, region_code, channel_code, valid_from, valid_to) VALUES
  ('LIC-2026-001',
   (SELECT id FROM program WHERE code = 'SF-001'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'SF-001' AND lv.code = 'ORIG-EN'),
   'CN', 'OTT-A', '2026-01-01T00:00:00Z', '2027-01-01T00:00:00Z');

-- 星河边界 / CN / OTT-A：中文配音两段窗口，中间留出 2026-07-01 ~ 2026-09-01 授权空档
INSERT INTO contract (contract_ref, program_id, language_version_id, region_code, channel_code, valid_from, valid_to) VALUES
  ('LIC-2026-002',
   (SELECT id FROM program WHERE code = 'SF-001'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'SF-001' AND lv.code = 'DUB-ZH'),
   'CN', 'OTT-A', '2026-01-01T00:00:00Z', '2026-07-01T00:00:00Z'),
  ('LIC-2026-003',
   (SELECT id FROM program WHERE code = 'SF-001'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'SF-001' AND lv.code = 'DUB-ZH'),
   'CN', 'OTT-A', '2026-09-01T00:00:00Z', '2027-01-01T00:00:00Z');

-- 星河边界 / CN / OTT-B：同地区不同渠道（英语原声 + 中文字幕）
INSERT INTO contract (contract_ref, program_id, language_version_id, region_code, channel_code, valid_from, valid_to) VALUES
  ('LIC-2026-004',
   (SELECT id FROM program WHERE code = 'SF-001'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'SF-001' AND lv.code = 'ORIG-EN'),
   'CN', 'OTT-B', '2026-03-01T00:00:00Z', '2026-09-01T00:00:00Z'),
  ('LIC-2026-006',
   (SELECT id FROM program WHERE code = 'SF-001'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'SF-001' AND lv.code = 'SUB-ZH'),
   'CN', 'OTT-B', '2026-03-01T00:00:00Z', '2026-09-01T00:00:00Z');

-- 星河边界 / JP / OTT-A：日语配音
INSERT INTO contract (contract_ref, program_id, language_version_id, region_code, channel_code, valid_from, valid_to) VALUES
  ('LIC-2026-005',
   (SELECT id FROM program WHERE code = 'SF-001'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'SF-001' AND lv.code = 'DUB-JA'),
   'JP', 'OTT-A', '2026-01-01T00:00:00Z', '2027-01-01T00:00:00Z');

-- 午夜面包房 / CN / TV-LINEAR：韩语原声（已过期窗口，便于演示 LICENSE_EXPIRED）
INSERT INTO contract (contract_ref, program_id, language_version_id, region_code, channel_code, valid_from, valid_to) VALUES
  ('LIC-2026-007',
   (SELECT id FROM program WHERE code = 'DR-002'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'DR-002' AND lv.code = 'ORIG-KO'),
   'CN', 'TV-LINEAR', '2026-02-01T00:00:00Z', '2026-05-01T00:00:00Z');

-- 午夜面包房 / CN / OTT-A：中文字幕（未来窗口，便于演示 NOT_YET_LICENSED）
INSERT INTO contract (contract_ref, program_id, language_version_id, region_code, channel_code, valid_from, valid_to) VALUES
  ('LIC-2026-008',
   (SELECT id FROM program WHERE code = 'DR-002'),
   (SELECT lv.id FROM language_version lv JOIN program p ON p.id = lv.program_id WHERE p.code = 'DR-002' AND lv.code = 'SUB-ZH'),
   'CN', 'OTT-A', '2026-10-01T00:00:00Z', '2027-03-01T00:00:00Z');

-- 排他约定：星河边界在 CN 地区 2026-04-01 ~ 2026-07-01 由 OTT-A 独播
INSERT INTO exclusivity (program_id, region_code, channel_code, valid_from, valid_to, note) VALUES
  ((SELECT id FROM program WHERE code = 'SF-001'),
   'CN', 'OTT-A', '2026-04-01T00:00:00Z', '2026-07-01T00:00:00Z',
   'OTT-A 独播窗口：窗口内 CN 地区其他渠道（含 OTT-B）即使持有合同也阻止上架');
