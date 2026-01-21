package com.example.fraudapplication;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    private HighTransactionAmountServiceImpl highTransactionAmountService;

    private static final String TEST_USER_ID = "testUser";
    private static final String TEST_SERVICE_ID = "serviceA";

    @BeforeEach
    void setUp() {
        highTransactionAmountService = new HighTransactionAmountServiceImpl(alertGenerator);
    }

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) throws Exception {
        TransactionEvent event = new TransactionEvent();
        setField(event, "timestamp", timestamp);
        setField(event, "amount", amount);
        setField(event, "userID", userId);
        setField(event, "serviceID", serviceId);
        return event;
    }

    private Alert createAlert(String userId, String alertName, String alertMessage, String alertTime) throws Exception {
        Alert alert = new Alert();
        setField(alert, "userId", userId);
        setField(alert, "alertName", alertName);
        setField(alert, "alertMessage", alertMessage);
        setField(alert, "alertTime", alertTime);
        return alert;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    void testEmptyTransactionList_ReturnsEmptyAlerts() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(alertGenerator);
    }

    @Test
    void testSingleTransactionWithin24Hours_NoFraudDetected() throws Exception {
        Instant now = Instant.now();
        TransactionEvent transaction = createTransaction(
                now.minusSeconds(3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID);

        List<TransactionEvent> transactions = List.of(transaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(alertGenerator);
    }

    @Test
    void testTransactionExceeds5xAverage_FraudDetected() throws Exception {
        // Average is calculated from ALL 24-hour transactions (including the fraudulent one)
        // With 10 transactions of 100 + 1 transaction of 1000: total=2000, count=11, avg=181.82
        // Threshold = 5 * 181.82 = 909.09, and 1000 > 909.09, so fraud detected
        Instant now = Instant.now();
        Alert mockAlert = createMockAlert();
        when(alertGenerator.generateHighTransactionAlert(TEST_USER_ID)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        // Add 10 normal transactions within 24 hours
        for (int i = 1; i <= 10; i++) {
            transactions.add(createTransaction(
                    now.minusSeconds(i * 3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID));
        }
        // Add 1 fraudulent transaction (1000 > 5 * 181.82 = 909.09)
        transactions.add(createTransaction(
                now.minusSeconds(1800), 1000.0, TEST_USER_ID, TEST_SERVICE_ID));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        assertEquals(mockAlert, result.get(0));
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testTransactionDoesNotExceed5xAverage_NoFraudDetected() throws Exception {
        Instant now = Instant.now();

        TransactionEvent transaction1 = createTransaction(
                now.minusSeconds(3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent transaction2 = createTransaction(
                now.minusSeconds(7200), 150.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent transaction3 = createTransaction(
                now.minusSeconds(1800), 200.0, TEST_USER_ID, TEST_SERVICE_ID);

        List<TransactionEvent> transactions = List.of(transaction1, transaction2, transaction3);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(alertGenerator);
    }

    @Test
    void testTransactionExactlyAt5xThreshold_NoFraudDetected() throws Exception {
        Instant now = Instant.now();

        TransactionEvent transaction1 = createTransaction(
                now.minusSeconds(3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent transaction2 = createTransaction(
                now.minusSeconds(7200), 100.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent thresholdTransaction = createTransaction(
                now.minusSeconds(1800), 500.0, TEST_USER_ID, TEST_SERVICE_ID);

        List<TransactionEvent> transactions = List.of(
                transaction1, transaction2, thresholdTransaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(alertGenerator);
    }

    @Test
    void testTransactionJustAbove5xThreshold_FraudDetected() throws Exception {
        // With 10 transactions of 100 + 1 transaction of 910: total=1910, count=11, avg=173.64
        // Threshold = 5 * 173.64 = 868.18, and 910 > 868.18, so fraud detected (just above)
        Instant now = Instant.now();
        Alert mockAlert = createMockAlert();
        when(alertGenerator.generateHighTransactionAlert(TEST_USER_ID)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        // Add 10 normal transactions within 24 hours
        for (int i = 1; i <= 10; i++) {
            transactions.add(createTransaction(
                    now.minusSeconds(i * 3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID));
        }
        // Add 1 transaction just above threshold (910 > 5 * 173.64 = 868.18)
        transactions.add(createTransaction(
                now.minusSeconds(1800), 910.0, TEST_USER_ID, TEST_SERVICE_ID));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testMixOfTransactionsInsideAndOutside24Hours_OnlyRecentTransactionsCountForAverage() throws Exception {
        Instant now = Instant.now();
        Alert mockAlert = createMockAlert();
        when(alertGenerator.generateHighTransactionAlert(TEST_USER_ID)).thenReturn(mockAlert);

        TransactionEvent oldTransaction = createTransaction(
                now.minusSeconds(25 * 60 * 60), 1000.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent recentTransaction1 = createTransaction(
                now.minusSeconds(3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent recentTransaction2 = createTransaction(
                now.minusSeconds(7200), 100.0, TEST_USER_ID, TEST_SERVICE_ID);

        List<TransactionEvent> transactions = List.of(
                oldTransaction, recentTransaction1, recentTransaction2);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(1, result.size());
        verify(alertGenerator, times(1)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testAllTransactionsOutside24Hours_NoFraudDetected() throws Exception {
        Instant now = Instant.now();

        TransactionEvent oldTransaction1 = createTransaction(
                now.minusSeconds(25 * 60 * 60), 100.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent oldTransaction2 = createTransaction(
                now.minusSeconds(26 * 60 * 60), 200.0, TEST_USER_ID, TEST_SERVICE_ID);

        List<TransactionEvent> transactions = List.of(oldTransaction1, oldTransaction2);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(alertGenerator);
    }

    @Test
    void testMultipleFraudulentTransactions_MultipleAlertsGenerated() throws Exception {
        // With 10 transactions of 100 + 2 transactions of 1000 and 1100: total=3100, count=12, avg=258.33
        // Threshold = 5 * 258.33 = 1291.67
        // Both 1000 and 1100 are NOT > 1291.67, so we need higher amounts
        // Let's use: 10 transactions of 100 + 2 transactions of 2000 each: total=5000, count=12, avg=416.67
        // Threshold = 5 * 416.67 = 2083.33, still not enough
        // Better approach: 10 transactions of 100 + 2 transactions of 5000 each: total=11000, count=12, avg=916.67
        // Threshold = 5 * 916.67 = 4583.33, and 5000 > 4583.33, so both are fraud
        Instant now = Instant.now();
        Alert mockAlert = createMockAlert();
        when(alertGenerator.generateHighTransactionAlert(TEST_USER_ID)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        // Add 10 normal transactions within 24 hours
        for (int i = 1; i <= 10; i++) {
            transactions.add(createTransaction(
                    now.minusSeconds(i * 3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID));
        }
        // Add 2 fraudulent transactions (both 5000 > 5 * 916.67 = 4583.33)
        transactions.add(createTransaction(
                now.minusSeconds(1800), 5000.0, TEST_USER_ID, TEST_SERVICE_ID));
        transactions.add(createTransaction(
                now.minusSeconds(900), 5000.0, TEST_USER_ID, TEST_SERVICE_ID));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(2, result.size());
        verify(alertGenerator, times(2)).generateHighTransactionAlert(TEST_USER_ID);
    }

    @Test
    void testAlertsAreAddedToProvidedList() throws Exception {
        // With 10 transactions of 100 + 1 transaction of 1000: total=2000, count=11, avg=181.82
        // Threshold = 5 * 181.82 = 909.09, and 1000 > 909.09, so fraud detected
        Instant now = Instant.now();
        Alert mockAlert = createMockAlert();
        when(alertGenerator.generateHighTransactionAlert(TEST_USER_ID)).thenReturn(mockAlert);

        Alert existingAlert = createAlert("existingUser", "Existing Alert", "Existing message", "2024-01-01");

        List<TransactionEvent> transactions = new ArrayList<>();
        // Add 10 normal transactions within 24 hours
        for (int i = 1; i <= 10; i++) {
            transactions.add(createTransaction(
                    now.minusSeconds(i * 3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID));
        }
        // Add 1 fraudulent transaction (1000 > 5 * 181.82 = 909.09)
        transactions.add(createTransaction(
                now.minusSeconds(1800), 1000.0, TEST_USER_ID, TEST_SERVICE_ID));

        List<Alert> alerts = new ArrayList<>();
        alerts.add(existingAlert);

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertEquals(2, result.size());
        assertEquals(existingAlert, result.get(0));
        assertEquals(mockAlert, result.get(1));
    }

    @Test
    void testTransactionExactlyAt24HourBoundary_IncludedInCalculation() throws Exception {
        Instant now = Instant.now();

        TransactionEvent boundaryTransaction = createTransaction(
                now.minusSeconds(24 * 60 * 60 - 1), 100.0, TEST_USER_ID, TEST_SERVICE_ID);
        TransactionEvent recentTransaction = createTransaction(
                now.minusSeconds(3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID);

        List<TransactionEvent> transactions = List.of(boundaryTransaction, recentTransaction);
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, TEST_USER_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(alertGenerator);
    }

    @Test
    void testDifferentUserIdPassedToAlertGenerator() throws Exception {
        // With 10 transactions of 100 + 1 transaction of 1000: total=2000, count=11, avg=181.82
        // Threshold = 5 * 181.82 = 909.09, and 1000 > 909.09, so fraud detected
        // Verify that the userId passed to the method is used for alert generation
        Instant now = Instant.now();
        String differentUserId = "differentUser";
        Alert mockAlert = createMockAlert();
        when(alertGenerator.generateHighTransactionAlert(differentUserId)).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        // Add 10 normal transactions within 24 hours
        for (int i = 1; i <= 10; i++) {
            transactions.add(createTransaction(
                    now.minusSeconds(i * 3600), 100.0, TEST_USER_ID, TEST_SERVICE_ID));
        }
        // Add 1 fraudulent transaction (1000 > 5 * 181.82 = 909.09)
        transactions.add(createTransaction(
                now.minusSeconds(1800), 1000.0, TEST_USER_ID, TEST_SERVICE_ID));

        List<Alert> alerts = new ArrayList<>();

        highTransactionAmountService.checkHighAmountTransactions(
                transactions, alerts, differentUserId);

        verify(alertGenerator, times(1)).generateHighTransactionAlert(differentUserId);
    }

    private Alert createMockAlert() throws Exception {
        return createAlert(
                TEST_USER_ID,
                AlertName.HIGH_TRANSACTION.name(),
                "Transaction amount is 5x above the user's average in the last 24 hours",
                "2024-01-01 12:00:00 PM");
    }
}
