package com.author.kurtkalwin.datastore.Exceptions;

public class InvalidKeyException extends Exception {
    public String exceptionMsg;

    public InvalidKeyException(String exceptionMsg) {
        this.exceptionMsg = exceptionMsg;
    }
}
