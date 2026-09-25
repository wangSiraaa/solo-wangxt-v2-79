package com.example.licensing.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 授权可用性判定核心（纯函数，不依赖数据库/Spring，便于对边界时间做单元测试）。
 *
 * 时间规则：全部使用 UTC；合同窗口为左闭右开 [startAt, endAt)，
 * endAt == null 表示开放结尾。判定时刻恰为 startAt 时有效，恰为 endAt 时无效。
 */
public final class AvailabilityEngine {

    public static final String TIME_RULE = "UTC；窗口左闭右开 [startAt, endAt)，含开始时刻、不含结束时刻";

    private AvailabilityEngine() {}

    /** 某地区+渠道下的一份授权合同（已带上语言版本集合） */
    public record License(long id, String contractNo, String licensee,
                          Instant startAt, Instant endAt, boolean exclusive,
                          List<String> languageCodes, String note) {}

    /** 生效中的排他约定（holder 为持有该约定的被授权方） */
    public record ExclusiveRight(long contractId, String holder,
                                 Instant startAt, Instant endAt, String note) {}

    /** 命中合同视图 */
    public record ContractHit(long id, String contractNo, String licensee,
                              String regionCode, String channelCode,
                              Instant startAt, Instant endAt, boolean exclusive,
                              List<String> languageCodes, String note) {}

    public record BlockingReason(String code, String message, List<String> contractNos) {}

    public record Gap(Instant gapStart, Instant gapEnd,
                      String previousContractNo, String nextContractNo, String message) {}

    public record Input(Instant evaluatedAt, String regionCode, String channelCode,
                        String languageCode,
                        List<License> licenses, List<ExclusiveRight> exclusiveRights) {}

    public record Decision(boolean available, String timeRule,
                           Instant evaluatedAt, String regionCode, String channelCode,
                           String languageCode,
                           List<ContractHit> matchedContracts,
                           List<ContractHit> coveringContracts,
                           List<BlockingReason> blockers,
                           Gap gap) {}

    /** 左闭右开：t == startAt 命中；t == endAt 不命中；endAt == null 为开放结尾 */
    static boolean covers(Instant start, Instant end, Instant t) {
        boolean afterOrAtStart = t.compareTo(start) >= 0;
        boolean beforeEnd = end == null || t.compareTo(end) < 0;
        return afterOrAtStart && beforeEnd;
    }

    public static Decision evaluate(Input in) {
        Instant at = in.evaluatedAt();

        // 1) 同地区、同渠道在该时刻生效的合同
        List<License> covering = in.licenses().stream()
                .filter(l -> covers(l.startAt(), l.endAt(), at))
                .sorted(Comparator.comparing(License::contractNo))
                .toList();

        if (covering.isEmpty()) {
            return noLicenseDecision(in, at);
        }

        // 2) 在生效合同中寻找携带所申请语言版本的
        String lang = in.languageCode();
        List<License> withLang = covering.stream()
                .filter(l -> l.languageCodes().contains(lang))
                .toList();

        if (withLang.isEmpty()) {
            List<String> nos = covering.stream().map(License::contractNo).toList();
            BlockingReason blocker = new BlockingReason(
                    "MISSING_LANGUAGE_VERSION",
                    String.format("在 %s 地区 %s 渠道，当前时刻有 %d 份生效合同（%s），但均未授予语言版本 %s 的上架权利。",
                            in.regionCode(), in.channelCode(), covering.size(),
                            String.join("、", nos), lang),
                    nos);
            return new Decision(false, TIME_RULE, at, in.regionCode(), in.channelCode(), lang,
                    List.of(), toHits(covering, in), List.of(blocker), null);
        }

        // 3) 排他冲突检查（同地区同渠道、不同被授权方）
        List<ExclusiveRight> activeRights = in.exclusiveRights().stream()
                .filter(r -> covers(r.startAt(), r.endAt(), at))
                .toList();

        List<BlockingReason> blockers = new ArrayList<>();
        List<String> conflictRefs = new ArrayList<>();

        for (ExclusiveRight right : activeRights) {
            for (License candidate : withLang) {
                if (!right.holder().equals(candidate.licensee())) {
                    conflictRefs.add(candidate.contractNo());
                    blockers.add(new BlockingReason(
                            "EXCLUSIVITY_CONFLICT",
                            String.format("排他冲突：%s 持有该地区/渠道在当前时刻的独家授权（来源合同 %d），"
                                            + "但 %s 也持有效授权（合同 %s，含语言版本 %s）。按排他约定必须阻止上架。",
                                    right.holder(), right.contractId(),
                                    candidate.licensee(), candidate.contractNo(), lang),
                            List.of(candidate.contractNo(), "EXCL#" + right.contractId())));
                }
            }
        }

        // 不同权利方之间存在相互重叠的独家窗口（数据异常也按冲突处理）
        long distinctHolders = activeRights.stream().map(ExclusiveRight::holder).distinct().count();
        if (activeRights.size() > 1 && distinctHolders > 1) {
            blockers.add(new BlockingReason(
                    "EXCLUSIVITY_OVERLAP",
                    "当前时刻存在多个不同权利方的排他约定互相重叠，无法确定合法上架方，必须阻止。",
                    activeRights.stream().map(r -> "EXCL#" + r.contractId()).toList()));
        }

        boolean available = blockers.isEmpty();
        return new Decision(available, TIME_RULE, at, in.regionCode(), in.channelCode(), lang,
                toHits(withLang, in), toHits(covering, in),
                List.copyOf(blockers), null);
    }

    private static Decision noLicenseDecision(Input in, Instant at) {
        // 授权空档：找时刻之前最近结束的合同与时刻之后最早开始的合同
        List<License> all = in.licenses();

        License previous = all.stream()
                .filter(l -> l.endAt() != null && !l.endAt().isAfter(at))
                .max(Comparator.comparing(License::endAt))
                .orElse(null);
        License next = all.stream()
                .filter(l -> l.startAt().isAfter(at))
                .min(Comparator.comparing(License::startAt))
                .orElse(null);

        Gap gap = null;
        String message;
        List<String> refs = new ArrayList<>();

        if (previous != null && next != null) {
            gap = new Gap(previous.endAt(), next.startAt(),
                    previous.contractNo(), next.contractNo(),
                    String.format("授权空档：%s 已于 %s（不含该时刻）到期，%s 要到 %s（含该时刻）才开始。",
                            previous.contractNo(), previous.endAt(),
                            next.contractNo(), next.startAt()));
            message = gap.message();
            refs.add(previous.contractNo());
            refs.add(next.contractNo());
        } else if (previous != null) {
            message = String.format("当前时刻无有效授权：最近的合同 %s 已于 %s（不含该时刻）到期，之后没有新合同。",
                    previous.contractNo(), previous.endAt());
            refs.add(previous.contractNo());
        } else if (next != null) {
            message = String.format("当前时刻无有效授权：最早的合同 %s 要到 %s（含该时刻）才开始。",
                    next.contractNo(), next.startAt());
            refs.add(next.contractNo());
        } else {
            message = String.format("在 %s 地区 %s 渠道没有任何授权合同记录。",
                    in.regionCode(), in.channelCode());
        }

        BlockingReason blocker = new BlockingReason("NO_VALID_CONTRACT", message, refs);
        return new Decision(false, TIME_RULE, at, in.regionCode(), in.channelCode(),
                in.languageCode(), List.of(), List.of(), List.of(blocker), gap);
    }

    private static List<ContractHit> toHits(List<License> licenses, Input in) {
        return licenses.stream()
                .map(l -> new ContractHit(l.id(), l.contractNo(), l.licensee(),
                        in.regionCode(), in.channelCode(),
                        l.startAt(), l.endAt(), l.exclusive(),
                        l.languageCodes().stream().sorted().toList(), l.note()))
                .toList();
    }

    /** 日历用：给定时刻列表，返回每段相邻合同区间，供前端绘制（这里只做简单去重排序） */
    public static List<License> sortByStart(List<License> licenses) {
        return licenses.stream()
                .sorted(Comparator.comparing(License::startAt)
                        .thenComparing(License::contractNo))
                .toList();
    }

    /** 供测试/接口引用的语言集合工具 */
    public static Set<String> languages(String... codes) {
        return Set.of(codes);
    }
}
