package com.example.licensing.web;

import com.example.licensing.service.AvailabilityEngine;
import com.example.licensing.service.AvailabilityService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private final AvailabilityService service;

    public AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    public record AvailabilityRequest(
            @NotBlank String programCode,
            @NotBlank String regionCode,
            @NotBlank String channelCode,
            @NotBlank String languageCode,
            @NotNull Instant plannedAt) {}

    /**
     * 运营输入计划上架时间，返回计划状态；同时独立返回“当前时刻”状态。
     * 计划状态与当前状态分开显示，互不混淆。
     */
    @PostMapping("/availability")
    public AvailabilityService.Result check(@RequestBody AvailabilityRequest req) {
        return service.evaluate(req.programCode(), req.regionCode(),
                req.channelCode(), req.languageCode(), req.plannedAt());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
                "error", "NOT_FOUND",
                "message", ex.getMessage()));
    }
}
