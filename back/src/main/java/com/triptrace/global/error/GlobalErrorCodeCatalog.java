package com.triptrace.global.error;

import com.triptrace.global.exception.ServiceException;

public final class GlobalErrorCodeCatalog {
    public static ServiceException badRequest() {
        return new ServiceException(DefaultErrorCode.BAD_REQUEST);
    }
    public static ServiceException badRequest(String message){
        return new ServiceException(DefaultErrorCode.BAD_REQUEST.getCode(),message);
    }
    public static ServiceException forbidden(){
        return new ServiceException(DefaultErrorCode.FORBIDDEN);
    }
    public static ServiceException forbidden(String message){
        return new ServiceException(DefaultErrorCode.FORBIDDEN.getCode(),message);
    }
    public static ServiceException unauthorized(){
        return new ServiceException(DefaultErrorCode.UNAUTHORIZED);
    }
    public static ServiceException unauthorized(String message){
        return new ServiceException(DefaultErrorCode.UNAUTHORIZED.getCode(),message);
    }
    public static ServiceException notFound(){
        return new ServiceException(DefaultErrorCode.NOT_FOUND);
    }
    public static ServiceException notFound(String message){
        return new ServiceException(DefaultErrorCode.NOT_FOUND.getCode(),message);
    }
    public static ServiceException conflict(){
        return new ServiceException(DefaultErrorCode.CONFLICT);
    }
    public static ServiceException conflict(String message){
        return new ServiceException(DefaultErrorCode.CONFLICT.getCode(),message);
    }
}
