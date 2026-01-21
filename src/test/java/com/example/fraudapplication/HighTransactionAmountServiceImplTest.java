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

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
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

    @Test
    void testCalculate24HourAverageWithMultipleTransactions() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 200.0));
        transactions.add(createTransaction(now.minusSeconds(10800), 150.0));
        transactions.add(createTransaction(now.minusSeconds(14400), 250.0));

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testAlertGeneratedWhenTransactionExceeds5xAverage() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0));
        transactions.add(createTransaction(now.minusSeconds(10800), 100.0));
        transactions.add(createTransaction(now.minusSeconds(14400), 100.0));
        transactions.add(createTransaction(now.minusSeconds(25 * 3600), 600.0));

        Alert mockAlert = createAlert(USER_ID, "HIGH_TRANSACTION", 
                "Transaction amount is 5x above the user's average in the last 24 hours", "2026-01-21");
        when(alertGenerator.generateHighTransactionAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, times(1)).generateHighTransactionAlert(USER_ID);
        assertEquals(1, alerts.size());
    }

    @Test
    void testNoAlertWhenTransactionIsExactly5xAverage() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0));
        transactions.add(createTransaction(now.minusSeconds(10800), 100.0));
        transactions.add(createTransaction(now.minusSeconds(14400), 100.0));
        transactions.add(createTransaction(now.minusSeconds(1800), 500.0));

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testEmptyTransactionsList() {
        List<TransactionEvent> transactions = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testSingleTransaction() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testAllTransactionsBelowThreshold() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 120.0));
        transactions.add(createTransaction(now.minusSeconds(10800), 80.0));
        transactions.add(createTransaction(now.minusSeconds(14400), 110.0));
        transactions.add(createTransaction(now.minusSeconds(1800), 150.0));

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testMultipleTransactionsExceed5xAverage() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0));
        transactions.add(createTransaction(now.minusSeconds(25 * 3600), 600.0));
        transactions.add(createTransaction(now.minusSeconds(26 * 3600), 700.0));

        Alert mockAlert = createAlert(USER_ID, "HIGH_TRANSACTION", 
                "Transaction amount is 5x above the user's average in the last 24 hours", "2026-01-21");
        when(alertGenerator.generateHighTransactionAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generateHighTransactionAlert(USER_ID);
    }

    @Test
    void testTransactionsOutside24HourWindowNotIncludedInAverage() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(25 * 3600), 1000.0));
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0));

        Alert mockAlert = createAlert(USER_ID, "HIGH_TRANSACTION", 
                "Transaction amount is 5x above the user's average in the last 24 hours", "2026-01-21");
        when(alertGenerator.generateHighTransactionAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, times(1)).generateHighTransactionAlert(USER_ID);
        assertEquals(1, alerts.size());
    }

    @Test
    void testAlertGeneratorCalledWithCorrectUserId() {
        Instant now = Instant.now();
        String specificUserId = "specificUser123";
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0));
        transactions.add(createTransaction(now.minusSeconds(25 * 3600), 600.0));

        Alert mockAlert = createAlert(specificUserId, "HIGH_TRANSACTION", 
                "Transaction amount is 5x above the user's average in the last 24 hours", "2026-01-21");
        when(alertGenerator.generateHighTransactionAlert(specificUserId)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, specificUserId);

        verify(alertGenerator).generateHighTransactionAlert(specificUserId);
    }

    private TransactionEvent createTransaction(Instant timestamp, double amount) {
        try {
            TransactionEvent event = TransactionEvent.class.getDeclaredConstructor().newInstance();
            setField(event, "timestamp", timestamp);
            setField(event, "amount", amount);
            setField(event, "userID", USER_ID);
            setField(event, "serviceID", "service1");
            return event;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TransactionEvent", e);
        }
    }

    private Alert createAlert(String userId, String alertName, String alertMessage, String alertTime) {
        try {
            Alert alert = Alert.class.getDeclaredConstructor().newInstance();
            setField(alert, "userId", userId);
            setField(alert, "alertName", alertName);
            setField(alert, "alertMessage", alertMessage);
            setField(alert, "alertTime", alertTime);
            return alert;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Alert", e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
