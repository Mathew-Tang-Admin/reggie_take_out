package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.RedisConstant;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    // private StringRedisTemplate redisTemplate;
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO: 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor（改用Redis缓存，mathewtang add）
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

        // 将菜品信息保存到Redis缓存中
        dishDto.setFlavors(flavors);
        redisTemplate.opsForValue().set(RedisConstant.DISH_DETAIL + "_" + dishId, dishDto, 60, TimeUnit.MINUTES);
    }

    /**
     * TODO: 根据id 查询菜品详细信息 以及 菜品口味信息（改用Redis缓存，mathewtang add）
     *
     * @param id {@link Long} 菜品id
     * @return {@link DishDto}
     */
    @Override
    public DishDto getDetailByIdWithFlavor(Long id) {

        // 根据id从Redis缓存中获取
        String key = RedisConstant.DISH_DETAIL + "_" + id;
        Object obj = redisTemplate.opsForValue().get(key);

        // Redis缓存中没有查到数据，从数据库中查询
        DishDto dishDto = null;
        if (null == obj) {
            // 查询菜品基本信息
            Dish dish = this.getById(id);

            dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto); // 【源数据，目标】

            // 添加条件构造器
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            // 添加一个过滤条件
            queryWrapper.eq(DishFlavor::getDishId,id);

            // 根据菜品id 从dish_flavor查询菜品口味信息
            List<DishFlavor> list = dishFlavorService.list(queryWrapper);
            dishDto.setFlavors(list);

            // 将菜品详细信息写入Redis缓存
            redisTemplate.opsForValue().set(key, dishDto, 60, TimeUnit.MINUTES);

            return dishDto;
        }

        // Redis缓存中有查到数据
        // return convertToDto(obj, DishDto.class);
        return RedisUtil.convertObj(obj, DishDto.class);
    }

    /* private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    private static <T> T convertToDto(Object obj, Class<T> targetType) {
        return mapper.convertValue(obj, targetType);
    } */

    /**
     * TODO: 修改菜品详细信息 以及 菜品口味信息（改用Redis缓存，mathewtang add）
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
        // boolean saveDishFlavor = dishFlavorService.saveBatch(dishDto.getFlavors());  // 咋感觉用这个，上面那一串代码就没用了嘞
        boolean saveDishFlavor = dishFlavorService.saveBatch(flavors); // 后改

        // 将菜品信息保存到Redis缓存中
        dishDto.setFlavors(flavors);
        String key = RedisConstant.DISH_DETAIL + "_" + dishId;
        // 删除旧的dish_detail
        redisTemplate.delete(key);
        // 更新dish_detail
        redisTemplate.opsForValue().set(key, dishDto, 60, TimeUnit.MINUTES);

    }

    /**
     * TODO: 批量删除菜品 以及菜品口味（改用Redis缓存，mathewtang add）
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

        // 删除缓存中分类商品，以及菜品详情【不能这么做，能执行到这里，一定list.size() == 0】
        /* for (Dish dish : dishList) {
            // 删除菜品分类
            String categoryKey = "dish_list_" + dish.getCategoryId() + "_1";
            Object o = redisTemplate.opsForValue().get(categoryKey);
            if (null != o) {
                redisTemplate.delete(categoryKey);
            }
            // 删除菜品详情
            String dishKey = RedisConstant.DISH_DETAIL + "_" + dish.getId();
            redisTemplate.delete(dishKey);
        } */


        // 删除page缓存
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, RedisConstant.DISH_PAGE);
        // 删除list缓存
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "dish_list");
        for (Long id : ids) {
            String dishKey = RedisConstant.DISH_DETAIL + "_" + id;
            redisTemplate.delete(dishKey);
        }

        return removeDish && removeFlavor;

    }
}
