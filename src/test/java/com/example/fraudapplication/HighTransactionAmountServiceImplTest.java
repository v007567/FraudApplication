package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.AlertGeneratorImpl;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HighTransactionAmountServiceImplTest {

    private AlertGenerator alertGenerator;
    private HighTransactionAmountServiceImpl highTransactionAmountService;

    @BeforeEach
    void setUp() {
        alertGenerator = new AlertGeneratorImpl();
        highTransactionAmountService = new HighTransactionAmountServiceImpl(alertGenerator);
    }

    @Test
    void testEmptyTransactionsList() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();
        String userId = "user1";

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testTransactionsWithinThresholdNoAlert() {
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
    }

    @Test
    void testTransactionExceeds5xAverageTriggersAlert() {
        String userId = "user1";
        Instant now = Instant.now();

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
            new TransactionEvent(now.minusSeconds(900), 1000.0, userId, "service10")
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    void testMixedTransactionsWithinAndOutside24HourWindow() {
        String userId = "user1";
        Instant now = Instant.now();
        
        List<TransactionEvent> transactions = List.of(
            new TransactionEvent(now.minusSeconds(3600), 100.0, userId, "service1"),
            new TransactionEvent(now.minusSeconds(100000), 100.0, userId, "service2"),
            new TransactionEvent(now.minusSeconds(7200), 100.0, userId, "service3")
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }
}
