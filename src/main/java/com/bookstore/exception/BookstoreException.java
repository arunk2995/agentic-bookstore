package com.bookstore.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BookstoreException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public BookstoreException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static BookstoreException notFound(String resource, Object id) {
        return new BookstoreException(resource + " not found: " + id,
            HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public static BookstoreException conflict(String message) {
        return new BookstoreException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static BookstoreException badRequest(String message) {
        return new BookstoreException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static BookstoreException forbidden(String message) {
        return new BookstoreException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static BookstoreException unauthorized(String message) {
        return new BookstoreException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static BookstoreException insufficientInventory(String message) {
        return new BookstoreException(message, HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY");
    }

    public static BookstoreException invalidOperation(String message) {
        return new BookstoreException(message, HttpStatus.BAD_REQUEST, "INVALID_OPERATION");
    }
}
