package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetMealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author MathewTang
 * @date 2025/06/18 2:21
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {


    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SetMealService setMealService;
    @Autowired
    private DishService dishService;

    /**
     * TODO: 根据id删除分类，删除前进行判断
     *
     * @param id {@link Long}
     */
    @Override
    public void remove(Long id) throws CustomException

    {
        // 根据id 获取 是菜品 或套餐 [老师没有先查询type]
        int list = 0;

        LambdaQueryWrapper<Category> cateWrapper = new LambdaQueryWrapper<>();
        cateWrapper.eq(Category::getId, id);
        Category category = categoryMapper.selectOne(cateWrapper);

        // 根据id查询该分类下是否关联了菜品（dish），如果已经关联，抛出一个业务异常
        if (category.getType() == 1) {
            LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
            dishWrapper.eq(Dish::getCategoryId, id);
            list = dishService.count(dishWrapper);
        // 根据id查询该分类下是否关联了套餐（setmeal），如果已经关联，抛出一个业务异常
        } else if (category.getType() == 2) {
            LambdaQueryWrapper<Setmeal> setMealWrapper = new LambdaQueryWrapper<>();
            setMealWrapper.eq(Setmeal::getCategoryId, id);
            list = setMealService.count(setMealWrapper);
        }

        // 已经关联 抛出业务异常
        if (list > 0) {
            throw new CustomException("当前分类下已经关联" + (category.getType() == 1 ? "菜品" : "套餐") + "不能删除");
        }

        // int count = categoryMapper.deleteById(id);
        boolean count = super.removeById(id);

    }

    @Override
    public List<Category> getByType(Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getType, type);
        return categoryMapper.selectList(queryWrapper);
    }
}