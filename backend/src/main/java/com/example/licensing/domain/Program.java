package com.example.licensing.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "program")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
}
