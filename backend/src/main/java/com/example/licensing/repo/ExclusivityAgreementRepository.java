package com.example.licensing.repo;

import com.example.licensing.domain.ExclusivityAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExclusivityAgreementRepository extends JpaRepository<ExclusivityAgreement, Long> {

    List<ExclusivityAgreement> findByRegionCodeAndChannelCode(
            String regionCode, String channelCode);
}
