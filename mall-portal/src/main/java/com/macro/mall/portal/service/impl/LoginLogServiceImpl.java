package com.macro.mall.portal.service.impl;

import com.macro.mall.portal.domain.LoginLog;
import com.macro.mall.portal.repository.LoginLogRepository;
import com.macro.mall.portal.service.LoginLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现
 * Created on 2026/8/6.
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginLogServiceImpl.class);

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Override
    public void save(LoginLog loginLog) {
        loginLogRepository.save(loginLog);
    }

    @Override
    public void logSuccess(Long memberId, String username, String ip, String loginType) {
        LoginLog log = new LoginLog(memberId, username, ip, loginType, true, null);
        loginLogRepository.save(log);
    }

    @Override
    public void logFailure(String username, String ip, String loginType, String failReason) {
        LoginLog log = new LoginLog(null, username, ip, loginType, false, failReason);
        loginLogRepository.save(log);
    }
}
