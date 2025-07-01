package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author MathewTang
 * @date 2025/06/19 16:57
 */
@Service
public interface SetMealService extends IService<Setmeal> {

    /**
     * TODO: 批量删除套餐 并将关联的菜品关系删除【注意：不是删除菜品】
     *
     * @param ids {@link String}
     * @return {@link boolean}
     */
    // boolean deleteByIdsWithDish(String ids);
    boolean removeByIdsWithDish(List<Long> ids);

    /**
     * TODO: 添加套餐 并添加关联的菜品关系
     *
     * @param setmealDto {@link SetmealDto}
     */
    void saveSetMealWithDish(SetmealDto setmealDto);

    /**
     * TODO: 根据id查询 套餐详细信息 以及关联的菜品关系
     *
     * @param id {@link Long}
     * @return {@link SetmealDto}
     */
    SetmealDto getDetailWithDish(Long id);

    /**
     * TODO: 更新 套餐详细信息 以及关联的菜品关系
     *
     * @param setmealDto {@link SetmealDto}
     * @return {@link boolean}
     */
    boolean updateWithDish(SetmealDto setmealDto);
}
