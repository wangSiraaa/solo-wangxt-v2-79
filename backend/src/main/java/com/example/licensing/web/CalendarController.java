package com.example.licensing.web;

import com.example.licensing.domain.Contract;
import com.example.licensing.domain.ExclusivityAgreement;
import com.example.licensing.domain.Program;
import com.example.licensing.repo.ContractRepository;
import com.example.licensing.repo.ExclusivityAgreementRepository;
import com.example.licensing.repo.ProgramRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 授权日历数据：节目、合同区间（含语言版本）、排他约定。
 * 全部为虚构版权数据，仅供日历展示。
 */
@RestController
@RequestMapping("/api")
public class CalendarController {

    private final ProgramRepository programRepo;
    private final ContractRepository contractRepo;
    private final ExclusivityAgreementRepository exclusivityRepo;

    public CalendarController(ProgramRepository programRepo,
                              ContractRepository contractRepo,
                              ExclusivityAgreementRepository exclusivityRepo) {
        this.programRepo = programRepo;
        this.contractRepo = contractRepo;
        this.exclusivityRepo = exclusivityRepo;
    }

    @GetMapping("/programs")
    public List<Map<String, Object>> programs() {
        return programRepo.findAll().stream()
                .map(p -> Map.<String, Object>of(
                        "code", p.getCode(),
                        "title", p.getTitle()))
                .toList();
    }

    @GetMapping("/contracts")
    public List<Map<String, Object>> contracts(@RequestParam String programCode) {
        Program program = programRepo.findByCode(programCode).orElseThrow(
                () -> new NotFoundException("节目不存在: " + programCode));
        return contractRepo.findByProgramId(program.getId()).stream()
                .map(c -> Map.<String, Object>of(
                        "contractNo", c.getContractNo(),
                        "licensee", c.getLicensee(),
                        "regionCode", c.getRegionCode(),
                        "channelCode", c.getChannelCode(),
                        "startAt", toInstant(c.getStartAt()).toString(),
                        "endAt", c.getEndAt() == null ? null : toInstant(c.getEndAt()).toString(),
                        "exclusive", c.isExclusive(),
                        "languageCodes", c.getLanguageCodes().stream().sorted().toList(),
                        "note", c.getNote() == null ? "" : c.getNote()))
                .toList();
    }

    @GetMapping("/exclusivities")
    public List<Map<String, Object>> exclusivities() {
        List<Contract> all = contractRepo.findAll();
        Map<Long, Contract> byId = all.stream()
                .collect(Collectors.toMap(Contract::getId, Function.identity()));

        return exclusivityRepo.findAll().stream()
                .map(a -> {
                    Contract c = byId.get(a.getContractId());
                    return Map.<String, Object>of(
                            "contractNo", c == null ? "#" + a.getContractId() : c.getContractNo(),
                            "holder", c == null ? "未知" : c.getLicensee(),
                            "programCode", programCodeOf(c),
                            "regionCode", a.getRegionCode(),
                            "channelCode", a.getChannelCode(),
                            "startAt", toInstant(a.getStartAt()).toString(),
                            "endAt", a.getEndAt() == null ? null : toInstant(a.getEndAt()).toString(),
                            "note", a.getNote() == null ? "" : a.getNote());
                })
                .toList();
    }

    private String programCodeOf(Contract c) {
        if (c == null) return "";
        return programRepo.findById(c.getProgramId())
                .map(Program::getCode).orElse("");
    }

    private static Instant toInstant(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
