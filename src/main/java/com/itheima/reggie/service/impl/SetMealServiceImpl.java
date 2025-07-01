package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetMealMapper;
import com.itheima.reggie.service.SetMealService;
import com.itheima.reggie.service.SetMealDishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.mbeans.MBeanUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MathewTang
 * @date 2025/06/19 16:59
 */
@Slf4j
@Service
public class SetMealServiceImpl extends ServiceImpl<SetMealMapper, Setmeal> implements SetMealService {

    @Autowired
    private SetMealDishService setmealDishService;

    /**
     * TODO: 批量删除套餐 并将关联的菜品关系删除【注意：不是删除菜品】
     *
     * @param ids {@link String}
     * @return {@link boolean}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // public boolean deleteByIdsWithDish(String ids) {
    public boolean removeByIdsWithDish(List<Long> ids) {

        // List<String> idList = Arrays.asList(ids.split(","));

        // 查询套餐状态
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Setmeal::getId, ids);
        wrapper.eq(Setmeal::getStatus, 1);
        // int count = this.count(wrapper); // 老师是用这个
        List<Setmeal> setmealList = this.list(wrapper);

        // 如果不能删除，抛出一个业务异常
        if (!setmealList.isEmpty()) {
            throw new CustomException("不能删除套餐，套餐正在售卖中！");
        }

        // 不能这么做，老师所的做法 只是抛出了异常
        /* setmealList = setmealList.stream().peek(item -> item.setStatus(0)).collect(Collectors.toList());

        // 将 在售的套餐 停售
        this.updateBatchById(setmealList); */

        // 根据id 批量删除 套餐信息
        boolean setMealFlag = this.removeByIds(ids);

        // 删除关联的菜品关系【注意：不是删除菜品】
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getSetmealId, ids);
        boolean remove = setmealDishService.remove(queryWrapper);

        return setMealFlag && remove;
    }

    /**
     * TODO: 添加套餐 并添加关联的菜品关系
     *
     * @param setmealDto {@link SetmealDto}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSetMealWithDish(SetmealDto setmealDto) {
        // 添加 套餐基本信息
        this.save(setmealDto);

        // 添加 套餐与菜品关联信息到 SetmealDish表【注意 将setmeal_id 填入 】
        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes();
        setmealDishList = setmealDishList.stream().map(item -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishList);
    }

    /**
     * TODO: 根据id查询 套餐详细信息 以及关联的菜品关系
     *
     * @param id {@link Long}
     * @return {@link SetmealDto}
     */
    @Override
    public SetmealDto getDetailWithDish(Long id) {
        // 查询套餐基本信息
        Setmeal setmeal = this.getById(id);

        if (setmeal != null) {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);

            // 查询 对应 关联信息
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> list = setmealDishService.list(queryWrapper);

            setmealDto.setSetmealDishes(list);

            return setmealDto;
        }
        return null;
    }

    /**
     * TODO: 更新 套餐详细信息 以及关联的菜品关系
     *
     * @param setmealDto {@link SetmealDto}
     * @return {@link boolean}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithDish(SetmealDto setmealDto) {
        // 更新套餐基础信息
        this.updateById(setmealDto);

        // 删除套餐才行【根据套餐id 删除】
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        boolean remove = setmealDishService.remove(queryWrapper);

        // 插入套餐菜品【将setmeal_id 注入】
        List<SetmealDish> setmealDishList = setmealDto.getSetmealDishes();
        setmealDishList = setmealDishList.stream().peek(item -> item.setSetmealId(setmealDto.getId())).collect(Collectors.toList());

        boolean saveBatch = setmealDishService.saveBatch(setmealDishList);

        return remove && saveBatch;
    }
}
