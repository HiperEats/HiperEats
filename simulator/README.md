# Fleet simulator

Plays the hardware of every scooter (battery, GPS, lock, fault sensor) and talks to the platform
only through MQTT, so another simulator that follows the same contract can replace it.
Spring Boot 4 (no web server), Java 21, Eclipse Paho.

## What it does

- Creates 3 stores with 10 scooters each, 7 standard and 3 long range (BR13, BR14):
  `AV-01`…`AV-10` (Aveiro), `IL-01`…`IL-10` (Ílhavo), `AG-01`…`AG-10` (Águeda). In each store,
  `-01` to `-07` are standard and `-08` to `-10` long range.
- Every scooter starts parked and **locked** at its store and publishes one telemetry reading every 5 s.
- When the platform sends an **unlock** command (BR12), the scooter acknowledges it, unlocks and
  rides to a random delivery address and back at 20 km/h, spending 4 % (standard) or 2 % (long range)
  of battery per km. Back at its store it locks again (BR6).
- 2 % of the trips report a fault from the delivery address on (BR8).

Parameters come from the Product Specification (4.3) and are in `src/main/resources/application.yml`.
Each can be overridden with an environment variable (`simulator.telemetry-interval` becomes
`SIMULATOR_TELEMETRYINTERVAL`). The seed is fixed, so every run gives the same fleet and trips.

Assumptions not in the specification: initial battery between 20 % and 100 %; deliveries between
0.2 km and 3.5 km; a parked scooter does not charge; a fault stays until the simulator restarts.

## MQTT contract

| Direction | Topic | QoS |
|---|---|---|
| simulator → platform | `hipereats/scooters/{scooterCode}/telemetry` | 0 |
| platform → simulator | `hipereats/scooters/{scooterCode}/commands` | 1 |
| simulator → platform | `hipereats/scooters/{scooterCode}/acks` | 1 |

Telemetry (fields as in `AGENTS.md`):

```json
{"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
```

`batteryPercent` is an integer (rounded down); `fault` is `null`, `BRAKE_FAULT`, `MOTOR_FAULT` or `BATTERY_FAULT`.

Unlock command and its acknowledgement:

```json
{"commandId":"c0ffee-1","type":"UNLOCK","sentAt":"2026-11-25T19:40:00Z"}
{"commandId":"c0ffee-1","scooterCode":"AV-01","type":"UNLOCK","acknowledgedAt":"2026-11-25T19:40:00Z"}
```

A command for an unknown scooter or of an unknown type gets no acknowledgement. The same
`commandId` received twice is acknowledged again but starts no second trip (QoS 1 may deliver twice).

## Run and test

```bash
./mvnw test      # unit tests
./mvnw verify    # + integration tests with Mosquitto in Testcontainers (needs Docker)
```

With the whole stack (`docker compose up --build` at the repository root), watch the messages and
unlock a scooter:

```bash
docker compose exec mosquitto mosquitto_sub -t 'hipereats/scooters/#' -v
docker compose exec mosquitto mosquitto_pub -t hipereats/scooters/AV-01/commands \
  -m '{"commandId":"demo-1","type":"UNLOCK","sentAt":"2026-11-25T19:40:00Z"}'
```
