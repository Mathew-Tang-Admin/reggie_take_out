package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishFlavorMapper;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
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
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    /* @Autowired
    private DishMapper dishMapper; */

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * TODO: 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor
     *
     * @param dishDto {@link DishDto}
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品的基本信息
        this.save(dishDto);

        // 菜品id
        Long dishId = dishDto.getId();

        List<DishFlavor> flavors = dishDto.getFlavors();
        /* for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishId);
        } */
        flavors = flavors.stream().map(item -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        // 保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * TODO: 根据id 查询菜品详细信息 以及 菜品口味信息
     *
     * @param id {@link Long} 菜品id
     * @return {@link DishDto}
     */
    @Override
    public DishDto getDetailByIdWithFlavor(Long id) {
        // 查询菜品基本信息
        Dish dish = this.getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto); // 【源数据，目标】

        // 添加条件构造器
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        // 添加一个过滤条件
        queryWrapper.eq(DishFlavor::getDishId,id);

        // 根据菜品id 从dish_flavor查询菜品口味信息
        List<DishFlavor> list = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(list);

        return dishDto;
    }

    /**
     * TODO: 修改菜品详细信息 以及 菜品口味信息
     *
     * @param dishDto {@link DishDto}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithFlavor(DishDto dishDto) {
        // 更新dish表基本信息
        this.updateById(dishDto);

        /* // 添加条件构造器
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        // 添加一个过滤条件
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        // 我其实口味 用这个也可以修改成功，但问题是，口味 我可能会删除 或者 增加新的 所以 用这个就不合适了，不过现在想想 构造器完全没有用到
        dishFlavorService.updateBatchById(dishDto.getFlavors()); */

        // 删除当前菜品对应口味数据  dish_flavor表的delete操作
        // 添加条件构造器
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        // 添加一个过滤条件
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        boolean removeDishFlavor = dishFlavorService.remove(queryWrapper);

        // 添加当前菜品对应口味数据  dish_flavor表的insert操作
        Long dishId = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map(item -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());
        boolean saveDishFlavor = dishFlavorService.saveBatch(dishDto.getFlavors());

    }

    /**
     * TODO: 批量删除菜品 以及菜品口味
     *
     * @param ids {@link String}
     * @return {@link boolean}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // public boolean removeByIdsWithFlavor(String ids) {
    public boolean removeByIdsWithFlavor(List<Long> ids) {

        // List<String> idList = Arrays.asList(ids.split(","));

        // 查询菜品状态
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Dish::getId, ids);
        wrapper.eq(Dish::getStatus, 1);
        // int count = this.count(wrapper); // 老师是用这个
        List<Dish> dishList = this.list(wrapper);

        // 如果不能删除，抛出一个业务异常
        if (!dishList.isEmpty()) {
            throw new CustomException("不能删除菜品，菜品正在售卖中！");
        }

        // 不能这么做，老师所的做法 只是抛出了异常
        /* dishList = dishList.stream().peek(item -> item.setStatus(0)).collect(Collectors.toList());

        // 将 在售的菜品 停售
        this.updateBatchById(dishList); */

        // 删除菜品
        boolean removeDish = this.removeByIds(ids);

        // 删除菜品口味
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId, ids);
        boolean removeFlavor = dishFlavorService.remove(queryWrapper);
        return removeDish && removeFlavor;

    }
}
