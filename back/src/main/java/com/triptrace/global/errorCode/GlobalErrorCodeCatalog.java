package com.triptrace.global.errorCode;

import com.triptrace.global.exception.ServiceException;

public final class GlobalErrorCodeCatalog {
    public static ServiceException invalid(){
        return new ServiceException(GlobalErrorCode.INVALID);
    }
    public static ServiceException invalid(String message){
        return new ServiceException(GlobalErrorCode.INVALID.getCode(),message);
    }
    public static ServiceException forbidden(){
        return new ServiceException(GlobalErrorCode.FORBIDDEN);
    }
    public static ServiceException forbidden(String message){
        return new ServiceException(GlobalErrorCode.FORBIDDEN.getCode(),message);
    }
    public static ServiceException unauthorized(){
        return new ServiceException(GlobalErrorCode.UNAUTHORIZED);
    }
    public static ServiceException unauthorized(String message){
        return new ServiceException(GlobalErrorCode.UNAUTHORIZED.getCode(),message);
    }
    public static ServiceException notFound(){
        return new ServiceException(GlobalErrorCode.NOT_FOUND);
    }
    public static ServiceException notFound(String message){
        return new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(),message);
    }
    public static ServiceException duplicate(){
        return new ServiceException(GlobalErrorCode.DUPLICATE);
    }
    public static ServiceException duplicate(String message){
        return new ServiceException(GlobalErrorCode.DUPLICATE.getCode(),message);
    }
}
