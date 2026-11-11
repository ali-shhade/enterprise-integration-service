package com.ali.integration.processor;

import com.ali.integration.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderXmlSerializerTest {

    @Test
    void shouldSerializeOrderToXml() {

        Order order = new Order(
                7001,
                "Ali",
                "Laptop",
                2
        );

        OrderXmlSerializer serializer =
                new OrderXmlSerializer();

        String xml =
                serializer.toXml(order);

        System.out.println(xml);

        assertTrue(
            xml.contains("<orderId>7001</orderId>")
        );

        assertTrue(
            xml.contains("<customer>Ali</customer>")
        );

        assertTrue(
            xml.contains("<product>Laptop</product>")
        );

        assertTrue(
            xml.contains("<quantity>2</quantity>")
        );
    }
}

