package pt.ua.hipereats.simulator;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ua.hipereats.simulator.adapters.mqtt.MqttGateway;
import pt.ua.hipereats.simulator.application.FleetFactory;
import pt.ua.hipereats.simulator.application.FleetSimulator;
import pt.ua.hipereats.simulator.application.TripPlanner;
import pt.ua.hipereats.simulator.domain.GeoPoint;
import pt.ua.hipereats.simulator.domain.Store;
import tools.jackson.databind.json.JsonMapper;

/** Wires the simulation (plain Java classes) to Spring, the clock and the MQTT adapter. */
@Configuration
public class SimulatorConfiguration {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Lisbon"));
    }

    @Bean
    MqttGateway mqttGateway(SimulatorProperties properties, JsonMapper json) throws MqttException {
        return new MqttGateway(properties.mqtt(), json);
    }

    @Bean
    FleetSimulator fleetSimulator(SimulatorProperties properties, MqttGateway gateway, Clock clock) {
        // One seeded generator: the same seed gives the same fleet and the same trips.
        Random random = new Random(properties.seed());
        List<Store> stores = properties.stores().stream()
                .map(s -> new Store(s.code(), s.name(), new GeoPoint(s.latitude(), s.longitude()),
                        s.standardScooters(), s.longRangeScooters()))
                .toList();
        FleetFactory factory = new FleetFactory(random, properties.standardBatteryUsePerKm(),
                properties.longRangeBatteryUsePerKm(), properties.initialBatteryMin(), properties.initialBatteryMax());
        SimulatorProperties.Delivery delivery = properties.delivery();
        TripPlanner planner = new TripPlanner(random, delivery.nearShare(), delivery.middleShare(),
                properties.faultProbability(), delivery.standardMaxKm(), delivery.longRangeMaxKm(),
                delivery.rangeMargin());
        return new FleetSimulator(factory.create(stores), planner, gateway, clock,
                properties.telemetryInterval(), properties.speedKmh());
    }

    /** Connects to the broker once everything is created; commands go to the simulator. */
    @Bean
    ApplicationRunner connectToBroker(MqttGateway gateway, FleetSimulator simulator) {
        return args -> gateway.connect(simulator::handleCommand);
    }

    /** Runs one simulation step every telemetry interval (5 s by default). */
    @Component
    static class SimulationClock {

        private final FleetSimulator simulator;

        SimulationClock(FleetSimulator simulator) {
            this.simulator = simulator;
        }

        @Scheduled(fixedRateString = "${simulator.telemetry-interval}")
        void tick() {
            simulator.tick();
        }
    }
}
