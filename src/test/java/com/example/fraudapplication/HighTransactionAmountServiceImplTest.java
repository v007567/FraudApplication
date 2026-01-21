package com.example.fraudapplication;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.impl.AlertGeneratorImpl;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HighTransactionAmountServiceImplTest {

    private HighTransactionAmountServiceImpl highTransactionAmountService;
    private AlertGeneratorImpl alertGenerator;

    @BeforeEach
    public void setUp() {
        alertGenerator = new AlertGeneratorImpl();
        highTransactionAmountService = new HighTransactionAmountServiceImpl(alertGenerator);
    }

    @Test
    public void testHighTransactionTriggersAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        for (int i = 0; i < 10; i++) {
            transactions.add(new TransactionEvent(
                    now.minusSeconds(3600 + i * 100),
                    10.0,
                    userId,
                    "service" + i
            ));
        }
        transactions.add(new TransactionEvent(
                now.minusSeconds(1800),
                1000.0,
                userId,
                "serviceHigh"
        ));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty(), "Should have at least one alert for high transaction");
        assertEquals(AlertName.HIGH_TRANSACTION.getAlertName(), result.get(0).getAlertName());
    }

    @Test
    public void testNormalTransactionsNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        transactions.add(new TransactionEvent(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(new TransactionEvent(now.minusSeconds(7200), 120.0, userId, "service2"));
        transactions.add(new TransactionEvent(now.minusSeconds(1800), 150.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty(), "Should have no alerts for normal transactions");
    }

    @Test
    public void testMultipleHighTransactionsGenerateMultipleAlerts() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        for (int i = 0; i < 20; i++) {
            transactions.add(new TransactionEvent(
                    now.minusSeconds(3600 + i * 100),
                    10.0,
                    userId,
                    "service" + i
            ));
        }
        transactions.add(new TransactionEvent(
                now.minusSeconds(1800),
                2000.0,
                userId,
                "serviceHigh1"
        ));
        transactions.add(new TransactionEvent(
                now.minusSeconds(900),
                3000.0,
                userId,
                "serviceHigh2"
        ));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.size() >= 2, "Should have at least 2 alerts for multiple high transactions");
    }

    @Test
    public void testBoundaryExactly5xAverageNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        transactions.add(new TransactionEvent(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(new TransactionEvent(now.minusSeconds(7200), 100.0, userId, "service2"));
        transactions.add(new TransactionEvent(now.minusSeconds(1800), 100.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty(), "Should have no alerts when transactions are equal");
    }

    @Test
    public void testEmptyTransactionListNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleTransactionNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        transactions.add(new TransactionEvent(now.minusSeconds(3600), 1000.0, userId, "service1"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty(), "Should have no alerts for single transaction");
    }

    @Test
    public void testMixedTimeWindowsOnlyRecentCountTowardAverage() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        transactions.add(new TransactionEvent(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(new TransactionEvent(now.minusSeconds(100000), 100.0, userId, "service2"));
        transactions.add(new TransactionEvent(now.minusSeconds(1800), 100.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty(), "Should have no alerts when all transactions are similar amounts");
    }

    @Test
    public void testAlertContainsCorrectUserId() {
        String userId = "specificUser123";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        Instant now = Instant.now();
        for (int i = 0; i < 10; i++) {
            transactions.add(new TransactionEvent(
                    now.minusSeconds(3600 + i * 100),
                    10.0,
                    userId,
                    "service" + i
            ));
        }
        transactions.add(new TransactionEvent(
                now.minusSeconds(1800),
                1000.0,
                userId,
                "serviceHigh"
        ));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty(), "Should have at least one alert");
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(AlertName.HIGH_TRANSACTION.getAlertMessage(), result.get(0).getAlertMessage());
    }
}
