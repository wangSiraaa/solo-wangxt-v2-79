package com.example.licensing.service;

import com.example.licensing.model.Contract;
import com.example.licensing.model.Exclusivity;

import java.time.Instant;
import java.util.List;

/**
 * 单次可用性判定结果。
 *
 * @param evaluatedAt        被评估的时刻（UTC）
 * @param status             AVAILABLE / BLOCKED
 * @param matchedContract    命中并覆盖该时刻的合同（不可用时为 null）
 * @param reasons            阻止原因列表（可用时为空）
 * @param relatedContracts   同组合（节目+语言版本+地区+渠道）的全部合同窗口，用于解释
 * @param activeExclusivities 该时刻生效的排他约定，用于解释
 */
public record Decision(
        Instant evaluatedAt,
        DecisionStatus status,
        Contract matchedContract,
        List<BlockReason> reasons,
        List<Contract> relatedContracts,
        List<Exclusivity> activeExclusivities) {

    public static Decision available(Instant at, Contract matched,
                                     List<Contract> related, List<Exclusivity> activeExcl) {
        return new Decision(at, DecisionStatus.AVAILABLE, matched, List.of(), related, activeExcl);
    }

    public static Decision blocked(Instant at, BlockReason reason,
                                   List<Contract> related, List<Exclusivity> activeExcl) {
        return new Decision(at, DecisionStatus.BLOCKED, null, List.of(reason), related, activeExcl);
    }
}
