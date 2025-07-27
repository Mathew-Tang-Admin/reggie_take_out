package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.common.RedisConstant;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author MathewTang
 * @date 2025/06/19 17:02
 */
@Slf4j
@RestController
@RequestMapping("/dish")
@Api(tags = "菜品接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    // private StringRedisTemplate redisTemplate;
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO: 菜品 分页查询（改用Redis缓存，mathewtang add）
     *
     * @param name {@link String}
     * @param page {@link Integer}
     * @param pageSize {@link Integer}
     * @return {@link R<Page<DishDto>>}
     */
    @GetMapping("/page")
    @ApiOperation(value = "菜品分页接口")
    /* @ApiImplicitParams({
            @ApiImplicitParam(name = "name",value = "菜品名称", required = false),
            @ApiImplicitParam(name = "page",value = "页码", required = true),
            @ApiImplicitParam(name = "pageSize",value = "每页记录数", required = true),
    }) */
    public R<Page<DishDto>> page(String name, Integer page, Integer pageSize) {
        log.info("菜品 分页查询 page={},pageSize={}",page, pageSize);

        // 从Redis缓存中获取 菜品分页 信息
        String key = RedisConstant.DISH_PAGE + "_" + page + "_" + pageSize;
        Object obj = redisTemplate.opsForValue().get(key);
        Page<DishDto> dishDtoPage = null;


        // 缓存中没有获取到数据，从数据库中获取
        if (null == obj) {
            // 构造分页构造器对象
            Page<Dish> dishPage = new Page<>(page, pageSize);
            dishDtoPage = new Page<>(page, pageSize);

            // 构造条件构造器
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            // 添加过滤条件
            queryWrapper.like(StringUtils.isNotEmpty(name),Dish::getName,name);
            // 添加排序条件
            queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

            // 执行分页查询
            dishService.page(dishPage, queryWrapper);

            // 对象拷贝【源数据，目标】
            BeanUtils.copyProperties(dishPage,dishDtoPage, "records");

            List<Dish> records = dishPage.getRecords();
            List<DishDto> list = records.stream().map(item -> {
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item,dishDto);

                Long categoryId = item.getCategoryId(); // 分类id
                // 根据分类id 查询分类对象
                Category category = categoryService.getById(categoryId);
                if (category != null) {
                    String categoryName = category.getName(); // 分类名
                    dishDto.setCategoryName(categoryName);
                }

                return dishDto;

            }).collect(Collectors.toList());

            dishDtoPage.setRecords(list);

            // 将菜品分页数据存入Redis缓存
            redisTemplate.opsForValue().set(key, dishDtoPage, 60, TimeUnit.MINUTES);
            return R.success(dishDtoPage);
        }

        // 缓存中有查到数据
        dishDtoPage = RedisUtil.convertObj(obj, Page.class);

        return R.success(dishDtoPage);
    }

    /**
     * TODO: 根据id查询 菜品详细信息 和对应 口味信息（改用Redis缓存）
     *
     * @param id {@link String} 菜品id
     * @return {@link R<DishDto>}
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询菜品详情接口")
    /* @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "菜品id", required = true),
    }) */
    public R<DishDto> getDetail(@PathVariable("id") Long id) {
        log.info("根据id 查询菜品详细信息id={}",id);

        DishDto dishDto = dishService.getDetailByIdWithFlavor(id);
        if (dishDto != null) {
            return R.success(dishDto);
        }
        return R.error("没有查询到对应菜品信息");
    }

    /**
     * TODO: 批量修改菜品状态（改用Redis缓存，mathewtang add）
     *
     * @param status {@link Integer}
     * @param ids {@link String}
     * @return {@link R<String>}
     */
    @PostMapping("/status/{status}")
    @ApiOperation(value = "修改菜品状态接口")
    /* @ApiImplicitParams({
            @ApiImplicitParam(name = "status",value = "状态", required = true),
            @ApiImplicitParam(name = "ids",value = "菜品id，逗号分割", required = true),
    }) */
    public R<String> status(@PathVariable("status") Integer status, String ids) {
        log.info("根据id 批量修改菜品状态，ids={},status={}",ids,status);

        /* String[] idsStr = ids.split(",");
        Collection<Dish> dishList = new ArrayList<>();

        for (String id : idsStr) {
            Dish dish = new Dish();
            dish.setId(Long.valueOf(id));
            dish.setStatus(status);
            dishList.add(dish);
        } */

        /* Collection<Dish> dishList = Arrays.stream(ids.split(","))
            .map(idStr -> {
                Dish dish = new Dish();
                dish.setId(Long.valueOf(idStr));
                dish.setStatus(status);
                return dish;
            })
            .collect(Collectors.toList());

        dishService.updateBatchById(dishList); */


        List<String> idsList = Arrays.asList(ids.split(","));
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<Dish>()
                                .in(Dish::getId, idsList)
                                // 如 我要停售 菜品 但是ids 对应的菜品已经有状态为停售的菜品 这些菜品就不需要再将他的状态变为停售了
                                .eq(Dish::getStatus, status == 1? 0 : 1)
                                .set(Dish::getStatus, status);
        dishService.update(updateWrapper);

        // 删除Redis缓存中旧的数据
        for (String id : idsList) {
            String key = RedisConstant.DISH_DETAIL + "_" + id;
            Object obj = redisTemplate.opsForValue().get(key);
            if (null != obj) {
                redisTemplate.delete(key);
            }
        }

        return R.success("批量修改菜品状态成功");
    }

    /**
     * TODO: 批量删除菜品 以及菜品口味（改用Redis缓存，mathewtang add）
     *
     * @param ids {@link String}
     * @return {@link R<String>}
     */
    // public R<String> delete(String ids) {
    @DeleteMapping
    @ApiOperation(value = "删除菜品接口")
    /* @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "菜品id，逗号分割", required = true),
    }) */
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        log.info("根据id 批量删除菜品，ids={}",ids);

        boolean flag = dishService.removeByIdsWithFlavor(ids);

        return R.success("批量删除菜品成功");
    }

    /**
     * TODO: 添加菜品（改用Redis缓存）
     *
     * @param dishDto {@link DishDto}
     * @return {@link R<String>}
     */
    @PostMapping
    @ApiOperation(value = "新增菜品接口")
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("添加菜品, dishDto={}",dishDto);

        dishService.saveWithFlavor(dishDto);

        // 删除缓存中旧的数据
        String keys = "dish_" + + dishDto.getCategoryId() + "_1";
        // String keys = redisTemplate.keys("dish_*").toString();
        redisTemplate.delete(keys);

        return R.success("添加菜品成功");
    }

    /**
     * TODO: 更新菜品（改用Redis缓存）
     *
     * @param dishDto {@link DishDto}
     * @return {@link R<String>}
     */
    @PutMapping
    @ApiOperation(value = "修改菜品接口")
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info("修改菜品, dishDto={}",dishDto);

        dishService.updateWithFlavor(dishDto);

        // 删除缓存中旧的数据
        String keys = "dish_" + + dishDto.getCategoryId() + "_1";
        // String keys = redisTemplate.keys("dish_*").toString();
        redisTemplate.delete(keys);

        return R.success("修改菜品成功");
    }

    /**
     * TODO: 根据条件（分类id） 查询菜品
     *
     * @param dish {@link Dish}
     * @return {@link R<List<Dish>>}
     */
    /* @GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {
        log.info("根据条件（分类id） 查询菜品, categoryId={}",dish.getCategoryId());

        // 添加条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件【仅查询 在售 的】
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus, 1);
        // 添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        // 执行查询，查询符合条件的菜品
        List<Dish> dishList = dishService.list(queryWrapper);

        return R.success(dishList);
    } */

    /**
     * TODO: 根据条件（分类id） 查询菜品（改用Redis缓存）
     *
     * @param dish {@link Dish}
     * @return {@link R<List<DishDto>>}
     */
    @GetMapping("/list")
    @ApiOperation(value = "根据分类id查询菜品接口")
    public R<List<DishDto>> list(Dish dish) {
        log.info("根据条件（分类id） 查询菜品, categoryId={}",dish.getCategoryId());

        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        List<DishDto> dishDtos = null;

        // 根据条件（分类id）从Redis缓存中获取菜品信息
        // List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        Object list = redisTemplate.opsForValue().get(key);

        // Redis缓存中没有查到菜品信息，查询数据库，将查询到的菜品缓存到Redis
        if (null == list) {
            // 添加条件构造器
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            // 添加过滤条件【仅查询 在售 的】
            queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
            queryWrapper.eq(Dish::getStatus, 1);
            // 添加排序条件
            queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
            // 执行查询，查询符合条件的菜品
            List<Dish> dishList = dishService.list(queryWrapper);

            // List<DishDto> dishDtos = dishList.stream().map(item -> {
            dishDtos = dishList.stream().map(item -> {
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item, dishDto);

                // 根据分类id 查询分类对象【我觉得这个应该不用查吧，毕竟分类名 在移动端是先查询出来的，到这里查询该分类下的所有菜品，分类就一定存在了吧】
                Category category = categoryService.getById(item.getCategoryId());
                if (null != category) {
                    String categoryName = category.getName();
                    dishDto.setCategoryName(categoryName);
                }

                // 添加条件构造器
                LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<>();
                // 添加过滤条件
                wrapper.eq(DishFlavor::getDishId, dishDto.getId());
                // 根据id 查询菜品口味信息
                List<DishFlavor> dishFlavors = dishFlavorService.list(wrapper);
                dishDto.setFlavors(dishFlavors);

                // 将菜品口味信息存入Redis缓存
                // redisTemplate.opsForList().rightPush(key, dishDto);

                return dishDto;
            }).collect(Collectors.toList());

            // 将菜品信息存入Redis缓存
            redisTemplate.opsForValue().set(key, dishDtos, 60, TimeUnit.MINUTES);
            return R.success(dishDtos);
        }

        // Redis缓存中有查到菜品信息，直接返回
        dishDtos = (List<DishDto>) list;
        return R.success(dishDtos);
    }
}
