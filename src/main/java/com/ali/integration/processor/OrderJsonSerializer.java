package com.ali.integration.processor;

import com.ali.integration.model.Order;
import tools.jackson.databind.ObjectMapper;

public class OrderJsonSerializer {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public String toJson(Order order){

        return objectMapper
                .writeValueAsString(order);
    }
}
