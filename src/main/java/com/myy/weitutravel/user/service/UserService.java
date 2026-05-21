package com.myy.weitutravel.user.service;

import com.myy.weitutravel.common.constants.Constants;
import com.myy.weitutravel.common.exception.BizException;
import com.myy.weitutravel.user.entity.User;
import com.myy.weitutravel.user.mapper.UserMapper;
import com.myy.weitutravel.user.vo.UserloginVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@AllArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 登录
     * @param loginVo
     * @return
     */
    public String login(UserloginVo loginVo, HttpServletRequest request) {
        User user = userMapper.selectById(loginVo.getUserId());
        Assert.notNull(user,"未获取到用户");
        request.getSession().setAttribute(Constants.USER_LOGIN, user);
        return "登录成功";
    }

    /**
     * 获取当前登录对象
     * @param request
     * @return
     */
    public User getLoginUser(HttpServletRequest request) {
        User user = (User)request.getSession().getAttribute(Constants.USER_LOGIN);
        //Assert.notNull(user, "请登录");
        if(user == null){
            throw new BizException("请登录");
        }
        return user;
    }
}
