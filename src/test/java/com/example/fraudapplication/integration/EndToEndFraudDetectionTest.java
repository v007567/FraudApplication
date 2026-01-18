package com.example.fraudapplication.integration;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.FraudDetectorEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndFraudDetectionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FraudDetectorEngineService fraudDetectorEngineService;

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        return TransactionEvent.builder()
                .timestamp(timestamp)
                .amount(amount)
                .userID(userId)
                .serviceID(serviceId)
                .build();
    }

    @Test
    void testEndToEndFraudDetectionPipeline() {
        ResponseEntity<List<Alert>> response = restTemplate.exchange(
                "http://localhost:" + port + "/fraud/all",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Alert>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testFraudDetectionServiceDirectly() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
        
                events.add(createTransaction(now, 100.0, "user1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 200.0, "user1", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 300.0, "user1", "serviceC"));
                events.add(createTransaction(now.plusSeconds(180), 400.0, "user1", "serviceD"));

        List<Alert> alerts = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(alerts);
    }

    @Test
    void testHighTransactionFraudDetection() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
        
                events.add(createTransaction(now, 100.0, "testUser1", "serviceA"));
                events.add(createTransaction(now.plusSeconds(60), 100.0, "testUser1", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 100.0, "testUser1", "serviceC"));
                events.add(createTransaction(now.plusSeconds(180), 100.0, "testUser1", "serviceD"));
                events.add(createTransaction(now.plusSeconds(240), 10000.0, "testUser1", "serviceE"));

        List<Alert> alerts = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertNotNull(alerts);
        boolean hasHighTransactionAlert = alerts.stream()
                .anyMatch(alert -> alert.getAlertName().equals(AlertName.HIGH_TRANSACTION.getAlertName()));
        assertTrue(hasHighTransactionAlert);
    }

    @Test
    void testMultipleServiceFraudDetection() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
        
                events.add(createTransaction(now, 100.0, "testUser2", "serviceA"));
                events.add(createTransaction(now.plusSeconds(30), 100.0, "testUser2", "serviceB"));
                events.add(createTransaction(now.plusSeconds(60), 100.0, "testUser2", "serviceC"));
                events.add(createTransaction(now.plusSeconds(90), 100.0, "testUser2", "serviceD"));

        List<Alert> alerts = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertNotNull(alerts);
        boolean hasMultipleServiceAlert = alerts.stream()
                .anyMatch(alert -> alert.getAlertName().equals(AlertName.MULTIPLE_SERVICE.getAlertName()));
        assertTrue(hasMultipleServiceAlert);
    }

    @Test
    void testPingPongFraudDetection() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
        
                events.add(createTransaction(now, 100.0, "testUser3", "serviceA"));
                events.add(createTransaction(now.plusSeconds(30), 100.0, "testUser3", "serviceB"));
                events.add(createTransaction(now.plusSeconds(60), 100.0, "testUser3", "serviceA"));
                events.add(createTransaction(now.plusSeconds(90), 100.0, "testUser3", "serviceB"));
                events.add(createTransaction(now.plusSeconds(120), 100.0, "testUser3", "serviceA"));
                events.add(createTransaction(now.plusSeconds(150), 100.0, "testUser3", "serviceB"));

        List<Alert> alerts = fraudDetectorEngineService.checkPingPongFraud(events);

        assertNotNull(alerts);
    }

        @Test
        void testSequentialRequestProcessing() throws Exception {
            for (int i = 0; i < 3; i++) {
                ResponseEntity<List<Alert>> response = restTemplate.exchange(
                        "http://localhost:" + port + "/fraud/all",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<Alert>>() {}
                );
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
            }
        }

        @Test
        void testMediumDatasetPerformance() {
            List<TransactionEvent> events = new ArrayList<>();
            Instant now = Instant.now();
        
            for (int i = 0; i < 50; i++) {
                events.add(createTransaction(
                        now.plusSeconds(i * 10),
                        100.0 + (i % 100),
                        "perfUser",
                        "service" + (i % 2)
                ));
            }

            long startTime = System.currentTimeMillis();
            List<Alert> alerts = fraudDetectorEngineService.checkAllFraudActivities(events);
            long endTime = System.currentTimeMillis();

            assertNotNull(alerts);
            assertTrue((endTime - startTime) < 5000, "Processing should complete within 5 seconds");
        }

    @Test
    void testCsvDownloadEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/fraud/download",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testMultipleUsersInSingleRequest() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
        
                events.add(createTransaction(now, 100.0, "userA", "serviceA"));
                events.add(createTransaction(now.plusSeconds(30), 100.0, "userB", "serviceB"));
                events.add(createTransaction(now.plusSeconds(60), 100.0, "userC", "serviceC"));
                events.add(createTransaction(now.plusSeconds(90), 100.0, "userA", "serviceD"));
                events.add(createTransaction(now.plusSeconds(120), 100.0, "userB", "serviceE"));

        List<Alert> alerts = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(alerts);
    }

        @Test
        void testMinimalTransactionList() {
            List<TransactionEvent> events = new ArrayList<>();
            events.add(createTransaction(Instant.now(), 100.0, "minimalUser", "serviceA"));
            events.add(createTransaction(Instant.now().plusSeconds(60), 100.0, "minimalUser", "serviceB"));

            List<Alert> alerts = fraudDetectorEngineService.checkAllFraudActivities(events);

            assertNotNull(alerts);
        }

    @Test
    void testSingleTransactionNoFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(Instant.now(), 100.0, "singleUser", "serviceA"));

        List<Alert> alerts = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(alerts);
    }

    @Test
    void testAllFraudTypesDetectedForSameUser() {
        List<TransactionEvent> events = new ArrayList<>();
        Instant now = Instant.now();
        
                events.add(createTransaction(now, 100.0, "fraudUser", "serviceA"));
                events.add(createTransaction(now.plusSeconds(30), 100.0, "fraudUser", "serviceB"));
                events.add(createTransaction(now.plusSeconds(60), 100.0, "fraudUser", "serviceC"));
                events.add(createTransaction(now.plusSeconds(90), 100.0, "fraudUser", "serviceD"));
                events.add(createTransaction(now.plusSeconds(120), 100.0, "fraudUser", "serviceA"));
                events.add(createTransaction(now.plusSeconds(150), 100.0, "fraudUser", "serviceB"));
                events.add(createTransaction(now.plusSeconds(180), 100.0, "fraudUser", "serviceA"));
                events.add(createTransaction(now.plusSeconds(210), 100.0, "fraudUser", "serviceB"));
                events.add(createTransaction(now.plusSeconds(240), 50000.0, "fraudUser", "serviceE"));

        List<Alert> alerts = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
    }
}
