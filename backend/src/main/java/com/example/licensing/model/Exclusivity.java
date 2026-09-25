package com.example.licensing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 排他约定：某节目在某地区、窗口 [validFrom, validTo) 内由 channelCode 独家持有。
 * 窗口内其他渠道即使持有覆盖合同也判定为排他冲突，阻止上架。
 */
@Entity
@Table(name = "exclusivity")
public class Exclusivity {

    @Id
    private Long id;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(nullable = false)
    private String note;

    protected Exclusivity() {
    }

    public Exclusivity(Long id, Long programId, String regionCode, String channelCode,
                       Instant validFrom, Instant validTo, String note) {
        this.id = id;
        this.programId = programId;
        this.regionCode = regionCode;
        this.channelCode = channelCode;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.note = note;
    }

    public boolean activeAt(Instant t) {
        return !t.isBefore(validFrom) && t.isBefore(validTo);
    }

    public Long getId() {
        return id;
    }

    public Long getProgramId() {
        return programId;
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

    public String getNote() {
        return note;
    }
}
