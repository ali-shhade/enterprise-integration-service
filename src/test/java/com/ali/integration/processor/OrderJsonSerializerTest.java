package com.ali.integration.processor;

import com.ali.integration.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderJsonSerializerTest {

    @Test
    void shouldSerializeOrderToJson()
            throws Exception {

        Order order = new Order(
                7001,
                "Ali",
                "Laptop",
                2
        );

        OrderJsonSerializer serializer =
                new OrderJsonSerializer();

        String json =
                serializer.toJson(order);

        System.out.println(json);

        assertTrue(json.contains("\"orderId\":7001"));
        assertTrue(json.contains("\"customer\":\"Ali\""));
        assertTrue(json.contains("\"product\":\"Laptop\""));
        assertTrue(json.contains("\"quantity\":2"));
    }
    @Test
void shouldConvertCsvToJson()
        throws Exception {

    OrderCsvParser parser =
            new OrderCsvParser();

    OrderJsonSerializer serializer =
            new OrderJsonSerializer();

    Order order1 =
            parser.parseLine(
                "7001,Ali,Laptop,2"
            );

      Order order2 =
            parser.parseLine(
                "7003,upomecano,boots,2"
            );

    String json1 =
            serializer.toJson(order1);

    System.out.println(json1);

     String json2 =
            serializer.toJson(order2);

    System.out.println(json2);
}


}
