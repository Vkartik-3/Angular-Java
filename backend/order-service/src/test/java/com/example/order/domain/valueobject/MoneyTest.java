package com.example.order.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void rejectsNullAmount() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("-0.01")));
    }

    @Test
    void zeroIsAccepted() {
        assertEquals(BigDecimal.ZERO, Money.ZERO.amount());
    }

    @Test
    void addsTwoAmounts() {
        Money result = new Money(new BigDecimal("10.00")).add(new Money(new BigDecimal("5.50")));
        assertEquals(new BigDecimal("15.50"), result.amount());
    }

    @Test
    void multipliesByQuantity() {
        Money result = new Money(new BigDecimal("34.99")).multiply(3);
        assertEquals(new BigDecimal("104.97"), result.amount());
    }

    @Test
    void isGreaterThanZeroReturnsTrueForPositive() {
        assertTrue(new Money(new BigDecimal("0.01")).isGreaterThanZero());
    }

    @Test
    void isGreaterThanZeroReturnsFalseForZero() {
        assertFalse(Money.ZERO.isGreaterThanZero());
    }
}
