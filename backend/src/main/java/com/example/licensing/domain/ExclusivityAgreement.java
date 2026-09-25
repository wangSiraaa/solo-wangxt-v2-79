package com.example.licensing.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exclusivity_agreement")
public class ExclusivityAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false, unique = true)
    private Long contractId;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    private String note;

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public String getRegionCode() { return regionCode; }
    public String getChannelCode() { return channelCode; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public String getNote() { return note; }
}
