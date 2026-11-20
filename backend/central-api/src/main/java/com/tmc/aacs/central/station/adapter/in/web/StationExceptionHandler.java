package com.tmc.aacs.central.station.adapter.in.web;

import java.time.Instant;
import java.util.List;

import com.tmc.aacs.central.shared.error.ApiError;
import com.tmc.aacs.central.station.domain.StationNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StationExceptionHandler {

    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(StationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("STATION_NOT_FOUND", exception.getMessage(), List.of(), Instant.now()));
    }
}
