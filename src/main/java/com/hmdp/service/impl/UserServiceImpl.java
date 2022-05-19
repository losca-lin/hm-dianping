package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误！");
        }
        //校验验证码
        String code = (String) session.getAttribute("code");
        String formCode = loginForm.getCode();
        if (code == null || !code.equals(formCode)){
            return Result.fail("验证码错误！");
        }
        //通过查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("phone",phone);
        User user = this.getOne(wrapper);
        //判断用户是否存在
        //不存在创建用户保存
        if(user == null){
            createUserWithPhone(phone);
        }
        //存在保持到session里面
        session.setAttribute("user",user);
        return Result.ok();
    }

    private void createUserWithPhone(String phone) {
        User user = new User();
        String user_nick = SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10);
        user.setPhone(phone);
        user.setNickName(user_nick);
        this.save(user);
    }
}
