package com.christianjaena.crypto_signals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "mexc.access-key=test-key",
    "mexc.secret-key=test-secret"
})
class CryptoSignalsApplicationTests {

	@Test
	void contextLoads() {
	}

}
