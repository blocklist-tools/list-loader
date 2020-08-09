package com.developerdan.blocklist.loader;

public class ApiException extends RuntimeException {
    public ApiException(Throwable ex)
    {
        super(ex);
    }

    public ApiException(String message, Throwable ex)
    {
        super(message, ex);
    }

    public ApiException(String message)
    {
        super(message);
    }
}
