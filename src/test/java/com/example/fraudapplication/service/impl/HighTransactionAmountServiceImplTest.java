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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    private HighTransactionAmountServiceImpl highTransactionAmountService;

    @BeforeEach
    void setUp() {
        highTransactionAmountService = new HighTransactionAmountServiceImpl(alertGenerator);
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
        void testAlertGeneratedWhenTransactionExceeds5xAverage() {
            String userId = "user1";
            List<Alert> alerts = new ArrayList<>();
            List<TransactionEvent> transactions = new ArrayList<>();
        
            Instant now = Instant.now();
            transactions.add(createTransaction(now.minusSeconds(100), 10.0, userId, "serviceA"));
            transactions.add(createTransaction(now.minusSeconds(200), 10.0, userId, "serviceB"));
            transactions.add(createTransaction(now.minusSeconds(300), 10.0, userId, "serviceC"));
            transactions.add(createTransaction(now.minusSeconds(400), 10.0, userId, "serviceD"));
            transactions.add(createTransaction(now.minusSeconds(500), 10.0, userId, "serviceE"));
            transactions.add(createTransaction(now.minusSeconds(600), 10.0, userId, "serviceF"));
            transactions.add(createTransaction(now.minusSeconds(700), 10.0, userId, "serviceG"));
            transactions.add(createTransaction(now.minusSeconds(800), 10.0, userId, "serviceH"));
            transactions.add(createTransaction(now.minusSeconds(900), 10.0, userId, "serviceI"));
            transactions.add(createTransaction(now.minusSeconds(1000), 1000.0, userId, "serviceJ"));

            Alert mockAlert = createMockAlert(userId, AlertName.HIGH_TRANSACTION);
            when(alertGenerator.generateHighTransactionAlert(userId)).thenReturn(mockAlert);

            List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

            assertFalse(result.isEmpty());
            verify(alertGenerator, atLeastOnce()).generateHighTransactionAlert(userId);
        }

    @Test
    void testNoAlertWhenTransactionWithinNormalRange() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now.minusSeconds(100), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.minusSeconds(200), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.minusSeconds(300), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.minusSeconds(400), 100.0, userId, "serviceD"));
        transactions.add(createTransaction(now.minusSeconds(500), 100.0, userId, "serviceE"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

        @Test
        void testCorrectAverageCalculationOver24HourWindow() {
            String userId = "user1";
            List<Alert> alerts = new ArrayList<>();
            List<TransactionEvent> transactions = new ArrayList<>();
        
            Instant now = Instant.now();
            transactions.add(createTransaction(now.minusSeconds(1000), 10.0, userId, "serviceA"));
            transactions.add(createTransaction(now.minusSeconds(2000), 10.0, userId, "serviceB"));
            transactions.add(createTransaction(now.minusSeconds(3000), 10.0, userId, "serviceC"));
            transactions.add(createTransaction(now.minusSeconds(4000), 10.0, userId, "serviceD"));
            transactions.add(createTransaction(now.minusSeconds(5000), 10.0, userId, "serviceE"));
            transactions.add(createTransaction(now.minusSeconds(6000), 10.0, userId, "serviceF"));
            transactions.add(createTransaction(now.minusSeconds(7000), 10.0, userId, "serviceG"));
            transactions.add(createTransaction(now.minusSeconds(8000), 10.0, userId, "serviceH"));
            transactions.add(createTransaction(now.minusSeconds(9000), 10.0, userId, "serviceI"));
            transactions.add(createTransaction(now.minusSeconds(10000), 2000.0, userId, "serviceJ"));

            Alert mockAlert = createMockAlert(userId, AlertName.HIGH_TRANSACTION);
            when(alertGenerator.generateHighTransactionAlert(userId)).thenReturn(mockAlert);

            List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

            assertFalse(result.isEmpty());
            verify(alertGenerator, atLeastOnce()).generateHighTransactionAlert(userId);
        }

    @Test
    void testEmptyTransactionListEdgeCase() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

        @Test
        void testTimeWindowFilteringOnlyLast24HoursConsidered() {
            String userId = "user1";
            List<Alert> alerts = new ArrayList<>();
            List<TransactionEvent> transactions = new ArrayList<>();
        
            Instant now = Instant.now();
            transactions.add(createTransaction(now.minusSeconds(100), 100.0, userId, "serviceA"));
            transactions.add(createTransaction(now.minusSeconds(200), 100.0, userId, "serviceB"));
            transactions.add(createTransaction(now.minusSeconds(300), 100.0, userId, "serviceC"));

            List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

            assertTrue(result.isEmpty());
            verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        }

    @Test
    void testSingleTransactionNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now.minusSeconds(100), 100.0, userId, "serviceA"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMultipleHighTransactionsGenerateMultipleAlerts() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        for (int i = 0; i < 20; i++) {
            transactions.add(createTransaction(now.minusSeconds(100 + i * 100), 10.0, userId, "service" + i));
        }
        transactions.add(createTransaction(now.minusSeconds(2100), 10000.0, userId, "serviceHigh1"));
        transactions.add(createTransaction(now.minusSeconds(2200), 10000.0, userId, "serviceHigh2"));

        Alert mockAlert = createMockAlert(userId, AlertName.HIGH_TRANSACTION);
        when(alertGenerator.generateHighTransactionAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeast(2)).generateHighTransactionAlert(userId);
    }
}
