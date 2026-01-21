package com.example.fraudapplication;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.utils.DateUtils;
import com.example.fraudapplication.service.impl.AlertGeneratorImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AlertGeneratorImplTest {

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    @Test
    public void testGenerateHighTransactionAlert() throws Exception {
        String userId = "testUser";
        AlertGeneratorImpl alertGenerator = new AlertGeneratorImpl();

        Alert alert = alertGenerator.generateHighTransactionAlert(userId);

        assertEquals(userId, getField(alert, "userId"));
        assertEquals("High Transaction Alert", getField(alert, "alertName"));
        assertEquals("Transaction amount is 5x above the user's average in the last 24 hours", getField(alert, "alertMessage"));
        assertEquals(DateUtils.stringifyDate(LocalDateTime.now()), getField(alert, "alertTime"));
    }

    @Test
    public void testGenerateMultipleServiceAlert() throws Exception {
        String userId = "testUser";
        AlertGeneratorImpl alertGenerator = new AlertGeneratorImpl();

        Alert alert = alertGenerator.generateMultipleServiceAlert(userId);

        assertEquals(userId, getField(alert, "userId"));
        assertEquals("Multiple Service Alert", getField(alert, "alertName"));
        assertEquals("User conducting transaction in more than 3 distinct services within 5-minute window ", getField(alert, "alertMessage"));
        assertEquals(DateUtils.stringifyDate(LocalDateTime.now()), getField(alert, "alertTime"));
    }

    @Test
    public void testGeneratePingPongAlert() throws Exception {
        String userId = "testUser";
        AlertGeneratorImpl alertGenerator = new AlertGeneratorImpl();

        Alert alert = alertGenerator.generatePingPongAlert(userId);

        assertEquals(userId, getField(alert, "userId"));
        assertEquals("Ping-Pong Activity Alert", getField(alert, "alertName"));
        assertEquals("User's transactions bouncing back and forth between two services within 10-minute window", getField(alert, "alertMessage"));
        assertEquals(DateUtils.stringifyDate(LocalDateTime.now()), getField(alert, "alertTime"));
    }
}
