package pt.ua.hipereats.adapters.in.web;

import pt.ua.hipereats.domain.fleet.ScooterState;

/**
 * Body of {@code PATCH /api/v1/scooters/{code}}: the state the operator wants the scooter in. A body
 * without a state, or with an unknown one, is refused while it is read, with {@code 400 Bad Request}.
 */
public record ChangeScooterStateRequest(ScooterState state) {

    public ChangeScooterStateRequest {
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
    }
}
