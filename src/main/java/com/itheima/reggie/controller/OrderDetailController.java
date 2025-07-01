package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.AddressBookService;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author MathewTang
 * @date 2025/06/28 21:37
 */
@Slf4j
@RequestMapping("/orderDetail")
@RestController
public class OrderDetailController {

    @Autowired
    private OrderDetailService orderDetailService;

    @PostMapping("/submit")
    public R<Orders> submit(@RequestBody Orders orders, HttpSession session) {
        log.info("提交订单，orders={}..", orders);

        return R.success(orders);
    }


    /**
     * TODO: 移动端 订单页面，分页查询
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @GetMapping("/userPage")
    public R<Page<Orders>> userPage(Integer page, Integer pageSize, HttpSession session) {
        log.info("移动端 订单页面，分页查询");
        return R.success(null);
    }
}
