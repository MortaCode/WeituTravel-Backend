package com.myy.weitutravel.user.controller;


import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.user.entity.User;
import com.myy.weitutravel.user.service.UserService;
import com.myy.weitutravel.user.vo.UserloginVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户登录
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("user")
public class UserController {

    private final UserService userService;

    /**
     * 登录
     * @param loginVo
     * @param request
     * @return
     */
    @PostMapping("login")
    public Result<String> login(@Validated @RequestBody UserloginVo loginVo, HttpServletRequest request){
        return Result.success(userService.login(loginVo, request));
    }

    /**
     * 获取当前登录对象
     * @param request
     * @return
     */
    @GetMapping("login/getCur")
    public Result<User> getLoginUser(HttpServletRequest request){
        return Result.success(userService.getLoginUser(request));
    }
}
