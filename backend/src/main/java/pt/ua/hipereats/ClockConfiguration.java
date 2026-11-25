package pt.ua.hipereats;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The platform clock. Code asks for this bean instead of calling now(), so tests can fix the time. */
@Configuration
public class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Lisbon"));
    }
}
