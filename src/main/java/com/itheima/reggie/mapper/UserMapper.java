package com.itheima.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author MathewTang
 * @date 2025/06/27 1:22
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
