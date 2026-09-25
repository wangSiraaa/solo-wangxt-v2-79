package com.example.licensing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "language_version")
public class LanguageVersion {

    @Id
    private Long id;

    @Column(name = "program_id", nullable = false)
    private Long programId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String label;

    protected LanguageVersion() {
    }

    public LanguageVersion(Long id, Long programId, String code, String label) {
        this.id = id;
        this.programId = programId;
        this.code = code;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public Long getProgramId() {
        return programId;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
