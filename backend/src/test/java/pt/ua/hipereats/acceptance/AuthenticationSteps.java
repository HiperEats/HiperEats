package pt.ua.hipereats.acceptance;

import io.cucumber.java.en.Given;

/**
 * Login steps shared by the features.
 *
 * <p>Placeholder until HE-28 (log in) is implemented: the API does not authenticate users or check
 * roles yet, so this step has no effect for any role. It is written for any one-word role, so that
 * it does not compete with a second definition of the same sentence; HE-28 replaces it with a real
 * login (and covers "chain manager", which is two words).
 */
public class AuthenticationSteps {

    @Given("a user logged in with the role {word}")
    public void aUserLoggedInWithTheRole(String role) {
        // Intentionally empty until HE-28: see the class comment.
    }
}
