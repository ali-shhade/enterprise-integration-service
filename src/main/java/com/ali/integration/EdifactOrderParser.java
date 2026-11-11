
package com.ali.integration;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EdifactOrderParser {

    public ParsedEdifactOrder parse(String ediMessage) {

        String orderId = null;
        String customer = null;
        String currency = null;

        BigDecimal amount = null;
        boolean isOrdersMessage = false;

        // Separate the message into segments.
        for (String rawSegment : ediMessage.split("'")) {

            String segment = rawSegment.trim();

            if (segment.isEmpty()) {
                continue;
            }

            // Separate each segment into data elements.
            String[] elements = segment.split("\\+", -1);

            switch (elements[0]) {

                case "UNH" -> {
                    if (elements.length > 2) {
                        String[] messageDetails =
                                elements[2].split(":", -1);

                        isOrdersMessage =
                                "ORDERS".equals(messageDetails[0]);
                    }
                }

                case "BGM" -> {
                    if (elements.length > 2
                            && "220".equals(elements[1])) {
                        orderId = elements[2];
                    }
                }

                case "NAD" -> {
                    if (elements.length > 2
                            && "BY".equals(elements[1])) {

                        String[] partyDetails =
                                elements[2].split(":", -1);

                        customer = partyDetails[0];
                    }
                }

                case "MOA" -> {
                    if (elements.length > 1) {

                        String[] amountDetails =
                                elements[1].split(":", -1);

                        if (amountDetails.length >= 2
                                && "203".equals(amountDetails[0])) {

                            amount = new BigDecimal(amountDetails[1]);

                            if (amountDetails.length >= 3) {
                                currency = amountDetails[2];
                            }
                        }
                    }
                }
            }
        }

        if (!isOrdersMessage
                || orderId == null
                || customer == null
		|| customer.isBlank()
        	|| amount == null
        	|| currency == null
        	|| currency.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid or incomplete EDIFACT order"
            );
        }

        return new ParsedEdifactOrder(
                orderId,
                "NEW",
                customer,
                amount,
                currency
        );
    }

    public record ParsedEdifactOrder(
            String orderId,
            String status,
            String customer,
            BigDecimal amount,
            String currency
    ) {}
}

