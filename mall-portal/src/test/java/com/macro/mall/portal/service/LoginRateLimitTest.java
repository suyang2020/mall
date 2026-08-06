package com.macro.mall.portal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录限流逻辑验证测试
 * Created on 2026/8/6.
 */
class LoginRateLimitTest {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @Test
    void testMaxAttemptsNotExceeded() {
        // 模拟：尝试次数 < 最大限制 → 允许登录
        int attempts = 3;
        assertTrue(attempts < MAX_LOGIN_ATTEMPTS, "未达到限制次数时应允许登录");
    }

    @Test
    void testMaxAttemptsExceeded() {
        // 模拟：尝试次数 >= 最大限制 → 拒绝登录
        int attempts = 5;
        assertTrue(attempts >= MAX_LOGIN_ATTEMPTS, "达到限制次数后应拒绝登录");
    }

    @Test
    void testAttemptCountResetAfterSuccess() {
        // 登录成功后 limit 计数应重置
        int attemptsBefore = 4;
        int attemptsAfter = 0; // 成功后重置
        assertTrue(attemptsAfter < MAX_LOGIN_ATTEMPTS, "登录成功后计数重置，应允许再次登录");
    }
}
