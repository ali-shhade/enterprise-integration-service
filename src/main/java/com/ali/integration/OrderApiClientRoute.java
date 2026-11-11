
package com.ali.integration;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderApiClientRoute extends RouteBuilder {

    @Value("${integration.api.url}")
    private String apiUrl;

    @Value("${integration.api.token}")
    private String apiToken;

    @Override
    public void configure() {

        from("file:data/day4/http-input?noop=true")
            .routeId("send-order-to-api")

            .log("Sending order from file: ${file:name}")

            .convertBodyTo(String.class)

            // Identify the request body as JSON
            .setHeader(
                Exchange.CONTENT_TYPE,
                constant("application/json")
            )

            // Request JSON response
            .setHeader(
                "Accept",
                constant("application/json")
            )

            // Add authentication
            .setHeader(
                "Authorization",
                constant("Bearer " + apiToken)
            )

            // Send POST request
            .to(apiUrl + "?httpMethod=POST")

            .convertBodyTo(String.class)

            .log(
                "API response: HTTP "
                + "${header.CamelHttpResponseCode}"
                + " - ${body}"
            );
    }
}
