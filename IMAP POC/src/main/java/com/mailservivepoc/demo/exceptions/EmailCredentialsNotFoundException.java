package com.mailservivepoc.demo.exceptions;

public class EmailCredentialsNotFoundException extends RuntimeException {
    public EmailCredentialsNotFoundException(String message) {
        super(message);
    }
}
