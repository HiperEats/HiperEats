package pt.ua.hipereats;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ua.hipereats.domain.fleet.RangeRule;

/** The fixed business rule values, read from configuration (Product Specification, 3.2). */
@Configuration
public class RulesConfiguration {

    @Bean
    RangeRule rangeRule(
            @Value("${hipereats.rules.range.standard-full-km}") BigDecimal standardFullRangeKm,
            @Value("${hipereats.rules.range.long-range-full-km}") BigDecimal longRangeFullRangeKm,
            @Value("${hipereats.rules.range.margin}") BigDecimal margin) {
        return new RangeRule(standardFullRangeKm, longRangeFullRangeKm, margin);
    }
}
