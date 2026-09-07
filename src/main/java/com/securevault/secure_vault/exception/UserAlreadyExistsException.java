package com.securevault.secure_vault.exception;


public class UserAlreadyExistsException extends RuntimeException {

     public UserAlreadyExistsException(String message) {
        super(message);
    }
}
