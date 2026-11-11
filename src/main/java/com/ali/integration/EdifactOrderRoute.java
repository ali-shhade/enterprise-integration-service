package com.ali.integration;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class EdifactOrderRoute extends RouteBuilder {

    private static final String SOURCE_SYSTEM = "PARTNER-B";

    private final EdifactOrderParser parser;
    private final OrderTrackingService trackingService;

    public EdifactOrderRoute(
            EdifactOrderParser parser,
            OrderTrackingService trackingService) {

        this.parser = parser;
        this.trackingService = trackingService;
    }

    @Override
    public void configure() {

        // Handle duplicate orders without retrying them.
        onException(DuplicateKeyException.class)
            .maximumRedeliveries(0)
            .handled(true)
            .log(
                LoggingLevel.WARN,
                "Duplicate order from ${header.Customer}: "
                    + "${header.OrderId}"
            );

        from("file:data/day5/edi-input?noop=true")

            .routeId("edifact-order-route")

            .log("Receiving EDIFACT file: ${header.CamelFileName}")

            .convertBodyTo(String.class)

            // STEP 1: Parse EDIFACT
            .process(exchange -> {

                String ediMessage =
                    exchange.getMessage().getBody(String.class);

                EdifactOrderParser.ParsedEdifactOrder order =
                    parser.parse(ediMessage);

                exchange.getMessage().setHeader(
                    "OrderId", order.orderId());

                exchange.getMessage().setHeader(
                    "Customer", order.customer());

                exchange.getMessage().setHeader(
                    "Amount", order.amount());

                exchange.getMessage().setHeader(
                    "Currency", order.currency());

                // STEP 2: Store order in MySQL
                long databaseId = trackingService.receiveOrder(
                    SOURCE_SYSTEM,
                    order.orderId(),
                    order.customer(),
                    order.amount(),
                    order.currency()
                );

                // STEP 3: Update status
                trackingService.changeStatus(
                    SOURCE_SYSTEM,
                    order.orderId(),
                    "VALIDATED",
                    "EDIFACT order parsed and validated"
                );

                exchange.getMessage().setHeader(
                    "DatabaseOrderId", databaseId);

            })

            .log(
                "Order ${header.OrderId} saved to MySQL. "
                    + "Database ID: ${header.DatabaseOrderId}"
            );
    }
}
