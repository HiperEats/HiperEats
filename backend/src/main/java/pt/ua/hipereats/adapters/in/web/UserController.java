package pt.ua.hipereats.adapters.in.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.ua.hipereats.application.port.in.ChangeAccountStatusUseCase;
import pt.ua.hipereats.application.port.in.ListAccountsQuery;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/** The accounts of the platform, for the admin only (AGENTS.md, section 5: {@code /users}). */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final ListAccountsQuery listAccounts;
    private final ChangeAccountStatusUseCase changeStatus;

    public UserController(ListAccountsQuery listAccounts, ChangeAccountStatusUseCase changeStatus) {
        this.listAccounts = listAccounts;
        this.changeStatus = changeStatus;
    }

    @GetMapping
    public List<UserResponse> list() {
        return listAccounts.listAccounts().stream().map(UserResponse::from).toList();
    }

    /**
     * Deactivates an account ({@code {"active": false}}) or activates it again (HE-36). The account
     * and its history stay; only logging in and using the API stop.
     */
    @PatchMapping("/{id}")
    public UserResponse changeStatus(@PathVariable long id, @RequestBody StatusChange change,
                                     @AuthenticationPrincipal Jwt admin) {
        if (change.active() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send {\"active\": true} or {\"active\": false}");
        }
        return UserResponse.from(changeStatus.setActive(id, change.active(), admin.getSubject()));
    }

    public record StatusChange(Boolean active) {
    }

    /** An account as the API shows it: never the password hash. */
    public record UserResponse(Long id, String email, Role role, boolean active) {
        static UserResponse from(UserAccount account) {
            return new UserResponse(account.id(), account.email(), account.role(), account.active());
        }
    }
}
