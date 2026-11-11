package com.ali.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "camel.main.auto-startup=false"
})

class EnterpriseIntegrationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
