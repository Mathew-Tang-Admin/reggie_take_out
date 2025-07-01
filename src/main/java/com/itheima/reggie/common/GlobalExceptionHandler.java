package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * @author MathewTang
 */
@ControllerAdvice(annotations = {RestController.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * TODO: 异常处理方法
     *     {@code SQLIntegrityConstraintViolationException.class} 新增员工时，username已存在时抛出的异常
     * @param exception {@link SQLIntegrityConstraintViolationException}
     * @return {@link R<String>}
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException exception) {
        log.error(exception.getMessage());
        if (exception.getMessage().contains("Duplicate entry")) {
            String[] split = exception.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }
        return R.error("未知错误");
    }

    /**
     * TODO: 异常处理方法
     *     {@code SQLIntegrityConstraintViolationException.class} 新增员工时，username已存在时抛出的异常
     *     删除菜品 / 套餐 套餐未停售
     * @param exception {@link SQLIntegrityConstraintViolationException}
     * @return {@link R<String>}
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException exception) {
        log.error(exception.getMessage());
        return R.error(exception.getMessage());
    }

}
