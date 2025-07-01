package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrderDto;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.AddressBookService;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MathewTang
 * @date 2025/06/28 21:37
 */
@Slf4j
@RequestMapping("/order")
@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * TODO: 用户下单
     *
     * @param orders {@link Orders}
     * @param session {@link HttpSession}
     * @return {@link R<Orders>}
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders, HttpSession session) {
        log.info("提交订单，orders={}..", orders);

        orderService.submit(orders);

        return R.success("下单成功");
    }


    /**
     * TODO: 订单分页查询
     *
     * @param session {@link HttpSession}
     * @return {@link R<Page<Orders>>}
     */
    @GetMapping("/page")
    public R<Page<Orders>> page(Integer page, Integer pageSize,
                                @RequestParam(value = "number", required = false) Long number,
                                @RequestParam(value = "beginTime", required = false) String beginTime,
                                @RequestParam(value = "endTime", required = false) String endTime, HttpSession session) {
        log.info("订单分页查询,number={},beginTime={},endTime={}",number, beginTime, endTime);

        Page<Orders> orderpage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Orders::getOrderTime);
        queryWrapper.like(null != number, Orders::getNumber, number);
        queryWrapper.gt(null != beginTime, Orders::getOrderTime, beginTime).lt(null != endTime, Orders::getOrderTime, endTime);
        orderService.page(orderpage, queryWrapper);

        return R.success(orderpage);
    }


    /**
     * TODO: 移动端 订单页面，分页查询，并查询订单中包含的 商品
     *
     * @param session {@link HttpSession}
     * @return {@link R<Page<OrderDto>>}
     */
    @GetMapping("/userPage")
    public R<Page<OrderDto>> userPage(Integer page, Integer pageSize, HttpSession session) {
        log.info("移动端 订单页面，分页查询");

        Page<OrderDto> orderDtoPage = orderService.userPage(page, pageSize);

        return R.success(orderDtoPage);  // 显示OrderDetail
    }


    /**
     * TODO: 移动端 订单页面, 再来一单
     *
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders, HttpSession session) {
        log.info("移动端 订单页面, 再来一单, id={}", orders.getId());

        boolean flag = orderService.again(orders.getId());

        return R.success("再来一单成功");
    }


    /**
     * TODO: 管理端 修改订单状态
     *
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @PutMapping
    public R<String> updateStatus(@RequestBody Orders orders, HttpSession session) {
        log.info("管理端 修改订单状态, id={}", orders.getId());

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getId, orders.getId());

        boolean update = orderService.updateById(orders);

        return R.success("管理端 修改订单状态成功");
    }
}
