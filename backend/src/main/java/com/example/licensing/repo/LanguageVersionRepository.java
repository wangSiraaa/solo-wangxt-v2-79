package com.example.licensing.repo;

import com.example.licensing.model.LanguageVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LanguageVersionRepository extends JpaRepository<LanguageVersion, Long> {

    List<LanguageVersion> findByProgramIdOrderByCode(Long programId);
}
