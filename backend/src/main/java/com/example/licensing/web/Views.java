package com.example.licensing.web;

import com.example.licensing.model.Contract;
import com.example.licensing.model.Exclusivity;
import com.example.licensing.service.BlockReason;
import com.example.licensing.service.Decision;
import com.example.licensing.service.DecisionStatus;

import java.time.Instant;
import java.util.List;

/** API 视图对象 */
public final class Views {

    private Views() {
    }

    public record ContractView(
            Long id, String contractRef, Long programId, Long languageVersionId,
            String regionCode, String channelCode, Instant validFrom, Instant validTo) {

        static ContractView of(Contract c) {
            return new ContractView(c.getId(), c.getContractRef(), c.getProgramId(),
                    c.getLanguageVersionId(), c.getRegionCode(), c.getChannelCode(),
                    c.getValidFrom(), c.getValidTo());
        }
    }

    public record ExclusivityView(
            Long id, Long programId, String regionCode, String channelCode,
            Instant validFrom, Instant validTo, String note) {

        static ExclusivityView of(Exclusivity e) {
            return new ExclusivityView(e.getId(), e.getProgramId(), e.getRegionCode(),
                    e.getChannelCode(), e.getValidFrom(), e.getValidTo(), e.getNote());
        }
    }

    public record ReasonView(String code, String message) {
    }

    public record DecisionView(
            Instant evaluatedAt,
            DecisionStatus status,
            ContractView matchedContract,
            List<ReasonView> reasons,
            List<ContractView> relatedContracts,
            List<ExclusivityView> activeExclusivities) {

        static DecisionView of(Decision d) {
            return new DecisionView(
                    d.evaluatedAt(),
                    d.status(),
                    d.matchedContract() == null ? null : ContractView.of(d.matchedContract()),
                    d.reasons().stream().map(r -> new ReasonView(r.name(), messageFor(r))).toList(),
                    d.relatedContracts().stream().map(ContractView::of).toList(),
                    d.activeExclusivities().stream().map(ExclusivityView::of).toList());
        }
    }

    public static String messageFor(BlockReason reason) {
        return switch (reason) {
            case NO_LICENSE -> "该节目在该地区、该渠道、该语言版本下没有任何授权合同";
            case MISSING_LANGUAGE_VERSION -> "该地区该渠道存在其他语言版本的授权，但缺少所请求语言版本的授权";
            case NOT_YET_LICENSED -> "计划时刻早于该组合全部授权窗口的起点，授权尚未生效";
            case LICENSE_EXPIRED -> "计划时刻晚于该组合全部授权窗口的终点，授权已过期";
            case LICENSE_GAP -> "计划时刻落在同一组合两段授权窗口之间的空档";
            case EXCLUSIVITY_CONFLICT -> "存在排他约定：该时刻另一渠道持有该地区独家权利，阻止本渠道上架";
        };
    }
}
