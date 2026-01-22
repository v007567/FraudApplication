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
    }

    @Test
    void testTransactionsWithNoHighAmounts() {
        String userId = "user1";
        Instant now = Instant.now();

        List<TransactionEvent> transactions = new ArrayList<>();
        TransactionEvent event1 = new TransactionEvent();
        event1.setTimestamp(now.minusSeconds(3600));
        event1.setAmount(100.0);
        event1.setUserID(userId);
        event1.setServiceID("service1");
        transactions.add(event1);

        TransactionEvent event2 = new TransactionEvent();
        event2.setTimestamp(now.minusSeconds(7200));
        event2.setAmount(100.0);
        event2.setUserID(userId);
        event2.setServiceID("service2");
        transactions.add(event2);

        TransactionEvent event3 = new TransactionEvent();
        event3.setTimestamp(now.minusSeconds(10800));
        event3.setAmount(100.0);
        event3.setUserID(userId);
        event3.setServiceID("service3");
        transactions.add(event3);

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testTransactionExceeds5xAverage() {
        String userId = "user1";
        Instant now = Instant.now();

        Alert mockAlert = new Alert();
        mockAlert.setUserId(userId);
        mockAlert.setAlertName("HIGH_TRANSACTION");
        mockAlert.setAlertMessage("Transaction amount is 5x above the user's average in the last 24 hours");
        mockAlert.setAlertTime("2026-01-22");

        when(alertGenerator.generateHighTransactionAlert(anyString())).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            TransactionEvent event = new TransactionEvent();
            event.setTimestamp(now.minusSeconds(3600 + i * 100));
            event.setAmount(100.0);
            event.setUserID(userId);
            event.setServiceID("service" + i);
            transactions.add(event);
        }
        TransactionEvent highTransaction = new TransactionEvent();
        highTransaction.setTimestamp(now.minusSeconds(1800));
        highTransaction.setAmount(5000.0);
        highTransaction.setUserID(userId);
        highTransaction.setServiceID("serviceHigh");
        transactions.add(highTransaction);

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    void testMixOfTransactionsInsideAndOutside24HourWindow() {
        String userId = "user1";
        Instant now = Instant.now();

        Alert mockAlert = new Alert();
        mockAlert.setUserId(userId);
        mockAlert.setAlertName("HIGH_TRANSACTION");
        mockAlert.setAlertMessage("Transaction amount is 5x above the user's average in the last 24 hours");
        mockAlert.setAlertTime("2026-01-22");

        when(alertGenerator.generateHighTransactionAlert(anyString())).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            TransactionEvent event = new TransactionEvent();
            event.setTimestamp(now.minusSeconds(3600 + i * 100));
            event.setAmount(100.0);
            event.setUserID(userId);
            event.setServiceID("service" + i);
            transactions.add(event);
        }
        TransactionEvent oldTransaction = new TransactionEvent();
        oldTransaction.setTimestamp(now.minusSeconds(25 * 3600));
        oldTransaction.setAmount(100.0);
        oldTransaction.setUserID(userId);
        oldTransaction.setServiceID("serviceOld");
        transactions.add(oldTransaction);

        TransactionEvent highTransaction = new TransactionEvent();
        highTransaction.setTimestamp(now.minusSeconds(1800));
        highTransaction.setAmount(5000.0);
        highTransaction.setUserID(userId);
        highTransaction.setServiceID("serviceHigh");
        transactions.add(highTransaction);

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, userId);

        assertEquals(1, result.size());
    }
}
