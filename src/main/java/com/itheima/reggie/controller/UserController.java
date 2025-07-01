package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.buf.UDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author MathewTang
 * @date 2025/06/27 1:41
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * TODO: 发送手机短信验证码
     *
     * @param user {@link User}
     * @return {@link R<String>}
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        // 目前个人无法开启 阿里云 短信发送服务

        // 获取手机号
        String phone = user.getPhone();

        if (StringUtils.isNotEmpty(phone)) {
            // 生成随机四位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("移动端手机发送短信，手机号={}, code={}...", phone, code);

            // 调用阿里云提供的短信服务API完成
            // SMSUtils.sendMessage("瑞吉外卖", "", phone, code);

            // 将生成的验证码保存到Session
            session.setAttribute(phone, code);
            return R.success("手机验证码短信发送成功");
        }
        return R.error("手机验证码短信发送失败");


    }

    @PostMapping("/login")
    // public R<String> login(@RequestBody User user, HttpSession session) {
    public R<User> login(@RequestBody Map<String, String> map, HttpSession session) {
        // log.info("移动端手机登录，手机号={}, code={}...", user.getPhone(), user.getCode());
        // String sessionCode = (String) session.getAttribute(user.getPhone());


        // 获取手机号
        String phone = map.get("phone");
        // 获取验证码
        String code = map.get("code");
        log.info("移动端手机登录，手机号={}, code={}...", phone, code);
        // 从session中获取保存的验证码
        String sessionCode = session.getAttribute(phone).toString();

        // 对比两个验证码是否一致
        if (sessionCode != null && sessionCode.equals(code)) {
            // 查询手机号是否存储到user表
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            return R.success(user);
        }

        return R.error("短信发送失败");
    }

    @PostMapping("/loginout")
    public R<String> loginout(HttpSession session) {
        // 清理Session中保存的当前登录用户的id

        session.removeAttribute("user");
        return R.success("退出成功");
    }
}
