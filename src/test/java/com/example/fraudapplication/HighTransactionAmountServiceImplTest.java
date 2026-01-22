package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    private HighTransactionAmountServiceImpl highTransactionAmountService;

    @BeforeEach
    void setUp() {
        highTransactionAmountService = new HighTransactionAmountServiceImpl(alertGenerator);
    }

    @Test
    void testEmptyTransactionsList() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();
        String userId = "user1";

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testNormalTransactionsNoAlert() {
        String userId = "user1";
        Instant now = Instant.now();
        
        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(3600), 100.0, userId, "service1"),
                new TransactionEvent(now.minusSeconds(7200), 100.0, userId, "service2"),
                new TransactionEvent(now.minusSeconds(10800), 100.0, userId, "service3")
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testHighTransactionTriggersAlert() {
        String userId = "user1";
        Instant now = Instant.now();
        Alert mockAlert = new Alert(userId, "HIGH_TRANSACTION", "High transaction detected", now.toString());

        when(alertGenerator.generateHighTransactionAlert(userId)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(3600), 20.0, userId, "service1"),
                new TransactionEvent(now.minusSeconds(7200), 20.0, userId, "service2"),
                new TransactionEvent(now.minusSeconds(10800), 20.0, userId, "service3"),
                new TransactionEvent(now.minusSeconds(14400), 20.0, userId, "service4"),
                new TransactionEvent(now.minusSeconds(18000), 20.0, userId, "service5"),
                new TransactionEvent(now.minusSeconds(21600), 600.0, userId, "service6")
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
        assertEquals(mockAlert, result.get(0));
        verify(alertGenerator, times(1)).generateHighTransactionAlert(userId);
    }

    @Test
    void testMixedTimestampsWithinAndOutside24Hours() {
        String userId = "user1";
        Instant now = Instant.now();
        
        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(3600), 100.0, userId, "service1"),
                new TransactionEvent(now.minusSeconds(7200), 100.0, userId, "service2"),
                new TransactionEvent(now.minusSeconds(25 * 60 * 60), 100.0, userId, "service3"),
                new TransactionEvent(now.minusSeconds(30 * 60 * 60), 100.0, userId, "service4")
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMultipleHighTransactionsGenerateMultipleAlerts() {
        String userId = "user1";
        Instant now = Instant.now();
        Alert mockAlert = new Alert(userId, "HIGH_TRANSACTION", "High transaction detected", now.toString());

        when(alertGenerator.generateHighTransactionAlert(userId)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(3600), 10.0, userId, "service1"),
                new TransactionEvent(now.minusSeconds(7200), 10.0, userId, "service2"),
                new TransactionEvent(now.minusSeconds(10800), 10.0, userId, "service3"),
                new TransactionEvent(now.minusSeconds(14400), 10.0, userId, "service4"),
                new TransactionEvent(now.minusSeconds(18000), 10.0, userId, "service5"),
                new TransactionEvent(now.minusSeconds(21600), 10.0, userId, "service6"),
                new TransactionEvent(now.minusSeconds(25200), 10.0, userId, "service7"),
                new TransactionEvent(now.minusSeconds(28800), 10.0, userId, "service8"),
                new TransactionEvent(now.minusSeconds(32400), 10.0, userId, "service9"),
                new TransactionEvent(now.minusSeconds(36000), 10.0, userId, "service10"),
                new TransactionEvent(now.minusSeconds(39600), 2000.0, userId, "service11"),
                new TransactionEvent(now.minusSeconds(43200), 2000.0, userId, "service12")
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(2, result.size());
        verify(alertGenerator, times(2)).generateHighTransactionAlert(userId);
    }
}
