package com.macro.mall.portal.repository;

import com.macro.mall.portal.domain.LoginLog;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 登录日志仓库
 * Created on 2026/8/6.
 */
public interface LoginLogRepository extends MongoRepository<LoginLog, String> {
}
