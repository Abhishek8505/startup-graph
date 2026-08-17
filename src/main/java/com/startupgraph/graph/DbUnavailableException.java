package com.startupgraph.graph;

public class DbUnavailableException extends RuntimeException {

    public DbUnavailableException(String message) {
        super(message);
    }

    public DbUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
