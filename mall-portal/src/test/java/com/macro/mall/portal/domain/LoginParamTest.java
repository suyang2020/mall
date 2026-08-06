package com.macro.mall.portal.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginParam 参数校验单元测试
 * Created on 2026/8/6.
 */
class LoginParamTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidLoginParam() {
        LoginParam param = new LoginParam();
        param.setUsername("test_user");
        param.setPassword("123456");

        Set<ConstraintViolation<LoginParam>> violations = validator.validate(param);
        assertTrue(violations.isEmpty(), "有效参数不应有校验错误");
    }

    @Test
    void testBlankUsername() {
        LoginParam param = new LoginParam();
        param.setUsername("");
        param.setPassword("123456");

        Set<ConstraintViolation<LoginParam>> violations = validator.validate(param);
        assertFalse(violations.isEmpty(), "空用户名应校验失败");
    }

    @Test
    void testBlankPassword() {
        LoginParam param = new LoginParam();
        param.setUsername("test_user");
        param.setPassword("");

        Set<ConstraintViolation<LoginParam>> violations = validator.validate(param);
        assertFalse(violations.isEmpty(), "空密码应校验失败");
    }

    @Test
    void testGettersAndSetters() {
        LoginParam param = new LoginParam();
        param.setUsername("admin");
        param.setPassword("admin123");

        assertEquals("admin", param.getUsername());
        assertEquals("admin123", param.getPassword());
    }
}
