package com.example.fraudapplication.domain.utils;

import com.example.fraudapplication.domain.model.TransactionEvent;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListToCsvTest {

    @Mock
    private HttpServletResponse httpServletResponse;

    private static final String TEST_FILE_NAME = "test_export";

    @AfterEach
    void cleanup() {
        try {
            Files.deleteIfExists(Paths.get(TEST_FILE_NAME + ".csv"));
        } catch (IOException e) {
        }
    }

    private TransactionEvent createTransaction(Instant timestamp, double amount, String userId, String serviceId) {
        return TransactionEvent.builder()
                .timestamp(timestamp)
                .amount(amount)
                .userID(userId)
                .serviceID(serviceId)
                .build();
    }

    @Test
    void testExportCSVToFileCreatesFile() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(Instant.now(), 100.0, "user1", "serviceA"));

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        File file = new File(TEST_FILE_NAME + ".csv");
        assertTrue(file.exists());
    }

    @Test
    void testExportCSVToFileWithMultipleRecords() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(Instant.now(), 100.0, "user1", "serviceA"));
        events.add(createTransaction(Instant.now(), 200.0, "user2", "serviceB"));
        events.add(createTransaction(Instant.now(), 300.0, "user3", "serviceC"));

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        File file = new File(TEST_FILE_NAME + ".csv");
        assertTrue(file.exists());
        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE_NAME + ".csv"));
        assertEquals(4, lines.size());
    }

    @Test
    void testExportCSVToHttpResponse() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(Instant.now(), 100.0, "user1", "serviceA"));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(httpServletResponse.getWriter()).thenReturn(printWriter);

        ListToCsv.exportCSV(events, httpServletResponse, "test");

        verify(httpServletResponse).setContentType("text/csv");
        verify(httpServletResponse).setHeader("Content-Disposition", "attachment; filename=\"test.csv\"");
    }

    @Test
    void testHeaderGeneration() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(Instant.now(), 100.0, "user1", "serviceA"));

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE_NAME + ".csv"));
        assertFalse(lines.isEmpty());
        String header = lines.get(0);
        assertTrue(header.contains("timestamp"));
        assertTrue(header.contains("amount"));
        assertTrue(header.contains("userID"));
        assertTrue(header.contains("serviceID"));
    }

    @Test
    void testDataGeneration() throws IOException {
        Instant timestamp = Instant.now();
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(timestamp, 100.0, "user1", "serviceA"));

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE_NAME + ".csv"));
        assertEquals(2, lines.size());
        String dataLine = lines.get(1);
        assertTrue(dataLine.contains("100.0"));
        assertTrue(dataLine.contains("user1"));
        assertTrue(dataLine.contains("serviceA"));
    }

    @Test
    void testEmptyDataListEdgeCase() {
        List<TransactionEvent> events = new ArrayList<>();

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        File file = new File(TEST_FILE_NAME + ".csv");
        assertFalse(file.exists());
    }

    @Test
    void testNullDataListEdgeCase() {
        ListToCsv.exportCSV(null, TEST_FILE_NAME);

        File file = new File(TEST_FILE_NAME + ".csv");
        assertFalse(file.exists());
    }

    @Test
    void testExportCSVToHttpResponseWithEmptyList() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();

        ListToCsv.exportCSV(events, httpServletResponse, "test");

        verify(httpServletResponse, never()).setContentType(anyString());
    }

    @Test
    void testExportCSVToHttpResponseWithNullList() throws IOException {
        ListToCsv.exportCSV(null, httpServletResponse, "test");

        verify(httpServletResponse, never()).setContentType(anyString());
    }

    @Test
    void testExportCSVToHttpResponseWritesData() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();
        Instant timestamp = Instant.now();
                events.add(createTransaction(timestamp, 100.0, "user1", "serviceA"));

                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                when(httpServletResponse.getWriter()).thenReturn(printWriter);

                ListToCsv.exportCSV(events, httpServletResponse, "test");

                printWriter.flush();
                String output = stringWriter.toString();
        assertTrue(output.contains("timestamp"));
        assertTrue(output.contains("amount"));
        assertTrue(output.contains("userID"));
        assertTrue(output.contains("serviceID"));
        assertTrue(output.contains("100.0"));
        assertTrue(output.contains("user1"));
        assertTrue(output.contains("serviceA"));
    }

    @Test
    void testExportCSVWithSpecialCharactersInData() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createTransaction(Instant.now(), 100.0, "user-1", "service_A"));

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE_NAME + ".csv"));
        String dataLine = lines.get(1);
        assertTrue(dataLine.contains("user-1"));
        assertTrue(dataLine.contains("service_A"));
    }

    @Test
    void testExportCSVWithLargeDataset() throws IOException {
        List<TransactionEvent> events = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            events.add(createTransaction(Instant.now(), 100.0 + i, "user" + i, "service" + i));
        }

        ListToCsv.exportCSV(events, TEST_FILE_NAME);

        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE_NAME + ".csv"));
        assertEquals(101, lines.size());
    }
}
