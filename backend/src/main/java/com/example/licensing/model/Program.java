package com.example.licensing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "program")
public class Program {

    @Id
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    protected Program() {
    }

    public Program(Long id, String code, String title, String genre) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }
}
