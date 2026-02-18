package com.hsj.exception;

public class OutOfStockException extends BusinessException {

    public OutOfStockException() {
        super(ErrorCode.OUT_OF_STOCK);
    }

    public OutOfStockException(String message) {
        super(ErrorCode.OUT_OF_STOCK, message);
    }
}
