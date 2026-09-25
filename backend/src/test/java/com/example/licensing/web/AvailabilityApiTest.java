package com.example.licensing.web;

import com.example.licensing.model.Contract;
import com.example.licensing.repo.ContractRepository;
import com.example.licensing.repo.ExclusivityRepository;
import com.example.licensing.service.AvailabilityService;
import com.example.licensing.service.BlockReason;
import com.example.licensing.service.Decision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvailabilityController.class)
class AvailabilityApiTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AvailabilityService availabilityService;

    @MockBean
    private ContractRepository contractRepository;

    @MockBean
    private ExclusivityRepository exclusivityRepository;

    @Test
    @DisplayName("API：响应同时包含计划状态与当前状态，并带中文阻止原因")
    void checkReturnsPlannedAndCurrent() throws Exception {
        Contract matched = new Contract(3L, "LIC-2026-003", 1L, 2L, "CN", "OTT-A",
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"));

        Decision planned = Decision.blocked(
                Instant.parse("2026-07-15T00:00:00Z"), BlockReason.LICENSE_GAP, List.of(), List.of());
        Decision current = Decision.available(
                Instant.parse("2026-09-25T00:00:00Z"), matched, List.of(matched), List.of());

        when(availabilityService.decideAt(anyLong(), anyLong(), anyString(), anyString(), any(Instant.class)))
                .thenReturn(planned);
        when(availabilityService.decideNow(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(current);

        mvc.perform(post("/api/availability/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "programId": 1,
                                  "languageVersionId": 2,
                                  "region": "CN",
                                  "channel": "OTT-A",
                                  "plannedAt": "2026-07-15T00:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planned.status").value("BLOCKED"))
                .andExpect(jsonPath("$.planned.reasons[0].code").value("LICENSE_GAP"))
                .andExpect(jsonPath("$.planned.reasons[0].message").value(
                        "计划时刻落在同一组合两段授权窗口之间的空档"))
                .andExpect(jsonPath("$.current.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.current.matchedContract.contractRef").value("LIC-2026-003"));
    }
}
