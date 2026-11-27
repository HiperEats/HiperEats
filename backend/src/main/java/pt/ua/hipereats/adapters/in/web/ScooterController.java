package pt.ua.hipereats.adapters.in.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.ChangeScooterStateUseCase;
import pt.ua.hipereats.application.port.in.ScooterQuery;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterNotFoundException;

/**
 * REST resource of the fleet's scooters. Only an operator registers scooters and changes their
 * state; any logged-in user can look one up.
 */
@RestController
@RequestMapping("/api/v1/scooters")
public class ScooterController {

    private final RegisterScooterUseCase registerScooter;
    private final ScooterQuery scooterQuery;
    private final ChangeScooterStateUseCase changeScooterState;

    public ScooterController(RegisterScooterUseCase registerScooter, ScooterQuery scooterQuery,
            ChangeScooterStateUseCase changeScooterState) {
        this.registerScooter = registerScooter;
        this.scooterQuery = scooterQuery;
        this.changeScooterState = changeScooterState;
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
                .orElseThrow(() -> new ScooterNotFoundException(code));
    }

    /**
     * The operator changes the scooter's state (HE-30): {@code AVAILABLE} activates it and
     * {@code INACTIVE} deactivates it. The other states are refused here with {@code 400}:
     * {@code RESERVED}, {@code IN_USE} and {@code MAINTENANCE} are reached through bookings and
     * maintenance, and {@code RETIRED} only when the operator retires an inactive scooter (HE-37;
     * Product Specification, 3.3). A change the scooter's current state does not allow is refused
     * with {@code 409}.
     */
    @PatchMapping("/{code}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ScooterResponse changeState(@PathVariable String code,
            @RequestBody ChangeScooterStateRequest request) {
        Scooter scooter = switch (request.state()) {
            case AVAILABLE -> changeScooterState.activate(code);
            case INACTIVE -> changeScooterState.deactivate(code);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The operator can only set the state to AVAILABLE or INACTIVE");
        };
        return ScooterResponse.from(scooter);
    }
}
