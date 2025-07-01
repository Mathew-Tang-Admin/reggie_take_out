package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.OrderDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author MathewTang
 * @date 2025/06/28 16:16
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private UserService userService;

    /**
     * TODO: 用户下单
     *
     * @param orders {@link Orders}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Orders orders) {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();

        // 查询购物车数据, 计算应付价格
        LambdaQueryWrapper<ShoppingCart> cartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        cartLambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(cartLambdaQueryWrapper);

        if (null == shoppingCartList || shoppingCartList.size() == 0) {
            throw new CustomException("购物车为空，无法下单");
        }

        // 查询用户数据
        User user = userService.getById(userId);
        orders.setUserId(userId);
        orders.setUserName(user.getName()); // 用户名

        // 根据地址ID 查询地址信息
        AddressBook addressBook = addressBookService.getById(orders.getAddressBookId());

        if (null == addressBook) {
            throw new CustomException("用户地址信息有误，无法下单");
        }

        orders.setPhone(addressBook.getPhone()); // 电话
        orders.setConsignee(addressBook.getConsignee());  // 收货人
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));


        long orderId = IdWorker.getId();
        AtomicInteger amount = new AtomicInteger();

        List<OrderDetail> orderDetailList = shoppingCartList.stream().map(item -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        orders.setNumber(String.valueOf(orderId));
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(String.valueOf(amount.get())) );

        // 向订单表插入数据，一条数据
        boolean save = this.save(orders);
        // 向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetailList);

        // 清空购物车数据
        if (save) {
            LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
            shoppingCartService.remove(wrapper);
        }
    }

    /**
     * TODO: 用户下单
     *
     * @param id {@link Long}
     * @return {@link boolean}
     */
    @Override
    public boolean again(Long id) {

        // 从 OrderDetail中根据orderId，查询商品
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetails = orderDetailService.list(queryWrapper);
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map(item -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(item, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        // 将上面的商品加入购物车
        return shoppingCartService.saveBatch(shoppingCarts);
    }

    /**
     * TODO: 移动端 订单页面，分页查询，并查询订单中包含的 商品
     *
     * @param page {@link Integer}
     * @param pageSize {@link Integer}
     * @return {@link Page<OrderDto>}
     */
    @Override
    public Page<OrderDto> userPage(Integer page, Integer pageSize) {

        Page<Orders> orderpage = new Page<>(page, pageSize);
        Page<OrderDto> orderDtoPage = new Page<>(page, pageSize);
        BeanUtils.copyProperties(orderpage, orderDtoPage, "records");

        // 查询user_id下所有的订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Orders::getOrderTime).orderByDesc(Orders::getCheckoutTime);
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        this.page(orderpage, queryWrapper);

        List<Orders> orderPageRecords = orderpage.getRecords();
        // 根据订单id 查询出所有OrderDetail
        List<OrderDto> orderDtoPageRecords = orderPageRecords.stream().map(item -> {

            OrderDto orderDto = new OrderDto();
            BeanUtils.copyProperties(item, orderDto);

            // 获取订单id
            Long orderId = item.getId();
            // 根据orderId查询OrderDetail
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> detailList = orderDetailService.list(orderDetailLambdaQueryWrapper);

            orderDto.setOrderDetails(detailList);
            return orderDto;
        }).collect(Collectors.toList());

        orderDtoPage.setRecords(orderDtoPageRecords);

        return orderDtoPage;
    }
}
