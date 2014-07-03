package com.manniwood.basicproj;

public class MPJWException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MPJWException() {
        super();
    }

    public MPJWException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MPJWException(String message, Throwable cause) {
        super(message, cause);
    }

    public MPJWException(String message) {
        super(message);
    }

    public MPJWException(Throwable cause) {
        super(cause);
    }

}
