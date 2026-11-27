package pt.ua.hipereats.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import pt.ua.hipereats.application.port.in.RegisterScooterCommand;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.ChangeScooterStateUseCase;
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
@Import(SecurityConfiguration.class)
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
    private ChangeScooterStateUseCase changeScooterState;

    /** A request authenticated as a user with this role, without signing a real token. */
    private static JwtRequestPostProcessor loggedInAs(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private MvcTestResult post(String json) {
        return post(json, loggedInAs("OPERATOR"));
    }

    private MvcTestResult post(String json, JwtRequestPostProcessor user) {
        return mvc.post().uri("/api/v1/scooters")
                .with(user)
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
    @Requirement("HE-29")
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
    @Requirement("HE-29")
    @DisplayName("BR13: registration at a store at capacity is refused naming the rule")
    void whenStoreIsAtCapacity_thenUnprocessableNamingBR13() {
        given(registerScooter.register(any())).willThrow(new StoreAtCapacityException(1L, 10));

        MvcTestResult result = post(REGISTER_AV01);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.rule").isEqualTo("BR13");
    }

    @Test
    @Requirement("HE-29")
    void whenStoreDoesNotExist_thenNotFound() {
        given(registerScooter.register(any())).willThrow(new StoreNotFoundException(1L));

        assertThat(post(REGISTER_AV01)).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @Requirement("HE-29")
    void whenCodeIsAlreadyUsed_thenConflict() {
        given(registerScooter.register(any()))
                .willThrow(new ScooterCodeAlreadyUsedException("AV-01"));

        assertThat(post(REGISTER_AV01)).hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @Requirement("HE-29")
    void whenCodeIsMissing_thenBadRequest() {
        assertBadRequest(post("""
                {"type": "STANDARD", "storeId": 1}
                """));
    }

    @Test
    @Requirement("HE-29")
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
    @Requirement("HE-29")
    @ValueSource(strings = {"AV/01", "AV 01", "AV#1", "AV-01 ", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA-01"})
    void whenCodeHasAnInvalidFormat_thenBadRequest(String code) {
        assertBadRequest(post("""
                {"code": "%s", "type": "STANDARD", "storeId": 1}
                """.formatted(code)));
    }

    @Test
    @Requirement("HE-29")
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
    @Requirement("HE-29")
    void whenTypeIsMissing_thenBadRequest() {
        assertBadRequest(post("""
                {"code": "AV-01", "storeId": 1}
                """));
    }

    @Test
    @Requirement("HE-29")
    void whenTypeIsUnknown_thenBadRequest() {
        assertBadRequest(post("""
                {"code": "AV-01", "type": "BICYCLE", "storeId": 1}
                """));
    }

    @Test
    @Requirement("HE-29")
    void whenStoreIsMissing_thenBadRequest() {
        assertBadRequest(post("""
                {"code": "AV-01", "type": "STANDARD"}
                """));
    }

    @Test
    @Requirement("HE-29")
    void whenScooterExists_thenItIsReturned() {
        given(scooterQuery.findByCode("AV-01")).willReturn(Optional.of(
                new Scooter("AV-01", ScooterType.STANDARD, ScooterState.INACTIVE, 1L)));

        MvcTestResult result = mvc.get().uri("/api/v1/scooters/AV-01").with(loggedInAs("COURIER")).exchange();

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"code": "AV-01", "type": "STANDARD", "state": "INACTIVE", "storeId": 1}
                """);
    }

    @Test
    @Requirement("HE-29")
    void whenScooterDoesNotExist_thenNotFound() {
        given(scooterQuery.findByCode("XX-99")).willReturn(Optional.empty());

        MvcTestResult result = mvc.get().uri("/api/v1/scooters/XX-99").with(loggedInAs("COURIER")).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Scooter not found");
        then(scooterQuery).should().findByCode("XX-99");
    }

    private MvcTestResult patch(String code, String json) {
        return patch(code, json, loggedInAs("OPERATOR"));
    }

    private MvcTestResult patch(String code, String json, JwtRequestPostProcessor user) {
        return mvc.patch().uri("/api/v1/scooters/{code}", code)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    /** A state change that is not allowed here is answered with a ProblemDetail and changes nothing. */
    private void assertStateChangeBadRequest(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        then(changeScooterState).should(never()).activate(any());
        then(changeScooterState).should(never()).deactivate(any());
    }

    @Test
    @Requirement("HE-30")
    void whenOperatorSetsTheStateToAvailable_thenTheScooterIsActivated() {
        given(changeScooterState.activate("AV-01")).willReturn(
                new Scooter("AV-01", ScooterType.STANDARD, ScooterState.AVAILABLE, 1L));

        MvcTestResult result = patch("AV-01", """
                {"state": "AVAILABLE"}
                """);

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"code": "AV-01", "type": "STANDARD", "state": "AVAILABLE", "storeId": 1}
                """);
        then(changeScooterState).should().activate("AV-01");
    }

    @Test
    @Requirement("HE-30")
    void whenOperatorSetsTheStateToInactive_thenTheScooterIsDeactivated() {
        given(changeScooterState.deactivate("AV-01")).willReturn(
                new Scooter("AV-01", ScooterType.STANDARD, ScooterState.INACTIVE, 1L));

        MvcTestResult result = patch("AV-01", """
                {"state": "INACTIVE"}
                """);

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"code": "AV-01", "type": "STANDARD", "state": "INACTIVE", "storeId": 1}
                """);
        then(changeScooterState).should().deactivate("AV-01");
    }

    @Test
    @Requirement("HE-30")
    void whenStateChangeIsRefused_thenConflict() {
        given(changeScooterState.deactivate("AV-01")).willThrow(new ScooterStateChangeRefusedException(
                "AV-01", ScooterState.IN_USE, ScooterState.INACTIVE));

        MvcTestResult result = patch("AV-01", """
                {"state": "INACTIVE"}
                """);

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail")
                .isEqualTo("Scooter AV-01 cannot go from IN_USE to INACTIVE");
    }

    /** Another change holds the scooter's lock for longer than the lock timeout. */
    @Test
    @Requirement("HE-30")
    void whenScooterToDeactivateIsLockedByAnotherChange_thenConflict() {
        given(changeScooterState.deactivate("AV-01"))
                .willThrow(new CannotAcquireLockException("lock timeout"));

        MvcTestResult result = patch("AV-01", """
                {"state": "INACTIVE"}
                """);

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @Requirement("HE-30")
    void whenScooterToChangeDoesNotExist_thenNotFound() {
        given(changeScooterState.activate("XX-99")).willThrow(new ScooterNotFoundException("XX-99"));

        MvcTestResult result = patch("XX-99", """
                {"state": "AVAILABLE"}
                """);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @Requirement("HE-30")
    void whenStateIsMissing_thenBadRequest() {
        assertStateChangeBadRequest(patch("AV-01", "{}"));
    }

    @Test
    @Requirement("HE-30")
    void whenStateIsUnknown_thenBadRequest() {
        assertStateChangeBadRequest(patch("AV-01", """
                {"state": "BROKEN"}
                """));
    }

    /**
     * Reserved, in use and maintenance are reached through bookings and maintenance; retired only
     * when the operator retires an inactive scooter (HE-37; Product Specification, 3.3).
     */
    @ParameterizedTest
    @ValueSource(strings = {"RESERVED", "IN_USE", "MAINTENANCE", "RETIRED"})
    @Requirement("HE-30")
    void whenOperatorSetsAStateOtherThanAvailableOrInactive_thenBadRequest(String state) {
        assertStateChangeBadRequest(patch("AV-01", """
                {"state": "%s"}
                """.formatted(state)));
    }

    @Test
    @Requirement("HE-29")
    void whenNoTokenIsSent_thenUnauthorizedAndNothingIsRegistered() {
        MvcTestResult result = mvc.post().uri("/api/v1/scooters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_AV01)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        then(registerScooter).should(never()).register(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"COURIER", "TECHNICIAN", "CHAIN_MANAGER", "ADMIN"})
    @Requirement("HE-29")
    void whenUserIsNotAnOperator_thenForbiddenAndNothingIsRegistered(String role) {
        MvcTestResult result = post(REGISTER_AV01, loggedInAs(role));

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        then(registerScooter).should(never()).register(any());
    }
}
