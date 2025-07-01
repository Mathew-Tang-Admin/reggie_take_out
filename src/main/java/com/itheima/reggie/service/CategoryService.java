package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.Category;

import java.util.List;

/**
 * MY NOTES：
 * @author MathewTang
 */
public interface CategoryService extends IService<Category> {
    /**
     * TODO: 根据id删除分类，删除前进行判断
     *
     * @param id {@link Long}
     */
    void remove(Long id) throws CustomException;

    /**
     * TODO: 根据条件（分类类型） 查询分类数据
     *
     * @param type {@link Integer}
     * @return {@link List< Category>}
     */
    List<Category> getByType(Integer type);
}
