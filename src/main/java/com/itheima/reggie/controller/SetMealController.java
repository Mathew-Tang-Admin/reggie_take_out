package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetMealDishService;
import com.itheima.reggie.service.SetMealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐 控制层
 * @author MathewTang
 * @date 2025/06/19 17:02
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetMealController {

    @Autowired
    private SetMealService setMealService;

    @Autowired
    private SetMealDishService setMealDishService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/page")
    public R<Page<SetmealDto>> page(Integer page, Integer pageSize, String name) {
        log.info("套餐分页查询，page={},pageSize={}", page, pageSize);

        // 构造分页构造器对象
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();// 添加一个过滤条件
        // 添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        // 执行分页查询
        setMealService.page(setmealPage, queryWrapper);

        // 对象拷贝【源数据，目标】
        BeanUtils.copyProperties(setmealPage, setmealDtoPage,"records");

        List<Setmeal> records = setmealPage.getRecords();
        List<SetmealDto> list = records.stream().map(item -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);

            // // 根据分类id 查询分类对象
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(list);

        return R.success(setmealDtoPage);
    }

    /**
     * TODO: 批量修改套餐状态
     *
     * @param status {@link Integer}
     * @param ids {@link String}
     * @return {@link R<String>}
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status, String ids) {
        log.info("根据id 批量修改套餐状态，ids={},status={}",ids,status);

        List<String> idList = Arrays.asList(ids.split(","));
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<Setmeal>()
                .in(Setmeal::getId, idList)
                // 如 我要停售 菜品 但是ids 对应的菜品已经有状态为停售的菜品 这些菜品就不需要再将他的状态变为停售了
                .eq(Setmeal::getStatus, status == 1? 0 : 1)
                .set(Setmeal::getStatus, status);
        boolean update = setMealService.update(updateWrapper);

        if (update) {
            return R.success("批量修改套餐状态成功");
        }

        return R.error("批量修改套餐状态失败");
    }

    /**
     * TODO: 批量删除套餐 并将关联的菜品关系删除【注意：不是删除菜品】
     *
     * @param ids {@link String}
     * @return {@link R<String>}
     */
    @DeleteMapping
    // public R<String> delete(String ids) {
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        log.info("根据id 批量删除套餐，ids={}",ids);

        boolean flag = setMealService.removeByIdsWithDish(ids);

        if (flag) {
            return R.success("批量删除套餐成功");
        }
        return R.error("批量删除套餐失败");
    }

    /**
     * TODO: 添加套餐 并添加关联的菜品关系
     *
     * @param setmealDto {@link SetmealDto}
     * @return {@link R<String>}
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("添加套餐 并添加关联的菜品关系，setmealDto={}",setmealDto);

        // 添加菜品的时候 注意 状态为停售的菜品，如果不筛选，可以直接选择不展示这些菜品
        setMealService.saveSetMealWithDish(setmealDto);

        return R.success("批量删除套餐成功");
    }

    /**
     * TODO: 根据id查询 套餐详细信息 以及关联的菜品关系
     *
     * @param id {@link Long}
     * @return {@link R<String>}
     */
    @GetMapping("{id}")
    public R<SetmealDto> getDetail(@PathVariable("id") Long id) {
        log.info("根据id查询 套餐详细信息 以及关联的菜品关系，id={}",id);

        SetmealDto setmealDto = setMealService.getDetailWithDish(id);

        if (setmealDto != null) {
            return R.success(setmealDto);
        }
        return R.error("没有查到该套餐");
    }

    /**
     * TODO: 更新 套餐详细信息 以及关联的菜品关系
     *
     * @param setmealDto {@link SetmealDto}
     * @return {@link R<String>}
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("更新 套餐详细信息 以及关联的菜品关系，setmealDto={}",setmealDto);

        boolean flag = setMealService.updateWithDish(setmealDto);

        if (flag) {
            return R.success("更新套餐成功");
        }
        return R.error("更新套餐失败");
    }

    /**
     * TODO: 根据条件（分类id）查询 已售套餐
     *
     * @param setmeal {@link Setmeal}
     * @return {@link R<List<Setmeal>>}
     */
    @GetMapping("list")
    // public R<List<Setmeal>> list(@RequestParam("categoryId") Long categoryId, @RequestParam("status") Integer status) {
    public R<List<Setmeal>> list(Setmeal setmeal) {
        Long categoryId = setmeal.getCategoryId();
        Integer status = setmeal.getStatus();
        log.info("根据条件（分类id）查询 已售套餐，categoryId={}，status={}",categoryId, status);

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(categoryId != null, Setmeal::getCategoryId, categoryId);
        queryWrapper.eq(status != null, Setmeal::getStatus, status);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setMealService.list(queryWrapper);

        if (list != null) {
            return R.success(list);
        }
        return R.error("没有查到套餐");
    }

    /**
     * TODO: 根据 套餐id 查询包含的菜品
     *
     * @param id {@link Long}
     * @return {@link R<SetmealDish>}
     */
    @GetMapping("/dish/{id}")
    public R<List<Dish>> list(@PathVariable("id") Long id) {
        log.info("根据 套餐id 查询包含的菜品，id={}",id);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setMealDishService.list(queryWrapper);     // 用他 没有图片，返回DTo也一样，但是似乎前端接收会有问题

        List<Dish> dishList = setmealDishList.stream().map(item ->{
            Long dishId = item.getDishId();

            LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Dish::getId, dishId);
            return dishService.getOne(wrapper);

        }).collect(Collectors.toList());      // 用他 用户视图 会有描述 但是无 份数

        return R.success(dishList);
    }
}
