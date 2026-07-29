package com.triptrace.global.error;

import com.triptrace.global.app.Domain;

public interface ErrorCode {
    public String getCode();
    public String getMessage();
    public default Domain getDomain() {return Domain.COMMON;}
}
