
package com.ali.integration;

import com.ali.integration.model.Order;
import com.ali.integration.processor.OrderCsvParser;
import com.ali.integration.processor.OrderJsonSerializer;
import com.ali.integration.processor.OrderXmlSerializer;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CsvOrderIntegrationRoute extends RouteBuilder {

    private final OrderCsvParser parser =
            new OrderCsvParser();

    private final OrderJsonSerializer jsonSerializer =
            new OrderJsonSerializer();

    private final OrderXmlSerializer xmlSerializer =
            new OrderXmlSerializer();

    @Override
    public void configure() {

        from("file:data/day3-final/input?noop=true")
            .routeId("csv-order-integration")

            .log("Received CSV file: ${file:name}")

            .convertBodyTo(String.class)

            // Split the CSV file into individual rows
            .split(body().tokenize("\n"))

                // STEP 1: Parse and validate each row
                .process(exchange -> {

                    String originalLine = exchange
                            .getMessage()
                            .getBody(String.class);

                    String line = originalLine.trim();

                    // Skip header and empty rows
                    if (line.isEmpty()
                            || line.startsWith("orderId,")) {

                        exchange.getMessage()
                                .setHeader("SkipLine", true);

                        return;
                    }

                    // Save original filename
                    String sourceFile = exchange.getMessage()
                            .getHeader(
                                Exchange.FILE_NAME_ONLY,
                                String.class
                            );

                    // Get row number
                    Integer splitIndex = exchange.getProperty(
                            Exchange.SPLIT_INDEX,
                            Integer.class
                    );

                    int rowNumber = splitIndex == null
                            ? 0
                            : splitIndex + 1;

                    try {

                        // Parse CSV into Java Order object
                        Order order = parser.parseLine(line);

                        // Save object for JSON and XML conversion
                        exchange.setProperty(
                                "ParsedOrder",
                                order
                        );

                        // Store Order ID in header
                        exchange.getMessage().setHeader(
                                "OrderId",
                                order.getOrderId()
                        );

                    } catch (IllegalArgumentException e) {

                        // Mark row as invalid
                        exchange.getMessage().setHeader(
                                "InvalidRow",
                                true
                        );

                        // Store validation error
                        exchange.getMessage().setHeader(
                                "RowError",
                                e.getMessage()
                        );

                        // Preserve original CSV row
                        exchange.getMessage()
                                .setBody(originalLine);

                        // Create unique rejected filename
                        String filename = sourceFile == null
                                ? "unknown.csv"
                                : sourceFile;

                        exchange.getMessage().setHeader(
                                Exchange.FILE_NAME,
                                "rejected-" + filename
                                        + "-row-" + rowNumber
                                        + ".csv"
                        );
                    }
                })

                // STEP 2: Route according to validation result
                .choice()

                    // Skip header or empty row
                    .when(header("SkipLine").isEqualTo(true))

                        .log("Skipping header or empty row")

                    // Handle invalid order
                    .when(header("InvalidRow").isEqualTo(true))

                        .log(
                            "Rejected CSV row: ${header.RowError}"
                        )

                        .to("file:data/day3-final/rejected")

                    // Process valid order
                    .otherwise()

                        .log(
                            "Order ${header.OrderId}: validation successful"
                        )

                        // STEP 3: Convert Order to JSON
                        .process(exchange -> {

                            Order order = exchange.getProperty(
                                    "ParsedOrder",
                                    Order.class
                            );

                            String json =
                                    jsonSerializer.toJson(order);

                            exchange.getMessage()
                                    .setBody(json);

                            exchange.getMessage().setHeader(
                                    Exchange.FILE_NAME,
                                    "order-" + order.getOrderId()
                                            + ".json"
                            );
                        })

                        // Save JSON file
                        .to("file:data/day3-final/json")

                        .log(
                            "Order ${header.OrderId}: JSON created"
                        )

                        // STEP 4: Convert same Order to XML
                        .process(exchange -> {

                            Order order = exchange.getProperty(
                                    "ParsedOrder",
                                    Order.class
                            );

                            String xml =
                                    xmlSerializer.toXml(order);

                            exchange.getMessage()
                                    .setBody(xml);

                            exchange.getMessage().setHeader(
                                    Exchange.FILE_NAME,
                                    "order-" + order.getOrderId()
                                            + ".xml"
                            );
                        })

                        // Save XML file
                        .to("file:data/xml")

                        .log(
                            "Order ${header.OrderId}: XML created"
                        )

                .end() // End choice

            .end(); // End split
    }
}
