package pt.ua.hipereats.adapters.in.web;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.hipereats.application.port.in.ListAccountsQuery;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/** The accounts of the platform, for the admin only (AGENTS.md, section 5: {@code /users}). */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final ListAccountsQuery listAccounts;

    public UserController(ListAccountsQuery listAccounts) {
        this.listAccounts = listAccounts;
    }

    @GetMapping
    public List<UserResponse> list() {
        return listAccounts.listAccounts().stream().map(UserResponse::from).toList();
    }

    /** An account as the API shows it: never the password hash. */
    public record UserResponse(Long id, String email, Role role) {
        static UserResponse from(UserAccount account) {
            return new UserResponse(account.id(), account.email(), account.role());
        }
    }
}
