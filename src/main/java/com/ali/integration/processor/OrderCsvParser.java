package com.ali.integration.processor;

import com.ali.integration.model.Order;

public class OrderCsvParser {

    public Order parseLine(String line) {

        String[] values = line.split(",", -1);

	  if (values.length != 4) {
        throw new IllegalArgumentException(
            "Expected 4 columns, found " + values.length
        );
    }

	 String customer = values[1].trim();
         String product = values[2].trim();

	if (customer.isEmpty() || product.isEmpty()) {
        throw new IllegalArgumentException(
            "Customer or product is empty"
        );
    }

    long orderId;
    int quantity;

    try {
        orderId = Long.parseLong(values[0].trim());
        quantity = Integer.parseInt(values[3].trim());

    } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid order ID or quantity", e
        );
    }

    if (orderId <= 0 || quantity <= 0) {
        throw new IllegalArgumentException(
            "Order ID and quantity must be positive"
        );
    }

        return new Order(
                orderId,
                customer,
                product,
                quantity
        );
    }
}
