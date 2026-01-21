package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private HighTransactionAmountServiceImpl highTransactionAmountService;

    private static final String TEST_USER_ID = "testUser";

    private Alert createMockAlert(String userId) {
        Alert alert = mock(Alert.class);
        lenient().when(alert.getUserId()).thenReturn(userId);
        return alert;
    }

    private TransactionEvent createTransaction(double amount, Instant timestamp) {
        TransactionEvent event = mock(TransactionEvent.class);
        when(event.getTimestamp()).thenReturn(timestamp);
        when(event.getAmount()).thenReturn(amount);
        return event;
    }

    private Instant recentTimestamp() {
        return Instant.now().minusSeconds(60 * 60);
    }

    private Instant oldTimestamp() {
        return Instant.now().minusSeconds(25 * 60 * 60);
    }

    @BeforeEach
    void setUp() {
        lenient().when(alertGenerator.generateHighTransactionAlert(anyString()))
                .thenAnswer(invocation -> createMockAlert(invocation.getArgument(0)));
    }

    @Test
    void testEmptyTransactionsList() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testAllTransactionsOlderThan24Hours() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, oldTimestamp()));
        transactions.add(createTransaction(200.0, oldTimestamp()));
        transactions.add(createTransaction(300.0, oldTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testSingleTransactionWithin24Hours() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMultipleTransactionsWithin24HoursNoAlert() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, recentTimestamp()));
        transactions.add(createTransaction(100.0, recentTimestamp()));
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testTransactionExceeds5xAverageGeneratesAlert() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(1000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testMultipleTransactionsExceed5xAverageGeneratesMultipleAlerts() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(1000.0, recentTimestamp()));
        transactions.add(createTransaction(1000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(2, result.size());
        verify(alertGenerator, times(2)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testTransactionExactlyAt5xThresholdNoAlert() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, recentTimestamp()));
        transactions.add(createTransaction(500.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testTransactionJustAbove5xThresholdGeneratesAlert() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(600.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testTransactionExactlyAt24HourBoundary() {
        List<TransactionEvent> transactions = new ArrayList<>();
        Instant exactlyAt24Hours = Instant.now().minusSeconds(24 * 60 * 60);
        transactions.add(createTransaction(100.0, exactlyAt24Hours));
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertNotNull(result);
    }

    @Test
    void testTransactionJustInsideThe24HourWindow() {
        List<TransactionEvent> transactions = new ArrayList<>();
        Instant justInside = Instant.now().minusSeconds(24 * 60 * 60 - 1);
        transactions.add(createTransaction(100.0, justInside));
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testTransactionJustOutsideThe24HourWindow() {
        List<TransactionEvent> transactions = new ArrayList<>();
        Instant justOutside = Instant.now().minusSeconds(24 * 60 * 60 + 1);
        transactions.add(createTransaction(100.0, justOutside));
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMixOfOldAndNewTransactions() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, oldTimestamp()));
        transactions.add(createTransaction(200.0, oldTimestamp()));
        transactions.add(createTransaction(100.0, recentTimestamp()));
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testMixOfOldAndNewTransactionsWithHighAmount() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, oldTimestamp()));
        for (int i = 0; i < 10; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(1000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testZeroAmountTransactions() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(0.0, recentTimestamp()));
        transactions.add(createTransaction(0.0, recentTimestamp()));
        transactions.add(createTransaction(0.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testNegativeAmountTransactions() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(-100.0, recentTimestamp()));
        transactions.add(createTransaction(-100.0, recentTimestamp()));
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertNotNull(result);
    }

    @Test
    void testLargeNumberOfTransactions() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(100000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testAlertGeneratorCalledWithCorrectUserId() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(1000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();
        String specificUserId = "specificUser123";

        highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, specificUserId);

        verify(alertGenerator, times(1)).generateHighTransactionAlert(specificUserId);
    }

    @Test
    void testAlertsAddedToExistingAlertsList() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(1000.0, recentTimestamp()));

        Alert existingAlert = mock(Alert.class);
        List<Alert> alerts = new ArrayList<>();
        alerts.add(existingAlert);

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(2, result.size());
        assertEquals(existingAlert, result.get(0));
    }

    @Test
    void testMultipleAlertsForSameUser() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transactions.add(createTransaction(10.0, recentTimestamp()));
        }
        transactions.add(createTransaction(10000.0, recentTimestamp()));
        transactions.add(createTransaction(10000.0, recentTimestamp()));
        transactions.add(createTransaction(10000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(3, result.size());
        verify(alertGenerator, times(3)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testOldTransactionWithHighAmountNoAlertWhenAverageIsNaN() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(1000.0, oldTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testReturnsSameAlertsListReference() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(100.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertSame(alerts, result);
    }

    @Test
    void testVerySmallAmountTransactions() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transactions.add(createTransaction(0.001, recentTimestamp()));
        }
        transactions.add(createTransaction(1.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testVeryLargeAmountTransactions() {
        List<TransactionEvent> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            transactions.add(createTransaction(1000000.0, recentTimestamp()));
        }
        transactions.add(createTransaction(100000000.0, recentTimestamp()));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }
}
