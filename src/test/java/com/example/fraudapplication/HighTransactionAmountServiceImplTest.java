package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
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

    private HighTransactionAmountServiceImpl service;
    private AlertGeneratorImpl alertGenerator;

    @BeforeEach
    void setUp() {
        alertGenerator = new AlertGeneratorImpl();
        service = new HighTransactionAmountServiceImpl(alertGenerator);
    }

    @Test
    void testEmptyTransactionsList() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
    }

    @Test
    void testSingleTransactionWithinTwentyFourHoursNoFraud() {
        TransactionEvent event = new TransactionEvent(
                Instant.now().minusSeconds(3600),
                100.0,
                "user1",
                "service1"
        );

        List<TransactionEvent> transactions = List.of(event);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
    }

    @Test
    void testMultipleTransactionsWithOneExceedingFiveTimesAverage() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(1000), 1.0, "user1", "service1"),
                new TransactionEvent(now.minusSeconds(2000), 1.0, "user1", "service2"),
                new TransactionEvent(now.minusSeconds(3000), 1.0, "user1", "service3"),
                new TransactionEvent(now.minusSeconds(4000), 1.0, "user1", "service4"),
                new TransactionEvent(now.minusSeconds(5000), 1.0, "user1", "service5"),
                new TransactionEvent(now.minusSeconds(6000), 30.0, "user1", "service6")
        );

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, "user1");

        assertEquals(1, result.size());
    }

    @Test
    void testTransactionsOlderThanTwentyFourHoursMixedWithRecent() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(48 * 3600), 100.0, "user1", "service1"),
                new TransactionEvent(now.minusSeconds(3600), 100.0, "user1", "service2")
        );

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
    }

    @Test
    void testAllTransactionsWithinTwentyFourHoursNoneExceedThreshold() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = List.of(
                new TransactionEvent(now.minusSeconds(1000), 100.0, "user1", "service1"),
                new TransactionEvent(now.minusSeconds(2000), 100.0, "user1", "service2"),
                new TransactionEvent(now.minusSeconds(3000), 100.0, "user1", "service3")
        );

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
    }
}
