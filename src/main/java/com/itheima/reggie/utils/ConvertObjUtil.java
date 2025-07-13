package com.itheima.reggie.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itheima.reggie.service.impl.DishServiceImpl;

/**
 * @author MathewTang
 * @date 2025/07/13 19:42
 */
public class ConvertObjUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * TODO: ${@link DishServiceImpl#getDetailByIdWithFlavor(Long)} L111
     *   为解决这样问题：java.util.LinkedHashMap cannot be cast to com.baomidou.mybatisplus.extension.plugins.pagination.Page
     *
     * @param obj {@link Object}
     * @param targetType {@link Class<T>}
     * @return {@link T}
     */
    public static <T> T convertObj(Object obj, Class<T> targetType) {
        return mapper.convertValue(obj, targetType);
    }
}
