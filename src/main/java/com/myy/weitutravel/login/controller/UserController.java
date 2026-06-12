package com.myy.weitutravel.login.controller;

import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.login.entity.User;
import com.myy.weitutravel.login.service.UserService;
import com.myy.weitutravel.login.sms.SmsService;
import com.myy.weitutravel.login.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("login")
public class UserController {

    private final UserService userService;
    private final SmsService smsService;

    /**
     * 微信登录
     * @param vo
     * @param request
     * @return
     */

    @PostMapping("wechat")
    public Result<LoginResultVo> wechatLogin(@Validated @RequestBody WechatLoginVo vo,
                                              HttpServletRequest request) {
        return Result.success(userService.wechatLogin(vo.getCode(), request));
    }

    /**
     * 微信授权后的回调地址（需配置到微信开放平台）。
     * 后端用 code 换 openid，判断绑定状态，然后 302 重定向回前端页面。
     */
    @GetMapping("wechat/callback")
    public void wechatCallback(@RequestParam String code, @RequestParam String state,
                                HttpServletRequest request, HttpServletResponse response) throws IOException {
        String targetUrl = userService.handleWechatCallback(code, state, request);
        response.sendRedirect(targetUrl);
    }

    /**
     * 电话绑定
     * @param vo
     * @param request
     * @return
     */

    @PostMapping("bind/wechat")
    public Result<LoginResultVo> wechatPhoneBind(@Validated @RequestBody WechatPhoneBindVo vo,
                                                  HttpServletRequest request) {
        return Result.success(userService.wechatPhoneBind(vo.getBindToken(), vo.getMobile(), vo.getSmsCode(), request));
    }


    /**
     * 发送短信
     * @param vo
     * @return
     */
    @PostMapping("sms/send")
    public Result<String> sendSms(@Validated @RequestBody SmsSendVo vo) {
        smsService.sendCode(vo.getMobile());
        return Result.success("发送成功");
    }

    /**
     * 手机号登录
     * @param vo
     * @param request
     * @return
     */
    @PostMapping("phone")
    public Result<LoginResultVo> phoneLogin(@Validated @RequestBody PhoneLoginVo vo,
                                            HttpServletRequest request) {
        return Result.success(userService.phoneLogin(vo.getMobile(), vo.getSmsCode(), request));
    }

    /**
     * 当前登录用户信息
     * @param request
     * @return
     */
    @GetMapping("getCur")
    public Result<User> getLoginUser(HttpServletRequest request) {
        return Result.success(userService.getLoginUser(request));
    }
}
