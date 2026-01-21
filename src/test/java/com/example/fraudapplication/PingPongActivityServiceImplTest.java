package com.example.fraudapplication;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.impl.PingPongActivityServiceImpl;
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
public class PingPongActivityServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private PingPongActivityServiceImpl pingPongActivityService;

    private static final String USER_ID = "testUser";

    @Test
    void testPhase1FindsExactlyTwoDistinctServicesWithin10Minutes() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(180), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(240), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(300), "serviceB"));

        Alert mockAlert = createAlert(USER_ID, "PING_PONG", 
                "User's transactions bouncing back and forth between two services within 10-minute window", "2026-01-21");
        when(alertGenerator.generatePingPongAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generatePingPongAlert(USER_ID);
    }

    @Test
    void testPhase2VerifiesServiceAlternationPattern() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(90), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(150), "serviceB"));

        Alert mockAlert = createAlert(USER_ID, "PING_PONG", 
                "User's transactions bouncing back and forth between two services within 10-minute window", "2026-01-21");
        when(alertGenerator.generatePingPongAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generatePingPongAlert(USER_ID);
    }

    @Test
    void testNoAlertWhenOnlySingleService() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generatePingPongAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testNoAlertWhenMoreThanTwoDistinctServices() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceC"));
        transactions.add(createTransaction(now.plusSeconds(180), "serviceA"));

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testNoAlertWhenServicesDoNotAlternate() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(180), "serviceB"));

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void test10MinuteWindowEnforcement() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(180), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(240), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(300), "serviceB"));

        Alert mockAlert = createAlert(USER_ID, "PING_PONG", 
                "User's transactions bouncing back and forth between two services within 10-minute window", "2026-01-21");
        when(alertGenerator.generatePingPongAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generatePingPongAlert(USER_ID);
    }

    @Test
    void testWindowExpirationAfter10Minutes() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(700), "serviceB"));

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testLinkedHashMapMaintainsOrderedServiceHistory() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceX"));
        transactions.add(createTransaction(now.plusSeconds(30), "serviceY"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceX"));
        transactions.add(createTransaction(now.plusSeconds(90), "serviceY"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceX"));
        transactions.add(createTransaction(now.plusSeconds(150), "serviceY"));

        Alert mockAlert = createAlert(USER_ID, "PING_PONG", 
                "User's transactions bouncing back and forth between two services within 10-minute window", "2026-01-21");
        when(alertGenerator.generatePingPongAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generatePingPongAlert(USER_ID);
    }

    @Test
    void testAlertGeneratorCalledWithCorrectUserId() {
        Instant now = Instant.now();
        String specificUserId = "specificUser789";
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(90), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(150), "serviceB"));

        Alert mockAlert = createAlert(specificUserId, "PING_PONG", 
                "User's transactions bouncing back and forth between two services within 10-minute window", "2026-01-21");
        when(alertGenerator.generatePingPongAlert(specificUserId)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, specificUserId);

        verify(alertGenerator, atLeast(1)).generatePingPongAlert(specificUserId);
    }

    @Test
    void testSingleTransaction() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generatePingPongAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testTwoTransactionsDifferentServices() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceB"));

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, never()).generatePingPongAlert(anyString());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testMultiplePingPongPatternsDetected() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(createTransaction(now, "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(30), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(60), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(90), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(120), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(150), "serviceB"));
        transactions.add(createTransaction(now.plusSeconds(180), "serviceA"));
        transactions.add(createTransaction(now.plusSeconds(210), "serviceB"));

        Alert mockAlert = createAlert(USER_ID, "PING_PONG", 
                "User's transactions bouncing back and forth between two services within 10-minute window", "2026-01-21");
        when(alertGenerator.generatePingPongAlert(USER_ID)).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();

        pingPongActivityService.checkPingPongActivity(transactions, alerts, USER_ID);

        verify(alertGenerator, atLeast(1)).generatePingPongAlert(USER_ID);
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
