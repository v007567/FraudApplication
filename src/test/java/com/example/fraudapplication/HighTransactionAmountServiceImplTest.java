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

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        TransactionEvent event = new TransactionEvent();
        event.setTimestamp(timestamp);
        event.setAmount(amount);
        event.setUserID(userId);
        event.setServiceID(serviceId);
        return event;
    }

    @Test
    public void testOldTransactionExceeds5xRecentAverage_shouldGenerateAlert() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(100000), 1000.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(AlertName.HIGH_TRANSACTION.getAlertName(), result.get(0).getAlertName());
    }

    @Test
    public void testAllTransactionsWithin24Hours_noneExceed5xAverage_shouldNotGenerateAlert() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(1800), 500.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMultipleOldHighTransactions_shouldGenerateMultipleAlerts() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 10.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 10.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(100000), 1000.0, userId, "service3"));
        transactions.add(createTransaction(now.minusSeconds(200000), 2000.0, userId, "service4"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(2, result.size());
    }

    @Test
    public void testEmptyTransactionList_shouldReturnEmptyAlerts() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleTransaction_shouldNotGenerateAlert() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 1000.0, userId, "service1"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransactionsBelowThreshold_shouldNotGenerateAlert() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 150.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(1800), 200.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testVerifyCorrectUserIdInAlert() {
        String userId = "specificUser123";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 10.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(100000), 1000.0, userId, "service2"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    public void testAlertIsAddedToExistingList() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Alert existingAlert = new Alert();
        existingAlert.setUserId("existingUser");
        existingAlert.setAlertName("EXISTING_ALERT");
        existingAlert.setAlertMessage("Existing alert message");
        existingAlert.setAlertTime("2024-01-01");
        alerts.add(existingAlert);
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 10.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(100000), 1000.0, userId, "service2"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(2, result.size());
        assertEquals("existingUser", result.get(0).getUserId());
        assertEquals(userId, result.get(1).getUserId());
    }

    @Test
    public void testVerySmallAmounts_shouldHandleCorrectly() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 0.001, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 0.001, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(100000), 0.10, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
    }

    @Test
    public void testVeryLargeAmounts_shouldHandleCorrectly() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 1000000.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 1000000.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(100000), 100000000.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
    }

    @Test
    public void testOldTransactionAtExactly5xAverage_shouldNotGenerateAlert() {
        String userId = "testUser";
        List<Alert> alerts = new ArrayList<>();
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(100000), 500.0, userId, "service3"));

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }
}
