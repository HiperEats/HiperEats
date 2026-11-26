package pt.ua.hipereats.adapters.in.web;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pt.ua.hipereats.domain.fleet.ScooterCodeAlreadyUsedException;
import pt.ua.hipereats.domain.fleet.ScooterNotFoundException;
import pt.ua.hipereats.domain.fleet.ScooterStateChangeRefusedException;
import pt.ua.hipereats.domain.fleet.StoreAtCapacityException;
import pt.ua.hipereats.domain.fleet.StoreNotFoundException;

/**
 * Error responses of the API as {@code ProblemDetail} ({@code application/problem+json}): the
 * exceptions of Spring MVC (invalid body, unknown resource, ...) through the base class, and the
 * refusals of the use cases here. A refusal by a business rule names the rule in {@code rule}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(StoreAtCapacityException.class)
    ProblemDetail storeAtCapacity(StoreAtCapacityException e) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
        problem.setTitle("Store at capacity");
        problem.setProperty("rule", "BR13");
        return problem;
    }

    @ExceptionHandler(StoreNotFoundException.class)
    ProblemDetail storeNotFound(StoreNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Store not found");
        return problem;
    }

    @ExceptionHandler(ScooterCodeAlreadyUsedException.class)
    ProblemDetail scooterCodeAlreadyUsed(ScooterCodeAlreadyUsedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Scooter code already used");
        return problem;
    }

    @ExceptionHandler(ScooterNotFoundException.class)
    ProblemDetail scooterNotFound(ScooterNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Scooter not found");
        return problem;
    }

    @ExceptionHandler(ScooterStateChangeRefusedException.class)
    ProblemDetail scooterStateChangeRefused(ScooterStateChangeRefusedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Scooter state change refused");
        return problem;
    }

    /**
     * Another change of the same scooter holds its lock for longer than the lock timeout, or changed
     * it first. Nothing was changed, and the client can try again.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    ProblemDetail concurrentChange(ConcurrencyFailureException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Another change of the same resource is in progress; try again");
        problem.setTitle("Concurrent change");
        return problem;
    }
}
