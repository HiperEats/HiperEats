package pt.ua.hipereats.simulator.application;

/**
 * Outbound port: where the simulated fleet sends its messages. The MQTT adapter implements it;
 * unit tests use an in-memory fake, so the simulation logic is tested without a broker.
 */
public interface FleetPublisher {

    void publishTelemetry(TelemetryMessage message);

    void publishAck(CommandAck ack);
}
