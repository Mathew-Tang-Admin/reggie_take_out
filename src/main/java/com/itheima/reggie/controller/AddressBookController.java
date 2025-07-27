package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.AddressBookService;
import com.itheima.reggie.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.lang.model.element.Element;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author MathewTang
 * @date 2025/06/28 21:37
 */
@Slf4j
@RequestMapping("/addressBook")
@RestController
@Api(tags = "地址簿接口")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private CacheManager cacheManager;
    // @Autowired
    @Resource(name = "objRedisTemplate")
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO:
     *     删除list缓存、增加地址缓存
     * @param addressBook {@link AddressBook}
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @CachePut(value = "addressBookCache",key = "'detail_' + #addressBook.id")
    @PostMapping
    @ApiOperation(value = "新增地址接口")
    public R<AddressBook> save(@RequestBody AddressBook addressBook, HttpSession session) {
        log.info("AddressBook.save AddressBook={} ...",addressBook);
        // addressBook.setUserId(Long.parseLong(session.getAttribute("user").toString()));
        addressBook.setUserId(BaseContext.getCurrentId());    // MY NOTES: 估计也是一个隐藏的问题...

        // MY NOTES: 还有关于默认地址的问题
        // 查询 是否存在默认地址
        R<AddressBook> aDefault = getDefault(session);

        // 如果不存在
        if (aDefault.getCode() == 0) {
            addressBook.setIsDefault(1);

            aDefault.setCode(1);
            aDefault.setData(addressBook);
            // 更新默认地址缓存
            Cache cache = cacheManager.getCache("addressBookCache");
            if (cache != null) {
                cache.put("default", R.success(addressBook));
            }
            // redisTemplate.opsForValue().set("addressBookCache::default", aDefault, 60, TimeUnit.MINUTES);
        }

        addressBookService.save(addressBook);

        // 删除list缓存
        // RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "addressBookCache::list");

        Cache cache = cacheManager.getCache("addressBookCache");
        if (cache != null) {
            // 增加地址缓存
            // cache.put("detail_" + addressBook.getId(), addressBook);

            // 删除list缓存
            String key = "list_user_" + addressBook.getUserId();
            cache.evict(key);
        }


        return R.success(addressBook);
    }


    /**
     * TODO: 设置默认地址
     *     删除list缓存、删除所有detail缓存、删除默认地址缓存、
     *     增加新的缓存 (一份详细信息缓存、一份默认地址缓存)     无法set，只传了 id
     * @param addressBook {@link AddressBook}
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "addressBookCache", key = "'list_user_' + #session.getAttribute('user').toString()"),
                    @CacheEvict(value = "addressBookCache", key = "'default'")
            }
    )
    @PutMapping("/default")
    @ApiOperation(value = "设置默认地址接口")
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook, HttpSession session) {
        log.info("AddressBook.default 设置默认地址 AddressBook={} ...",addressBook);
        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();
        // queryWrapper.eq(AddressBook::getUserId, Long.parseLong(session.getAttribute("user").toString()) );
        updateWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        updateWrapper.set(AddressBook::getIsDefault, 0);
        // 先将 user_id 的所有地址 的 is_default 改为0
        addressBookService.update(addressBook, updateWrapper);

        // 删除所有地址信息缓存
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "addressBookCache::detail");

        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }


    /**
     * TODO: 查询默认地址
     *     崽商品页面，点击去结算，到达add-order.html，如果没有默认地址，他会让你创建一个新地址
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @Cacheable(value = "addressBookCache", key = "'default'")
    @GetMapping("/default")
    @ApiOperation(value = "获取默认地址接口")
    public R<AddressBook> getDefault(HttpSession session) {
        log.info("查询默认地址 ...");

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        // queryWrapper.eq(AddressBook::getUserId, Long.parseLong(session.getAttribute("user").toString()) );
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null != addressBook) {
            return R.success(addressBook);
        }
        return R.error("没有查询到默认地址");
    }


    /**
     * TODO: 查询 指定用户所有地址信息
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @Cacheable(value = "addressBookCache", key = "'list_user_' + #session.getAttribute('user').toString()")
    @GetMapping("/list")
    @ApiOperation(value = "查询所有地址接口")
    public R<List<AddressBook>> list(AddressBook addressBook, HttpSession session) {
        log.info("查询 指定用户所有地址信息 ...");
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        // queryWrapper.eq(AddressBook::getUserId, Long.parseLong(session.getAttribute("user").toString()) );
        queryWrapper.eq(null != addressBook.getUserId(), AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        List<AddressBook> list = addressBookService.list(queryWrapper);

        return R.success(list);
    }


    /**
     * TODO: 根据id 查询地址详细信息
     *     妈的，我发现一个有毒的地方，每次点击编辑地址信息的按钮，他都会给我发一个设置默认地址的请求？？？
     *     再Address.html L42复制一行，减少很多，但是显示有点问题
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @Cacheable(value = "addressBookCache", key = "'detail_' + #id")
    @GetMapping("/{id}")
    @ApiOperation(value = "查询地址详情接口")
    /* @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "地址簿id", required = true),
            @ApiImplicitParam(name = "session",value = "session", required = false),
    }) */
    public R<AddressBook> getDetail(@PathVariable("id") Long id,HttpSession session) {
        log.info("根据id 查询地址详细信息 id={} ...",id);
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        // queryWrapper.eq(AddressBook::getUserId, Long.parseLong(session.getAttribute("user").toString()) );
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());

        AddressBook addressBook = addressBookService.getById(id);

        return R.success(addressBook);    // 这个 查询出的 标签回显有问题
    }


    /**
     * TODO: 更新地址详细信息
     *     删除list缓存，删除地址缓存，新增新的缓存
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "addressBookCache", key = "'list_user_' + #addressBook.userId"),
                    @CacheEvict(value = "addressBookCache", key = "'detail_' + #addressBook.id", beforeInvocation = true)},
            put = {@CachePut(value = "addressBookCache", key = "'detail_' + #addressBook.id")}
    )
    @PutMapping
    @ApiOperation(value = "更新地址接口")
    public R<AddressBook> update(@RequestBody AddressBook addressBook,HttpSession session) {
        log.info("更新地址详细信息 addressBook={} ...",addressBook);

        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }


    /**
     * TODO: 删除地址信息
     *     删除list缓存、地址缓存
     *     默认地址缓存（因为如果删除的是默认地址，所以需要删除默认地址缓存）
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "addressBookCache", key = "'detail_' + #ids", beforeInvocation = true),
                    @CacheEvict(value = "addressBookCache", key = "'default'"),}
    )
    @DeleteMapping
    @ApiOperation(value = "删除地址接口")
    /* @ApiImplicitParams({
            @ApiImplicitParam(name = "ids",value = "地址簿id", required = true),
            @ApiImplicitParam(name = "session",value = "session", required = false),
    }) */
    public R<String> delete(Long ids, HttpSession session) {
        log.info("删除单个地址信息 ids={} ...",ids);

        addressBookService.removeById(ids);

        // 删除list缓存
        RedisUtil.deleteKeysByPrefixAsync(redisTemplate, "addressBookCache::list");


        // 也可以在这里更新 默认地址的缓存【调用getDefault方法】

        return R.success("删除地址信息成功");
    }
}