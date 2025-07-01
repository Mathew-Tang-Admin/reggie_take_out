package com.itheima.reggie.controller;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdcardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * @author MathewTang
 */
@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * TODO: 员工登陆
     *
     * @param request  {@link HttpServletRequest}
     * @param employee {@link Employee}
     * @return {@link R<Employee>}
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        // 1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(wrapper);

        // 3、如果没有查询到则返回登录失败结果
        if (emp == null) {
            return R.error("用户不存在，登录失败");
        }

        // 4、密码比对，如果不一致则返回登录失败结果
        if (!emp.getPassword().equals(password)) {
            return R.error("密码输入错误，登录失败");
        }

        // 5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }

        // 6、登录成功，将员工id存入Session并返回登录成功结果
        request.getSession().setAttribute("employee", emp.getId());

        return R.success(emp);
    }

    /**
     * TODO: 员工退出
     *
     * @param request {@link HttpServletRequest}
     * @return {@link R<String>}
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        // 清理Session中保存的当前登录员工的id

        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * TODO: 添加员工
     *
     * @param request {@link HttpServletRequest}
     * @return {@link R<String>}
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增员工，员工信息{}", employee.toString());
        /* // 1. 查询要添加的username是否已存在【唯一约束】
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(wrapper);

        if (emp != null) {
            return R.error("当前用户："+ employee.getUsername() +"已存在");
        } */

        /* // 测试Hutool工具之IdCardUtil工具
        boolean idCardValid = IdcardUtil.isValidCard(employee.getIdNumber());
        if (!idCardValid) {
            return R.error("身份证号："+ employee.getIdNumber() +"无效");
        }
        boolean mobileValid = Validator.isMobile(employee.getPhone());
        if (!mobileValid) {
            return R.error("手机号："+ employee.getPhone() +"无效");
        } */

        // 2. 新增用户
        // 设置初始密码123456，并进行md5加密处理
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        // employee.setCreateTime(LocalDateTime.now());
        // employee.setUpdateTime(LocalDateTime.now());

        // 获取当前登录用户的id
        Long empId = (Long) request.getSession().getAttribute("employee");
        // employee.setCreateUser(empId);
        // employee.setUpdateUser(empId);

        boolean save = employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * TODO: 员工信息分页查询
     *
     * @param page     {@link Integer}
     * @param pageSize {@link Integer}
     * @param name     {@link String}
     * @return {@link R<Page>}
     */
    @GetMapping("/page")
    // public R<Page> page(@RequestParam("page") Integer page,
    public R<Page> page(Integer page, Integer pageSize, String name) {
        log.info("page = {},pageSize = {},name = {}", page, pageSize, name);

        // 构造分页构造器
        Page pageInfo = new Page(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        // 添加一个过滤条件
        wrapper.like(StringUtils.isNotEmpty(name),Employee::getName, name);
        // 添加一个排序条件
        wrapper.orderByDesc(Employee::getUpdateTime);
        // 执行查询
        employeeService.page(pageInfo,wrapper);

        return R.success(pageInfo);
    }

    /**
     * TODO: 根据id修改员工信息 禁用启用员工账号
     * @return {@link R<String>}
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        long threadId = Thread.currentThread().getId();

        log.info(" 员工信息 线程Id：{}  employee:{}",threadId,employee);

        Long empId = (Long) request.getSession().getAttribute("employee");
        // employee.setUpdateTime(LocalDateTime.now());
        // employee.setUpdateUser(empId);
        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    /**
     * TODO: 根据id查询员工详细信息
     * @return {@link R<Employee>}
     */
    @GetMapping("/{id}")
    public R<Employee> employee(@PathVariable("id") Long id) {
        log.info("根据id查询员工详细信息...");
        Employee emp = employeeService.getById(id);
        if (emp != null) {
            return R.success(emp);
        }
        return R.error("没有查询到对应员工信息");
    }
}
