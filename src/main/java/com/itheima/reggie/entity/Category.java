package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.annotations.Delete;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类
 */
@Data
@ApiModel("分类")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;


    //类型 1 菜品分类 2 套餐分类
    @ApiModelProperty("类型 1 菜品分类 2 套餐分类")
    private Integer type;


    //分类名称
    @ApiModelProperty("分类名称")
    private String name;


    //顺序
    @ApiModelProperty("顺序")
    private Integer sort;


    //创建时间
    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    //更新时间
    @ApiModelProperty("更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    //创建人
    @ApiModelProperty("创建人")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;


    //修改人
    @ApiModelProperty("修改人")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;


    /**
     * {@code @TableField(exist = false)} MathewTang添加 初始版本 数据库没有这个字段
     * {@code @TableLogic} 逻辑删除注解
     * 是否删除 */
    // @TableField(exist = false)
    @ApiModelProperty("是否删除")
    @TableLogic // 暂时无
    private Integer isDeleted;

}
