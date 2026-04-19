package org.example.musicbooru.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            GenericException e,
            HttpServletRequest request) {

        log.error("Exception occurred: {}, Request details: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                e.getStatus(),
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, e.getStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException e,
            HttpServletRequest request) {

        log.error("Resource not found: {}, Request details: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND,
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request) {

        log.error("Illegal argument: {}, Request details: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
