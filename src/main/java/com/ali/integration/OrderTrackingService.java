
package com.ali.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderTrackingService {

    private final JdbcTemplate jdbcTemplate;

   private static final Logger log =
        LoggerFactory.getLogger(OrderTrackingService.class);

    public OrderTrackingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long receiveOrder(
            String sourceSystem,
            String externalOrderId,
            String customer,
            BigDecimal amount,
            String currency) {

        // 1. Insert the order.
        jdbcTemplate.update("""
            INSERT INTO integration_orders
                (source_system, external_order_id,
                 customer, amount, currency, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            sourceSystem,
            externalOrderId,
            customer,
            amount,
            currency,
            "RECEIVED"
        );

        // 2. Retrieve the generated internal database ID.
        Long orderId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM integration_orders
            WHERE source_system = ?
              AND external_order_id = ?
            """,
            Long.class,
            sourceSystem,
            externalOrderId
        );

        // 3. Create the initial history entry.
        jdbcTemplate.update("""
            INSERT INTO order_status_history
                (order_id, previous_status, new_status, details)
            VALUES (?, ?, ?, ?)
            """,
            orderId,
            null,
            "RECEIVED",
            "Order received from " + sourceSystem
        );

        return orderId;
    }

    @Transactional
    public void changeStatus(
            String sourceSystem,
            String externalOrderId,
            String newStatus,
            String details) {

        // Find the internal database ID.
        Long orderId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM integration_orders
            WHERE source_system = ?
              AND external_order_id = ?
            """,
            Long.class,
            sourceSystem,
            externalOrderId
        );

        // Read the previous status.
        String previousStatus = jdbcTemplate.queryForObject("""
            SELECT status
            FROM integration_orders
            WHERE id = ?
            """,
            String.class,
            orderId
        );

        // Update the current order status.
        int updatedRows = jdbcTemplate.update("""
            UPDATE integration_orders
            SET status = ?
            WHERE id = ?
            """,
            newStatus,
            orderId
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                "Expected exactly one updated order"
            );
        }

        // Record the transition.
        jdbcTemplate.update("""
            INSERT INTO order_status_history
                (order_id, previous_status, new_status, details)
            VALUES (?, ?, ?, ?)
            """,
            orderId,
            previousStatus,
            newStatus,
            details
        );

	log.atInfo()
    .addKeyValue("event", "order_status_changed")
    .addKeyValue("status", newStatus)
    .addKeyValue("sourceSystem", sourceSystem)
    .addKeyValue("orderId", externalOrderId)
    .addKeyValue(
        "correlationId",
        sourceSystem + ":" + externalOrderId
    )
    .log("Order processing status updated");
    }

    public List<Map<String, Object>> findByStatus(String status) {

        return jdbcTemplate.queryForList("""
            SELECT
                id,
                source_system,
                external_order_id,
                customer,
                amount,
                currency,
                status,
                created_at,
                updated_at
            FROM integration_orders
            WHERE status = ?
            ORDER BY created_at DESC
            """,
            status
        );
    }
}
