package com.example.fraudapplication.controller;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.service.FraudDetectorEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FraudDetectionController.class)
class FraudDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudDetectorEngineService fraudDetectorEngineService;

    @Test
    void testCheckAllFraudTransactionsEndpoint() throws Exception {
        List<Alert> mockAlerts = new ArrayList<>();
        mockAlerts.add(Alert.builder()
                .userId("user1")
                .alertName(AlertName.HIGH_TRANSACTION.getAlertName())
                .alertMessage(AlertName.HIGH_TRANSACTION.getAlertMessage())
                .alertTime("2024-01-01 12:00:00 PM")
                .build());

        when(fraudDetectorEngineService.checkAllFraudActivities(anyList())).thenReturn(mockAlerts);

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[0].alertName").value(AlertName.HIGH_TRANSACTION.getAlertName()));
    }

    @Test
    void testCheckAllFraudTransactionsReturnsEmptyList() throws Exception {
        when(fraudDetectorEngineService.checkAllFraudActivities(anyList())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testCheckAllFraudTransactionsReturnsMultipleAlerts() throws Exception {
        List<Alert> mockAlerts = new ArrayList<>();
        mockAlerts.add(Alert.builder()
                .userId("user1")
                .alertName(AlertName.HIGH_TRANSACTION.getAlertName())
                .alertMessage(AlertName.HIGH_TRANSACTION.getAlertMessage())
                .alertTime("2024-01-01 12:00:00 PM")
                .build());
        mockAlerts.add(Alert.builder()
                .userId("user1")
                .alertName(AlertName.PING_PONG.getAlertName())
                .alertMessage(AlertName.PING_PONG.getAlertMessage())
                .alertTime("2024-01-01 12:00:00 PM")
                .build());
        mockAlerts.add(Alert.builder()
                .userId("user2")
                .alertName(AlertName.MULTIPLE_SERVICE.getAlertName())
                .alertMessage(AlertName.MULTIPLE_SERVICE.getAlertMessage())
                .alertTime("2024-01-01 12:00:00 PM")
                .build());

        when(fraudDetectorEngineService.checkAllFraudActivities(anyList())).thenReturn(mockAlerts);

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].alertName").value(AlertName.HIGH_TRANSACTION.getAlertName()))
                .andExpect(jsonPath("$[1].alertName").value(AlertName.PING_PONG.getAlertName()))
                .andExpect(jsonPath("$[2].alertName").value(AlertName.MULTIPLE_SERVICE.getAlertName()));
    }

    @Test
    void testDownloadTransactionEventsEndpoint() throws Exception {
        mockMvc.perform(get("/fraud/download"))
                .andExpect(status().isOk());
    }

    @Test
    void testDownloadTransactionEventsContentType() throws Exception {
        mockMvc.perform(get("/fraud/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    void testDownloadTransactionEventsContentDisposition() throws Exception {
        mockMvc.perform(get("/fraud/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fraudDetection.csv\""));
    }
}
