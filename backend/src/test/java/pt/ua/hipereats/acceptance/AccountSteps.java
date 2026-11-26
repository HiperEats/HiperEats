package pt.ua.hipereats.acceptance;

import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import pt.ua.hipereats.application.port.out.PasswordHasher;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/**
 * Steps about accounts and logging in (HE-28, reused by HE-35 and HE-36). Accounts are created
 * directly through the repository port; logging in and every request go through the REST API.
 */
public class AccountSteps {

    private final RestTestClient client;
    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final JdbcClient jdbc;

    private ResponseSpec response;
    private String accessToken;

    public AccountSteps(RestTestClient client, UserAccountRepository accounts, PasswordHasher passwordHasher,
                        JdbcClient jdbc) {
        this.client = client;
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.jdbc = jdbc;
    }

    /** Every scenario starts without accounts: the database is shared by the whole test run. */
    @Before
    public void deleteAllAccounts() {
        jdbc.sql("DELETE FROM user_accounts").update();
    }

    /** "courier", "chain manager", ... as written in the features. */
    @ParameterType("admin|courier|operator|technician|chain manager")
    public Role role(String name) {
        return Role.valueOf(name.toUpperCase(Locale.ROOT).replace(' ', '_'));
    }

    private void createAccount(String email, String password, Role role) {
        accounts.save(UserAccount.newAccount(email, passwordHasher.hash(password), role));
    }

    private ResponseSpec logIn(String email, String password) {
        return client.post().uri("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .exchange();
    }

    @Given("an active {role} account {string} with password {string}")
    public void anActiveAccountWithPassword(Role role, String email, String password) {
        createAccount(email, password, role);
    }

    @Given("a user logged in with the role {role}")
    public void aUserLoggedInWithTheRole(Role role) {
        String email = role.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@example.test";
        createAccount(email, "Secret-123", role);
        accessToken = logIn(email, "Secret-123")
                .expectStatus().isOk()
                .returnResult(SessionBody.class)
                .getResponseBody()
                .accessToken();
    }

    @When("{string} logs in with password {string}")
    public void logsInWithPassword(String email, String password) {
        response = logIn(email, password);
    }

    @When("a client asks for the scooters without logging in")
    public void aClientAsksForTheScootersWithoutLoggingIn() {
        response = client.get().uri("/api/v1/scooters").exchange();
    }

    @When("the user tries to list the user accounts")
    public void theUserTriesToListTheUserAccounts() {
        response = client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + accessToken)
                .exchange();
    }

    @When("the user tries to change the rules of use")
    public void theUserTriesToChangeTheRulesOfUse() {
        response = client.put().uri("/api/v1/rules")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reservationValidityMinutes", 10))
                .exchange();
    }

    @Then("the login succeeds with the role {role}")
    public void theLoginSucceedsWithTheRole(Role role) {
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.role").isEqualTo(role.name())
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.expiresAt").isNotEmpty();
    }

    @Then("the login is refused")
    public void theLoginIsRefused() {
        response.expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.accessToken").doesNotExist();
    }

    @Then("the request is refused as not authenticated")
    public void theRequestIsRefusedAsNotAuthenticated() {
        response.expectStatus().isUnauthorized();
    }

    @Then("the request is {word}")
    public void theRequestIs(String result) {
        switch (result) {
            case "allowed" -> response.expectStatus().is2xxSuccessful();
            case "forbidden" -> response.expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
            default -> throw new IllegalArgumentException("Unknown result: " + result);
        }
    }

    /** The part of the login response the steps need. */
    record SessionBody(String accessToken) {
    }
}
