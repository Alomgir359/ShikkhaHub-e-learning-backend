package com.lms.lms_backend.controller;

import com.lms.lms_backend.service.ClassroomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the {success, message, ...} JSON the React dashboards expect. */
final class ClassroomResponses {

    private ClassroomResponses() { }

    /** ok("message", "Saved", "item", obj) -> {success:true, message:"Saved", item:{...}} */
    static ResponseEntity<Map<String, Object>> ok(Object... keyValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        for (int i = 0; i + 1 < keyValues.length; i += 2) body.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        return ResponseEntity.ok(body);
    }

    static ResponseEntity<Map<String, Object>> fail(Exception e) {
        HttpStatus status;
        String message = e.getMessage();
        if (e instanceof ClassroomException ce) {
            status = ce.getStatus();
        } else if (e instanceof MaxUploadSizeExceededException) {
            status = HttpStatus.PAYLOAD_TOO_LARGE;
            message = "The file is too large.";
        } else if (e instanceof IOException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "The file could not be saved. Please try again.";
            e.printStackTrace();
        } else if (e instanceof NumberFormatException) {
            status = HttpStatus.BAD_REQUEST;
            message = "A number field has an invalid value.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Something went wrong. Please try again.";
            e.printStackTrace();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    static Map<String, Object> plain(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, value);
        return m;
    }
}
