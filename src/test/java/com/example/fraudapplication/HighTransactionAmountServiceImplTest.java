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

    private static final String USER_ID = "testUser";

    @BeforeEach
    void setUp() {
        highTransactionAmountService = new HighTransactionAmountServiceImpl(alertGenerator);
    }

    @Test
    void testCheckHighAmountTransactions_EmptyList_ReturnsEmptyAlerts() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckHighAmountTransactions_SingleTransaction_NoAlert() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = List.of(
                TransactionEvent.builder()
                        .timestamp(now)
                        .amount(100.0)
                        .userID(USER_ID)
                        .serviceID("service1")
                        .build()
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckHighAmountTransactions_TransactionExceeds5xAverage_GeneratesAlert() {
        Instant now = Instant.now();
        Alert mockAlert = Alert.builder()
                .userId(USER_ID)
                .alertName("HIGH_TRANSACTION")
                .alertMessage("Transaction amount is 5x above the user's average in the last 24 hours")
                .build();
        when(alertGenerator.generateHighTransactionAlert(anyString())).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            transactions.add(TransactionEvent.builder().timestamp(now).amount(10.0).userID(USER_ID).serviceID("service1").build());
        }
        transactions.add(TransactionEvent.builder().timestamp(now).amount(600.0).userID(USER_ID).serviceID("service1").build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertEquals(1, result.size());
        assertEquals("HIGH_TRANSACTION", result.get(0).getAlertName());
    }

    @Test
    void testCheckHighAmountTransactions_TransactionDoesNotExceed5xAverage_NoAlert() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = List.of(
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build()
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckHighAmountTransactions_TransactionExactly5xAverage_NoAlert() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = List.of(
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(100.0).userID(USER_ID).serviceID("service1").build(),
                TransactionEvent.builder().timestamp(now).amount(500.0).userID(USER_ID).serviceID("service1").build()
        );
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckHighAmountTransactions_MultipleHighTransactions_GeneratesMultipleAlerts() {
        Instant now = Instant.now();
        Alert mockAlert = Alert.builder()
                .userId(USER_ID)
                .alertName("HIGH_TRANSACTION")
                .alertMessage("Transaction amount is 5x above the user's average in the last 24 hours")
                .build();
        when(alertGenerator.generateHighTransactionAlert(anyString())).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            transactions.add(TransactionEvent.builder().timestamp(now).amount(10.0).userID(USER_ID).serviceID("service1").build());
        }
        transactions.add(TransactionEvent.builder().timestamp(now).amount(600.0).userID(USER_ID).serviceID("service1").build());
        transactions.add(TransactionEvent.builder().timestamp(now).amount(700.0).userID(USER_ID).serviceID("service1").build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertEquals(2, result.size());
    }

    @Test
    void testCheckHighAmountTransactions_OldTransactionsExcludedFromAverage() {
        Instant now = Instant.now();
        Instant oldTimestamp = now.minusSeconds(25 * 60 * 60);
        Alert mockAlert = Alert.builder()
                .userId(USER_ID)
                .alertName("HIGH_TRANSACTION")
                .alertMessage("Transaction amount is 5x above the user's average in the last 24 hours")
                .build();
        when(alertGenerator.generateHighTransactionAlert(anyString())).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(oldTimestamp).amount(1000.0).userID(USER_ID).serviceID("service1").build());
        for (int i = 0; i < 9; i++) {
            transactions.add(TransactionEvent.builder().timestamp(now).amount(10.0).userID(USER_ID).serviceID("service1").build());
        }
        transactions.add(TransactionEvent.builder().timestamp(now).amount(600.0).userID(USER_ID).serviceID("service1").build());

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        assertTrue(result.size() >= 1);
    }
}
