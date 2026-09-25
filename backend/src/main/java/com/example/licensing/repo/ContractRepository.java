package com.example.licensing.repo;

import com.example.licensing.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByProgramIdAndLanguageVersionIdAndRegionCodeAndChannelCodeOrderByValidFrom(
            Long programId, Long languageVersionId, String regionCode, String channelCode);

    List<Contract> findByProgramIdAndRegionCodeAndChannelCodeOrderByValidFrom(
            Long programId, String regionCode, String channelCode);

    List<Contract> findByProgramIdAndRegionCodeOrderByChannelCodeAscValidFromAsc(
            Long programId, String regionCode);

    List<Contract> findByProgramIdOrderByValidFrom(Long programId);
}
