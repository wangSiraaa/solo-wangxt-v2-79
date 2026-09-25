package com.example.licensing.repo;

import com.example.licensing.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByProgramIdAndRegionCodeAndChannelCode(
            Long programId, String regionCode, String channelCode);

    List<Contract> findByProgramId(Long programId);
}
