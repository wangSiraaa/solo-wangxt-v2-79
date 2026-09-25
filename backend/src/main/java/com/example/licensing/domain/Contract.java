package com.example.licensing.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(name = "contract_no", nullable = false, unique = true)
    private String contractNo;

    @Column(nullable = false)
    private String licensee;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    /** UTC，含该时刻 */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /** UTC，不含该时刻；NULL 表示开放结尾 */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean exclusive;

    private String note;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contract_language", joinColumns = @JoinColumn(name = "contract_id"))
    @Column(name = "language_code")
    private Set<String> languageCodes = new HashSet<>();

    public Long getId() { return id; }
    public Long getProgramId() { return programId; }
    public String getContractNo() { return contractNo; }
    public String getLicensee() { return licensee; }
    public String getRegionCode() { return regionCode; }
    public String getChannelCode() { return channelCode; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public boolean isExclusive() { return exclusive; }
    public String getNote() { return note; }
    public Set<String> getLanguageCodes() { return languageCodes; }
}
