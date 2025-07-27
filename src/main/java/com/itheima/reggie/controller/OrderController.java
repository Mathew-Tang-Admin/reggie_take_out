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
import com.itheima.reggie.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
@Api(tags = "订单接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private CacheManager cacheManager;
    // @Resource(name = "objRedisTemplate")
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO: 用户下单      (mathewtang 改用SpringCache)
     *     删除购物车缓存数据、删除userPage数据、删除page数据
     * @param orders {@link Orders}
     * @param session {@link HttpSession}
     * @return {@link R<Orders>}
     */
    @CacheEvict(value = "shoppingCartCache", key = "'cart_list_' + #session.getAttribute('user').toString()")
    @PostMapping("/submit")
    @ApiOperation(value = "提交订单接口")
    public R<String> submit(@RequestBody Orders orders, HttpSession session) {
        log.info("提交订单，orders={}..", orders);

        orderService.submit(orders);

        String userId = session.getAttribute("user").toString();
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "ordersCache::page");
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "ordersCache::userPage_user_" + userId);

        return R.success("下单成功");
    }


    /**
     * TODO: 订单分页查询      (mathewtang 改用SpringCache)
     *
     * @param session {@link HttpSession}
     * @return {@link R<Page<Orders>>}
     */
    @Cacheable(value = "ordersCache", key = "'page_' + #page + '_' + #pageSize + '_number_' + #number + '_beginTime_' + #beginTime + '_endTime_' + #endTime")
    @GetMapping("/page")
    @ApiOperation(value = "订单分页接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "页码", required = true),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true),
            @ApiImplicitParam(name = "number", value = "订单号", required = false),
            @ApiImplicitParam(name = "beginTime", value = "开始时间", required = false),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false),
    })
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
     *       (mathewtang 改用SpringCache)
     * @param session {@link HttpSession}
     * @return {@link R<Page<OrderDto>>}
     */
    @Cacheable(value = "ordersCache", key = "'userPage_user_' + #session.getAttribute('user').toString() + '_' + #page + '_' + #pageSize")
    @GetMapping("/userPage")
    @ApiOperation(value = "移动端订单分页接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page",value = "页码", required = true),
            @ApiImplicitParam(name = "pageSize",value = "每页记录数", required = true),
    })
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
    @ApiOperation(value = "再来一单接口")
    public R<String> again(@RequestBody Orders orders, HttpSession session) {
        log.info("移动端 订单页面, 再来一单, id={}", orders.getId());

        boolean flag = orderService.again(orders.getId());

        return R.success("再来一单成功");
    }


    /**
     * TODO: 管理端 修改订单状态      (mathewtang 改用SpringCache)
     *     删除page分页缓存、删除userPage分页缓存
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @PutMapping
    @ApiOperation(value = "更新订单状态接口")
    public R<String> updateStatus(@RequestBody Orders orders, HttpSession session) {
        log.info("管理端 修改订单状态, id={}", orders.getId());

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getId, orders.getId());

        boolean update = orderService.updateById(orders);

        // 删除page分页缓存
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "ordersCache::page");
        // 删除userPage分页缓存
        String userId = session.getAttribute("user").toString();
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "ordersCache::userPage_user_" + userId);

        return R.success("管理端 修改订单状态成功");
    }
}
