package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * TODO:    (mathewtang add 改用SpringCache)
 * @author MathewTang
 * @date 2025/06/28 16:17
 */
@Slf4j
@RequestMapping("/shoppingCart")
@RestController
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO: 查看购物车
     *     这里/front/index.html L382、L241、L319的接受形式是数组，采用默认的json无法接受，因此修改了代码
     * @param session {@link HttpSession}
     * @return {@link R<List<ShoppingCart>>}
     */
    @Cacheable(value = "shoppingCartCache", key = "'cart_list_' + #session.getAttribute('user').toString()")
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(HttpSession session) {
        log.info("查看购物车...");     // cart_list::user:

        // 这里我发现了一个问题，如果先登录管理员端，然后再登录移动端，上下文存储的是管理员的id，这个时候，这里是查不到移动端用户的购物车数据

        Long userId = Long.parseLong(session.getAttribute("user").toString());

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        // queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        return R.success(list);
    }

    @CacheEvict(value = "shoppingCartCache", key = "'cart_list_' + #session.getAttribute('user').toString()", beforeInvocation = true)
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart, HttpSession session) {
        log.info("加入购物车，增加数量 shoppingCart={}...",shoppingCart);

        Long userId = Long.parseLong(session.getAttribute("user").toString());
        // 查询购物车里是否有该商品【老师是菜品 套餐分开查询、添加】
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(null != shoppingCart.getDishId(), ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(null != shoppingCart.getSetmealId(), ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        queryWrapper.eq(ShoppingCart::getUserId, userId);

        ShoppingCart cart = shoppingCartService.getOne(queryWrapper);
        // 第一次加入购物车
        if (null == cart) {
            shoppingCart.setUserId(userId);
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            boolean save = shoppingCartService.save(shoppingCart);
            return R.success(shoppingCart);
        }

        cart.setNumber(cart.getNumber() + 1);
        boolean updateById = shoppingCartService.updateById(cart);

        return R.success(cart);

    }

    @CacheEvict(value = "shoppingCartCache", key = "'cart_list_' + #session.getAttribute('user').toString()", beforeInvocation = true)
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart, HttpSession session) {
        Long userId = Long.parseLong(session.getAttribute("user").toString());
        log.info("购物车，减少数量 shoppingCart={} BaseContextUserId={}, sessionUserId={}...",shoppingCart, BaseContext.getCurrentId(), userId);


        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(null != shoppingCart.getSetmealId(), ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        queryWrapper.eq(null != shoppingCart.getDishId(), ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(ShoppingCart::getUserId, userId);

        ShoppingCart cart = shoppingCartService.getOne(queryWrapper);
        // 查询购物车里该商品 是否 > 1

        if (cart.getNumber() == 1) {
            shoppingCartService.remove(queryWrapper);
            return R.success(cart);
        }
        cart.setNumber(cart.getNumber() - 1);
        boolean updateById = shoppingCartService.updateById(cart);
        return R.success(cart);
    }

    @CacheEvict(value = "shoppingCartCache", key = "'cart_list_' + #session.getAttribute('user').toString()")
    @DeleteMapping("/clean")
    public R<String> clean(HttpSession session) {
        log.info("清空购物车 ...");
        Long userId = Long.parseLong(session.getAttribute("user").toString());

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);

        shoppingCartService.remove(queryWrapper);

        return R.success("清空成功");
    }
}
