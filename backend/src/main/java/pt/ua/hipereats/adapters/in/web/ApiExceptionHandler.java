package pt.ua.hipereats.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.ua.hipereats.application.AccountDeactivatedException;
import pt.ua.hipereats.application.AccountNotFoundException;
import pt.ua.hipereats.application.CannotDeactivateOwnAccountException;
import pt.ua.hipereats.application.InvalidCredentialsException;

/** Turns the exceptions of the use cases into HTTP answers with a {@link ProblemDetail} body. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    ProblemDetail accountDeactivated(AccountDeactivatedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail accountNotFound(AccountNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CannotDeactivateOwnAccountException.class)
    ProblemDetail cannotDeactivateOwnAccount(CannotDeactivateOwnAccountException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
