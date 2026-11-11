package com.ali.integration;

import com.ali.integration.model.Order;
import com.ali.integration.processor.OrderCsvParser;
import com.ali.integration.processor.OrderXmlSerializer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CsvToXmlRoute extends RouteBuilder {

    private final OrderCsvParser parser =
            new OrderCsvParser();

    private final OrderXmlSerializer serializer =
            new OrderXmlSerializer();

    @Override
    public void configure() {

        from("file:data/csv-xml-input?noop=true")
            .routeId("csv-to-xml-route")
            .log("Received CSV file: ${file:name}")
            .convertBodyTo(String.class)

            .split(body().tokenize("\n"))

                .process(exchange -> {

                    String line = exchange.getMessage()
                            .getBody(String.class)
                            .trim();

                    // Skip header and empty lines
                    if (line.isEmpty()
                            || line.startsWith("orderId,")) {

                        exchange.getMessage()
                                .setHeader("SkipLine", true);

                        return;
                    }

                    try {

                        Order order = parser.parseLine(line);

                        String xml = serializer.toXml(order);

                        exchange.getMessage().setBody(xml);

                        // Successful order ID
                        exchange.getMessage().setHeader(
                                "OrderId",
                                order.getOrderId()
                        );

                        // Output XML filename
                        exchange.getMessage().setHeader(
                                Exchange.FILE_NAME,
                                "order-" + order.getOrderId() + ".xml"
                        );

                    } catch (IllegalArgumentException e) {

                        // Mark the row as invalid
                        exchange.getMessage().setHeader(
                                "InvalidRow",
                                true
                        );

                        exchange.getMessage().setHeader(
                                "RowError",
                                e.getMessage()
                        );

                        // Keep original CSV content
                        exchange.getMessage().setBody(line);

                        // Identify original CSV file
                        String sourceFile = exchange.getMessage()
                                .getHeader(
                                    Exchange.FILE_NAME_ONLY,
                                    String.class
                                );

                        // Split index starts at 0
                        int rowNumber = exchange.getProperty(
                                Exchange.SPLIT_INDEX,
                                Integer.class
                        ) + 1;

                        // Unique rejected filename per input row
                        exchange.getMessage().setHeader(
                                Exchange.FILE_NAME,
                                "rejected-" + sourceFile
                                    + "-row-" + rowNumber + ".csv"
                        );
                    }
                })

                .choice()

                    .when(header("SkipLine").isEqualTo(true))
                        .log("Skipping header/empty line")

                    .when(header("InvalidRow").isEqualTo(true))
                        .log(
                            "Rejected CSV row: ${header.RowError}"
                        )
                        .to("file:data/rejected")

                    .otherwise()
                        .log(
                            "Converted order ${header.OrderId} to XML"
                        )
                        .to("file:data/xml")

                .end()

            .end();
    }
}
