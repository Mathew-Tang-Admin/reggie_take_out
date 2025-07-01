package com.itheima.reggie.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MY NOTES：配合 Response1.java 使用
 * @author MathewTang
 */
@NoArgsConstructor
@AllArgsConstructor
public enum CodeEnum {

    /** */
    OK(1, "成功"),
    // OK(200, "成功"),
    FAIL(400, "失败"),
    BAD_REQUEST(400, "请求错误"),
    NOT_FOUND(404, "未找到资源"),
    INTERNAL_ERROR(500, "内部服务器错误"),
    MODIFICATION_FAILED(400, "修改失败"),
    DELETION_FAILED(400, "删除失败"),
    CREATION_FAILED(400, "创建失败");

    /** */
    @Getter
    @Setter
    private int code;

    /** */
    @Getter
    @Setter
    private String msg;

}