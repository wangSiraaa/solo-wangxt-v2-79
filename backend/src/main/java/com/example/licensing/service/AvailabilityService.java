package com.example.licensing.service;

import com.example.licensing.domain.Contract;
import com.example.licensing.domain.ExclusivityAgreement;
import com.example.licensing.domain.Program;
import com.example.licensing.repo.ContractRepository;
import com.example.licensing.repo.ExclusivityAgreementRepository;
import com.example.licensing.repo.ProgramRepository;
import com.example.licensing.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final ContractRepository contractRepo;
    private final ExclusivityAgreementRepository exclusivityRepo;
    private final ProgramRepository programRepo;
    private final Clock clock;

    public AvailabilityService(ContractRepository contractRepo,
                               ExclusivityAgreementRepository exclusivityRepo,
                               ProgramRepository programRepo, Clock clock) {
        this.contractRepo = contractRepo;
        this.exclusivityRepo = exclusivityRepo;
        this.programRepo = programRepo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Result evaluate(String programCode, String regionCode, String channelCode,
                           String languageCode, Instant plannedAt) {
        Program program = programRepo.findByCode(programCode)
                .orElseThrow(() -> new NotFoundException("节目不存在: " + programCode));

        AvailabilityEngine.Input plannedInput = buildInput(
                program.getId(), regionCode, channelCode, languageCode, plannedAt);
        AvailabilityEngine.Decision planned = AvailabilityEngine.evaluate(plannedInput);

        Instant now = Instant.now(clock);
        AvailabilityEngine.Input currentInput = buildInput(
                program.getId(), regionCode, channelCode, languageCode, now);
        AvailabilityEngine.Decision current = AvailabilityEngine.evaluate(currentInput);

        return new Result(program.getCode(), program.getTitle(), planned, now, current);
    }

    public record Result(String programCode, String programTitle,
                         AvailabilityEngine.Decision plannedStatus,
                         Instant evaluatedNow,
                         AvailabilityEngine.Decision currentStatus) {}

    private AvailabilityEngine.Input buildInput(Long programId, String regionCode,
                                                String channelCode, String languageCode,
                                                Instant at) {
        List<Contract> contracts =
                contractRepo.findByProgramIdAndRegionCodeAndChannelCode(
                        programId, regionCode, channelCode);

        List<AvailabilityEngine.License> licenses = contracts.stream()
                .map(this::toLicense)
                .toList();

        // 排他约定通过 contractId 关联回合同拿到权利方（licensee）
        Map<Long, Contract> byId = contracts.stream()
                .collect(Collectors.toMap(Contract::getId, Function.identity()));

        List<AvailabilityEngine.ExclusiveRight> rights = exclusivityRepo
                .findByRegionCodeAndChannelCode(regionCode, channelCode).stream()
                .filter(a -> byId.containsKey(a.getContractId()))
                .map(a -> new AvailabilityEngine.ExclusiveRight(
                        a.getContractId(),
                        byId.get(a.getContractId()).getLicensee(),
                        toInstant(a.getStartAt()), toInstant(a.getEndAt()),
                        a.getNote()))
                .toList();

        return new AvailabilityEngine.Input(at, regionCode, channelCode, languageCode,
                licenses, rights);
    }

    private AvailabilityEngine.License toLicense(Contract c) {
        return new AvailabilityEngine.License(
                c.getId(), c.getContractNo(), c.getLicensee(),
                toInstant(c.getStartAt()), toInstant(c.getEndAt()),
                c.isExclusive(),
                c.getLanguageCodes().stream().sorted().toList(),
                c.getNote());
    }

    static Instant toInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
    }
}
