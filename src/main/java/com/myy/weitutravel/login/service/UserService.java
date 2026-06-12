package com.myy.weitutravel.login.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myy.weitutravel.common.constants.Constants;
import com.myy.weitutravel.common.exception.BizException;
import com.myy.weitutravel.login.entity.User;
import com.myy.weitutravel.login.entity.UserWechat;
import com.myy.weitutravel.login.mapper.UserMapper;
import com.myy.weitutravel.login.mapper.UserWechatMapper;
import com.myy.weitutravel.login.sms.SmsService;
import com.myy.weitutravel.login.wechat.WechatService;
import com.myy.weitutravel.login.wechat.WechatService.BindInfo;
import com.myy.weitutravel.login.wechat.WechatService.WechatSession;
import com.myy.weitutravel.login.vo.LoginResultVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserWechatMapper userWechatMapper;
    private final SmsService smsService;
    private final WechatService wechatService;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");


    /**
     * 手机号登录
     * @param mobile
     * @param smsCode
     * @param request
     * @return
     */

    public LoginResultVo phoneLogin(String mobile, String smsCode, HttpServletRequest request) {
        if (!PHONE_PATTERN.matcher(mobile).matches()) {
            throw new BizException("手机号格式不正确");
        }
        if (!smsService.verifyCode(mobile, smsCode)) {
            throw new BizException("验证码错误或已过期");
        }

        User user = findOrCreateByMobile(mobile);
        setSession(user, request);
        log.info("手机号登录成功: userId={}, mobile={}", user.getId(), mobile);
        return new LoginResultVo(false, null, user, null);
    }


    /**
     * 微信登录
     * @param code
     * @param request
     * @return
     */
    public LoginResultVo wechatLogin(String code, HttpServletRequest request) {
        WechatSession session;
        try {
            session = wechatService.code2session(code);//授权码 to Session
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信 code2session 异常", e);
            throw new BizException("微信登录失败，请稍后重试");
        }

        UserWechat wechat = userWechatMapper.selectOne(
                new LambdaQueryWrapper<UserWechat>().eq(UserWechat::getOpenid, session.openid()));

        if (wechat != null) {
            User user = userMapper.selectById(wechat.getUserId());
            if (user == null) {
                log.error("微信绑定记录存在但用户不存在: userId={}, openid={}", wechat.getUserId(), session.openid());
                throw new BizException("账号数据异常，请联系客服");
            }
            setSession(user, request);
            log.info("微信登录成功(已绑定): userId={}, openid={}", user.getId(), session.openid());
            return new LoginResultVo(false, null, user, null);
        }

        String bindToken = wechatService.createBindToken(session.openid(), session.unionid(), session.sessionKey());
        log.info("微信首次登录, 需绑定手机号: openid={}", session.openid());
        return new LoginResultVo(true, bindToken, null, null);
    }


    /**
     * 微信网页授权回调处理。
     * 微信重定向回后端后，换取 openid，判断是否已绑定，
     * 返回组装好的前端重定向 URL（含 status 参数）。
     */
    public String handleWechatCallback(String code, String state, HttpServletRequest request) {
        String redirectUrl = wechatService.consumeState(state);
        if (redirectUrl == null) {
            return "/login/error?message=" + urlEncode("授权会话已过期，请重新登录");
        }

        try {
            WechatSession session = wechatService.exchangeCodeForOpenid(code);

            UserWechat wechat = userWechatMapper.selectOne(
                    new LambdaQueryWrapper<UserWechat>().eq(UserWechat::getOpenid, session.openid()));

            if (wechat != null) {
                User user = userMapper.selectById(wechat.getUserId());
                if (user == null) {
                    log.error("微信绑定记录存在但用户不存在: userId={}, openid={}", wechat.getUserId(), session.openid());
                    return redirectUrl + "?status=error&message=" + urlEncode("账号数据异常");
                }
                setSession(user, request);
                log.info("微信网页登录成功(已绑定): userId={}, openid={}", user.getId(), session.openid());
                return redirectUrl + "?status=success";
            }

            String bindToken = wechatService.createBindToken(session.openid(), session.unionid(), session.sessionKey());
            log.info("微信网页首次登录, 需绑定手机号: openid={}", session.openid());
            return redirectUrl + "?status=bind&bindToken=" + bindToken;

        } catch (BizException e) {
            return redirectUrl + "?status=error&message=" + urlEncode(e.getMessage());
        } catch (Exception e) {
            log.error("微信网页回调异常", e);
            return redirectUrl + "?status=error&message=" + urlEncode("微信登录失败，请稍后重试");
        }
    }

    /**
     * 微信绑定手机号
     * @param bindToken
     * @param mobile
     * @param smsCode
     * @param request
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVo wechatPhoneBind(String bindToken, String mobile, String smsCode, HttpServletRequest request) {
        if (!PHONE_PATTERN.matcher(mobile).matches()) {
            throw new BizException("手机号格式不正确");
        }
        if (!smsService.verifyCode(mobile, smsCode)) {
            throw new BizException("验证码错误或已过期");
        }

        BindInfo bindInfo = wechatService.consumeBindToken(bindToken);

        Long count = userWechatMapper.selectCount(
                new LambdaQueryWrapper<UserWechat>().eq(UserWechat::getOpenid, bindInfo.getOpenid()));
        if (count > 0) {
            throw new BizException("该微信已绑定其他账号");
        }

        User user = findOrCreateByMobile(mobile);

        UserWechat wechat = new UserWechat();
        wechat.setId(IdUtil.fastSimpleUUID());
        wechat.setUserId(user.getId());
        wechat.setOpenid(bindInfo.getOpenid());
        wechat.setUnionid(bindInfo.getUnionid());
        userWechatMapper.insert(wechat);

        setSession(user, request);
        log.info("微信绑定手机号登录成功: userId={}, mobile={}, openid={}", user.getId(), mobile, bindInfo.getOpenid());
        return new LoginResultVo(false, null, user, null);
    }


    /**
     * 公共方法
     * @param mobile
     * @return
     */
    private User findOrCreateByMobile(String mobile) {
        User user = userMapper.selectByMobile(mobile);
        if (user == null) {
            user = new User();
            user.setId(IdUtil.fastSimpleUUID());
            user.setMobile(mobile);
            user.setNickname("用户" + mobile.substring(mobile.length() - 4));
            userMapper.insert(user);
            log.info("创建新用户: userId={}, mobile={}", user.getId(), mobile);
        }
        return user;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void setSession(User user, HttpServletRequest request) {
        request.getSession().setAttribute(Constants.USER_LOGIN, user);
    }

    public User getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(Constants.USER_LOGIN);
        if (user == null) {
            throw new BizException("请登录");
        }
        return user;
    }
}
