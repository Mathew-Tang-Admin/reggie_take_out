package com.itheima.reggie.dto;

import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: 由唐轩明添加
 *
 * @author Administrator
 */
@Data
public class OrderDto extends Orders {

    private List<OrderDetail> orderDetails = new ArrayList<>();

    /** 商品总量 */
    private Integer sumNum;

}

    // Generating equals/hashCode implementation but without a call to superclass, even though this class does not extend java.lang.Object. If this is intentional, add '(callSuper=false)' to your type.
    //     Inspection info: Offers general inspections for Lombok annotations.