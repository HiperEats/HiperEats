package pt.ua.hipereats.adapters.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Walking skeleton endpoint (HE-16): lets the web UI and the CI check that the backend is up.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final String serviceName;

    public StatusController(@Value("${spring.application.name}") String serviceName) {
        this.serviceName = serviceName;
    }

    @GetMapping
    public StatusResponse status() {
        return new StatusResponse(serviceName, "UP");
    }

    public record StatusResponse(String service, String status) {
    }
}
