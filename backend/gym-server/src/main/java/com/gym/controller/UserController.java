package com.gym.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gym.AOP.RateLimit;
import com.gym.bloomFilter.BloomFilterUtil;
import com.gym.dto.*;
import com.gym.dto.redis.PendingPasswordReset;
import com.gym.entity.TrainerProfile;
import com.gym.service.AuthService;
import com.gym.service.TrainerProfileService;
import com.gym.service.impl.RedisCacheServiceImpl;
import com.gym.util.IpUtil;
import com.gym.util.SecurityUtils;
import com.gym.util.TencentCaptchaUtil;
import com.gym.vo.LoginResponse;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.result.RestResult;
import com.gym.service.MailService;
import com.gym.service.UserService;
import com.gym.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TencentCaptchaUtil tencentCaptchaUtil;

    // 用于生成腾讯机器人验证码
    private void validateCaptcha(String captchaTicket, String captchaRandstr, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        boolean captchaValid = tencentCaptchaUtil.verifyCaptcha(captchaTicket, captchaRandstr, clientIp);
        if (!captchaValid) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Captcha verification failed.");
        }
    }

    /**
     * 注册第一步：发送验证码邮件
     */
    @PostMapping("/signup")
    // 限流注解，60 秒内最多 5 次请求
    @RateLimit(timeWindowSeconds = 60, maxRequests = 5,
            message = "Too many signup requests. Please try again later.")
    public RestResult<?> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        // 先进行验证码校验
        // validateCaptcha(request.getCaptchaTicket(), request.getCaptchaRandstr(), httpRequest);

        log.info("signup request: {}", request);
        userService.sendSignupVerification(request);
        return RestResult.success(null, "Verification code has been sent to your email. Please enter it to complete registration.");
    }

    /**
     * 注册第二步：验证验证码并完成注册
     */
    @PostMapping("/verify-code")
    public RestResult<?> verifyCode(@Valid @RequestBody VerifyCodeRequest verifyReq) {
        userService.verifySignupCode(verifyReq);
        return RestResult.success(null, "Registration successful. Awaiting admin approval.");
    }

    /**
     * 登录接口（包含JWT认证）
     */
    @PostMapping("/login")
    public RestResult<LoginResponse> login(@Valid @RequestBody LoginRequest loginReq,
                                           HttpServletRequest httpRequest) {
        // 先进行验证码校验
        // validateCaptcha(loginReq.getCaptchaTicket(), loginReq.getCaptchaRandstr(), httpRequest);
        LoginResponse response = authService.login(loginReq);
        return RestResult.success(response, "Login success.");
    }

    /**
     * 忘记密码：发送重置链接
     */
    @PostMapping("/forgot-password")
    @RateLimit(timeWindowSeconds = 60, maxRequests = 5,
            message = "Too many reset password requests. Please try again later.")
    public RestResult<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                        HttpServletRequest httpRequest) {
        // 先进行验证码校验
        // validateCaptcha(request.getCaptchaTicket(), request.getCaptchaRandstr(), httpRequest);
        authService.forgotPassword(request);
        return RestResult.success(null, "A password reset link has been sent to your email.");
    }

    /**
     * 重置密码接口
     */
    @PostMapping("/reset-password")
    public RestResult<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return RestResult.success(null, "Password reset successful. " +
                "Please log in with your new password.");
    }

    /**
     * 更新用户个人资料
     */
    @PutMapping("/user-profile")
    public RestResult<?> updateUserProfile(@Valid @RequestBody UserProfileDTO request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated or session is invalid.");
        }
        userService.updateUserProfile(currentUserId, request);
        return RestResult.success("Updated", "User profile updated successfully.");
    }
}
