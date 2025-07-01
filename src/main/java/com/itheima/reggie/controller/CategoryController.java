package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetMealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author MathewTang
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SetMealService setMealService;
    @Autowired
    private DishService dishService;


    /**
     * TODO: 添加分类
     *     1:新增菜品, 2:套餐分类
     *
     * @param request  {@link HttpServletRequest}
     * @param category {@link Category}
     * @return {@link R<String>}
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Category category) {
        log.info("新增分类，分类信息{}", category.toString());

        // 分类名name唯一约束

        // category中增加了一个isDelete字段，如果这个分类已经删除了 再增加的时候 又提示已存在  不是感觉有问题
        // 是不是 也得 查 如果已存在 看 isDelete字段 1的话 就......
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();

        // sort约束【mathewTang添加】
        /* wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getSort,category.getSort());
        Category cate = categoryService.getOne(wrapper);
        if (cate != null && cate.getIsDeleted() == 0) {
            return R.error("sort已存在");
        } */


        Long empId = (Long) request.getSession().getAttribute("employee");
        // category.setCreateTime(LocalDateTime.now());
        // category.setCreateUser(empId);
        // category.setUpdateTime(LocalDateTime.now());
        // category.setUpdateUser(empId);
        // log.info("新增分类，分类信息{}", category.toString());
        boolean flag = categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * TODO: 分类信息分页查询
     *
     * @param page     {@link Integer}
     * @param pageSize {@link Integer}
     * @return {@link R<Page>}
     */
    @GetMapping("/page")
    public R<Page<Category>> page(Integer page, Integer pageSize) {
        log.info("page = {},pageSize = {}", page, pageSize);

        // 构造分页构造器
        Page<Category> pageInfo = new Page<>(page, pageSize);
        // 构造条件构造器
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        // 添加一个排序条件，根据sort进行排序
        // 【mathewTang添加 sort相同，根据UpdateTime降序排序，sort理论上不应该相同的吧，但是没看到有sort的约束
        // 其实我想按照type、sort、updateTime排序】
        wrapper.orderByAsc(Category::getSort);
        wrapper.orderByDesc(Category::getUpdateTime);
        // 执行查询
        categoryService.page(pageInfo, wrapper);

        return R.success(pageInfo);
    }

    /**
     * TODO: 根据id修改分类信息
     *
     * @return {@link R<String>}
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Category category) {
        log.info(" 员工信息修改 category:{}", category);

        // 这里注意 公共字段 的更新

        categoryService.updateById(category);
        return R.success("分类信息修改成功");
    }

    /**
     * TODO: 根据id删除分类
     *
     * @param id {@link Long}
     * @return {@link R<String>}
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("id") Long id) {
        log.info("准备删除分类，id为：{}...", id);

        /* // V1
        // 根据id查询该分类下是否还有商品（dish）或套餐（setmeal）
        // 根据id 获取 是菜品 或套餐
        Map<String, Object> map = new HashMap<>();
        LambdaQueryWrapper<Category> cateWrapper = new LambdaQueryWrapper<>();
        cateWrapper.eq(Category::getId, id);
        Category category = categoryService.getOne(cateWrapper);
        // 是菜品
        if (category.getType() == 1) {
            LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
            dishWrapper.eq(Dish::getCategoryId, id);
            map = dishService.getMap(dishWrapper);
        // 是套餐
        } else if (category.getType() == 2) {
            LambdaQueryWrapper<Setmeal> setMealWrapper = new LambdaQueryWrapper<>();
            setMealWrapper.eq(Setmeal::getCategoryId, id);
            map = setMealService.getMap(setMealWrapper);
        }

        if (map == null) {
            boolean flag = categoryService.removeById(id);
            return R.success("分类信息删除成功");
        }
        return R.error("分类信息删除失败"); */

        /* // V2
         try {
            categoryService.remove(id);
            return R.success("分类信息删除成功");
        } catch (CustomException e) {
            return R.error(e.getMessage());
        } */

        // 改用全局异常处理
        categoryService.remove(id);
        return R.success("分类信息删除成功");

    }



    /**
     * TODO: 根据条件（分类类型） 查询分类数据
     *
     * @return {@link R<String>}
     */
    @GetMapping("/list")
    public R<List<Category>> list(HttpServletRequest request, Category category) {
        log.info(" 根据分类类型查询分类 type:{}", category.getType());

        // List<Category> categoryList = categoryService.getByType(category.getType());

        // 添加条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 添加条件
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType());
        // 添加排序条件
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> categoryList = categoryService.list(queryWrapper);
        return R.success(categoryList);
    }
}
