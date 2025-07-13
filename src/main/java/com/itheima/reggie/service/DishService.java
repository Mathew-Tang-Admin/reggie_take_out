package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

import java.util.List;

/**
 * @author MathewTang
 * @date 2025/06/19 16:57
 */
public interface DishService extends IService<Dish> {



    /**
     * TODO: 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor（改用Redis缓存）
     *
     * @param dishDto {@link DishDto}
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * TODO: 根据id 查询菜品详细信息 以及 菜品口味信息（改用Redis缓存）
     *
     * @param id {@link Long} 菜品id
     * @return {@link DishDto}
     */
    DishDto getDetailByIdWithFlavor(Long id);

    /**
     * TODO: 修改菜品详细信息 以及 菜品口味信息
     *
     * @param dishDto {@link DishDto}
     */
    void updateWithFlavor(DishDto dishDto);

    /**
     * TODO: 批量删除菜品 以及菜品口味
     *
     * @param ids {@link String}
     * @return {@link boolean}
     */
    // boolean removeByIdsWithFlavor(String ids);
    boolean removeByIdsWithFlavor(List<Long> ids);
}
