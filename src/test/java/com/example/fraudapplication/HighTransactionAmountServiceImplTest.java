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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private HighTransactionAmountServiceImpl service;

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        TransactionEvent event = new TransactionEvent();
        event.setTimestamp(timestamp);
        event.setAmount(amount);
        event.setUserID(userId);
        event.setServiceID(serviceId);
        return event;
    }

    private Alert createAlert(String userId, String alertName, String alertMessage, String alertTime) {
        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setAlertName(alertName);
        alert.setAlertMessage(alertMessage);
        alert.setAlertTime(alertTime);
        return alert;
    }

    @Test
    void testHighTransactionDetection_AlertGeneratedWhenAmountExceeds5xAverage() {
        String userId = "testUser";
        Instant now = Instant.now();
        
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(10800), 100.0, userId, "service3"));
        transactions.add(createTransaction(now.minusSeconds(14400), 100.0, userId, "service4"));
        transactions.add(createTransaction(now.minusSeconds(18000), 100.0, userId, "service5"));
        transactions.add(createTransaction(now.minusSeconds(21600), 100.0, userId, "service6"));
        transactions.add(createTransaction(now.minusSeconds(25200), 100.0, userId, "service7"));
        transactions.add(createTransaction(now.minusSeconds(28800), 100.0, userId, "service8"));
        transactions.add(createTransaction(now.minusSeconds(32400), 100.0, userId, "service9"));
        transactions.add(createTransaction(now.minusSeconds(36000), 100.0, userId, "service10"));
        transactions.add(createTransaction(now.minusSeconds(1800), 1000.0, userId, "service11"));

        Alert mockAlert = createAlert(userId, "HIGH_TRANSACTION", 
                "Transaction amount is 5x above the user's average in the last 24 hours", 
                "2026-01-19 8:30:00 PM");
        when(alertGenerator.generateHighTransactionAlert(userId)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, userId);

        verify(alertGenerator, times(1)).generateHighTransactionAlert(userId);
        assertEquals(1, result.size());
        assertEquals(mockAlert, result.get(0));
    }

    @Test
    void testNoAlertGeneration_WhenAllTransactionsBelowThreshold() {
        String userId = "testUser";
        Instant now = Instant.now();
        
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now.minusSeconds(3600), 100.0, userId, "service1"));
        transactions.add(createTransaction(now.minusSeconds(7200), 100.0, userId, "service2"));
        transactions.add(createTransaction(now.minusSeconds(10800), 100.0, userId, "service3"));
        transactions.add(createTransaction(now.minusSeconds(14400), 100.0, userId, "service4"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = service.checkHighAmountTransactions(transactions, alerts, userId);

        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
        assertEquals(0, result.size());
    }
}
