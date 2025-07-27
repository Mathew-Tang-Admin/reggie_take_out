package com.itheima.reggie.controller;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdcardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author MathewTang
 */
@Slf4j
@RestController
@RequestMapping("/employee")
@Api(tags = "员工接口")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * TODO: 员工登陆
     *
     * @param request  {@link HttpServletRequest}
     * @param employee {@link Employee}
     * @return {@link R<Employee>}
     */
    @PostMapping("/login")
    @ApiOperation(value = "员工登录接口")
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
    @ApiOperation(value = "员工退出接口")
    public R<String> logout(HttpServletRequest request) {
        // 清理Session中保存的当前登录员工的id

        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * TODO: 添加员工
     *     删除分页缓存、增加新的缓存          如果为了实现更加简单可以只是用 id 作为key
     *     突然发现这里使用 @CachePut注解 即可  【脑壳抽了😂】
     * @param request {@link HttpServletRequest}
     * @return {@link R<String>}
     */
    @PostMapping
    @ApiOperation(value = "新增员工接口")
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

        System.out.println("employee.getId() = " + employee.getId());

        // 删除分页缓存
        String prefix= "employeeCache::";
        Set<Object> pageKeys = redisTemplate.keys(prefix + "page*");
        if (pageKeys != null) {
            for (Object pageKey : pageKeys) {
                redisTemplate.delete(pageKey);
            }
        }
        // 新增员工缓存
        redisTemplate.opsForValue().set(prefix + "detail_" + employee.getId(), employee, 60, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(prefix + "detail_" + employee.getUsername(), employee, 60, TimeUnit.MINUTES);

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
    // public R<Page> page(@RequestParam("page") Integer page,
    @Cacheable(value = "employeeCache", key = "'page_' + #page + '_' + #pageSize + '_' + #name")
    @GetMapping("/page")
    @ApiOperation(value = "员工分页接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "页码", required = true),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true),
            @ApiImplicitParam(name = "name", value = "员工姓名", required = false),
    })
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
     *     删除分页缓存，删除账户缓存、新增缓存
     *     为了解决修改了 username 导致旧的缓存未删除，只能在更新前执行查询    或者为了方便 也可以删除全部缓存
     * @return {@link R<String>}
     */
    @Caching(
            evict = {
                    @CacheEvict(value = "employeeCache", key = "'detail_' + #employee.username", beforeInvocation = true),
                    @CacheEvict(value = "employeeCache", key = "'detail_' + #employee.id", beforeInvocation = true)
            }
    )
    @PutMapping
    @ApiOperation(value = "更新员工接口")
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        long threadId = Thread.currentThread().getId();

        log.info(" 员工信息 线程Id：{}  employee:{}",threadId,employee);

        // 查询执行更新前的username，通过username查询缓存，如果未改，这里应该是没有查到缓存    或者禁用启用账号时也需要
        Employee employeeById = employeeService.getById(employee.getId());
        String usernameKey = "employeeCache::detail_" + employeeById.getUsername();
        Employee employeeRedis = (Employee) redisTemplate.opsForValue().get(usernameKey);
        if (null != employeeRedis) {
            redisTemplate.delete(usernameKey);                 // 或者为了方便也可以直接只用id作为key    这里蛮保存吧...
        }

        Long empId = (Long) request.getSession().getAttribute("employee");
        // employee.setUpdateTime(LocalDateTime.now());
        // employee.setUpdateUser(empId);
        employeeService.updateById(employee);

        String prefix = "employeeCache::";
        // 删除分页缓存
        Set<Object> pageKeys = redisTemplate.keys(prefix + "page*");
        if (pageKeys != null) {
            for (Object pageKey : pageKeys) {
                redisTemplate.delete(pageKey);
            }
        }
        // 新增员工缓存
        if (employee.getUsername() != null) {      // 不然会有   detail_null 或者其他值没有数据
            redisTemplate.opsForValue().set(prefix + "detail_" + employee.getUsername(), employee, 60, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(prefix + "detail_" + employee.getId(), employee, 60, TimeUnit.MINUTES);
        }

        return R.success("员工信息修改成功");
    }

    /**
     * TODO: 根据id查询员工详细信息
     *     通过查询资料得，再增加一个key为 id 的缓存更高效
     *     由于前面员工数据都是 未包装 的，这个也应该是为包装的吧？？
     * @return {@link R<Employee>}
     */
    // @Cacheable(value = "cacheManager", key = "'detail_' + #id")       // 为了统一结构，只能手动实现
    @GetMapping("/{id}")
    @ApiOperation(value = "查询员工详情接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "员工id", required = true),
    })
    public R<Employee> getDetail(@PathVariable("id") Long id) {
        log.info("根据id查询员工详细信息...");

        // 获取缓存数据
        String prefix = "employeeCache::";
        /* Set<Object> empKeys = redisTemplate.keys(prefix + "detail_*");
        if (empKeys != null) {
            for (Object empKey : empKeys) {
                Employee employee = (Employee) redisTemplate.opsForValue().get(empKey);
                if (employee != null && id.equals(employee.getId())) {
                    return R.success(employee);
                }
            }
        } */
        Cache cache = cacheManager.getCache("employeeCache");
        Employee employee = null;
        if (cache != null) {
            employee = cache.get("detail_" + id, Employee.class);
            if (null != employee) {
                return R.success(employee);
            }
        }

        // 缓存中没有数据
        employee = employeeService.getById(id);
        if (employee != null) {
            // 新增员工缓存
            // redisTemplate.opsForValue().set(prefix + "detail_" + employee.getUsername(), employee, 60, TimeUnit.MINUTES);

            redisTemplate.opsForValue().set(prefix + "detail_" + employee.getUsername(), employee, 60, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(prefix + "detail_" + employee.getId(), employee, 60, TimeUnit.MINUTES);
            return R.success(employee);
        }
        return R.error("没有查询到对应员工信息");
    }
}
