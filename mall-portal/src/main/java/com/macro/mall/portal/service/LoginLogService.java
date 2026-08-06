package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.LoginLog;

/**
 * 登录日志服务
 * Created on 2026/8/6.
 */
public interface LoginLogService {

    /**
     * 保存登录日志
     */
    void save(LoginLog loginLog);

    /**
     * 记录登录成功
     */
    void logSuccess(Long memberId, String username, String ip, String loginType);

    /**
     * 记录登录失败
     */
    void logFailure(String username, String ip, String loginType, String failReason);
}
