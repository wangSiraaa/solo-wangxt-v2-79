package com.example.licensing.web;

import com.example.licensing.service.AvailabilityService;
import com.example.licensing.service.Decision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.example.licensing.web.Views.DecisionView;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final com.example.licensing.repo.ContractRepository contractRepository;
    private final com.example.licensing.repo.ExclusivityRepository exclusivityRepository;

    public AvailabilityController(AvailabilityService availabilityService,
                                  com.example.licensing.repo.ContractRepository contractRepository,
                                  com.example.licensing.repo.ExclusivityRepository exclusivityRepository) {
        this.availabilityService = availabilityService;
        this.contractRepository = contractRepository;
        this.exclusivityRepository = exclusivityRepository;
    }

    public record CheckRequest(
            @NotNull Long programId,
            @NotNull Long languageVersionId,
            @NotBlank String region,
            @NotBlank String channel,
            /** 计划上架时刻，ISO-8601 UTC，例如 2026-07-15T00:00:00Z */
            @NotNull Instant plannedAt) {
    }

    public record CheckResponse(DecisionView planned, DecisionView current) {
    }

    /**
     * 运营输入计划上架时间后，返回计划时刻与当前时刻两份相互独立的判定。
     */
    @PostMapping("/check")
    public CheckResponse check(@RequestBody @Valid CheckRequest req) {
        Decision planned = availabilityService.decideAt(
                req.programId(), req.languageVersionId(), req.region(), req.channel(), req.plannedAt());
        Decision current = availabilityService.decideNow(
                req.programId(), req.languageVersionId(), req.region(), req.channel());
        return new CheckResponse(DecisionView.of(planned), DecisionView.of(current));
    }

    public record DayStatus(String date, String status, String reason, String reasonText) {
    }

    public record ChannelCalendar(String channel, List<DayStatus> days) {
    }

    public record CalendarResponse(String month, List<ChannelCalendar> channels) {
    }

    /**
     * 授权日历：按“每日 00:00 UTC”采样判定，输出该地区各渠道当月每天的状态。
     *
     * @param month 格式 yyyy-MM，按 UTC 解释
     */
    @GetMapping("/calendar")
    public CalendarResponse calendar(@RequestParam Long programId,
                                     @RequestParam Long languageVersionId,
                                     @RequestParam String region,
                                     @RequestParam String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        Set<String> channelCodes = new LinkedHashSet<>();
        contractRepository.findByProgramIdAndRegionCodeOrderByChannelCodeAscValidFromAsc(programId, region)
                .forEach(c -> channelCodes.add(c.getChannelCode()));
        exclusivityRepository.findByProgramIdAndRegionCode(programId, region)
                .forEach(e -> channelCodes.add(e.getChannelCode()));

        LocalDate first = yearMonth.atDay(1);
        int days = yearMonth.lengthOfMonth();

        List<ChannelCalendar> channels = channelCodes.stream().map(channel -> {
            List<DayStatus> dayStatuses = new java.util.ArrayList<>(days);
            for (int i = 0; i < days; i++) {
                LocalDate day = first.plusDays(i);
                Instant sample = day.atStartOfDay().toInstant(ZoneOffset.UTC);
                Decision d = availabilityService.decideAt(programId, languageVersionId, region, channel, sample);
                String reason = d.reasons().isEmpty() ? null : d.reasons().get(0).name();
                String reasonText = d.reasons().isEmpty() ? null : Views.messageFor(d.reasons().get(0));
                dayStatuses.add(new DayStatus(day.toString(), d.status().name(), reason, reasonText));
            }
            return new ChannelCalendar(channel, dayStatuses);
        }).toList();

        return new CalendarResponse(yearMonth.toString(), channels);
    }
}
