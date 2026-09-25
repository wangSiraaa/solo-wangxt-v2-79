package com.example.licensing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 边界与解释验证（纯引擎测试，不依赖数据库）：
 * 同地区不同渠道、授权空档、缺少语言版本、排他冲突。
 */
class AvailabilityEngineTest {

    // ---- 虚构合同数据（与 V2__seed.sql 对齐）----
    private static final AvailabilityEngine.License HK_WEB = new AvailabilityEngine.License(
            101, "C-AURORA-WEB-HK", "Aurora Stream Ltd（虚构）",
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
            false, List.of("en", "zh-Hant"), "香港 WEB 年度授权");

    private static final AvailabilityEngine.License HK_MOBILE = new AvailabilityEngine.License(
            102, "C-AURORA-MOBILE-HK", "Aurora Stream Ltd（虚构）",
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
            false, List.of("zh-Hant"), "仅繁体中文版");

    private static final AvailabilityEngine.License HK_OTT_P1 = new AvailabilityEngine.License(
            103, "C-BOREALIS-OTT-HK-P1", "Borealis Media（虚构）",
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-06-30T16:00:00Z"),
            false, List.of("en", "zh-Hant"), "上半年");

    private static final AvailabilityEngine.License HK_OTT_P2 = new AvailabilityEngine.License(
            104, "C-BOREALIS-OTT-HK-P2", "Borealis Media（虚构）",
            Instant.parse("2026-07-15T16:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
            false, List.of("en", "zh-Hant"), "下半年");

    private static final AvailabilityEngine.License SG_OTT_EXCL = new AvailabilityEngine.License(
            201, "C-MERIDIAN-SG-OTT-EXCL", "Meridian Exclusive Pte（虚构）",
            Instant.parse("2026-03-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"),
            true, List.of("en", "zh-Hans"), "独家");

    private static final AvailabilityEngine.License SG_OTT_NOVA = new AvailabilityEngine.License(
            202, "C-NOVA-SG-OTT", "Nova Channels（虚构）",
            Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-09-01T00:00:00Z"),
            false, List.of("en"), "非独家，落在独家窗口内");

    private static final AvailabilityEngine.License SG_WEB = new AvailabilityEngine.License(
            203, "C-ACME-SG-WEB", "Acme Web SG（虚构）",
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
            false, List.of("en", "zh-Hans"), "WEB 不受 OTT 独家影响");

    private static final List<AvailabilityEngine.ExclusiveRight> SG_OTT_RIGHTS = List.of(
            new AvailabilityEngine.ExclusiveRight(
                    201, "Meridian Exclusive Pte（虚构）",
                    Instant.parse("2026-03-01T00:00:00Z"),
                    Instant.parse("2026-10-01T00:00:00Z"), "新加坡 OTT 独家"));

    private AvailabilityEngine.Input input(Instant at, String region, String channel,
                                           String lang,
                                           List<AvailabilityEngine.License> licenses,
                                           List<AvailabilityEngine.ExclusiveRight> rights) {
        return new AvailabilityEngine.Input(at, region, channel, lang, licenses, rights);
    }

    @Nested
    @DisplayName("案例1：同地区不同渠道 + 左闭右开边界")
    class DifferentChannel {

        @Test
        @DisplayName("CN-HK WEB 在 2026-01-01T00:00Z（=开始时刻，含）→ 可用")
        void webAtExactStart() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-01-01T00:00:00Z"),
                    "CN-HK", "WEB", "zh-Hant", List.of(HK_WEB), List.of()));
            assertTrue(d.available());
            assertTrue(d.blockers().isEmpty());
            assertEquals(List.of("C-AURORA-WEB-HK"),
                    d.matchedContracts().stream().map(AvailabilityEngine.ContractHit::contractNo).toList());
        }

        @Test
        @DisplayName("CN-HK WEB 在 2026-12-31T23:59:59Z（结束前 1 秒）→ 可用")
        void webOneSecondBeforeEnd() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-12-31T23:59:59Z"),
                    "CN-HK", "WEB", "en", List.of(HK_WEB), List.of()));
            assertTrue(d.available());
        }

        @Test
        @DisplayName("CN-HK WEB 在 2027-01-01T00:00Z（=结束时刻，不含）→ 阻止")
        void webAtExactEnd() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2027-01-01T00:00:00Z"),
                    "CN-HK", "WEB", "zh-Hant", List.of(HK_WEB), List.of()));
            assertFalse(d.available());
            assertEquals("NO_VALID_CONTRACT", d.blockers().get(0).code());
        }

        @Test
        @DisplayName("同地区不同渠道：HK MOBILE 申请繁中可用，且不会被 WEB 合同影响")
        void mobileIndependentOfWeb() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-05-01T08:00:00Z"),
                    "CN-HK", "MOBILE", "zh-Hant", List.of(HK_MOBILE), List.of()));
            assertTrue(d.available());
            assertEquals(1, d.matchedContracts().size());
            assertEquals("C-AURORA-MOBILE-HK", d.matchedContracts().get(0).contractNo());
        }
    }

    @Nested
    @DisplayName("案例2：授权空档与边界（CN-HK OTT）")
    class LicensingGap {

        private final List<AvailabilityEngine.License> ott = List.of(HK_OTT_P1, HK_OTT_P2);

        @Test
        @DisplayName("空档开始边界 2026-06-30T16:00Z（P1 结束时刻，不含）→ 阻止且给出空档解释")
        void atGapStart() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-06-30T16:00:00Z"),
                    "CN-HK", "OTT", "zh-Hant", ott, List.of()));
            assertFalse(d.available());
            assertEquals("NO_VALID_CONTRACT", d.blockers().get(0).code());
            assertNotNull(d.gap());
            assertEquals("C-BOREALIS-OTT-HK-P1", d.gap().previousContractNo());
            assertEquals("C-BOREALIS-OTT-HK-P2", d.gap().nextContractNo());
            assertEquals(Instant.parse("2026-06-30T16:00:00Z"), d.gap().gapStart());
            assertEquals(Instant.parse("2026-07-15T16:00:00Z"), d.gap().gapEnd());
            assertTrue(d.blockers().get(0).message().contains("授权空档"));
        }

        @Test
        @DisplayName("空档前一刻 2026-06-30T15:59:59Z（P1 内）→ 可用")
        void justBeforeGap() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-06-30T15:59:59Z"),
                    "CN-HK", "OTT", "en", ott, List.of()));
            assertTrue(d.available());
            assertEquals("C-BOREALIS-OTT-HK-P1", d.matchedContracts().get(0).contractNo());
        }

        @Test
        @DisplayName("空档正中 2026-07-08T00:00Z → 阻止")
        void middleOfGap() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-07-08T00:00:00Z"),
                    "CN-HK", "OTT", "en", ott, List.of()));
            assertFalse(d.available());
            assertEquals("NO_VALID_CONTRACT", d.blockers().get(0).code());
            assertNotNull(d.gap());
        }

        @Test
        @DisplayName("空档结束边界 2026-07-15T16:00Z（P2 开始时刻，含）→ 可用")
        void atGapEnd() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-07-15T16:00:00Z"),
                    "CN-HK", "OTT", "zh-Hant", ott, List.of()));
            assertTrue(d.available());
            assertEquals("C-BOREALIS-OTT-HK-P2", d.matchedContracts().get(0).contractNo());
        }
    }

    @Nested
    @DisplayName("案例3：缺少语言版本（CN-HK MOBILE 只有繁中）")
    class MissingLanguage {

        @Test
        @DisplayName("申请简中 zh-Hans 但合同只授 zh-Hant → 阻止，原因 MISSING_LANGUAGE_VERSION")
        void missingSimplifiedChinese() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-05-01T08:00:00Z"),
                    "CN-HK", "MOBILE", "zh-Hans", List.of(HK_MOBILE), List.of()));
            assertFalse(d.available());
            assertEquals("MISSING_LANGUAGE_VERSION", d.blockers().get(0).code());
            assertTrue(d.blockers().get(0).message().contains("zh-Hans"));
            // 生效合同作为“命中/覆盖合同”仍需展示，便于运营核对
            assertEquals(1, d.coveringContracts().size());
            assertEquals("C-AURORA-MOBILE-HK", d.coveringContracts().get(0).contractNo());
            // 但 matchedContracts（含语言版本的命中）为空
            assertTrue(d.matchedContracts().isEmpty());
        }

        @Test
        @DisplayName("申请繁中 zh-Hant → 可用")
        void grantedLanguage() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-05-01T08:00:00Z"),
                    "CN-HK", "MOBILE", "zh-Hant", List.of(HK_MOBILE), List.of()));
            assertTrue(d.available());
        }
    }

    @Nested
    @DisplayName("案例4：排他冲突（SG OTT）与同地区不同渠道不受影响")
    class Exclusivity {

        private final List<AvailabilityEngine.License> sgOtt = List.of(SG_OTT_EXCL, SG_OTT_NOVA);

        @Test
        @DisplayName("重叠期 2026-07-01T00:00Z：Nova 申请英文 → 被 Meridian 独家阻止")
        void novaBlockedByMeridian() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-07-01T00:00:00Z"),
                    "SG", "OTT", "en", sgOtt, SG_OTT_RIGHTS));
            assertFalse(d.available());
            assertEquals("EXCLUSIVITY_CONFLICT", d.blockers().get(0).code());
            assertTrue(d.blockers().get(0).message().contains("Meridian"));
            assertTrue(d.blockers().get(0).message().contains("Nova"));
            // 独家方自身的合同也覆盖该时刻，但冲突必须阻止
            assertEquals(2, d.coveringContracts().size());
        }

        @Test
        @DisplayName("非重叠期 2026-05-01T00:00Z：只有 Meridian 独家方 → 可用")
        void exclusiveHolderOnly() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-05-01T00:00:00Z"),
                    "SG", "OTT", "en", sgOtt, SG_OTT_RIGHTS));
            assertTrue(d.available());
            assertEquals(1, d.matchedContracts().size());
            assertEquals("C-MERIDIAN-SG-OTT-EXCL", d.matchedContracts().get(0).contractNo());
        }

        @Test
        @DisplayName("同地区不同渠道：SG WEB 不受 OTT 排他约定影响 → 可用")
        void webUnaffectedByOttExclusivity() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-07-01T00:00:00Z"),
                    "SG", "WEB", "en", List.of(SG_WEB), List.of()));
            assertTrue(d.available());
        }

        @Test
        @DisplayName("独家窗口结束时刻 2026-10-01T00:00Z（不含）：Meridian 自身也已失效")
        void atExclusiveEnd() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-10-01T00:00:00Z"),
                    "SG", "OTT", "en", sgOtt, SG_OTT_RIGHTS));
            assertFalse(d.available());
            assertEquals("NO_VALID_CONTRACT", d.blockers().get(0).code());
        }
    }

    @Nested
    @DisplayName("补充：无任何合同的地区")
    class NoContractAtAll {

        @Test
        @DisplayName("JP MOBILE 没有合同 → 阻止并说明无授权记录")
        void japanNoContract() {
            var d = AvailabilityEngine.evaluate(input(
                    Instant.parse("2026-05-01T00:00:00Z"),
                    "JP", "MOBILE", "ja", List.of(), List.of()));
            assertFalse(d.available());
            assertEquals("NO_VALID_CONTRACT", d.blockers().get(0).code());
            assertNull(d.gap());
            assertTrue(d.blockers().get(0).message().contains("没有任何授权合同"));
        }
    }
}
