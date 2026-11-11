
package com.ali.integration;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import com.ali.integration.model.Order;
import com.ali.integration.processor.OrderCsvParser;
import com.ali.integration.processor.OrderJsonSerializer;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CsvToApiRoute extends RouteBuilder {

    // API configuration from application.properties
    @Value("${integration.api.url}")
    private String apiUrl;

    @Value("${integration.api.token}")
    private String apiToken;

    // Existing parser and serializer
    private final OrderCsvParser parser =
            new OrderCsvParser();

    private final OrderJsonSerializer serializer =
            new OrderJsonSerializer();


    @Override
    public void configure() {

        // ==========================================
        // HANDLER 1: HTTP 503 - RETRY TWICE
        // ==========================================

        onException(HttpOperationFailedException.class)

            .onWhen(exchange -> {

                HttpOperationFailedException failure =
                        exchange.getException(
                            HttpOperationFailedException.class
                        );

                return failure != null
                        && failure.getStatusCode() == 503;
            })

            .maximumRedeliveries(2)
            .redeliveryDelay(1000)
            .retryAttemptedLogLevel(LoggingLevel.WARN)

            .handled(true)

            .process(this::writeHttpFailure)

            .log(
                LoggingLevel.ERROR,
                "Order ${header.OrderId} failed after retries"
            )

            .to("file:data/day4/api-unavailable");


        // ==========================================
        // HANDLER 2: OTHER HTTP ERRORS - NO RETRIES
        // ==========================================

        onException(HttpOperationFailedException.class)

            .maximumRedeliveries(0)

            .handled(true)

            .process(this::writeHttpFailure)

            .log(
                LoggingLevel.ERROR,
                "Order ${header.OrderId} rejected by API"
            )

            .to("file:data/day4/api-rejected");
	// HANDLE NETWORK CONNECTION FAILURES

	onException(ConnectException.class, SocketTimeoutException.class)

    .maximumRedeliveries(2)
    .redeliveryDelay(1000)
    .retryAttemptedLogLevel(LoggingLevel.WARN)

    .handled(true)

    .process(this::writeConnectionFailure)

    .log(
        LoggingLevel.ERROR,
        "Order ${header.OrderId} failed because "
            + "the destination API is unavailable"
    )

    .to("file:data/day4/api-unavailable");



        // ==========================================
        // MAIN CSV TO REST API ROUTE
        // ==========================================

        from("file:data/day4/csv-input?noop=true")

            .routeId("csv-to-rest-api-route")

            .log("Received CSV file: ${file:name}")

            .convertBodyTo(String.class)

            // Split the CSV file into individual rows
            .split(body().tokenize("\n"))

                // STEP 1: PARSE AND VALIDATE
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

                    // Preserve source filename
                    String sourceFile = exchange.getMessage()
                            .getHeader(
                                Exchange.FILE_NAME_ONLY,
                                String.class
                            );

                    Integer splitIndex = exchange.getProperty(
                            Exchange.SPLIT_INDEX,
                            Integer.class
                    );

                    int rowNumber = splitIndex == null
                            ? 0
                            : splitIndex + 1;

                    try {

                        // Convert CSV row into Java Order
                        Order order = parser.parseLine(line);

                        // Store Order for later processing
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

                        exchange.getMessage().setHeader(
                                "RowError",
                                e.getMessage()
                        );

                        // Preserve original CSV row
                        exchange.getMessage()
                                .setBody(originalLine);

                        String filename = sourceFile == null
                                ? "unknown.csv"
                                : sourceFile;

                        // Generate unique rejected filename
                        exchange.getMessage().setHeader(
                                Exchange.FILE_NAME,
                                "rejected-" + filename
                                    + "-row-" + rowNumber + ".csv"
                        );
                    }
                })

                // STEP 2: ROUTE BASED ON VALIDATION
                .choice()

                    // Skip header and empty rows
                    .when(header("SkipLine").isEqualTo(true))

                        .log("Skipping header/empty row")


                    // Invalid CSV row
                    .when(header("InvalidRow").isEqualTo(true))

                        .log(
                            "Rejected row: ${header.RowError}"
                        )

                        .to("file:data/day4/rejected")


                    // Valid order
                    .otherwise()

                        // STEP 3: SERIALIZE JAVA ORDER TO JSON
                        .process(exchange -> {

                            Order order = exchange.getProperty(
                                    "ParsedOrder",
                                    Order.class
                            );

                            String json =
                                    serializer.toJson(order);

                            exchange.getMessage()
                                    .setBody(json);
                        })

                        // STEP 4: SET HTTP HEADERS

                        .setHeader(
                            Exchange.CONTENT_TYPE,
                            constant("application/json")
                        )

                        .setHeader(
                            "Accept",
                            constant("application/json")
                        )

                        .setHeader(
                            "Authorization",
                            constant("Bearer " + apiToken)
                        )

                        // STEP 5: SEND HTTP POST REQUEST
                        .log(
                            "Sending order ${header.OrderId} to REST API"
                        )

                        .to(
                            apiUrl + "?httpMethod=POST"
                        )

                        // STEP 6: LOG API RESPONSE
                        .log(
                            "Order ${header.OrderId}: HTTP "
                            + "${header.CamelHttpResponseCode}"
                            + " - ${body}"
                        )

                .end() // End choice

            .end(); // End split
    }


    // ==========================================
    // SAVE HTTP FAILURE INFORMATION
    // ==========================================

    private void writeHttpFailure(Exchange exchange) {

        HttpOperationFailedException failure =
                exchange.getProperty(
                    Exchange.EXCEPTION_CAUGHT,
                    HttpOperationFailedException.class
                );

        if (failure == null) {
            throw new IllegalStateException(
                "HTTP failure details are unavailable"
            );
        }

        // Retrieve the original Order
        Order order = exchange.getProperty(
                "ParsedOrder",
                Order.class
        );

        String orderId = order == null
                ? "UNKNOWN"
                : String.valueOf(order.getOrderId());

        int statusCode = failure.getStatusCode();

        String responseBody = failure.getResponseBody() == null
                ? ""
                : failure.getResponseBody();

        // Create failure report
        exchange.getMessage().setBody(
            "Order ID: " + orderId +
            "\nHTTP Status: " + statusCode +
            "\nResponse: " + responseBody
        );

        // Create unique output filename
        exchange.getMessage().setHeader(
            Exchange.FILE_NAME,
            "order-" + orderId +
            "-http-" + statusCode + ".txt"
        );

        exchange.getMessage().setHeader(
            "HttpStatusCode",
            statusCode
        );
    }


private void writeConnectionFailure(Exchange exchange) {

    Exception failure = exchange.getProperty(
        Exchange.EXCEPTION_CAUGHT,
        Exception.class
    );

    Order order = exchange.getProperty(
        "ParsedOrder",
        Order.class
    );

    String orderId = order == null
        ? "UNKNOWN"
        : String.valueOf(order.getOrderId());

    String errorMessage = failure == null
        ? "Unknown connection failure"
        : failure.getMessage();

    exchange.getMessage().setBody(
        "Order ID: " + orderId
        + "\nFailure: Network connection error"
        + "\nDetails: " + errorMessage
    );

    exchange.getMessage().setHeader(
        Exchange.FILE_NAME,
        "order-" + orderId + "-connection-failure.txt"
    );
}
}
