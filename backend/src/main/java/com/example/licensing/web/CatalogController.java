package com.example.licensing.web;

import com.example.licensing.model.LanguageVersion;
import com.example.licensing.model.Program;
import com.example.licensing.repo.ContractRepository;
import com.example.licensing.repo.ExclusivityRepository;
import com.example.licensing.repo.LanguageVersionRepository;
import com.example.licensing.repo.ProgramRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.TreeSet;

import static com.example.licensing.web.Views.ContractView;
import static com.example.licensing.web.Views.ExclusivityView;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProgramRepository programRepository;
    private final LanguageVersionRepository languageVersionRepository;
    private final ContractRepository contractRepository;
    private final ExclusivityRepository exclusivityRepository;

    public CatalogController(ProgramRepository programRepository,
                             LanguageVersionRepository languageVersionRepository,
                             ContractRepository contractRepository,
                             ExclusivityRepository exclusivityRepository) {
        this.programRepository = programRepository;
        this.languageVersionRepository = languageVersionRepository;
        this.contractRepository = contractRepository;
        this.exclusivityRepository = exclusivityRepository;
    }

    public record LanguageVersionView(Long id, String code, String label) {
        static LanguageVersionView of(LanguageVersion lv) {
            return new LanguageVersionView(lv.getId(), lv.getCode(), lv.getLabel());
        }
    }

    public record ProgramView(Long id, String code, String title, String genre,
                              List<LanguageVersionView> languageVersions) {
    }

    public record CatalogView(List<ProgramView> programs, List<String> regions, List<String> channels) {
    }

    /** 目录：节目及其语言版本，以及数据中出现过的地区/渠道代码 */
    @GetMapping("/catalog")
    public CatalogView catalog() {
        List<Program> programs = programRepository.findAll();
        List<ProgramView> programViews = programs.stream()
                .map(p -> new ProgramView(p.getId(), p.getCode(), p.getTitle(), p.getGenre(),
                        languageVersionRepository.findByProgramIdOrderByCode(p.getId()).stream()
                                .map(LanguageVersionView::of).toList()))
                .toList();

        TreeSet<String> regions = new TreeSet<>();
        TreeSet<String> channels = new TreeSet<>();
        contractRepository.findAll().forEach(c -> {
            regions.add(c.getRegionCode());
            channels.add(c.getChannelCode());
        });
        exclusivityRepository.findAll().forEach(e -> {
            regions.add(e.getRegionCode());
            channels.add(e.getChannelCode());
        });
        return new CatalogView(programViews, List.copyOf(regions), List.copyOf(channels));
    }

    public record RightsView(List<ContractView> contracts, List<ExclusivityView> exclusivities) {
    }

    /** 某节目（可选地区）的合同与排他约定，用于前端时间线/列表展示 */
    @GetMapping("/rights")
    public RightsView rights(@RequestParam Long programId,
                             @RequestParam(required = false) String region) {
        var contracts = (region == null || region.isBlank()
                ? contractRepository.findByProgramIdOrderByValidFrom(programId)
                : contractRepository.findByProgramIdAndRegionCodeOrderByChannelCodeAscValidFromAsc(programId, region))
                .stream().map(ContractView::of).toList();
        var exclusivities = (region == null || region.isBlank()
                ? exclusivityRepository.findByProgramId(programId)
                : exclusivityRepository.findByProgramIdAndRegionCode(programId, region))
                .stream().map(ExclusivityView::of).toList();
        return new RightsView(contracts, exclusivities);
    }
}
