package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.HighTransactionAmountService;
import com.example.fraudapplication.service.MultipleServiceTransaction;
import com.example.fraudapplication.service.PingPongActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectorEngineServiceImplTest {

    @Mock
    private HighTransactionAmountService highTransactionAmountService;

    @Mock
    private MultipleServiceTransaction multipleServiceTransaction;

    @Mock
    private PingPongActivityService pingPongActivityService;

    private FraudDetectorEngineServiceImpl fraudDetectorEngineService;

        @BeforeEach
        void setUp() {
            fraudDetectorEngineService = new FraudDetectorEngineServiceImpl(
                    highTransactionAmountService,
                    multipleServiceTransaction,
                    pingPongActivityService
            );
        }

        private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
            return TransactionEvent.builder()
                    .timestamp(timestamp)
                    .amount(amount)
                    .userID(userId)
                    .serviceID(serviceId)
                    .build();
        }

        @Test
        void testCheckAllFraudActivitiesCallsAllDetectionServices() {
            List<TransactionEvent> events = new ArrayList<>();
            Instant now = Instant.now();
            events.add(createTransaction(now, 100.0, "user1", "serviceA"));
            events.add(createTransaction(now.plusSeconds(60), 200.0, "user1", "serviceB"));

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
    }

    @Test
    void testUserTransactionMapping() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user2", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 300.0, "user1", "serviceC"));

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user2"));
    }

    @Test
    void testCheckHighTransactionFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 5000.0, "user1", "serviceB"));

        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName(AlertName.HIGH_TRANSACTION.getAlertName())
                .alertMessage(AlertName.HIGH_TRANSACTION.getAlertMessage())
                .build();
        List<Alert> alertList = new ArrayList<>();
        alertList.add(mockAlert);

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenReturn(alertList);

        List<Alert> result = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
    }

    @Test
    void testCheckPingPongFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user1", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 300.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(180), 400.0, "user1", "serviceB"));

        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName(AlertName.PING_PONG.getAlertName())
                .alertMessage(AlertName.PING_PONG.getAlertMessage())
                .build();
        List<Alert> alertList = new ArrayList<>();
        alertList.add(mockAlert);

        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenReturn(alertList);

        List<Alert> result = fraudDetectorEngineService.checkPingPongFraud(events);

        assertNotNull(result);
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
    }

    @Test
    void testCheckMultipleServiceFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user1", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 300.0, "user1", "serviceC"));
                events.add(createTransaction(now.plusSeconds(180), 400.0, "user1", "serviceD"));

        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName(AlertName.MULTIPLE_SERVICE.getAlertName())
                .alertMessage(AlertName.MULTIPLE_SERVICE.getAlertMessage())
                .build();
        List<Alert> alertList = new ArrayList<>();
        alertList.add(mockAlert);

        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenReturn(alertList);

        List<Alert> result = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertNotNull(result);
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
    }

    @Test
    void testAlertAggregationFromAllServices() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user1", "serviceB"));

                Alert highTransactionAlert= Alert.builder()
                .userId("user1")
                .alertName(AlertName.HIGH_TRANSACTION.getAlertName())
                .alertMessage(AlertName.HIGH_TRANSACTION.getAlertMessage())
                .build();
        Alert pingPongAlert = Alert.builder()
                .userId("user1")
                .alertName(AlertName.PING_PONG.getAlertName())
                .alertMessage(AlertName.PING_PONG.getAlertMessage())
                .build();
        Alert multipleServiceAlert = Alert.builder()
                .userId("user1")
                .alertName(AlertName.MULTIPLE_SERVICE.getAlertName())
                .alertMessage(AlertName.MULTIPLE_SERVICE.getAlertMessage())
                .build();

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(highTransactionAlert);
                    return alerts;
                });
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(pingPongAlert);
                    return alerts;
                });
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(multipleServiceAlert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testEmptyEventsList() {
        List<TransactionEvent> events = new ArrayList<>();

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(highTransactionAmountService, never()).checkHighAmountTransactions(anyList(), anyList(), anyString());
    }

    @Test
    void testMultipleUsersProcessedSeparately() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user2", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 300.0, "user3", "serviceC"));

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenReturn(new ArrayList<>());

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verify(highTransactionAmountService, times(3)).checkHighAmountTransactions(anyList(), anyList(), anyString());
        verify(pingPongActivityService, times(3)).checkPingPongActivity(anyList(), anyList(), anyString());
        verify(multipleServiceTransaction, times(3)).checkMultipleServiceTransactions(anyList(), anyList(), anyString());
    }

    @Test
    void testCheckHighTransactionFraudWithMultipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user2", "serviceB"));

                when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                        .thenReturn(new ArrayList<>());

                List<Alert> result = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertNotNull(result);
        verify(highTransactionAmountService, times(2)).checkHighAmountTransactions(anyList(), anyList(), anyString());
    }

    @Test
    void testCheckPingPongFraudWithMultipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user2", "serviceB"));

                when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                        .thenReturn(new ArrayList<>());

                List<Alert> result = fraudDetectorEngineService.checkPingPongFraud(events);

        assertNotNull(result);
        verify(pingPongActivityService, times(2)).checkPingPongActivity(anyList(), anyList(), anyString());
    }

    @Test
    void testCheckMultipleServiceFraudWithMultipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user2", "serviceB"));

                when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                        .thenReturn(new ArrayList<>());

                List<Alert> result = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertNotNull(result);
        verify(multipleServiceTransaction, times(2)).checkMultipleServiceTransactions(anyList(), anyList(), anyString());
    }
}
