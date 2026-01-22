package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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

@SpringBootTest
public class HighTransactionAmountServiceImplTest {

    @MockBean
    private AlertGenerator alertGenerator;

    @Autowired
    private HighTransactionAmountServiceImpl highTransactionAmountService;

    @Test
    void testEmptyTransactionList_ReturnsAlertsUnchanged() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testTransactionsWithin24Hours_NoneExceedingThreshold_NoAlertGenerated() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(3600))
                .amount(100.0)
                .userID("user1")
                .serviceID("service1")
                .build());
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(7200))
                .amount(100.0)
                .userID("user1")
                .serviceID("service2")
                .build());
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(10800))
                .amount(100.0)
                .userID("user1")
                .serviceID("service3")
                .build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testTransactionsWithin24Hours_OneExceedsThreshold_AlertGenerated() {
        Instant now = Instant.now();
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("HIGH_TRANSACTION")
                .alertMessage("Transaction amount is 5x above the user's average")
                .build();
        when(alertGenerator.generateHighTransactionAlert("user1")).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            transactions.add(TransactionEvent.builder()
                    .timestamp(now.minusSeconds(3600 + i * 100))
                    .amount(10.0)
                    .userID("user1")
                    .serviceID("service" + i)
                    .build());
        }
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(7200))
                .amount(10000.0)
                .userID("user1")
                .serviceID("serviceHigh")
                .build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertEquals(1, result.size());
        assertEquals(mockAlert, result.get(0));
        verify(alertGenerator, times(1)).generateHighTransactionAlert("user1");
    }

    @Test
    void testTransactionsOutside24HourWindow_FilteredOut() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(25 * 60 * 60))
                .amount(100.0)
                .userID("user1")
                .serviceID("service1")
                .build());
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(26 * 60 * 60))
                .amount(100.0)
                .userID("user1")
                .serviceID("service2")
                .build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMixedTransactions_OnlyRecentConsideredForAverage() {
        Instant now = Instant.now();
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("HIGH_TRANSACTION")
                .alertMessage("Transaction amount is 5x above the user's average")
                .build();
        when(alertGenerator.generateHighTransactionAlert("user1")).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(3600))
                .amount(100.0)
                .userID("user1")
                .serviceID("service1")
                .build());
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(25 * 60 * 60))
                .amount(10000.0)
                .userID("user1")
                .serviceID("service2")
                .build());
        transactions.add(TransactionEvent.builder()
                .timestamp(now.minusSeconds(7200))
                .amount(100.0)
                .userID("user1")
                .serviceID("service3")
                .build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert("user1");
    }
}
