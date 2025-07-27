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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐 控制层
 * @author MathewTang
 * @date 2025/06/19 17:02
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
@Api(tags = "套餐相关接口")
public class SetMealController {

    @Autowired
    private SetMealService setMealService;

    @Autowired
    private SetMealDishService setMealDishService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO: mathewtang add
     *
     * @param page {@link Integer}
     * @param pageSize {@link Integer}
     * @param name {@link String}
     * @return {@link R<Page<SetmealDto>>}
     */
    @Cacheable(value = "setMealCache", key = "'page_' + #page + '_' + #pageSize + '_' + #name")
    @GetMapping("/page")
    @ApiOperation(value = "套餐分页接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page",value = "页码", required = true),
            @ApiImplicitParam(name = "pageSize",value = "每页记录数", required = true),
            @ApiImplicitParam(name = "name",value = "套餐名称", required = false),
    })
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
     * TODO: 批量修改套餐状态      (mathewtang add)
     *     list缓存、page缓存、套餐缓存删除
     * @param status {@link Integer}
     * @param ids {@link String}
     * @return {@link R<String>}
     */
    // public R<String> status(@PathVariable("status") Integer status, String ids) {
    @PostMapping("/status/{status}")
    @ApiOperation(value = "修改套餐状态接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "status",value = "状态", paramType = "path", required = true),
            @ApiImplicitParam(name = "ids",value = "套餐id，逗号分割", required = true),
    })
    public R<String> status(@PathVariable("status") Integer status, @RequestParam("ids") List<Long> ids) {
        log.info("根据id 批量修改套餐状态，ids={},status={}",ids,status);

        // List<String> idList = Arrays.asList(ids.split(","));
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<Setmeal>()
                // .in(Setmeal::getId, idList)
                .in(Setmeal::getId, ids)
                // 如 我要停售 菜品 但是ids 对应的菜品已经有状态为停售的菜品 这些菜品就不需要再将他的状态变为停售了
                .eq(Setmeal::getStatus, status == 1? 0 : 1)
                .set(Setmeal::getStatus, status);
        boolean update = setMealService.update(updateWrapper);

        if (update) {

            String prefix= "setMealCache::";
            Cache cache = cacheManager.getCache("setMealCache");
            if (null != cache) {
                // 删除套餐缓存
                for (Long id : ids) {
                    cache.evict(id);
                }
                // 删除list缓存
                Set<Object> listKeys = redisTemplate.keys(prefix + "list*");
                if (listKeys != null) {
                    for (Object listKey : listKeys) {
                        redisTemplate.delete(listKey);
                    }
                }
                // 删除page
                Set<Object> pageKeys = redisTemplate.keys(prefix + "page*");
                for (Object pageKey : pageKeys) {
                    redisTemplate.delete(pageKey);
                }

            }


            return R.success("批量修改套餐状态成功");
        }

        return R.error("批量修改套餐状态失败");
    }

    /**
     * TODO: 批量删除套餐 并将关联的菜品关系删除【注意：不是删除菜品】
     *     删除 分页缓存 和 list缓存 以及 套餐缓存【因为SpringCache不支持模糊匹配，只好手动删除了】
     * @param ids {@link String}
     * @return {@link R<String>}
     */
    // @CacheEvict(value = "setMealCache", allEntries = true)   // 老师是这么实现的
    // public R<String> delete(String ids) {
    @DeleteMapping
    @ApiOperation(value = "删除套餐接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids",value = "套餐id，逗号分割", required = true),
    })
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        log.info("根据id 批量删除套餐，ids={}",ids);

        boolean flag = setMealService.removeByIdsWithDish(ids);

        if (flag) {

            String prefix= "setMealCache::";
            Cache cache = cacheManager.getCache("setMealCache");
            if (cache != null) {
                // 删除套餐缓存
                for (Long id : ids) {
                    cache.evict(id);
                }
                Set<Object> listSet = redisTemplate.keys(prefix + "list*");
                // 删除list缓存
                if (listSet != null) {
                    listSet.forEach(list -> {
                        redisTemplate.delete(list);
                    });
                }
                Set<Object> pageKeys = redisTemplate.keys(prefix + "page*");
                // 删除分页缓存
                if (pageKeys != null) {
                    for (Object pageKey : pageKeys) {
                        redisTemplate.delete(pageKey);
                    }
                }
            }

            return R.success("批量删除套餐成功");
        }
        return R.error("批量删除套餐失败");
    }

    /**
     * TODO: 添加套餐 并添加关联的菜品关系
     *     将管理端分页查询缓存删除   移动端list缓存删除
     *     这里要注意的是 key 不支持 模糊匹配
     *     如果想要实现也可以选择 注解 + redisTemplate组合的方式
     *
     * TODO:
     *     我这里想尝试redisTemplate删除旧的缓存，注解插入新的缓存，通过资料查询，所得，完全可以实现，不用担心执行顺序的问题，他会先执行方法体的代码，再执行注解的代码
     * @param setmealDto {@link SetmealDto}
     * @return {@link R<String>}
     */
    @Caching(
            // evict = {@CacheEvict(value = "setMealCache", allEntries = true)},   // 老师是这么实现的
            evict = { // 即使又两条，也可以加上
                    // @CacheEvict(value = "setMealCache", key = "'list_' + #setmealDto.categoryId + '_1'", beforeInvocation = true),
                    @CacheEvict(value = "setMealCache", key = "'list_' + #setmealDto.categoryId + '_1'", beforeInvocation = true),
            }
    )
    @PostMapping
    @ApiOperation(value = "新增套餐接口")
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("添加套餐 并添加关联的菜品关系，setmealDto={}",setmealDto);

        // 添加菜品的时候 注意 状态为停售的菜品，如果不筛选，可以直接选择不展示这些菜品
        setMealService.saveSetMealWithDish(setmealDto);

        // 删除分页缓存
        String prefix= "setMealCache::";
        Set<Object> pageKeys = redisTemplate.keys(prefix + "page*");
        if (pageKeys != null) {
            pageKeys.forEach(pageKey -> {
                redisTemplate.delete(pageKey);
            });
        }

        // 将套餐数据加入缓存
        redisTemplate.opsForValue().set(prefix + setmealDto.getId(), setmealDto, 60, TimeUnit.MINUTES);

        return R.success("添加套餐成功");
    }

    /**
     * TODO: 根据id查询 套餐详细信息 以及关联的菜品关系     (mathewtang add)
     * )
     *
     * @param id {@link Long}
     * @return {@link R<String>}
     */
    @Cacheable(value = "setMealCache", key = "#id")
    @GetMapping("{id}")
    @ApiOperation(value = "获取套餐详情接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "套餐id", required = true),
    })
    public R<SetmealDto> getDetail(@PathVariable("id") Long id) {
        log.info("根据id查询 套餐详细信息 以及关联的菜品关系，id={}",id);

        SetmealDto setmealDto = setMealService.getDetailWithDish(id);

        if (setmealDto != null) {
            return R.success(setmealDto);
        }
        return R.error("没有查到该套餐");
    }

    /**
     * TODO: 更新 套餐详细信息 以及关联的菜品关系         (mathewtang add)
     *     ${@code beforeInvocation = true} 将注解的执行时机修改为方法执行前
     *     list缓存、page分页缓存、套餐缓存 删除  插入新的缓存
     * @param setmealDto {@link SetmealDto}
     * @return {@link R<String>}
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "setMealCache", key = "#setmealDto.id", beforeInvocation = true),
                    @CacheEvict(value = "setMealCache", key = "'list_' + #setmealDto.categoryId + '_1'"),
            } // , beforeInvocation = true
    )
    @PutMapping
    @ApiOperation(value = "更新套餐接口")
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("更新 套餐详细信息 以及关联的菜品关系，setmealDto={}",setmealDto);

        boolean flag = setMealService.updateWithDish(setmealDto);

        if (flag) {

            String prefix= "setMealCache::";
            // 删除分页缓存
            Set<Object> pageKeys = redisTemplate.keys(prefix + "page*");
            for (Object pageKey : pageKeys) {
                redisTemplate.delete(pageKey);
            }
            // 插入新的套餐缓存
            redisTemplate.opsForValue().set(prefix + setmealDto.getId(), setmealDto, 60, TimeUnit.MINUTES);

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
    @Cacheable(value = "setMealCache", key = "'list_'+ #setmeal.categoryId + '_' + #setmeal.status")
    @GetMapping("list")
    @ApiOperation(value = "根据条件（分类id）查询 已售套餐接口")
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
     * TODO: 根据 套餐id 查询包含的菜品       (mathewtang add)
     *
     * @param id {@link Long}
     * @return {@link R<SetmealDish>}
     */
    @Cacheable(value = "setMealCache", key = "#id + '_contain_dish'")
    @GetMapping("/dish/{id}")
    @ApiOperation(value = "根据 套餐id 查询包含的菜品接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "套餐id", required = true),
    })
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
