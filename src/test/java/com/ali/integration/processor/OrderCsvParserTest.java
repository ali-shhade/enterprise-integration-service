package com.ali.integration.processor;

import com.ali.integration.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCsvParserTest {

    @Test
    void shouldParseCsvLine() {

        OrderCsvParser parser =
                new OrderCsvParser();

        Order order =
                parser.parseLine(
                    "7001,Ali,Laptop,2"
                );

        assertEquals(7001, order.getOrderId());
        assertEquals("Ali", order.getCustomer());
        assertEquals("Laptop", order.getProduct());
        assertEquals(2, order.getQuantity());
    }
}
