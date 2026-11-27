package pt.ua.hipereats.acceptance;

import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.HashMap;
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
    /** The token of the admin of the scenario, kept when another role logs in after it. */
    private String adminToken;
    /** Tokens of the named users who logged in during the scenario. */
    private final Map<String, String> tokensByEmail = new HashMap<>();

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
        accessToken = tokenOf(email, "Secret-123");
        if (role == Role.ADMIN) {
            adminToken = accessToken;
        }
    }

    private String tokenOf(String email, String password) {
        return logIn(email, password)
                .expectStatus().isOk()
                .returnResult(SessionBody.class)
                .getResponseBody()
                .accessToken();
    }

    /** The id of an account, as the admin sees it in the list of accounts. */
    private long idOf(String email) {
        UserBody[] users = client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .returnResult(UserBody[].class)
                .getResponseBody();
        return Arrays.stream(users).filter(u -> u.email().equals(email)).findFirst()
                .orElseThrow(() -> new AssertionError("No account " + email))
                .id();
    }

    private ResponseSpec deactivate(String email, String token) {
        return client.patch().uri("/api/v1/users/{id}", idOf(email))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("active", false))
                .exchange();
    }

    @Given("{string} is logged in with password {string}")
    public void isLoggedInWithPassword(String email, String password) {
        tokensByEmail.put(email, tokenOf(email, password));
    }

    @When("the admin deactivates the account {string}")
    public void theAdminDeactivatesTheAccount(String email) {
        deactivate(email, adminToken).expectStatus().isOk()
                .expectBody().jsonPath("$.active").isEqualTo(false);
    }

    @When("the user tries to deactivate the account {string}")
    public void theUserTriesToDeactivateTheAccount(String email) {
        response = deactivate(email, accessToken);
    }

    @Then("{string} cannot log in with password {string}")
    public void cannotLogInWithPassword(String email, String password) {
        logIn(email, password).expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.accessToken").doesNotExist();
    }

    @Then("{string} can still log in with password {string}")
    public void canStillLogInWithPassword(String email, String password) {
        logIn(email, password).expectStatus().isOk();
    }

    @Then("the access token of {string} is no longer accepted")
    public void theAccessTokenIsNoLongerAccepted(String email) {
        client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + tokensByEmail.get(email))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /** The token of the user who logged in last in the scenario, for the steps of other features. */
    public String accessToken() {
        return accessToken;
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

    /** The part of an account in the list that the steps need. */
    record UserBody(long id, String email) {
    }
}
