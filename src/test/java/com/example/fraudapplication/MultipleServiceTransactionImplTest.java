package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.MultipleServiceTransactionImpl;
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
public class MultipleServiceTransactionImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private MultipleServiceTransactionImpl multipleServiceTransaction;

    private static final String USER_ID = "testUser";

    @Test
    void testAlertGeneratedWhenMoreThan3DistinctServicesWithin5Minutes() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));
        transactions.add(createTransaction(now.plusSeconds(120), "service3"));
        transactions.add(createTransaction(now.plusSeconds(180), "service4"));

        Alert mockAlert = createAlert(USER_ID, "MULTIPLE_SERVICE", 
                "User conducting transaction in more than 3 distinct services within 5-minute window", "2026-01-21");
        when(alertGenerator.generateMultipleServiceAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generateMultipleServiceAlert(USER_ID);
    }

    @Test
    void testNoAlertWhenExactly3DistinctServicesWithin5Minutes() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));
        transactions.add(createTransaction(now.plusSeconds(120), "service3"));

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testNoAlertWhenLessThan3DistinctServicesWithin5Minutes() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testDistinctServiceCountingWithDuplicateServices() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(30), "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));
        transactions.add(createTransaction(now.plusSeconds(90), "service2"));
        transactions.add(createTransaction(now.plusSeconds(120), "service3"));

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testServiceRemovalWhenTransactionsFallOutside5MinuteWindow() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));
        transactions.add(createTransaction(now.plusSeconds(400), "service3"));
        transactions.add(createTransaction(now.plusSeconds(420), "service4"));

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void test5MinuteWindowBoundary() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(299), "service2"));
        transactions.add(createTransaction(now.plusSeconds(300), "service3"));
        transactions.add(createTransaction(now.plusSeconds(300), "service4"));

        Alert mockAlert = createAlert(USER_ID, "MULTIPLE_SERVICE", 
                "User conducting transaction in more than 3 distinct services within 5-minute window", "2026-01-21");
        when(alertGenerator.generateMultipleServiceAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generateMultipleServiceAlert(USER_ID);
    }

    @Test
    void testSingleTransaction() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testAlertGeneratorCalledWithCorrectUserId() {
        Instant now = Instant.now();
        String specificUserId = "specificUser456";
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));
        transactions.add(createTransaction(now.plusSeconds(120), "service3"));
        transactions.add(createTransaction(now.plusSeconds(180), "service4"));

        Alert mockAlert = createAlert(specificUserId, "MULTIPLE_SERVICE", 
                "User conducting transaction in more than 3 distinct services within 5-minute window", "2026-01-21");
        when(alertGenerator.generateMultipleServiceAlert(specificUserId)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, specificUserId);

        verify(alertGenerator, atLeast(1)).generateMultipleServiceAlert(specificUserId);
    }

    @Test
    void testHashMapTrackingDistinctServices() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(90), "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceD"));

        Alert mockAlert = createAlert(USER_ID, "MULTIPLE_SERVICE", 
                "User conducting transaction in more than 3 distinct services within 5-minute window", "2026-01-21");
        when(alertGenerator.generateMultipleServiceAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generateMultipleServiceAlert(USER_ID);
    }

    @Test
    void testWindowSlidesCorrectlyWhenTransactionsExpire() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "service1"));
        transactions.add(createTransaction(now.plusSeconds(60), "service2"));
        transactions.add(createTransaction(now.plusSeconds(350), "service3"));
        transactions.add(createTransaction(now.plusSeconds(360), "service4"));
        transactions.add(createTransaction(now.plusSeconds(370), "service5"));

        List<Alert> alerts = new ArrayList<>();

        Alert mockAlert = createAlert(USER_ID, "MULTIPLE_SERVICE", 
                "User conducting transaction in more than 3 distinct services within 5-minute window", "2026-01-21");
        lenient().when(alertGenerator.generateMultipleServiceAlert(USER_ID)).thenReturn(mockAlert);

        multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, USER_ID);
    }

    private TransactionEvent createTransaction(Instant timestamp, String serviceId) {
        try {
            TransactionEvent event = TransactionEvent.class.getDeclaredConstructor().newInstance();
            setField(event, "timestamp", timestamp);
            setField(event, "amount", 100.0);
            setField(event, "userID", USER_ID);
            setField(event, "serviceID", serviceId);
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
