package com.itheima.reggie.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Response<T> {

    private int code; // 响应的状态码
    private String msg; // 响应的消息
    private T data; // 响应的数据体

    // 用于构建成功的响应，不携带数据
    public static <T> Response<T> OK() {
        return Response.<T>builder()
                .code(CodeEnum.OK.getCode())
                .msg(CodeEnum.OK.getMsg())
                .build();
    }

    // 用于构建成功的响应，携带数据
    public static <T> Response<T> OK(T data) {
        return Response.<T>builder()
                .code(CodeEnum.OK.getCode())
                .msg(CodeEnum.OK.getMsg())
                .data(data)
                .build();
    }
    
    /** MathewTang添加
     * TODO: 用于构建成功的响应，自定义消息，不携带数据
     * @param msg {@link String}
     * @return {@link Response <T>}
     */
    public static <T> Response<T> OK(String msg) {
        return Response.<T>builder()
                .code(CodeEnum.OK.getCode())
                .msg(msg)
                .build();
    }

    /** MathewTang添加
     * TODO: 用于构建成功的响应，自定义消息，携带数据
     * @param msg {@link String}
     * @return {@link Response <T>}
     */
    public static <T> Response<T> OK(String msg, T data) {
        return Response.<T>builder()
                .code(CodeEnum.OK.getCode())
                .msg(msg)
                .data(data)
                .build();
    }

    // 用于构建失败的响应，不带任何参数，默认状态码为400，消息为"失败"
    public static <T> Response<T> FAIL() {
        return Response.<T>builder()
                .code(CodeEnum.FAIL.getCode())
                .msg(CodeEnum.FAIL.getMsg())
                .build();
    }

    // 用于构建失败的响应，自定义状态码和消息
    public static <T> Response<T> FAIL(CodeEnum codeEnum) {
        return Response.<T>builder()
                .code(codeEnum.getCode())
                .msg(codeEnum.getMsg())
                .build();
    }
}

/** MathewTang添加，使用方式 Controller层中：*/
/* @GetMapping("/detail")
public Response1<User> detail() {
    // 这个数据是service返回的数据
    User user = new User("jackson", 20);
    // 响应给前端
    // return R.OK(user);
    // return R.OK();
    // return R.FAIL();
    // return R.FAIL(404, "资源找不到");

    // return R.FAIL(CodeEnum.NOT_FOUND);

    return Response1.OK(CodeEnum.OK.getMsg());
    // return R.OK(CodeEnum.OK.getMsg(), user);
} */
