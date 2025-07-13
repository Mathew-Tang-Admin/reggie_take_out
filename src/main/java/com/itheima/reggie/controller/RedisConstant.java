package com.itheima.reggie.controller;

import com.itheima.reggie.dto.DishDto;

/**
 * Redis常量类
 *
 * @author MathewTang
 * @date 2025/07/13 18:24
 */
public class RedisConstant {

    /** 菜品详情
     *      ${@link DishController#getDetail(Long)}
     *      ${@link DishController#save(DishDto)} */
    public static final String DISH_DETAIL = "dish_detail";

    /** 菜品分页信息
     *      ${@link DishController#page(String, Integer, Integer)} */
    public static final String DISH_PAGE = "dish_page";

}