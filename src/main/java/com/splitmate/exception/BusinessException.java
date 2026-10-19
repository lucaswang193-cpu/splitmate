package com.splitmate.exception;

public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    //
    public BusinessException(String message) {
        // 把訊息存進 RuntimeException的 detailMessage欄位，之後e.getMessage()就可以取得
        super(message);
    }

}
