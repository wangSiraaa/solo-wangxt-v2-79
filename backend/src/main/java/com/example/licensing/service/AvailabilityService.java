package com.example.licensing.service;

import com.example.licensing.model.Contract;
import com.example.licensing.model.Exclusivity;
import com.example.licensing.repo.ContractRepository;
import com.example.licensing.repo.ExclusivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 可用性判定服务。
 *
 * 统一规则：
 *  - 所有时刻按 UTC 比较；
 *  - 授权/排他窗口均为 [validFrom, validTo)：起始包含、结束不包含；
 *  - 上架条件 = 存在覆盖计划时刻的合同 且 该时刻无针对其他渠道的排他约定生效。
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final ContractRepository contractRepository;
    private final ExclusivityRepository exclusivityRepository;
    private final Clock clock;

    public AvailabilityService(ContractRepository contractRepository,
                               ExclusivityRepository exclusivityRepository,
                               Clock clock) {
        this.contractRepository = contractRepository;
        this.exclusivityRepository = exclusivityRepository;
        this.clock = clock;
    }

    /** 判定“计划时刻”能否上架 */
    public Decision decideAt(Long programId, Long languageVersionId,
                             String region, String channel, Instant plannedAt) {
        List<Contract> comboContracts = contractRepository
                .findByProgramIdAndLanguageVersionIdAndRegionCodeAndChannelCodeOrderByValidFrom(
                        programId, languageVersionId, region, channel);

        List<Exclusivity> activeExclusivities = exclusivityRepository
                .findByProgramIdAndRegionCode(programId, region).stream()
                .filter(e -> e.activeAt(plannedAt))
                .toList();

        Contract covering = comboContracts.stream()
                .filter(c -> c.covers(plannedAt))
                .findFirst()
                .orElse(null);

        if (covering != null) {
            // 有覆盖合同：再查排他。排他渠道 != 请求渠道 才构成冲突。
            boolean conflict = activeExclusivities.stream()
                    .anyMatch(e -> !e.getChannelCode().equals(channel));
            if (conflict) {
                return Decision.blocked(plannedAt, BlockReason.EXCLUSIVITY_CONFLICT,
                        comboContracts, activeExclusivities);
            }
            return Decision.available(plannedAt, covering, comboContracts, activeExclusivities);
        }

        // 无覆盖合同：区分具体原因
        if (comboContracts.isEmpty()) {
            boolean otherLanguageLicensed = !contractRepository
                    .findByProgramIdAndRegionCodeAndChannelCodeOrderByValidFrom(programId, region, channel)
                    .isEmpty();
            BlockReason reason = otherLanguageLicensed
                    ? BlockReason.MISSING_LANGUAGE_VERSION
                    : BlockReason.NO_LICENSE;
            return Decision.blocked(plannedAt, reason, comboContracts, activeExclusivities);
        }

        boolean allInFuture = comboContracts.stream()
                .allMatch(c -> plannedAt.isBefore(c.getValidFrom()));
        if (allInFuture) {
            return Decision.blocked(plannedAt, BlockReason.NOT_YET_LICENSED,
                    comboContracts, activeExclusivities);
        }

        boolean allInPast = comboContracts.stream()
                .allMatch(c -> !plannedAt.isBefore(c.getValidTo()));
        if (allInPast) {
            return Decision.blocked(plannedAt, BlockReason.LICENSE_EXPIRED,
                    comboContracts, activeExclusivities);
        }

        // 既不在全部窗口之前、也不在全部窗口之后，且没有任何窗口覆盖 → 落在窗口之间的空档
        return Decision.blocked(plannedAt, BlockReason.LICENSE_GAP,
                comboContracts, activeExclusivities);
    }

    /** 判定“当前时刻”能否上架（与计划状态分开展示） */
    public Decision decideNow(Long programId, Long languageVersionId,
                              String region, String channel) {
        return decideAt(programId, languageVersionId, region, channel, Instant.now(clock));
    }
}
