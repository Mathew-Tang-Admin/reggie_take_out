package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("员工")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    //员工账号
    @ApiModelProperty("员工账号")
    private String username;

    //员工姓名
    @ApiModelProperty("员工姓名")
    private String name;

    //密码
    @ApiModelProperty("密码")
    private String password;

    //手机号
    @ApiModelProperty("手机号")
    private String phone;

    //性别
    @ApiModelProperty("性别")
    private String sex;

    //身份证号
    @ApiModelProperty("身份证号")
    private String idNumber;

    //状态
    @ApiModelProperty("状态")
    private Integer status;

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

}
