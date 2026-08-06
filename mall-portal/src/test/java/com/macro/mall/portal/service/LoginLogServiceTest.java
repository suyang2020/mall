package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.LoginLog;
import com.macro.mall.portal.repository.LoginLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 登录日志服务单元测试
 * Created on 2026/8/6.
 */
@ExtendWith(MockitoExtension.class)
class LoginLogServiceTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    @InjectMocks
    private com.macro.mall.portal.service.impl.LoginLogServiceImpl loginLogService;

    @Test
    void testLogSuccess() {
        // Given
        Long memberId = 1L;
        String username = "test_user";
        String ip = "192.168.1.100";

        // When
        loginLogService.logSuccess(memberId, username, ip, "PASSWORD");

        // Then
        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository, times(1)).save(captor.capture());
        LoginLog saved = captor.getValue();

        assertEquals(memberId, saved.getMemberId());
        assertEquals(username, saved.getUsername());
        assertEquals(ip, saved.getIp());
        assertEquals("PASSWORD", saved.getLoginType());
        assertTrue(saved.isSuccess());
        assertNull(saved.getFailReason());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    void testLogFailure() {
        // Given
        String username = "test_user";
        String ip = "10.0.0.1";
        String failReason = "密码不正确";

        // When
        loginLogService.logFailure(username, ip, "PASSWORD", failReason);

        // Then
        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository, times(1)).save(captor.capture());
        LoginLog saved = captor.getValue();

        assertNull(saved.getMemberId());
        assertEquals(username, saved.getUsername());
        assertEquals(ip, saved.getIp());
        assertFalse(saved.isSuccess());
        assertEquals(failReason, saved.getFailReason());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    void testSave() {
        // Given
        LoginLog log = new LoginLog(2L, "user2", "127.0.0.1", "PHONE_CODE", true, null);

        // When
        loginLogService.save(log);

        // Then
        verify(loginLogRepository, times(1)).save(log);
    }
}
