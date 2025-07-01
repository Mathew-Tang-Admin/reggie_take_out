package com.itheima.reggie.common;

/**
 * 自定义业务异常
 *
 * @author MathewTang
 * @date 2025/06/20 17:23
 */
public class CustomException extends RuntimeException {
    public CustomException() {
        super();
    }

    public CustomException(String message) {
        super(message);
    }
}
