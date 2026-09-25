package com.example.licensing.service;

import com.example.licensing.model.Contract;
import com.example.licensing.model.Exclusivity;
import com.example.licensing.repo.ContractRepository;
import com.example.licensing.repo.ExclusivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 边界时间与解释结果验证。
 *
 * 虚构数据（与 data.sql 对齐，全部 UTC，窗口 [from, to)）：
 *  节目1 星河边界：ORIG-EN=1, DUB-ZH=2, SUB-ZH=3, DUB-JA=4
 *   CN/OTT-A: ORIG-EN [01-01, 次年01-01)；DUB-ZH [01-01, 07-01) 与 [09-01, 次年01-01)（7~8月空档）
 *   CN/OTT-B: ORIG-EN [03-01, 09-01)；SUB-ZH [03-01, 09-01)
 *   JP/OTT-A: DUB-JA [01-01, 次年01-01)
 *   排他：CN 地区 OTT-A 独家 [04-01, 07-01)
 *  节目2 午夜面包房：ORIG-KO=5, SUB-ZH=6
 *   CN/TV-LINEAR: ORIG-KO [02-01, 05-01)（已过期）
 *   CN/OTT-A: SUB-ZH [2026-10-01, 2027-03-01)（尚未生效）
 */
class AvailabilityServiceTest {

    private static final long P1 = 1L, P2 = 2L;
    private static final long ORIG_EN = 1L, DUB_ZH = 2L, SUB_ZH = 3L, DUB_JA = 4L;
    private static final long ORIG_KO = 5L, P2_SUB_ZH = 6L;

    private final List<Contract> contracts = new ArrayList<>();
    private final List<Exclusivity> exclusivities = new ArrayList<>();

    private AvailabilityService service;

    private static Instant t(String iso) {
        return Instant.parse(iso);
    }

    private Contract c(long id, long programId, long lvId, String region, String channel,
                       String from, String to) {
        return new Contract(id, "LIC-TEST-" + id, programId, lvId, region, channel, t(from), t(to));
    }

    @BeforeEach
    void setUp() {
        contracts.add(c(1, P1, ORIG_EN, "CN", "OTT-A", "2026-01-01T00:00:00Z", "2027-01-01T00:00:00Z"));
        contracts.add(c(2, P1, DUB_ZH, "CN", "OTT-A", "2026-01-01T00:00:00Z", "2026-07-01T00:00:00Z"));
        contracts.add(c(3, P1, DUB_ZH, "CN", "OTT-A", "2026-09-01T00:00:00Z", "2027-01-01T00:00:00Z"));
        contracts.add(c(4, P1, ORIG_EN, "CN", "OTT-B", "2026-03-01T00:00:00Z", "2026-09-01T00:00:00Z"));
        contracts.add(c(5, P1, DUB_JA, "JP", "OTT-A", "2026-01-01T00:00:00Z", "2027-01-01T00:00:00Z"));
        contracts.add(c(6, P1, SUB_ZH, "CN", "OTT-B", "2026-03-01T00:00:00Z", "2026-09-01T00:00:00Z"));
        contracts.add(c(7, P2, ORIG_KO, "CN", "TV-LINEAR", "2026-02-01T00:00:00Z", "2026-05-01T00:00:00Z"));
        contracts.add(c(8, P2, P2_SUB_ZH, "CN", "OTT-A", "2026-10-01T00:00:00Z", "2027-03-01T00:00:00Z"));

        exclusivities.add(new Exclusivity(1L, P1, "CN", "OTT-A",
                t("2026-04-01T00:00:00Z"), t("2026-07-01T00:00:00Z"), "OTT-A 独播窗口"));

        ContractRepository contractRepository = mock(ContractRepository.class);
        when(contractRepository.findByProgramIdAndLanguageVersionIdAndRegionCodeAndChannelCodeOrderByValidFrom(
                anyLong(), anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> contracts.stream()
                        .filter(x -> x.getProgramId().equals(inv.getArgument(0))
                                && x.getLanguageVersionId().equals(inv.getArgument(1))
                                && x.getRegionCode().equals(inv.getArgument(2))
                                && x.getChannelCode().equals(inv.getArgument(3)))
                        .sorted(java.util.Comparator.comparing(Contract::getValidFrom))
                        .toList());
        when(contractRepository.findByProgramIdAndRegionCodeAndChannelCodeOrderByValidFrom(
                anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> contracts.stream()
                        .filter(x -> x.getProgramId().equals(inv.getArgument(0))
                                && x.getRegionCode().equals(inv.getArgument(1))
                                && x.getChannelCode().equals(inv.getArgument(2)))
                        .sorted(java.util.Comparator.comparing(Contract::getValidFrom))
                        .toList());

        ExclusivityRepository exclusivityRepository = mock(ExclusivityRepository.class);
        when(exclusivityRepository.findByProgramIdAndRegionCode(anyLong(), anyString()))
                .thenAnswer(inv -> exclusivities.stream()
                        .filter(e -> e.getProgramId().equals(inv.getArgument(0))
                                && e.getRegionCode().equals(inv.getArgument(1)))
                        .toList());

        Clock fixedClock = Clock.fixed(t("2026-09-25T00:00:00Z"), ZoneOffset.UTC);
        service = new AvailabilityService(contractRepository, exclusivityRepository, fixedClock);
    }

    // ---------- 边界时间 ----------

    @Test
    @DisplayName("边界：窗口起点时刻包含（t == validFrom → 可用）")
    void windowStartIsInclusive() {
        Decision d = service.decideAt(P1, DUB_ZH, "CN", "OTT-A", t("2026-01-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.AVAILABLE);
        assertThat(d.matchedContract().getContractRef()).isEqualTo("LIC-TEST-2");
    }

    @Test
    @DisplayName("边界：窗口终点时刻不包含（t == validTo → 不可用）")
    void windowEndIsExclusive() {
        // DUB-ZH 第一段在 2026-07-01T00:00:00Z 结束；该瞬间不再被覆盖，且落在空档起点
        Decision d = service.decideAt(P1, DUB_ZH, "CN", "OTT-A", t("2026-07-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.LICENSE_GAP);
    }

    @Test
    @DisplayName("边界：空档结束即第二段窗口起点（t == 09-01 → 可用）")
    void secondWindowStartIsInclusive() {
        Decision d = service.decideAt(P1, DUB_ZH, "CN", "OTT-A", t("2026-09-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.AVAILABLE);
        assertThat(d.matchedContract().getContractRef()).isEqualTo("LIC-TEST-3");
    }

    @Test
    @DisplayName("边界：终点前一瞬仍可用")
    void justBeforeEndIsAvailable() {
        Decision d = service.decideAt(P1, DUB_ZH, "CN", "OTT-A", t("2026-06-30T23:59:59Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.AVAILABLE);
    }

    // ---------- 授权空档 ----------

    @Test
    @DisplayName("空档：计划时刻落在两段窗口之间 → LICENSE_GAP，并给出相邻窗口解释")
    void licenseGap() {
        Decision d = service.decideAt(P1, DUB_ZH, "CN", "OTT-A", t("2026-07-15T12:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.LICENSE_GAP);
        assertThat(d.relatedContracts()).extracting(Contract::getContractRef)
                .containsExactly("LIC-TEST-2", "LIC-TEST-3");
    }

    // ---------- 同地区不同渠道 + 排他 ----------

    @Test
    @DisplayName("同地区不同渠道：排他窗口内 OTT-A 自身可用")
    void exclusiveChannelItselfIsAvailable() {
        Decision d = service.decideAt(P1, ORIG_EN, "CN", "OTT-A", t("2026-05-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.AVAILABLE);
        assertThat(d.matchedContract().getContractRef()).isEqualTo("LIC-TEST-1");
    }

    @Test
    @DisplayName("同地区不同渠道：排他窗口内 OTT-B 虽有合同仍被排他冲突阻止")
    void otherChannelBlockedByExclusivity() {
        Decision d = service.decideAt(P1, ORIG_EN, "CN", "OTT-B", t("2026-05-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.EXCLUSIVITY_CONFLICT);
        assertThat(d.activeExclusivities()).hasSize(1);
        assertThat(d.activeExclusivities().get(0).getChannelCode()).isEqualTo("OTT-A");
    }

    @Test
    @DisplayName("边界：排他窗口起点包含（t == 排他起点 → 冲突）")
    void exclusivityStartIsInclusive() {
        Decision d = service.decideAt(P1, ORIG_EN, "CN", "OTT-B", t("2026-04-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.EXCLUSIVITY_CONFLICT);
    }

    @Test
    @DisplayName("边界：排他窗口终点不包含（t == 排他终点 → OTT-B 恢复可用）")
    void exclusivityEndIsExclusive() {
        Decision d = service.decideAt(P1, ORIG_EN, "CN", "OTT-B", t("2026-07-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.AVAILABLE);
        assertThat(d.matchedContract().getContractRef()).isEqualTo("LIC-TEST-4");
    }

    // ---------- 缺少语言版本 ----------

    @Test
    @DisplayName("缺少语言版本：OTT-A 有其他语言授权但无中文字幕 → MISSING_LANGUAGE_VERSION")
    void missingLanguageVersion() {
        Decision d = service.decideAt(P1, SUB_ZH, "CN", "OTT-A", t("2026-03-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.MISSING_LANGUAGE_VERSION);
    }

    @Test
    @DisplayName("无任何合同：未授权地区 → NO_LICENSE")
    void noLicenseAtAll() {
        Decision d = service.decideAt(P1, ORIG_EN, "SEA", "OTT-A", t("2026-03-01T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.NO_LICENSE);
    }

    // ---------- 尚未生效 / 已过期 ----------

    @Test
    @DisplayName("尚未生效：计划时刻早于全部窗口 → NOT_YET_LICENSED")
    void notYetLicensed() {
        Decision d = service.decideAt(P2, P2_SUB_ZH, "CN", "OTT-A", t("2026-09-25T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.NOT_YET_LICENSED);
    }

    @Test
    @DisplayName("已过期：计划时刻晚于全部窗口 → LICENSE_EXPIRED")
    void licenseExpired() {
        Decision d = service.decideAt(P2, ORIG_KO, "CN", "TV-LINEAR", t("2026-09-25T00:00:00Z"));
        assertThat(d.status()).isEqualTo(DecisionStatus.BLOCKED);
        assertThat(d.reasons()).containsExactly(BlockReason.LICENSE_EXPIRED);
    }

    // ---------- 计划状态与当前状态分开 ----------

    @Test
    @DisplayName("当前状态：decideNow 使用注入的 UTC 时钟，与计划判定相互独立")
    void decideNowUsesClock() {
        // 固定时钟 2026-09-25：P1 DUB-ZH CN/OTT-A 处于第二段窗口内 → 当前可用
        Decision now = service.decideNow(P1, DUB_ZH, "CN", "OTT-A");
        assertThat(now.status()).isEqualTo(DecisionStatus.AVAILABLE);
        assertThat(now.evaluatedAt()).isEqualTo(t("2026-09-25T00:00:00Z"));
        // 同一组合计划到空档期则不可用，两者互不影响
        Decision planned = service.decideAt(P1, DUB_ZH, "CN", "OTT-A", t("2026-07-15T00:00:00Z"));
        assertThat(planned.status()).isEqualTo(DecisionStatus.BLOCKED);
    }
}
