package com.ali.integration;

import com.ali.integration.model.Order;
import com.ali.integration.processor.OrderCsvParser;
import com.ali.integration.processor.OrderJsonSerializer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CsvToJsonRoute extends RouteBuilder {

    private final OrderCsvParser parser =
            new OrderCsvParser();

    private final OrderJsonSerializer serializer =
            new OrderJsonSerializer();

    @Override
    public void configure() {

        from("file:data/csv-input?noop=true")
            .routeId("csv-to-json-route")
            .log("Received CSV file: ${file:name}")
            .convertBodyTo(String.class)

            // Split the full CSV file into individual lines
            .split(body().tokenize("\n"))

                .process(exchange -> {

                    String line = exchange
                            .getMessage()
                            .getBody(String.class)
                            .trim();

                    // Ignore the header and empty rows
                    if (line.isEmpty()
                            || line.startsWith("orderId,")) {

                        exchange.getMessage()
                                .setHeader("SkipLine", true);

                        return;
                    }

                    Order order =
                            parser.parseLine(line);

                    String json =
                            serializer.toJson(order);

                    exchange.getMessage()
                            .setBody(json);

                    exchange.getMessage()
                            .setHeader(
                                "OrderId",
                                order.getOrderId()
                            );

                    exchange.getMessage()
                            .setHeader(
                                Exchange.FILE_NAME,
                                "order-" +
                                order.getOrderId() +
                                ".json"
                            );
                })

                .choice()

                    .when(
                        header("SkipLine")
                            .isEqualTo(true)
                    )
                        .log("Skipping CSV header/empty line")

                    .otherwise()
                        .log(
                            "Converted order " +
                            "${header.OrderId} to JSON"
                        )
                        .to("file:data/json")

                .end()

            .end();
    }
}
