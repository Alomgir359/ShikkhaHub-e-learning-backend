package com.lms.lms_backend.service;

import org.springframework.http.HttpStatus;

/** Business-rule error for the classroom features; the controller maps it to {success:false, message}. */
public class ClassroomException extends RuntimeException {
    private final HttpStatus status;

    public ClassroomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ClassroomException badRequest(String message) { return new ClassroomException(HttpStatus.BAD_REQUEST, message); }
    public static ClassroomException forbidden(String message) { return new ClassroomException(HttpStatus.FORBIDDEN, message); }
    public static ClassroomException notFound(String message) { return new ClassroomException(HttpStatus.NOT_FOUND, message); }
    public static ClassroomException conflict(String message) { return new ClassroomException(HttpStatus.CONFLICT, message); }

    public HttpStatus getStatus() { return status; }
}
