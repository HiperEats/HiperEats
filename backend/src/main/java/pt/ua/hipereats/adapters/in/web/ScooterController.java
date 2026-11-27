package pt.ua.hipereats.adapters.in.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.ScooterQuery;
import pt.ua.hipereats.domain.fleet.Scooter;

/**
 * REST resource of the fleet's scooters. Only an operator registers scooters; any logged-in user can
 * look one up.
 */
@RestController
@RequestMapping("/api/v1/scooters")
public class ScooterController {

    private final RegisterScooterUseCase registerScooter;
    private final ScooterQuery scooterQuery;

    public ScooterController(RegisterScooterUseCase registerScooter, ScooterQuery scooterQuery) {
        this.registerScooter = registerScooter;
        this.scooterQuery = scooterQuery;
    }

    /** The operator registers a scooter at a store (HE-29); it starts inactive. */
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ScooterResponse> register(@RequestBody RegisterScooterRequest request) {
        Scooter scooter = registerScooter.register(request.toCommand());
        URI location = UriComponentsBuilder.fromPath("/api/v1/scooters/{code}")
                .buildAndExpand(scooter.code())
                .encode()
                .toUri();
        return ResponseEntity.created(location).body(ScooterResponse.from(scooter));
    }

    @GetMapping("/{code}")
    public ScooterResponse find(@PathVariable String code) {
        return scooterQuery.findByCode(code)
                .map(ScooterResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Scooter " + code + " does not exist"));
    }
}
