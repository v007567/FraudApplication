package com.example.fraudapplication.domain.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testStringifyDateWithValidDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-01-15 2:30:45 PM", result);
    }

    @Test
    void testStringifyDateWithMorningTime() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 20, 9, 15, 30);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-06-20 9:15:30 AM", result);
    }

    @Test
    void testStringifyDateWithMidnight() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 31, 0, 0, 0);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-12-31 12:00:00 AM", result);
    }

    @Test
    void testStringifyDateWithNoon() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 7, 4, 12, 0, 0);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-07-04 12:00:00 PM", result);
    }

    @Test
    void testStringifyDateWithEndOfDay() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 23, 59, 59);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-03-15 11:59:59 PM", result);
    }

    @Test
    void testStringifyDateWithLeapYear() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 10, 30, 0);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-02-29 10:30:00 AM", result);
    }

    @Test
    void testStringifyDateWithFirstDayOfYear() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 1);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-01-01 12:00:01 AM", result);
    }

    @Test
    void testStringifyDateWithLastDayOfYear() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-12-31 11:59:59 PM", result);
    }

    @Test
    void testStringifyDateFormatPattern() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 5, 10, 15, 45, 30);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{1,2}:\\d{2}:\\d{2} [AP]M"));
    }

    @Test
    void testStringifyDateWithSingleDigitHour() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 8, 25, 5, 30, 0);
        
        String result = DateUtils.stringifyDate(dateTime);
        
        assertEquals("2024-08-25 5:30:00 AM", result);
    }
}
