package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @author MathewTang
 * @date 2025/06/28 21:37
 */
@Slf4j
@RequestMapping("/addressBook")
@RestController
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    @PostMapping
    public R<AddressBook> save(@RequestBody AddressBook addressBook, HttpSession session) {
        log.info("AddressBook.save AddressBook={} ...",addressBook);
        // addressBook.setUserId(Long.parseLong(session.getAttribute("user").toString()));
        addressBook.setUserId(BaseContext.getCurrentId());

        addressBookService.save(addressBook);
        return R.success(addressBook);
    }


    /**
     * TODO: 设置默认地址
     *
     * @param addressBook {@link AddressBook}
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @PutMapping("/default")
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook, HttpSession session) {
        log.info("AddressBook.default 设置默认地址 AddressBook={} ...",addressBook);
        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();
        // queryWrapper.eq(AddressBook::getUserId, Long.parseLong(session.getAttribute("user").toString()) );
        updateWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        updateWrapper.set(AddressBook::getIsDefault, 0);
        // 先将 user_id 的所有地址 的 is_default 改为0
        addressBookService.update(addressBook, updateWrapper);

        addressBook.setIsDefault(1);
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }


    /**
     * TODO: 查询默认地址
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @GetMapping("/default")
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
    @GetMapping("/list")
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
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @GetMapping("/{id}")
    public R<AddressBook> list(@PathVariable("id") Long id,HttpSession session) {
        log.info("根据id 查询地址详细信息 id={} ...",id);
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        // queryWrapper.eq(AddressBook::getUserId, Long.parseLong(session.getAttribute("user").toString()) );
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());

        AddressBook addressBook = addressBookService.getById(id);

        return R.success(addressBook);    // 这个 查询出的 标签回显有问题
    }


    /**
     * TODO: 更新地址详细信息
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @PutMapping
    public R<AddressBook> update(@RequestBody AddressBook addressBook,HttpSession session) {
        log.info("更新地址详细信息 addressBook={} ...",addressBook);

        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }


    /**
     * TODO: 删除地址信息
     *
     * @param session {@link HttpSession}
     * @return {@link R<AddressBook>}
     */
    @DeleteMapping
    public R<String> delete(Long ids, HttpSession session) {
        log.info("更新地址详细信息 ids={} ...",ids);

        addressBookService.removeById(ids);
        return R.success("删除地址信息成功");
    }
}
