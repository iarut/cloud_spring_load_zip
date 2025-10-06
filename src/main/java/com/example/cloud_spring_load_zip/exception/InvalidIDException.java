package com.example.cloud_spring_load_zip.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidIDException extends  RuntimeException{
    public InvalidIDException(String message) {
        super(message);
    }
}
