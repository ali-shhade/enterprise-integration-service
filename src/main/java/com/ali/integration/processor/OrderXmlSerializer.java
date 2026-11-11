package com.ali.integration.processor;

import com.ali.integration.model.Order;
import tools.jackson.dataformat.xml.XmlMapper;

public class OrderXmlSerializer {

    private final XmlMapper xmlMapper =
            new XmlMapper();

    public String toXml(Order order) {

        return xmlMapper.writeValueAsString(order);
    }
}
