
package com.ali.integration.api;

import com.ali.integration.model.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

@Value("${integration.api.token}")
private String expectedToken;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Order order,

		  @RequestHeader(
            value = HttpHeaders.AUTHORIZATION,
            required = false
	)
        String authorization

) {

	if (!("Bearer " + expectedToken).equals(authorization)) {

    return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error",
                    "Missing or invalid bearer token"
                )
            );
}

        // Validate incoming order
        if (order.getOrderId() <= 0 ||
            order.getQuantity() <= 0 ||
            order.getCustomer() == null ||
            order.getCustomer().isBlank() ||
            order.getProduct() == null ||
            order.getProduct().isBlank()) {

            return ResponseEntity.badRequest().body(
                Map.of(
                    "error", "Invalid order data"
                )
            );
        }
	// Simulate a temporary service outage
if ("SIMULATE_503".equalsIgnoreCase(order.getProduct())) {

    return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                Map.of(
                    "error",
                    "Simulated temporary service outage"
                )
            );
}

// Simulate an API business validation error
if (order.getQuantity() > 100) {

    return ResponseEntity
            .badRequest()
            .body(
                Map.of(
                    "error",
                    "Quantity exceeds API limit of 100"
                )
            );
}


        return ResponseEntity.ok(
            Map.of(
                "message", "Order received successfully",
                "orderId", order.getOrderId()
            )
        );
    }
}
