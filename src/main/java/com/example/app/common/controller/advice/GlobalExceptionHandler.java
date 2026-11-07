package com.example.app.common.controller.advice;

import com.example.app.common.enums.ResponseStatusEnums;
import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.exception.DeleteFailException;
import com.example.app.common.exception.DuplicatedDataException;
import com.example.app.common.exception.InsertionFailException;
import com.example.app.common.models.RespMsg;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespMsg> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(error.getDefaultMessage());
        }
        log.warn("Validation failed: {}", sb);
        return new ResponseEntity<>(createFailResponse(sb.toString()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<RespMsg> handleDataNotFoundException(DataNotFoundException ex) {
        log.warn("Data not found: {}", ex.getMessage());
        return new ResponseEntity<>(createFailResponse(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicatedDataException.class)
    public ResponseEntity<RespMsg> handleDuplicatedDataException(DuplicatedDataException ex) {
        log.warn("Duplicate data: {}", ex.getMessage());
        return new ResponseEntity<>(createFailResponse(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsertionFailException.class)
    public ResponseEntity<RespMsg> handleInsertionFailException(InsertionFailException ex) {
        log.error("Insertion failed: {}", ex.getMessage());
        return new ResponseEntity<>(createFailResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeleteFailException.class)
    public ResponseEntity<RespMsg> handleDeleteFailException(DeleteFailException ex) {
        log.warn("Delete failed: {}", ex.getMessage());
        return new ResponseEntity<>(createFailResponse(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    // Hibernate
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespMsg> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getConstraintName());
        return new ResponseEntity<>(createFailResponse("Constraint violation: " + ex.getConstraintName()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespMsg> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return new ResponseEntity<>(createFailResponse("DB Data integrity violation: " + ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespMsg> handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        return new ResponseEntity<>(createFailResponse("Unexpected error: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private RespMsg createFailResponse(String message) {
        RespMsg resp = new RespMsg();
        resp.setStatus(ResponseStatusEnums.FAIL.getStatus());
        resp.setMessage(message);
        return resp;
    }

}
