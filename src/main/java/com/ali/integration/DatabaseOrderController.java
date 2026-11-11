
package com.ali.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/db/orders")
public class DatabaseOrderController {

    private final OrderTrackingService orderTrackingService;

    public DatabaseOrderController(
            OrderTrackingService orderTrackingService) {
        this.orderTrackingService = orderTrackingService;
    }

    public record ReceiveOrderRequest(
            String sourceSystem,
            String externalOrderId,
            String customer,
            BigDecimal amount,
            String currency
    ) {}

    @PostMapping
    public ResponseEntity<String> receiveOrder(
            @RequestBody ReceiveOrderRequest request) {

        try {
            long id = orderTrackingService.receiveOrder(
                    request.sourceSystem(),
                    request.externalOrderId(),
                    request.customer(),
                    request.amount(),
                    request.currency()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Order stored with database ID: " + id);

        } catch (DuplicateKeyException exception) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("This order already exists for this source system");
        }
    }

    @PatchMapping("/{sourceSystem}/{externalOrderId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable String sourceSystem,
            @PathVariable String externalOrderId,
            @RequestParam String status,
            @RequestParam(defaultValue = "Status updated")
                    String details) {

        orderTrackingService.changeStatus(
                sourceSystem,
                externalOrderId,
                status,
                details
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Map<String, Object>> findByStatus(
            @RequestParam String status) {

        return orderTrackingService.findByStatus(status);
    }
}

