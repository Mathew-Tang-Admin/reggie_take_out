package com.itheima.reggie.common;

/**
 * 基于ThreadLocal封装的工具类，用于保存和获取当前登录用户id
 *
 * @author MathewTang
 * @date 2025/06/18 22:08
 */
public class BaseContext {
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    /**
     * TODO: 设置值
     *
     * @param id {@link Long}
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }
    /**
     * TODO: 获取值
     *
     * @return {@link Long}
     */
    public  static Long getCurrentId() {
        return threadLocal.get();
    }
}
