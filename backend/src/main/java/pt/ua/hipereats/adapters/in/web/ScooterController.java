package pt.ua.hipereats.adapters.in.web;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import pt.ua.hipereats.application.port.in.AvailableScootersQuery;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.ScooterQuery;
import pt.ua.hipereats.domain.fleet.Scooter;

/**
 * REST resource of the fleet's scooters.
 *
 * <p>No role check yet: login and roles come with HE-28.
 */
@RestController
@RequestMapping("/api/v1/scooters")
public class ScooterController {

    private final RegisterScooterUseCase registerScooter;
    private final ScooterQuery scooterQuery;
    private final AvailableScootersQuery availableScooters;

    public ScooterController(RegisterScooterUseCase registerScooter, ScooterQuery scooterQuery,
                             AvailableScootersQuery availableScooters) {
        this.registerScooter = registerScooter;
        this.scooterQuery = scooterQuery;
        this.availableScooters = availableScooters;
    }

    /** The operator registers a scooter at a store (HE-29); it starts inactive. */
    @PostMapping
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

    /**
     * The courier sees the scooters of a store that can serve a delivery at this distance (HE-31,
     * BR1). The store is a parameter until login (HE-28) tells which store the courier works at.
     */
    @GetMapping
    public List<AvailableScooterResponse> available(@RequestParam long storeId,
                                                    @RequestParam BigDecimal deliveryDistanceKm) {
        throw new UnsupportedOperationException("HE-31: not implemented yet");
    }
}
