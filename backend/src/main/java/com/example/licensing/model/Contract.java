package com.example.licensing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 授权合同：某节目的某语言版本在某地区、某渠道的有效授权窗口。
 * 窗口语义：[validFrom, validTo)，起始时刻包含、结束时刻不包含，全部按 UTC 比较。
 */
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    private Long id;

    @Column(name = "contract_ref", nullable = false)
    private String contractRef;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "language_version_id", nullable = false)
    private Long languageVersionId;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    protected Contract() {
    }

    public Contract(Long id, String contractRef, Long programId, Long languageVersionId,
                    String regionCode, String channelCode, Instant validFrom, Instant validTo) {
        this.id = id;
        this.contractRef = contractRef;
        this.programId = programId;
        this.languageVersionId = languageVersionId;
        this.regionCode = regionCode;
        this.channelCode = channelCode;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    /** 起始包含、结束不包含：validFrom <= t < validTo */
    public boolean covers(Instant t) {
        return !t.isBefore(validFrom) && t.isBefore(validTo);
    }

    public Long getId() {
        return id;
    }

    public String getContractRef() {
        return contractRef;
    }

    public Long getProgramId() {
        return programId;
    }

    public Long getLanguageVersionId() {
        return languageVersionId;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }
}
