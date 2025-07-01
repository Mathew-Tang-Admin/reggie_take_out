package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author MathewTang
 * @date 2025/06/28 16:17
 */
@Slf4j
@RequestMapping("/shoppingCart")
@RestController
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * TODO: 查看购物车
     *
     * @param session {@link HttpSession}
     * @return {@link R<List<ShoppingCart>>}
     */

    @GetMapping("/list")
    public R<List<ShoppingCart>> list(HttpSession session) {
        log.info("查看购物车...");

        Long userId = Long.parseLong(session.getAttribute("user").toString());

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        return R.success(list);
    }

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
