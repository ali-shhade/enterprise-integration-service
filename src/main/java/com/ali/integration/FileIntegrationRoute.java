package com.ali.integration;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class FileIntegrationRoute extends RouteBuilder {

    @Override
    public void configure() {

        // Handles unexpected technical failures
        onException(Exception.class)
                .maximumRedeliveries(2)
                .redeliveryDelay(1000)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .handled(true)
                .log(
                    LoggingLevel.ERROR,
                    "Order ${header.OrderId} from ${file:name} failed: " +
                    "${exception.message}"
                )
                .to("file:data/error");


        from("file:data/incoming?noop=true")
                .routeId("file-integration-route")

                .convertBodyTo(String.class)

                // Extract fields and perform validation
                .process(exchange -> {

                    String body = exchange.getMessage()
                            .getBody(String.class);

                    String orderId =
                            getFieldValue(body, "ORDER_ID");

                    String status =
                            getFieldValue(body, "STATUS");

                    String customer =
                            getFieldValue(body, "CUSTOMER");

                    /*
                     * We put OrderId in the header immediately.
                     * This means later logs and exception handlers
                     * can identify the order.
                     */
                    exchange.getMessage()
                            .setHeader(
                                "OrderId",
                                orderId.isEmpty()
                                    ? "UNKNOWN"
                                    : orderId
                            );

                    boolean hasOrderId = !orderId.isEmpty();
                    boolean hasStatus = !status.isEmpty();
                    boolean hasCustomer = !customer.isEmpty();

                    boolean valid =
                            hasOrderId
                            && hasStatus
                            && hasCustomer;

                    exchange.getMessage()
                            .setHeader("ValidOrder", valid);

                    if (!hasOrderId) {

                        exchange.getMessage().setHeader(
                                "ValidationError",
                                "Missing or empty ORDER_ID"
                        );

                    } else if (!hasStatus) {

                        exchange.getMessage().setHeader(
                                "ValidationError",
                                "Missing or empty STATUS"
                        );

                    } else if (!hasCustomer) {

                        exchange.getMessage().setHeader(
                                "ValidationError",
                                "Missing or empty CUSTOMER"
                        );
                    }
                })

                .log(
                    LoggingLevel.INFO,
                    "Received order ${header.OrderId} " +
                    "from file ${file:name}"
                )

                .choice()

                    // VALID ORDER
                    .when(
                        header("ValidOrder")
                            .isEqualTo(true)
                    )

                        .log(
                            LoggingLevel.INFO,
                            "Order ${header.OrderId}: " +
                            "validation successful"
                        )

                        // Simulate technical processing
                        .process(exchange -> {

                            String body =
                                    exchange.getMessage()
                                            .getBody(String.class);

                            if (
                                body.contains(
                                    "SIMULATE_ERROR"
                                )
                            ) {
                                throw new RuntimeException(
                                    "Simulated external system failure"
                                );
                            }
                        })

                        .to("file:data/processed")

                        .log(
                            LoggingLevel.INFO,
                            "Order ${header.OrderId}: " +
                            "processed successfully"
                        )

                    // INVALID ORDER
                    .otherwise()

                        .log(
                            LoggingLevel.ERROR,
                            "Order ${header.OrderId}: rejected - " +
                            "${header.ValidationError}"
                        )

                        .to("file:data/invalid")

                .end();
    }


    private String getFieldValue(
            String body,
            String fieldName) {

        String prefix = fieldName + "=";

        for (String line : body.split("\\R")) {

            if (line.startsWith(prefix)) {

                return line
                        .substring(prefix.length())
                        .trim();
            }
        }

        return "";
    }
}
