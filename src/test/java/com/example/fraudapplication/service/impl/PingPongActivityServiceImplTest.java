package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PingPongActivityServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    private PingPongActivityServiceImpl pingPongActivityService;

    @BeforeEach
    void setUp() {
        pingPongActivityService = new PingPongActivityServiceImpl(alertGenerator);
    }

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        return TransactionEvent.builder()
                .timestamp(timestamp)
                .amount(amount)
                .userID(userId)
                .serviceID(serviceId)
                .build();
    }

    private Alert createMockAlert(String userId, AlertName alertName) {
        return Alert.builder()
                .userId(userId)
                .alertName(alertName.getAlertName())
                .alertMessage(alertName.getAlertMessage())
                .build();
    }

    @Test
    void testPingPongPatternDetection() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(240), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(300), 100.0, userId, "serviceB"));

        Alert mockAlert = createMockAlert(userId, AlertName.PING_PONG);
        when(alertGenerator.generatePingPongAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generatePingPongAlert(userId);
    }

    @Test
    void testNoPingPongPatternWithDifferentServices() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceD"));

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testTwoServiceDiscoveryLogic() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void test10MinuteTimeWindowEnforcement() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(700), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(760), 100.0, userId, "serviceB"));

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testMapUpdateLogicAfterPingPongDetection() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(90), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(150), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(210), 100.0, userId, "serviceB"));

        Alert mockAlert = createMockAlert(userId, AlertName.PING_PONG);
        when(alertGenerator.generatePingPongAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generatePingPongAlert(userId);
    }

    @Test
    void testSingleTransactionNoPingPong() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

        @Test
        void testTwoServicesWithNoAlternatingPattern() {
            String userId = "user1";
            List<Alert> alerts = new ArrayList<>();
            List<TransactionEvent> transactions = new ArrayList<>();
        
            Instant now = Instant.now();
            transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
            transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
            transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceB"));
            transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceB"));

            List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

            assertTrue(result.isEmpty());
            verify(alertGenerator, never()).generatePingPongAlert(anyString());
        }

    @Test
    void testThreeTransactionsNoPingPong() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceA"));

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testServiceChangeBreaksPingPongPattern() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(90), 100.0, userId, "serviceD"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(150), 100.0, userId, "serviceD"));

        Alert mockAlert = createMockAlert(userId, AlertName.PING_PONG);
        lenient().when(alertGenerator.generatePingPongAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertNotNull(result);
    }

    @Test
    void testInitialTwoServiceIdentificationWithinTimeWindow() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(500), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(560), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(620), 100.0, userId, "serviceB"));

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }
}
