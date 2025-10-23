package com.clement.dexwin.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();
        return exceptionResponse(HttpStatus.BAD_REQUEST, String.join(", ", errors));
    }


    @ExceptionHandler({Exception.class,GenericException.class})
    public ResponseEntity<Object> handleGenericException(Exception e) {
        return exceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolationException(
        ConstraintViolationException ex) {
        List<String> errors =
            ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();
        return exceptionResponse(HttpStatus.BAD_REQUEST, String.join(", ", errors));
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handle403Exceptions(Exception e) {
        return exceptionResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handle409Exceptions(Exception e) {
        return exceptionResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({DuplicateEmailException.class, BadRequestException.class})
    public ResponseEntity<Object> handle400Exceptions(Exception e) {
        return exceptionResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<Object> handle404Exceptions(Exception e) {
        return exceptionResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    private ResponseEntity<Object> exceptionResponse(HttpStatus status, String details) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setDetail(details);
        return ResponseEntity.status(status).body(problemDetail);
    }
}