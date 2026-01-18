package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private HighTransactionAmountServiceImpl highTransactionAmountService;

    private static final String USER_ID = "testUser";

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        TransactionEvent event = new TransactionEvent();
        event.setTimestamp(timestamp);
        event.setAmount(amount);
        event.setUserID(userId);
        event.setServiceID(serviceId);
        return event;
    }

    private Alert createMockAlert(String userId) {
        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setAlertName("HIGH_TRANSACTION");
        alert.setAlertMessage("Transaction amount is 5x above the user's average");
        alert.setAlertTime("2026-01-18");
        return alert;
    }

    @Test
    void testEmptyTransactionList_NoAlertGenerated() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testSingleTransaction_NoAlert() {
        Instant now = Instant.now();
        TransactionEvent transaction = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");

        List<TransactionEvent> transactions = List.of(transaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testSingleTransactionExceedingThreshold_NoAlertBecauseAverageEqualsSelf() {
        Instant now = Instant.now();
        TransactionEvent transaction = createTransaction(now.minusSeconds(3600), 1000.0, USER_ID, "service1");

        List<TransactionEvent> transactions = List.of(transaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMultipleTransactionsWithinThreshold_NoAlert() {
        Instant now = Instant.now();
        TransactionEvent t1 = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");
        TransactionEvent t2 = createTransaction(now.minusSeconds(7200), 150.0, USER_ID, "service2");
        TransactionEvent t3 = createTransaction(now.minusSeconds(10800), 120.0, USER_ID, "service3");

        List<TransactionEvent> transactions = List.of(t1, t2, t3);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testHighTransactionAlertGeneration() {
        Instant now = Instant.now();
        TransactionEvent t1 = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");
        TransactionEvent t2 = createTransaction(now.minusSeconds(7200), 100.0, USER_ID, "service2");
        TransactionEvent t3 = createTransaction(now.minusSeconds(10800), 100.0, USER_ID, "service3");
        TransactionEvent t4 = createTransaction(now.minusSeconds(14400), 100.0, USER_ID, "service4");
        TransactionEvent highTransaction = createTransaction(now.minusSeconds(25 * 60 * 60), 1000.0, USER_ID, "service5");

        Alert mockAlert = createMockAlert(USER_ID);
        when(alertGenerator.generateHighTransactionAlert(USER_ID)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = List.of(t1, t2, t3, t4, highTransaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(USER_ID);
    }

    @Test
    void test24HourWindowFiltering() {
        Instant now = Instant.now();
        TransactionEvent recentTransaction = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");
        TransactionEvent oldTransaction = createTransaction(now.minusSeconds(25 * 60 * 60), 100.0, USER_ID, "service2");

        List<TransactionEvent> transactions = List.of(recentTransaction, oldTransaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testOldTransactionExceedingThreshold_AlertGenerated() {
        Instant now = Instant.now();
        TransactionEvent recentTransaction1 = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");
        TransactionEvent recentTransaction2 = createTransaction(now.minusSeconds(7200), 100.0, USER_ID, "service2");
        TransactionEvent oldHighTransaction = createTransaction(now.minusSeconds(25 * 60 * 60), 10000.0, USER_ID, "service3");

        Alert mockAlert = createMockAlert(USER_ID);
        when(alertGenerator.generateHighTransactionAlert(USER_ID)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = List.of(recentTransaction1, recentTransaction2, oldHighTransaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(USER_ID);
    }

    @Test
    void testMultipleHighTransactions_MultipleAlertsGenerated() {
        Instant now = Instant.now();
        TransactionEvent normalTransaction = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");
        TransactionEvent highTransaction1 = createTransaction(now.minusSeconds(25 * 60 * 60), 1000.0, USER_ID, "service2");
        TransactionEvent highTransaction2 = createTransaction(now.minusSeconds(26 * 60 * 60), 1500.0, USER_ID, "service3");

        Alert mockAlert = createMockAlert(USER_ID);
        when(alertGenerator.generateHighTransactionAlert(USER_ID)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = List.of(normalTransaction, highTransaction1, highTransaction2);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertEquals(2, result.size());
        verify(alertGenerator, times(2)).generateHighTransactionAlert(USER_ID);
    }

    @Test
    void testBoundaryCondition_Exactly5xAverage_NoAlert() {
        Instant now = Instant.now();
        TransactionEvent t1 = createTransaction(now.minusSeconds(3600), 100.0, USER_ID, "service1");
        TransactionEvent boundaryTransaction = createTransaction(now.minusSeconds(1800), 500.0, USER_ID, "service2");

        List<TransactionEvent> transactions = List.of(t1, boundaryTransaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testZeroAmountTransactions_NoException() {
        Instant now = Instant.now();
        TransactionEvent zeroTransaction1 = createTransaction(now.minusSeconds(3600), 0.0, USER_ID, "service1");
        TransactionEvent zeroTransaction2 = createTransaction(now.minusSeconds(7200), 0.0, USER_ID, "service2");

        List<TransactionEvent> transactions = List.of(zeroTransaction1, zeroTransaction2);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }
}
