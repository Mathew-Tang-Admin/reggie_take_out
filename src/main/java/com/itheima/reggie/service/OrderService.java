package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.OrderDto;
import com.itheima.reggie.entity.Orders;

/**
 * @author MathewTang
 * @date 2025/06/28 16:15
 */
public interface OrderService extends IService<Orders> {

    /**
     * TODO: 提交订单
     *
     * @param orders {@link Orders}
     */
    void  submit(Orders orders);

    /**
     * TODO: 再来一单
     *
     * @param id {@link Long}
     * @return {@link boolean}
     */
    boolean again(Long id);

    /**
     * TODO: 移动端 订单页面，分页查询，并查询订单中包含的 商品
     *
     * @param page {@link Integer}
     * @param pageSize {@link Integer}
     * @return {@link Page< Orders>}
     */
    Page<OrderDto> userPage(Integer page, Integer pageSize);
}
