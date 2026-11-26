package pt.ua.hipereats.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import pt.ua.hipereats.application.port.in.RegisterScooterCommand;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.RetireScooterUseCase;
import pt.ua.hipereats.application.port.in.ScooterQuery;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterCodeAlreadyUsedException;
import pt.ua.hipereats.domain.fleet.ScooterNotFoundException;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterStateChangeRefusedException;
import pt.ua.hipereats.domain.fleet.ScooterType;
import pt.ua.hipereats.domain.fleet.StoreAtCapacityException;
import pt.ua.hipereats.domain.fleet.StoreNotFoundException;

@WebMvcTest(ScooterController.class)
class ScooterControllerTest {

    private static final String REGISTER_AV01 = """
            {"code": "AV-01", "type": "STANDARD", "storeId": 1}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegisterScooterUseCase registerScooter;

    @MockitoBean
    private ScooterQuery scooterQuery;

    @MockitoBean
    private RetireScooterUseCase retireScooter;

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/v1/scooters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    /** Invalid input is answered with a ProblemDetail and never reaches the use case. */
    private void assertBadRequest(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        then(registerScooter).should(never()).register(any());
    }

    @Test
    void whenScooterIsRegistered_thenCreatedWithItsLocation() {
        given(registerScooter.register(any())).willReturn(
                new Scooter("AV-08", ScooterType.LONG_RANGE, ScooterState.INACTIVE, 1L));

        MvcTestResult result = post("""
                {"code": "AV-08", "type": "LONG_RANGE", "storeId": 1}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .headers().hasValue(HttpHeaders.LOCATION, "/api/v1/scooters/AV-08");
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                {"code": "AV-08", "type": "LONG_RANGE", "state": "INACTIVE", "storeId": 1}
                """);
        then(registerScooter).should()
                .register(new RegisterScooterCommand("AV-08", ScooterType.LONG_RANGE, 1L));
    }

    @Test
    @DisplayName("BR13: registration at a store at capacity is refused naming the rule")
    void whenStoreIsAtCapacity_thenUnprocessableNamingBR13() {
        given(registerScooter.register(any())).willThrow(new StoreAtCapacityException(1L, 10));

        MvcTestResult result = post(REGISTER_AV01);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.rule").isEqualTo("BR13");
    }

    @Test
    void whenStoreDoesNotExist_thenNotFound() {
        given(registerScooter.register(any())).willThrow(new StoreNotFoundException(1L));

        assertThat(post(REGISTER_AV01)).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void whenCodeIsAlreadyUsed_thenConflict() {
        given(registerScooter.register(any()))
                .willThrow(new ScooterCodeAlreadyUsedException("AV-01"));

        assertThat(post(REGISTER_AV01)).hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void whenCodeIsMissing_thenBadRequest() {
        assertBadRequest(post("""
                {"type": "STANDARD", "storeId": 1}
                """));
    }

    @Test
    void whenCodeIsBlank_thenBadRequest() {
        assertBadRequest(post("""
                {"code": " ", "type": "STANDARD", "storeId": 1}
                """));
    }

    /**
     * The code is part of the scooter's URL and of its MQTT topic, so it is limited to letters,
     * digits and hyphens, 1 to 32 characters (assumption of HE-29: the specification sets no format).
     */
    @ParameterizedTest
    @ValueSource(strings = {"AV/01", "AV 01", "AV#1", "AV-01 ", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA-01"})
    void whenCodeHasAnInvalidFormat_thenBadRequest(String code) {
        assertBadRequest(post("""
                {"code": "%s", "type": "STANDARD", "storeId": 1}
                """.formatted(code)));
    }

    @Test
    void whenCodeHasTheMaximumLength_thenItIsAccepted() {
        String code = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAA-01";
        given(registerScooter.register(any())).willReturn(
                new Scooter(code, ScooterType.STANDARD, ScooterState.INACTIVE, 1L));

        MvcTestResult result = post("""
                {"code": "%s", "type": "STANDARD", "storeId": 1}
                """.formatted(code));

        assertThat(result).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void whenTypeIsMissing_thenBadRequest() {
        assertBadRequest(post("""
                {"code": "AV-01", "storeId": 1}
                """));
    }

    @Test
    void whenTypeIsUnknown_thenBadRequest() {
        assertBadRequest(post("""
                {"code": "AV-01", "type": "BICYCLE", "storeId": 1}
                """));
    }

    @Test
    void whenStoreIsMissing_thenBadRequest() {
        assertBadRequest(post("""
                {"code": "AV-01", "type": "STANDARD"}
                """));
    }

    @Test
    void whenScooterExists_thenItIsReturned() {
        given(scooterQuery.findByCode("AV-01")).willReturn(Optional.of(
                new Scooter("AV-01", ScooterType.STANDARD, ScooterState.INACTIVE, 1L)));

        MvcTestResult result = mvc.get().uri("/api/v1/scooters/AV-01").exchange();

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"code": "AV-01", "type": "STANDARD", "state": "INACTIVE", "storeId": 1}
                """);
    }

    @Test
    void whenScooterDoesNotExist_thenNotFound() {
        given(scooterQuery.findByCode("XX-99")).willReturn(Optional.empty());

        MvcTestResult result = mvc.get().uri("/api/v1/scooters/XX-99").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        then(scooterQuery).should().findByCode("XX-99");
    }

    private MvcTestResult patch(String code, String json) {
        return mvc.patch().uri("/api/v1/scooters/{code}", code)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @Test
    void whenOperatorSetsTheStateToRetired_thenTheScooterIsRetired() {
        given(retireScooter.retire("AV-01")).willReturn(
                new Scooter("AV-01", ScooterType.STANDARD, ScooterState.RETIRED, 1L));

        MvcTestResult result = patch("AV-01", """
                {"state": "RETIRED"}
                """);

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"code": "AV-01", "type": "STANDARD", "state": "RETIRED", "storeId": 1}
                """);
        then(retireScooter).should().retire("AV-01");
    }

    @Test
    void whenRetirementIsRefused_thenConflict() {
        given(retireScooter.retire("AV-01")).willThrow(new ScooterStateChangeRefusedException(
                "AV-01", ScooterState.AVAILABLE, ScooterState.RETIRED));

        MvcTestResult result = patch("AV-01", """
                {"state": "RETIRED"}
                """);

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Scooter AV-01 cannot go from AVAILABLE to RETIRED");
    }

    @Test
    void whenScooterToRetireDoesNotExist_thenNotFound() {
        given(retireScooter.retire("XX-99")).willThrow(new ScooterNotFoundException("XX-99"));

        MvcTestResult result = patch("XX-99", """
                {"state": "RETIRED"}
                """);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    /** RESERVED is reached through bookings, never set by the operator. */
    @Test
    void whenOperatorSetsAStateReachedThroughBookings_thenBadRequestAndNothingIsRetired() {
        MvcTestResult result = patch("AV-01", """
                {"state": "RESERVED"}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        then(retireScooter).should(never()).retire(any());
    }
}
