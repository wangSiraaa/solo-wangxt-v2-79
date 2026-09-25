package com.example.licensing.repo;

import com.example.licensing.model.Exclusivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExclusivityRepository extends JpaRepository<Exclusivity, Long> {

    List<Exclusivity> findByProgramIdAndRegionCode(Long programId, String regionCode);

    List<Exclusivity> findByProgramId(Long programId);
}
