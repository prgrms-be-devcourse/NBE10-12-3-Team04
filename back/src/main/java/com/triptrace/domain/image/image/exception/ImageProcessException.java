package com.triptrace.domain.image.image.exception;

import com.triptrace.global.error.ErrorCode;

public class ImageProcessException extends RuntimeException {
    private final String resultCode;
    private final String msg;
    public ImageProcessException(String resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
        this.msg = message;
    }
    public ImageProcessException(String resultCode, String message) {
        this(resultCode, message, null);
    }

    public ImageProcessException(ErrorCode errorCode, String message) {
        super(message);
        this.resultCode = "%s-%s".formatted(errorCode.getCode(), errorCode.getDomain().getCode());
        this.msg = message;
    }
    public ImageProcessException(ErrorCode errorCode){
        this(errorCode, errorCode.getMessage());
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMsg() {
        return msg;
    }
}
