package pt.ua.hipereats.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void whenStatusRequested_thenServiceNameAndUpAreReturned() {
        assertThat(mvc.get().uri("/api/status"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"service": "hipereats-backend", "status": "UP"}
                        """);
    }
}
