package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultipleServiceTransactionImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    private MultipleServiceTransactionImpl multipleServiceTransaction;

    @BeforeEach
    void setUp() {
        multipleServiceTransaction = new MultipleServiceTransactionImpl(alertGenerator);
    }

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        return TransactionEvent.builder()
                .timestamp(timestamp)
                .amount(amount)
                .userID(userId)
                .serviceID(serviceId)
                .build();
    }

    private Alert createMockAlert(String userId, AlertName alertName) {
        return Alert.builder()
                .userId(userId)
                .alertName(alertName.getAlertName())
                .alertMessage(alertName.getAlertMessage())
                .build();
    }

    @Test
    void testAlertWhenMoreThan3DistinctServicesIn5Minutes() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceD"));

        Alert mockAlert = createMockAlert(userId, AlertName.MULTIPLE_SERVICE);
        when(alertGenerator.generateMultipleServiceAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generateMultipleServiceAlert(userId);
    }

    @Test
    void testNoAlertWhen3OrFewerServicesAccessed() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceC"));

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testSlidingWindowResetsAfter5Minutes() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(400), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(460), 100.0, userId, "serviceD"));

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testServiceCountingLogicWithDuplicateServices() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceB"));

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testSameServiceTransactionsNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(120), 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(180), 100.0, userId, "serviceA"));

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testExactly4ServicesIn5MinutesGeneratesAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(90), 100.0, userId, "serviceD"));

        Alert mockAlert = createMockAlert(userId, AlertName.MULTIPLE_SERVICE);
        when(alertGenerator.generateMultipleServiceAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generateMultipleServiceAlert(userId);
    }

    @Test
    void testWindowResetWithNewFirstTransaction() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), 100.0, userId, "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(400), 100.0, userId, "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(420), 100.0, userId, "serviceD"));
        transactions.add(createTransaction(now.plusSeconds(440), 100.0, userId, "serviceE"));
        transactions.add(createTransaction(now.plusSeconds(460), 100.0, userId, "serviceF"));

        Alert mockAlert = createMockAlert(userId, AlertName.MULTIPLE_SERVICE);
        when(alertGenerator.generateMultipleServiceAlert(userId)).thenReturn(mockAlert);

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generateMultipleServiceAlert(userId);
    }

    @Test
    void testSingleTransactionNoAlert() {
        String userId = "user1";
        List<Alert> alerts = new ArrayList<>();
        List<TransactionEvent> transactions = new ArrayList<>();
        
        Instant now = Instant.now();
        transactions.add(createTransaction(now, 100.0, userId, "serviceA"));

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, userId);

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }
}
